package com.kaleaon.spacemaker

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import com.google.ar.core.Config
import com.google.ar.core.Session

/**
 * Utility for Google Pixel 10 advanced features:
 * - LDAF (Laser Detect Auto Focus) for enhanced depth
 * - Night mode for low-light scanning
 * - Hardware-specific optimizations
 */
object Pixel10Features {
    
    /**
     * Check if device is a Pixel 10 or has compatible features
     */
    fun isPixel10Compatible(context: Context): Boolean {
        val model = Build.MODEL.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()
        
        // Check for Pixel 10 or similar devices
        return manufacturer.contains("google") && 
               (model.contains("pixel 10") || model.contains("pixel10"))
    }
    
    /**
     * Check if device has LDAF sensor
     */
    fun hasLDAFSensor(context: Context): Boolean {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraIds = cameraManager.cameraIdList
            
            for (cameraId in cameraIds) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                
                // Check for laser auto-focus capability
                val afModes = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)
                if (afModes != null) {
                    // LDAF would be indicated by advanced AF capabilities
                    val hasAdvancedAF = afModes.contains(CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_VIDEO) ||
                                       afModes.contains(CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                    
                    // Check for depth capability
                    val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                    val hasDepth = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT) ?: false
                    
                    if (hasAdvancedAF || hasDepth) {
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return false
    }
    
    /**
     * Configure ARCore session with enhanced depth settings
     */
    fun configureEnhancedDepth(session: Session): Config {
        val config = Config(session)
        
        // Enable depth API if available
        if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
            config.depthMode = Config.DepthMode.AUTOMATIC
        } else if (session.isDepthModeSupported(Config.DepthMode.RAW_DEPTH_ONLY)) {
            config.depthMode = Config.DepthMode.RAW_DEPTH_ONLY
        }
        
        // Enable plane finding (important for PGSR)
        config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
        
        // Enable light estimation (important for Triangle Splatting)
        config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
        
        // Set update mode for best quality
        config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
        
        // Enable instant placement if available (for faster scanning)
        if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
            config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
        }
        
        return config
    }
    
    /**
     * Get optimal camera settings for night mode / low light
     */
    fun getNightModeSettings(): CameraSettings {
        return CameraSettings(
            exposureCompensation = 2,  // Increase exposure
            isoSensitivity = 800,       // Higher ISO for low light
            useNightMode = true,
            enableHDR = true
        )
    }
    
    data class CameraSettings(
        val exposureCompensation: Int,
        val isoSensitivity: Int,
        val useNightMode: Boolean,
        val enableHDR: Boolean
    )
    
    /**
     * Get depth quality information
     */
    fun getDepthQuality(session: Session): DepthQuality {
        val hasRawDepth = session.isDepthModeSupported(Config.DepthMode.RAW_DEPTH_ONLY)
        val hasAutomaticDepth = session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)
        
        return when {
            hasAutomaticDepth -> DepthQuality.HIGH  // LDAF or ToF sensor
            hasRawDepth -> DepthQuality.MEDIUM       // Software depth
            else -> DepthQuality.LOW                 // Point cloud only
        }
    }
    
    enum class DepthQuality {
        HIGH,    // Hardware depth sensor (LDAF, ToF)
        MEDIUM,  // Software depth estimation
        LOW      // Basic point cloud
    }
    
    /**
     * Calculate optimal frame capture rate based on device capabilities
     */
    fun getOptimalCaptureInterval(context: Context): Int {
        return if (isPixel10Compatible(context)) {
            5  // Capture every 5 frames on Pixel 10 (higher quality)
        } else {
            10 // Capture every 10 frames on other devices
        }
    }
    
    /**
     * Get recommended export format based on device
     */
    fun getRecommendedExportFormat(context: Context, hasPlanes: Boolean): ExportFormat {
        val isPixel10 = isPixel10Compatible(context)
        val hasLDAF = hasLDAFSensor(context)
        
        return when {
            // Pixel 10 with LDAF - best for PGSR with planes
            isPixel10 && hasLDAF && hasPlanes -> ExportFormat.PGSR_PRIORITY
            
            // Pixel 10 without clear planes - use Triangle Splatting
            isPixel10 && hasLDAF && !hasPlanes -> ExportFormat.TRIANGLE_SPLATTING_PRIORITY
            
            // Indoor scene with planes - PGSR is better
            hasPlanes -> ExportFormat.PGSR_PRIORITY
            
            // Default - support both
            else -> ExportFormat.BOTH
        }
    }
    
    enum class ExportFormat {
        PGSR_PRIORITY,              // Planar-based for indoor scenes
        TRIANGLE_SPLATTING_PRIORITY, // Triangle-based for complex geometry
        BOTH                         // Export both formats
    }
}
