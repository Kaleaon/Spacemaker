package com.kaleaon.spacemaker

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil

/**
 * Utility class for extracting frames from uploaded video files.
 * Extracted frames can be used for Gaussian splatting reconstruction.
 */
class VideoFrameExtractor(private val context: Context) {

    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    
    /**
     * Frame extraction settings
     */
    data class ExtractionSettings(
        val framesPerSecond: Float = 2.0f,      // Extract 2 frames per second by default
        val maxFrames: Int = 200,                // Maximum frames to extract
        val imageQuality: Int = 95,              // JPEG quality (0-100)
        val outputFormat: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
    )
    
    /**
     * Result of video frame extraction
     */
    data class ExtractionResult(
        val outputDirectory: File,
        val frameCount: Int,
        val videoDurationMs: Long,
        val videoWidth: Int,
        val videoHeight: Int,
        val frameFiles: List<File>,
        val metadata: VideoMetadata
    )
    
    /**
     * Video metadata information
     */
    data class VideoMetadata(
        val durationMs: Long,
        val width: Int,
        val height: Int,
        val rotation: Int,
        val frameRate: String?,
        val bitrate: String?
    )
    
    /**
     * Callback interface for extraction progress
     */
    interface ExtractionProgressListener {
        fun onProgressUpdate(currentFrame: Int, totalFrames: Int)
        fun onFrameExtracted(frameNumber: Int, bitmap: Bitmap)
        fun onExtractionComplete(result: ExtractionResult)
        fun onExtractionError(error: Exception)
    }
    
    /**
     * Extract frames from a video URI
     * 
     * @param videoUri The URI of the video file
     * @param settings Frame extraction settings
     * @param listener Progress callback listener (optional)
     * @return ExtractionResult containing extracted frame information
     */
    fun extractFrames(
        videoUri: Uri,
        settings: ExtractionSettings = ExtractionSettings(),
        listener: ExtractionProgressListener? = null
    ): ExtractionResult? {
        var retriever: MediaMetadataRetriever? = null
        
        try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, videoUri)
            
            // Get video metadata
            val metadata = getVideoMetadata(retriever)
            
            // Calculate frame extraction timestamps
            val durationMs = metadata.durationMs
            val intervalMs = (1000.0f / settings.framesPerSecond).toLong()
            val totalFrames = minOf(
                ceil(durationMs.toFloat() / intervalMs).toInt(),
                settings.maxFrames
            )
            
            // Create output directory
            val scanName = "video_scan_${dateFormat.format(Date())}"
            val outputDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                scanName
            )
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            
            val imagesDir = File(outputDir, "images")
            imagesDir.mkdirs()
            
            val frameFiles = mutableListOf<File>()
            var currentTime = 0L
            var frameIndex = 0
            
            while (currentTime < durationMs && frameIndex < totalFrames) {
                // Extract frame at current timestamp
                val bitmap = retriever.getFrameAtTime(
                    currentTime * 1000, // Convert ms to microseconds
                    MediaMetadataRetriever.OPTION_CLOSEST
                )
                
                if (bitmap != null) {
                    // Save frame to file
                    val frameFile = File(imagesDir, String.format("frame_%04d.jpg", frameIndex))
                    saveFrame(bitmap, frameFile, settings)
                    frameFiles.add(frameFile)
                    
                    listener?.onProgressUpdate(frameIndex + 1, totalFrames)
                    listener?.onFrameExtracted(frameIndex, bitmap)
                    
                    // Recycle bitmap if we don't need to keep it
                    if (listener == null) {
                        bitmap.recycle()
                    }
                }
                
                currentTime += intervalMs
                frameIndex++
            }
            
            // Generate metadata file
            generateMetadataFile(outputDir, metadata, frameFiles.size, settings)
            
            val result = ExtractionResult(
                outputDirectory = outputDir,
                frameCount = frameFiles.size,
                videoDurationMs = durationMs,
                videoWidth = metadata.width,
                videoHeight = metadata.height,
                frameFiles = frameFiles,
                metadata = metadata
            )
            
            listener?.onExtractionComplete(result)
            
            return result
            
        } catch (e: Exception) {
            e.printStackTrace()
            listener?.onExtractionError(e)
            return null
        } finally {
            retriever?.release()
        }
    }
    
    /**
     * Get video metadata from the retriever
     */
    private fun getVideoMetadata(retriever: MediaMetadataRetriever): VideoMetadata {
        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
        val frameRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
        val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
        
        return VideoMetadata(
            durationMs = durationStr?.toLongOrNull() ?: 0L,
            width = widthStr?.toIntOrNull() ?: 0,
            height = heightStr?.toIntOrNull() ?: 0,
            rotation = rotationStr?.toIntOrNull() ?: 0,
            frameRate = frameRate,
            bitrate = bitrate
        )
    }
    
    /**
     * Save a bitmap frame to a file
     */
    private fun saveFrame(
        bitmap: Bitmap,
        file: File,
        settings: ExtractionSettings
    ) {
        FileOutputStream(file).use { outputStream ->
            bitmap.compress(settings.outputFormat, settings.imageQuality, outputStream)
        }
    }
    
    /**
     * Generate metadata JSON file for the extraction
     */
    private fun generateMetadataFile(
        outputDir: File,
        metadata: VideoMetadata,
        frameCount: Int,
        settings: ExtractionSettings
    ) {
        val metadataContent = """
{
    "source": "video_extraction",
    "extraction_date": "${dateFormat.format(Date())}",
    "video_duration_ms": ${metadata.durationMs},
    "video_width": ${metadata.width},
    "video_height": ${metadata.height},
    "video_rotation": ${metadata.rotation},
    "frame_count": $frameCount,
    "frames_per_second": ${settings.framesPerSecond},
    "image_quality": ${settings.imageQuality},
    "compatible_with": "Triangle Splatting / PGSR / 3D Gaussian Splatting",
    "format_version": "1.0",
    "notes": "Frames extracted from video for Gaussian splatting reconstruction. Camera poses not available - use COLMAP or similar SfM tool to estimate poses."
}
        """.trimIndent()
        
        File(outputDir, "metadata.json").writeText(metadataContent)
        
        // Create README with instructions
        createVideoReadme(outputDir, metadata, frameCount)
    }
    
    /**
     * Create README file with processing instructions
     */
    private fun createVideoReadme(
        outputDir: File,
        metadata: VideoMetadata,
        frameCount: Int
    ) {
        val readme = """
# Video Frame Extraction for Gaussian Splatting

This directory contains frames extracted from a video file for 3D reconstruction using Gaussian splatting techniques.

## Source Video Information

- **Duration**: ${metadata.durationMs / 1000.0} seconds
- **Resolution**: ${metadata.width} x ${metadata.height}
- **Rotation**: ${metadata.rotation}°
- **Extracted Frames**: $frameCount

## Contents

- `images/` - Extracted video frames in JPEG format
- `metadata.json` - Extraction metadata

## Important Notes

Unlike AR-captured data, video-extracted frames **do not include camera pose information**. 
You will need to estimate camera poses using Structure-from-Motion (SfM) before training.

## Processing Pipeline

### Step 1: Camera Pose Estimation with COLMAP

```bash
# Install COLMAP
# Ubuntu/Debian: sudo apt install colmap
# macOS: brew install colmap

# Run automatic reconstruction
colmap automatic_reconstructor \
    --workspace_path ${outputDir.name} \
    --image_path ${outputDir.name}/images

# This will create sparse reconstruction in ${outputDir.name}/sparse/0/
```

### Step 2: Process with Triangle Splatting

```bash
# Clone repository
git clone https://github.com/trianglesplatting/triangle-splatting --recursive
cd triangle-splatting

# Install dependencies
micromamba create -f requirements.yaml
bash compile.sh

# Train model
python train.py -s <path_to_this_directory> -m output/model --eval

# Render results
python render.py -m output/model
```

### Step 3: Process with PGSR (for indoor scenes)

```bash
# Clone repository
git clone git@github.com:zju3dv/PGSR.git
cd PGSR

# Install dependencies
conda create -n pgsr python=3.8
conda activate pgsr
pip install -r requirements.txt

# Train model
python train.py -s <path_to_this_directory> -m output/pgsr_model \
    --max_abs_split_points 0 \
    --opacity_cull_threshold 0.05

# Render and extract mesh
python render.py -m output/pgsr_model \
    --max_depth 10.0 \
    --voxel_size 0.01 \
    --use_depth_filter
```

## Alternative: 3D Gaussian Splatting

```bash
# Clone original 3DGS repository
git clone https://github.com/graphdeco-inria/gaussian-splatting --recursive
cd gaussian-splatting

# Install dependencies
pip install submodules/diff-gaussian-rasterization
pip install submodules/simple-knn

# Train model (after COLMAP processing)
python train.py -s <path_to_this_directory> -m output/model

# Render views
python render.py -m output/model
```

## Tips for Best Results

1. **Video Quality**: Use high-resolution video (1080p or 4K recommended)
2. **Smooth Motion**: Avoid jerky camera movements in the source video
3. **Good Lighting**: Ensure consistent lighting throughout the video
4. **Coverage**: Video should cover the scene from multiple angles
5. **Overlap**: Ensure adjacent frames have significant visual overlap
6. **Frame Rate**: Higher extraction FPS improves COLMAP matching but increases processing time

## Troubleshooting

### COLMAP fails to find enough matches
- Try extracting more frames (increase frames_per_second)
- Ensure video has enough feature points (not plain walls)
- Use SIFT features instead of default

### Poor reconstruction quality
- Add more training iterations
- Adjust learning rate
- Check if COLMAP sparse points cover the scene adequately

## Support

- Triangle Splatting: https://github.com/trianglesplatting/triangle-splatting
- PGSR: https://github.com/zju3dv/PGSR
- 3D Gaussian Splatting: https://github.com/graphdeco-inria/gaussian-splatting
- COLMAP: https://colmap.github.io/
- Spacemaker: https://github.com/Kaleaon/Spacemaker
        """.trimIndent()
        
        File(outputDir, "README.md").writeText(readme)
    }
    
    /**
     * Convert extracted frames to ARFrame format for use with existing exporters
     * Note: Camera poses will be identity matrices (no real pose data from video)
     */
    fun convertToARFrames(result: ExtractionResult): List<ARFrame> {
        val frames = mutableListOf<ARFrame>()
        
        // Create default camera intrinsics (estimated from video resolution)
        // These are approximations - actual values would need calibration
        val focalLength = (result.videoWidth * 0.8f)
        val cameraIntrinsics = floatArrayOf(
            focalLength,                          // fx
            focalLength,                          // fy
            result.videoWidth / 2.0f,             // cx
            result.videoHeight / 2.0f             // cy
        )
        
        // Identity pose matrix (no real camera pose available)
        val identityPose = FloatArray(16).apply {
            this[0] = 1f; this[5] = 1f; this[10] = 1f; this[15] = 1f
        }
        
        result.frameFiles.forEachIndexed { index, file ->
            try {
                val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    frames.add(
                        ARFrame(
                            timestamp = System.currentTimeMillis() + (index * 100L),
                            image = bitmap,
                            cameraIntrinsics = cameraIntrinsics,
                            cameraPose = identityPose.clone(),
                            pointCloud = emptyList(),
                            width = result.videoWidth,
                            height = result.videoHeight
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        return frames
    }
}
