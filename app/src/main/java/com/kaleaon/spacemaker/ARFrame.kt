package com.kaleaon.spacemaker

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.media.Image
import java.nio.ByteBuffer

/**
 * Data class representing a camera frame with AR pose information
 * Compatible with Triangle Splatting pipeline
 */
data class ARFrame(
    val timestamp: Long,
    val image: Bitmap?,
    val cameraIntrinsics: FloatArray,
    val cameraPose: FloatArray,  // 4x4 transformation matrix
    val pointCloud: List<FloatArray>,
    val width: Int,
    val height: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ARFrame

        if (timestamp != other.timestamp) return false
        if (image != other.image) return false
        if (!cameraIntrinsics.contentEquals(other.cameraIntrinsics)) return false
        if (!cameraPose.contentEquals(other.cameraPose)) return false
        if (width != other.width) return false
        if (height != other.height) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + (image?.hashCode() ?: 0)
        result = 31 * result + cameraIntrinsics.contentHashCode()
        result = 31 * result + cameraPose.contentHashCode()
        result = 31 * result + width
        result = 31 * result + height
        return result
    }
}

/**
 * Utility for converting AR camera images to bitmaps
 */
object ImageConverter {
    
    fun imageToBitmap(image: Image): Bitmap? {
        return try {
            val planes = image.planes
            val buffer: ByteBuffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width
            
            val bitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            
            // Crop to actual image size if there's padding
            if (rowPadding > 0) {
                Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun yuvToRgb(image: Image): Bitmap? {
        if (image.format != ImageFormat.YUV_420_888) {
            return null
        }
        
        val width = image.width
        val height = image.height
        
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]
        
        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        
        // Convert YUV to RGB
        var yIndex = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                val yValue = yBuffer[yIndex].toInt() and 0xFF
                val uvIndex = (y / 2) * uPlane.rowStride + (x / 2) * uPlane.pixelStride
                
                val uValue = (uBuffer[uvIndex].toInt() and 0xFF) - 128
                val vValue = (vBuffer[uvIndex].toInt() and 0xFF) - 128
                
                val r = (yValue + 1.402 * vValue).toInt().coerceIn(0, 255)
                val g = (yValue - 0.344 * uValue - 0.714 * vValue).toInt().coerceIn(0, 255)
                val b = (yValue + 1.772 * uValue).toInt().coerceIn(0, 255)
                
                pixels[yIndex] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                yIndex++
            }
        }
        
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }
}
