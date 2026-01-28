package com.kaleaon.spacemaker

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * High-fidelity data exporter for Triangle Splatting pipeline
 * Exports ARCore capture data in a format compatible with neural radiance field training
 */
class TriangleSplattingExporter(private val context: Context) {
    
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    
    /**
     * Export captured AR frames in Triangle Splatting compatible format
     * Creates a directory structure with images, camera poses, and point cloud data
     */
    fun exportHighFidelityData(
        frames: List<ARFrame>,
        pointCloud: List<FloatArray>,
        scanName: String = "scan_${dateFormat.format(Date())}"
    ): File? {
        try {
            val baseDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                scanName
            )
            
            if (!baseDir.exists()) {
                baseDir.mkdirs()
            }
            
            // Create subdirectories
            val imagesDir = File(baseDir, "images")
            val sparseCloudsDir = File(baseDir, "sparse")
            
            imagesDir.mkdirs()
            sparseCloudsDir.mkdirs()
            
            // Export camera frames and poses
            exportFrames(frames, imagesDir)
            
            // Export camera intrinsics and poses in COLMAP-like format
            exportCameraData(frames, baseDir)
            
            // Export point cloud
            exportPointCloud(pointCloud, sparseCloudsDir)
            
            // Export metadata
            exportMetadata(frames, pointCloud, baseDir)
            
            return baseDir
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    private fun exportFrames(frames: List<ARFrame>, imagesDir: File) {
        frames.forEachIndexed { index, frame ->
            frame.image?.let { bitmap ->
                val imageFile = File(imagesDir, String.format("frame_%04d.jpg", index))
                FileOutputStream(imageFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
            }
        }
    }
    
    private fun exportCameraData(frames: List<ARFrame>, baseDir: File) {
        // Export in a format similar to COLMAP/NeRF datasets
        val camerasJson = JSONArray()
        val transformsJson = JSONObject()
        val framesArray = JSONArray()
        
        frames.forEachIndexed { index, frame ->
            // Camera intrinsics (assuming same for all frames)
            if (index == 0) {
                val cameraObj = JSONObject()
                cameraObj.put("camera_id", 0)
                cameraObj.put("width", frame.width)
                cameraObj.put("height", frame.height)
                cameraObj.put("fx", frame.cameraIntrinsics[0])
                cameraObj.put("fy", frame.cameraIntrinsics[1])
                cameraObj.put("cx", frame.cameraIntrinsics[2])
                cameraObj.put("cy", frame.cameraIntrinsics[3])
                cameraObj.put("model", "PINHOLE")
                camerasJson.put(cameraObj)
            }
            
            // Camera pose (extrinsics)
            val frameObj = JSONObject()
            frameObj.put("file_path", "images/frame_${String.format("%04d", index)}.jpg")
            frameObj.put("timestamp", frame.timestamp)
            
            // Convert 4x4 transformation matrix to JSON
            val transformMatrix = JSONArray()
            for (i in 0 until 4) {
                val row = JSONArray()
                for (j in 0 until 4) {
                    row.put(frame.cameraPose[i * 4 + j])
                }
                transformMatrix.put(row)
            }
            frameObj.put("transform_matrix", transformMatrix)
            
            framesArray.put(frameObj)
        }
        
        transformsJson.put("camera_angle_x", calculateFOV(frames[0].cameraIntrinsics[0], frames[0].width))
        transformsJson.put("frames", framesArray)
        
        // Write cameras.json
        File(baseDir, "cameras.json").writeText(camerasJson.toString(2))
        
        // Write transforms.json (NeRF format)
        File(baseDir, "transforms.json").writeText(transformsJson.toString(2))
        
        // Write cameras.txt (COLMAP format)
        writeCOLMAPCameras(frames, baseDir)
        
        // Write images.txt (COLMAP format)
        writeCOLMAPImages(frames, baseDir)
    }
    
    private fun writeCOLMAPCameras(frames: List<ARFrame>, baseDir: File) {
        if (frames.isEmpty()) return
        
        val frame = frames[0]
        FileWriter(File(baseDir, "cameras.txt")).use { writer ->
            writer.write("# Camera list with one line of data per camera:\n")
            writer.write("#   CAMERA_ID, MODEL, WIDTH, HEIGHT, PARAMS[]\n")
            writer.write("# Number of cameras: 1\n")
            writer.write("1 PINHOLE ${frame.width} ${frame.height} ")
            writer.write("${frame.cameraIntrinsics[0]} ${frame.cameraIntrinsics[1]} ")
            writer.write("${frame.cameraIntrinsics[2]} ${frame.cameraIntrinsics[3]}\n")
        }
    }
    
    private fun writeCOLMAPImages(frames: List<ARFrame>, baseDir: File) {
        FileWriter(File(baseDir, "images.txt")).use { writer ->
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
                writer.write("images/frame_${String.format("%04d", index)}.jpg\n")
                writer.write("\n") // Empty line for POINTS2D
            }
        }
    }
    
    private fun exportPointCloud(pointCloud: List<FloatArray>, sparseCloudsDir: File) {
        // Export as PLY format (preferred by Triangle Splatting)
        val plyFile = File(sparseCloudsDir, "points.ply")
        FileWriter(plyFile).use { writer ->
            writer.write("ply\n")
            writer.write("format ascii 1.0\n")
            writer.write("element vertex ${pointCloud.size}\n")
            writer.write("property float x\n")
            writer.write("property float y\n")
            writer.write("property float z\n")
            writer.write("end_header\n")
            
            for (point in pointCloud) {
                writer.write("${point[0]} ${point[1]} ${point[2]}\n")
            }
        }
        
        // Also export as XYZ for compatibility
        val xyzFile = File(sparseCloudsDir, "points.xyz")
        FileWriter(xyzFile).use { writer ->
            for (point in pointCloud) {
                writer.write("${point[0]} ${point[1]} ${point[2]}\n")
            }
        }
    }
    
    private fun exportMetadata(frames: List<ARFrame>, pointCloud: List<FloatArray>, baseDir: File) {
        val metadata = JSONObject()
        metadata.put("scan_date", dateFormat.format(Date()))
        metadata.put("num_frames", frames.size)
        metadata.put("num_points", pointCloud.size)
        metadata.put("compatible_with", "Triangle Splatting / 3D Gaussian Splatting")
        metadata.put("format_version", "1.0")
        
        if (frames.isNotEmpty()) {
            metadata.put("image_width", frames[0].width)
            metadata.put("image_height", frames[0].height)
        }
        
        File(baseDir, "metadata.json").writeText(metadata.toString(2))
        
        // Create README
        createReadme(baseDir)
    }
    
    private fun createReadme(baseDir: File) {
        val readme = """
# High-Fidelity AR Scan Data

This directory contains AR scan data exported from Spacemaker in a format compatible with Triangle Splatting.

## Contents

- `images/` - Captured camera frames in JPEG format
- `sparse/` - Point cloud data in PLY and XYZ formats
- `cameras.json` - Camera intrinsics in JSON format
- `transforms.json` - Camera poses in NeRF format
- `cameras.txt` - Camera intrinsics in COLMAP format
- `images.txt` - Camera poses in COLMAP format
- `metadata.json` - Scan metadata

## Processing with Triangle Splatting

1. Clone the Triangle Splatting repository:
   ```bash
   git clone https://github.com/trianglesplatting/triangle-splatting --recursive
   cd triangle-splatting
   ```

2. Install dependencies (requires Python 3.11 and CUDA 12.6):
   ```bash
   micromamba create -f requirements.yaml
   bash compile.sh
   ```

3. Convert this scan data to Triangle Splatting format (if needed)

4. Train the model:
   ```bash
   python train.py -s <path_to_this_directory> -m <output_model_path> --eval
   ```

5. Render the results:
   ```bash
   python render.py -m <output_model_path>
   ```

## Alternative Processing

This data is also compatible with:
- 3D Gaussian Splatting
- NeRF implementations (Instant-NGP, Nerfstudio)
- COLMAP for traditional reconstruction
- CloudCompare for point cloud processing

## Notes

- Images are stored in JPEG format at 95% quality
- Camera poses are provided in OpenGL convention
- Point cloud is filtered by confidence threshold
- All coordinates are in ARCore's coordinate system
        """.trimIndent()
        
        File(baseDir, "README.md").writeText(readme)
    }
    
    private fun calculateFOV(fx: Float, width: Int): Double {
        return 2.0 * Math.atan(width / (2.0 * fx))
    }
    
    private fun poseToQuaternion(pose: FloatArray): FloatArray {
        // Extract rotation matrix from 4x4 pose matrix
        // Convert to quaternion (simplified version)
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
            // Simplified fallback
            floatArrayOf(1f, 0f, 0f, 0f)
        }
    }
}
