package com.kaleaon.spacemaker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity for uploading videos and extracting frames for Gaussian splatting reconstruction.
 */
class VideoUploadActivity : AppCompatActivity() {

    private lateinit var selectVideoButton: MaterialButton
    private lateinit var extractFramesButton: MaterialButton
    private lateinit var exportGaussianSplatButton: MaterialButton
    private lateinit var statusText: TextView
    private lateinit var videoInfoText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var frameCountText: TextView
    
    private var selectedVideoUri: Uri? = null
    private var extractionResult: VideoFrameExtractor.ExtractionResult? = null
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    
    private val videoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                handleVideoSelected(uri)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_upload)
        
        initializeViews()
        setupListeners()
    }
    
    private fun initializeViews() {
        selectVideoButton = findViewById(R.id.selectVideoButton)
        extractFramesButton = findViewById(R.id.extractFramesButton)
        exportGaussianSplatButton = findViewById(R.id.exportGaussianSplatButton)
        statusText = findViewById(R.id.statusText)
        videoInfoText = findViewById(R.id.videoInfoText)
        progressBar = findViewById(R.id.progressBar)
        frameCountText = findViewById(R.id.frameCountText)
        
        // Initial state
        extractFramesButton.isEnabled = false
        exportGaussianSplatButton.isEnabled = false
        progressBar.visibility = View.GONE
        frameCountText.visibility = View.GONE
    }
    
    private fun setupListeners() {
        selectVideoButton.setOnClickListener {
            if (checkStoragePermission()) {
                openVideoPicker()
            }
        }
        
        extractFramesButton.setOnClickListener {
            selectedVideoUri?.let { uri ->
                startFrameExtraction(uri)
            }
        }
        
        exportGaussianSplatButton.setOnClickListener {
            extractionResult?.let { result ->
                exportForGaussianSplatting(result)
            }
        }
    }
    
    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses READ_MEDIA_VIDEO
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_MEDIA_VIDEO),
                    PERMISSION_REQUEST_CODE
                )
                false
            } else {
                true
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10-12 uses scoped storage, no permission needed for picker
            true
        } else {
            // Android 9 and below
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_CODE
                )
                false
            } else {
                true
            }
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openVideoPicker()
            } else {
                Toast.makeText(this, R.string.storage_permission_required, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun openVideoPicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI).apply {
            type = "video/*"
        }
        videoPickerLauncher.launch(intent)
    }
    
    private fun handleVideoSelected(uri: Uri) {
        selectedVideoUri = uri
        extractFramesButton.isEnabled = true
        exportGaussianSplatButton.isEnabled = false
        extractionResult = null
        
        // Display video info
        val videoInfo = getVideoInfo(uri)
        videoInfoText.text = videoInfo
        videoInfoText.visibility = View.VISIBLE
        statusText.text = getString(R.string.video_selected)
        frameCountText.visibility = View.GONE
    }
    
    private fun getVideoInfo(uri: Uri): String {
        val extractor = VideoFrameExtractor(this)
        val retriever = android.media.MediaMetadataRetriever()
        
        return try {
            retriever.setDataSource(this, uri)
            val duration = retriever.extractMetadata(
                android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L
            val width = retriever.extractMetadata(
                android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
            )
            val height = retriever.extractMetadata(
                android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
            )
            
            val durationSec = duration / 1000.0
            getString(R.string.video_info_format, width ?: "?", height ?: "?", durationSec)
        } catch (e: Exception) {
            getString(R.string.video_info_unavailable)
        } finally {
            retriever.release()
        }
    }
    
    private fun startFrameExtraction(uri: Uri) {
        statusText.text = getString(R.string.extracting_frames)
        progressBar.visibility = View.VISIBLE
        progressBar.isIndeterminate = false
        progressBar.progress = 0
        extractFramesButton.isEnabled = false
        selectVideoButton.isEnabled = false
        frameCountText.visibility = View.VISIBLE
        frameCountText.text = getString(R.string.frames_extracted, 0)
        
        // Run extraction in background thread
        Thread {
            val extractor = VideoFrameExtractor(this)
            val settings = VideoFrameExtractor.ExtractionSettings(
                framesPerSecond = 2.0f,
                maxFrames = 200,
                imageQuality = 95
            )
            
            val result = extractor.extractFrames(
                uri,
                settings,
                object : VideoFrameExtractor.ExtractionProgressListener {
                    override fun onProgressUpdate(currentFrame: Int, totalFrames: Int) {
                        runOnUiThread {
                            progressBar.max = totalFrames
                            progressBar.progress = currentFrame
                            frameCountText.text = getString(R.string.frames_extracted, currentFrame)
                        }
                    }
                    
                    override fun onFrameExtracted(frameNumber: Int, bitmap: android.graphics.Bitmap) {
                        // We can optionally show a preview here
                        bitmap.recycle() // Free memory since we saved to file
                    }
                    
                    override fun onExtractionComplete(result: VideoFrameExtractor.ExtractionResult) {
                        runOnUiThread {
                            handleExtractionComplete(result)
                        }
                    }
                    
                    override fun onExtractionError(error: Exception) {
                        runOnUiThread {
                            handleExtractionError(error)
                        }
                    }
                }
            )
            
            // If listener was not called (no frames extracted)
            if (result != null && result.frameCount == 0) {
                runOnUiThread {
                    handleExtractionError(Exception("No frames could be extracted"))
                }
            }
        }.start()
    }
    
    private fun handleExtractionComplete(result: VideoFrameExtractor.ExtractionResult) {
        extractionResult = result
        progressBar.visibility = View.GONE
        extractFramesButton.isEnabled = true
        selectVideoButton.isEnabled = true
        exportGaussianSplatButton.isEnabled = true
        
        statusText.text = getString(R.string.extraction_complete)
        frameCountText.text = getString(R.string.frames_extracted_total, result.frameCount)
        
        Toast.makeText(
            this,
            getString(R.string.frames_saved_to, result.outputDirectory.absolutePath),
            Toast.LENGTH_LONG
        ).show()
    }
    
    private fun handleExtractionError(error: Exception) {
        progressBar.visibility = View.GONE
        extractFramesButton.isEnabled = true
        selectVideoButton.isEnabled = true
        
        statusText.text = getString(R.string.extraction_failed)
        Toast.makeText(this, getString(R.string.extraction_error, error.message), Toast.LENGTH_LONG).show()
    }
    
    private fun exportForGaussianSplatting(result: VideoFrameExtractor.ExtractionResult) {
        statusText.text = getString(R.string.exporting_gaussian_splat)
        progressBar.visibility = View.VISIBLE
        progressBar.isIndeterminate = true
        
        Thread {
            try {
                val extractor = VideoFrameExtractor(this)
                val frames = extractor.convertToARFrames(result)
                
                // Export using Triangle Splatting format
                val triangleExporter = TriangleSplattingExporter(this)
                val exportDir = triangleExporter.exportHighFidelityData(
                    frames = frames,
                    pointCloud = emptyList(), // No point cloud from video
                    scanName = "video_gaussian_${dateFormat.format(Date())}"
                )
                
                // Clean up bitmaps
                frames.forEach { frame ->
                    frame.image?.recycle()
                }
                
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    
                    if (exportDir != null) {
                        statusText.text = getString(R.string.export_complete)
                        Toast.makeText(
                            this,
                            getString(R.string.gaussian_splat_exported, exportDir.absolutePath),
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        statusText.text = getString(R.string.export_failed)
                        Toast.makeText(this, R.string.export_error, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    statusText.text = getString(R.string.export_failed)
                    Toast.makeText(this, getString(R.string.export_error_detail, e.message), Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 200
    }
}
