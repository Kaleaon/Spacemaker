package com.kaleaon.spacemaker

import android.content.Context
import android.os.Environment
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Exporter for PGSR (Planar-based Gaussian Splatting Reconstruction)
 * Optimized for indoor scenes with flat surfaces like floors, walls, and ceilings
 */
class PGSRExporter(private val context: Context) {
    
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    
    /**
     * Export AR data with plane information for PGSR processing
     */
    fun exportPlanarData(
        frames: List<ARFrame>,
        pointCloud: List<FloatArray>,
        detectedPlanes: List<DetectedPlane>,
        scanName: String = "pgsr_scan_${dateFormat.format(Date())}"
    ): File? {
        try {
            val baseDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                scanName
            )
            
            if (!baseDir.exists()) {
                baseDir.mkdirs()
            }
            
            // Create PGSR-compatible directory structure
            val inputDir = File(baseDir, "input")
            inputDir.mkdirs()
            
            // Export images (PGSR uses "input" directory)
            exportImages(frames, inputDir)
            
            // Export camera data in COLMAP format (required by PGSR)
            exportCOLMAPData(frames, baseDir)
            
            // Export plane information
            exportPlaneData(detectedPlanes, baseDir)
            
            // Export point cloud with plane assignments
            exportPointCloudWithPlanes(pointCloud, detectedPlanes, baseDir)
            
            // Export PGSR-specific metadata
            exportPGSRMetadata(frames, pointCloud, detectedPlanes, baseDir)
            
            return baseDir
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    private fun exportImages(frames: List<ARFrame>, inputDir: File) {
        frames.forEachIndexed { index, frame ->
            frame.image?.let { bitmap ->
                val imageFile = File(inputDir, String.format("%04d.jpg", index))
                java.io.FileOutputStream(imageFile).use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, out)
                }
            }
        }
    }
    
    private fun exportCOLMAPData(frames: List<ARFrame>, baseDir: File) {
        if (frames.isEmpty()) return
        
        val sparseDir = File(baseDir, "sparse/0")
        sparseDir.mkdirs()
        
        // cameras.txt
        exportCOLMAPCameras(frames, sparseDir)
        
        // images.txt
        exportCOLMAPImages(frames, sparseDir)
        
        // points3D.txt (initial point cloud - optional)
        File(sparseDir, "points3D.txt").writeText("# 3D point list\n")
    }
    
    private fun exportCOLMAPCameras(frames: List<ARFrame>, sparseDir: File) {
        val frame = frames[0]
        FileWriter(File(sparseDir, "cameras.txt")).use { writer ->
            writer.write("# Camera list with one line of data per camera:\n")
            writer.write("#   CAMERA_ID, MODEL, WIDTH, HEIGHT, PARAMS[]\n")
            writer.write("# Number of cameras: 1\n")
            writer.write("1 PINHOLE ${frame.width} ${frame.height} ")
            writer.write("${frame.cameraIntrinsics[0]} ${frame.cameraIntrinsics[1]} ")
            writer.write("${frame.cameraIntrinsics[2]} ${frame.cameraIntrinsics[3]}\n")
        }
    }
    
    private fun exportCOLMAPImages(frames: List<ARFrame>, sparseDir: File) {
        FileWriter(File(sparseDir, "images.txt")).use { writer ->
            writer.write("# Image list with two lines of data per image:\n")
            writer.write("#   IMAGE_ID, QW, QX, QY, QZ, TX, TY, TZ, CAMERA_ID, NAME\n")
            writer.write("#   POINTS2D[] as (X, Y, POINT3D_ID)\n")
            writer.write("# Number of images: ${frames.size}\n")
            
            frames.forEachIndexed { index, frame ->
                val quat = poseToQuaternion(frame.cameraPose)
                val trans = floatArrayOf(
                    frame.cameraPose[3],
                    frame.cameraPose[7],
                    frame.cameraPose[11]
                )
                
                writer.write("${index + 1} ${quat[0]} ${quat[1]} ${quat[2]} ${quat[3]} ")
                writer.write("${trans[0]} ${trans[1]} ${trans[2]} 1 ")
                writer.write("input/${String.format("%04d", index)}.jpg\n")
                writer.write("\n")
            }
        }
    }
    
    private fun exportPlaneData(planes: List<DetectedPlane>, baseDir: File) {
        val planesJson = JSONArray()
        
        for (plane in planes) {
            val planeObj = JSONObject()
            planeObj.put("id", plane.id)
            planeObj.put("type", plane.planeType.name)
            planeObj.put("extent_x", plane.extentX)
            planeObj.put("extent_z", plane.extentZ)
            
            // Normal vector
            val normalArray = JSONArray()
            plane.normalVector.forEach { normalArray.put(it) }
            planeObj.put("normal", normalArray)
            
            // Plane equation (ax + by + cz + d = 0)
            val equation = PlaneProcessor.getPlaneEquation(plane)
            val eqArray = JSONArray()
            equation.forEach { eqArray.put(it) }
            planeObj.put("equation", eqArray)
            
            // Center pose
            val poseArray = JSONArray()
            plane.centerPose.forEach { poseArray.put(it) }
            planeObj.put("center_pose", poseArray)
            
            // Polygon boundary
            val polygonArray = JSONArray()
            plane.polygon.forEach { point ->
                val pointArray = JSONArray()
                point.forEach { pointArray.put(it) }
                polygonArray.put(pointArray)
            }
            planeObj.put("polygon", polygonArray)
            
            planesJson.put(planeObj)
        }
        
        val planesData = JSONObject()
        planesData.put("planes", planesJson)
        planesData.put("num_planes", planes.size)
        
        // Count by type
        val typeCount = JSONObject()
        planes.groupBy { it.planeType }.forEach { (type, list) ->
            typeCount.put(type.name, list.size)
        }
        planesData.put("plane_counts", typeCount)
        
        File(baseDir, "planes.json").writeText(planesData.toString(2))
    }
    
    private fun exportPointCloudWithPlanes(
        points: List<FloatArray>,
        planes: List<DetectedPlane>,
        baseDir: File
    ) {
        // Assign points to planes
        val assignments = PlaneProcessor.assignPointsToPlanes(points, planes)
        
        // Export full point cloud
        val allPointsFile = File(baseDir, "points3d.ply")
        FileWriter(allPointsFile).use { writer ->
            writer.write("ply\n")
            writer.write("format ascii 1.0\n")
            writer.write("element vertex ${points.size}\n")
            writer.write("property float x\n")
            writer.write("property float y\n")
            writer.write("property float z\n")
            writer.write("property uchar red\n")
            writer.write("property uchar green\n")
            writer.write("property uchar blue\n")
            writer.write("end_header\n")
            
            for (point in points) {
                // Default color: white
                writer.write("${point[0]} ${point[1]} ${point[2]} 255 255 255\n")
            }
        }
        
        // Export per-plane point clouds
        val planesDir = File(baseDir, "plane_points")
        planesDir.mkdirs()
        
        assignments.forEach { (planeId, planePoints) ->
            if (planePoints.isNotEmpty() && planeId != "unassigned") {
                val planeFile = File(planesDir, "plane_${planeId}.xyz")
                FileWriter(planeFile).use { writer ->
                    planePoints.forEach { point ->
                        writer.write("${point[0]} ${point[1]} ${point[2]}\n")
                    }
                }
            }
        }
        
        // Export assignment information
        val assignmentJson = JSONObject()
        assignments.forEach { (planeId, planePoints) ->
            assignmentJson.put(planeId, planePoints.size)
        }
        File(baseDir, "plane_assignments.json").writeText(assignmentJson.toString(2))
    }
    
    private fun exportPGSRMetadata(
        frames: List<ARFrame>,
        pointCloud: List<FloatArray>,
        planes: List<DetectedPlane>,
        baseDir: File
    ) {
        val metadata = JSONObject()
        metadata.put("scan_date", dateFormat.format(Date()))
        metadata.put("num_frames", frames.size)
        metadata.put("num_points", pointCloud.size)
        metadata.put("num_planes", planes.size)
        metadata.put("format", "PGSR")
        metadata.put("version", "1.0")
        
        if (frames.isNotEmpty()) {
            metadata.put("image_width", frames[0].width)
            metadata.put("image_height", frames[0].height)
        }
        
        // Plane statistics
        val planeStats = JSONObject()
        val planeGroups = planes.groupBy { it.planeType }
        planeGroups.forEach { (type, list) ->
            planeStats.put(type.name, list.size)
        }
        metadata.put("plane_statistics", planeStats)
        
        File(baseDir, "metadata.json").writeText(metadata.toString(2))
        
        // Create README with PGSR instructions
        createPGSRReadme(baseDir)
    }
    
    private fun createPGSRReadme(baseDir: File) {
        val readme = """
# PGSR-Compatible AR Scan Data

This directory contains AR scan data optimized for PGSR (Planar-based Gaussian Splatting Reconstruction).

## Why PGSR?

PGSR is specifically designed for scenes with planar surfaces like:
- **Floors** (horizontal upward-facing planes)
- **Walls** (vertical planes)
- **Ceilings** (horizontal downward-facing planes)

This makes it ideal for indoor environments like homes, offices, and buildings.

## Contents

- `input/` - Camera images in JPEG format
- `sparse/0/` - COLMAP-format camera data
  - `cameras.txt` - Camera intrinsics
  - `images.txt` - Camera poses
  - `points3D.txt` - Optional initial points
- `planes.json` - Detected planar surfaces with properties
- `points3d.ply` - Full point cloud
- `plane_points/` - Points assigned to each plane
- `plane_assignments.json` - Point-to-plane assignments
- `metadata.json` - Scan metadata

## Detected Planes

This scan detected ${baseDir.parentFile?.name ?: "N/A"} planar surfaces including:
- Floors (horizontal upward-facing)
- Walls (vertical)
- Ceilings (horizontal downward-facing)

## Processing with PGSR

### 1. Install PGSR

```bash
git clone git@github.com:zju3dv/PGSR.git
cd PGSR

conda create -n pgsr python=3.8
conda activate pgsr

pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu118
pip install -r requirements.txt
pip install submodules/diff-plane-rasterization
pip install submodules/simple-knn
```

### 2. Prepare Data

Copy this directory to PGSR's data folder:
```bash
cp -r <this_directory> PGSR/data/
```

### 3. Train PGSR Model

For indoor scenes (recommended settings):
```bash
python train.py -s data/<scan_name> -m output/<scan_name> \\
    --max_abs_split_points 0 \\
    --opacity_cull_threshold 0.05
```

Options explained:
- `--max_abs_split_points 0`: Prevents overfitting in weakly textured areas
- `--opacity_cull_threshold 0.05`: Reduces Gaussian count for efficiency

### 4. Render and Extract Mesh

```bash
python render.py -m output/<scan_name> \\
    --max_depth 10.0 \\
    --voxel_size 0.01 \\
    --use_depth_filter
```

Options:
- `--max_depth`: Maximum depth for reconstruction (adjust for room size)
- `--voxel_size`: Mesh resolution (smaller = higher detail)
- `--use_depth_filter`: Remove inaccurate depth points

## Alternative: Triangle Splatting

This data is also compatible with Triangle Splatting for non-planar details:

```bash
# Clone Triangle Splatting
git clone https://github.com/trianglesplatting/triangle-splatting --recursive
cd triangle-splatting

# Install
micromamba create -f requirements.yaml
bash compile.sh

# Train
python train.py -s <path_to_this_directory> -m <output> --eval
```

## Combining Both Methods

For best results on indoor scenes:
1. Use **PGSR** for planar surfaces (walls, floors, ceilings)
2. Use **Triangle Splatting** for non-planar elements (furniture, decorations)
3. Merge the results for complete reconstruction

## Quality Tips

- Ensure good lighting during scanning
- Move slowly and cover all angles
- Capture floor, walls, and ceiling thoroughly
- Scan from multiple heights for better coverage

## Data Format

- Images: JPEG at 95% quality
- Camera format: COLMAP (PINHOLE model)
- Point cloud: PLY (ASCII format)
- Planes: JSON with equations and boundaries

## Support

- PGSR: https://github.com/zju3dv/PGSR
- Triangle Splatting: https://github.com/trianglesplatting/triangle-splatting
- Spacemaker: https://github.com/Kaleaon/Spacemaker
        """.trimIndent()
        
        File(baseDir, "README.md").writeText(readme)
    }
    
    private fun poseToQuaternion(pose: FloatArray): FloatArray {
        val trace = pose[0] + pose[5] + pose[10]
        
        return if (trace > 0) {
            val s = Math.sqrt(trace + 1.0).toFloat() * 2
            floatArrayOf(
                0.25f * s,
                (pose[9] - pose[6]) / s,
                (pose[2] - pose[8]) / s,
                (pose[4] - pose[1]) / s
            )
        } else {
            floatArrayOf(1f, 0f, 0f, 0f)
        }
    }
}
