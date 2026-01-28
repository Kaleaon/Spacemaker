package com.kaleaon.spacemaker

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Environment
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.ar.core.*
import com.google.ar.core.exceptions.*
import java.io.File
import java.io.FileWriter
import java.io.IOException
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ARScanActivity : AppCompatActivity(), GLSurfaceView.Renderer {

    private lateinit var surfaceView: GLSurfaceView
    private lateinit var statusText: TextView
    private lateinit var pointCountText: TextView
    private lateinit var stopScanButton: MaterialButton
    private lateinit var saveBlueprintButton: MaterialButton
    
    private var session: Session? = null
    private val pointCloud = mutableListOf<FloatArray>()
    private val capturedFrames = mutableListOf<ARFrame>()
    private val detectedPlanes = mutableListOf<DetectedPlane>()
    private var isScanning = true
    private var frameCount = 0
    private val FRAME_CAPTURE_INTERVAL = 10 // Capture every 10th frame
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar_scan)
        
        statusText = findViewById(R.id.statusText)
        pointCountText = findViewById(R.id.pointCountText)
        stopScanButton = findViewById(R.id.stopScanButton)
        saveBlueprintButton = findViewById(R.id.saveBlueprintButton)
        
        // Initialize AR Surface View
        surfaceView = GLSurfaceView(this)
        surfaceView.preserveEGLContextOnPause = true
        surfaceView.setEGLContextClientVersion(2)
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        surfaceView.setRenderer(this)
        surfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        
        val container = findViewById<android.widget.FrameLayout>(R.id.arContainer)
        container.addView(surfaceView)
        
        setupListeners()
        initializeARSession()
    }
    
    private fun setupListeners() {
        stopScanButton.setOnClickListener {
            isScanning = false
            statusText.text = getString(R.string.scan_complete)
            Toast.makeText(this, R.string.scan_complete, Toast.LENGTH_SHORT).show()
        }
        
        saveBlueprintButton.setOnClickListener {
            saveBlueprint()
        }
    }
    
    private fun initializeARSession() {
        try {
            session = Session(this)
            val config = Config(session)
            config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
            config.lightEstimationMode = Config.LightEstimationMode.AMBIENT_INTENSITY
            session?.configure(config)
        } catch (e: UnavailableException) {
            handleSessionException(e)
        }
    }
    
    private fun handleSessionException(exception: Exception) {
        val message = when (exception) {
            is UnavailableArcoreNotInstalledException -> "ARCore not installed"
            is UnavailableApkTooOldException -> "ARCore APK too old"
            is UnavailableSdkTooOldException -> "SDK too old"
            is UnavailableDeviceNotCompatibleException -> "Device not compatible"
            else -> "AR session creation failed"
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }
    
    override fun onResume() {
        super.onResume()
        try {
            session?.resume()
            surfaceView.onResume()
        } catch (e: CameraNotAvailableException) {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    override fun onPause() {
        super.onPause()
        session?.pause()
        surfaceView.onPause()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        session?.close()
        session = null
    }
    
    // GLSurfaceView.Renderer methods
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
    }
    
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        session?.setDisplayGeometry(windowManager.defaultDisplay.rotation, width, height)
    }
    
    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        
        session?.let { session ->
            try {
                session.setCameraTextureName(0)
                val frame = session.update()
                
                if (isScanning) {
                    // Capture point cloud data
                    frame.acquirePointCloud().use { points ->
                        val pointsData = points.points
                        val buffer = pointsData.asReadOnlyBuffer()
                        
                        val pointsList = mutableListOf<FloatArray>()
                        while (buffer.hasRemaining()) {
                            val x = buffer.float
                            val y = buffer.float
                            val z = buffer.float
                            val confidence = buffer.float
                            
                            if (confidence > 0.5f) {
                                pointsList.add(floatArrayOf(x, y, z))
                            }
                        }
                        
                        if (pointsList.isNotEmpty()) {
                            pointCloud.addAll(pointsList)
                            runOnUiThread {
                                pointCountText.text = getString(R.string.points_captured, pointCloud.size)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle exceptions silently during rendering
            }
        }
    }
    
    private fun saveBlueprint() {
        if (pointCloud.isEmpty()) {
            Toast.makeText(this, "No data captured", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            val timestamp = System.currentTimeMillis()
            val fileName = "blueprint_$timestamp.xyz"
            
            // Save to app-specific directory (no permissions needed)
            val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            
            FileWriter(file).use { writer ->
                // Write header
                writer.write("# Spacemaker Blueprint\n")
                writer.write("# Points: ${pointCloud.size}\n")
                writer.write("# Format: X Y Z\n")
                
                // Write point cloud data
                for (point in pointCloud) {
                    writer.write("${point[0]} ${point[1]} ${point[2]}\n")
                }
            }
            
            Toast.makeText(
                this, 
                "${getString(R.string.blueprint_saved)}\n${file.absolutePath}", 
                Toast.LENGTH_LONG
            ).show()
            
        } catch (e: IOException) {
            Toast.makeText(this, R.string.error_saving, Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
}
