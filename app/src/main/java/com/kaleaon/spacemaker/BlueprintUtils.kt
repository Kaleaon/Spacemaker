package com.kaleaon.spacemaker

import kotlin.math.sqrt

/**
 * Utility class for processing and analyzing point cloud data
 */
object BlueprintUtils {
    
    /**
     * Calculate the bounding box of a point cloud
     */
    fun calculateBoundingBox(points: List<FloatArray>): FloatArray {
        if (points.isEmpty()) return floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f)
        
        var minX = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE
        var minZ = Float.MAX_VALUE
        var maxZ = Float.MIN_VALUE
        
        for (point in points) {
            minX = minOf(minX, point[0])
            maxX = maxOf(maxX, point[0])
            minY = minOf(minY, point[1])
            maxY = maxOf(maxY, point[1])
            minZ = minOf(minZ, point[2])
            maxZ = maxOf(maxZ, point[2])
        }
        
        return floatArrayOf(minX, maxX, minY, maxY, minZ, maxZ)
    }
    
    /**
     * Calculate the dimensions of a scanned space
     */
    fun calculateDimensions(points: List<FloatArray>): FloatArray {
        val bounds = calculateBoundingBox(points)
        val width = bounds[1] - bounds[0]
        val height = bounds[3] - bounds[2]
        val depth = bounds[5] - bounds[4]
        return floatArrayOf(width, height, depth)
    }
    
    /**
     * Calculate distance between two points
     */
    fun distance(p1: FloatArray, p2: FloatArray): Float {
        val dx = p2[0] - p1[0]
        val dy = p2[1] - p1[1]
        val dz = p2[2] - p1[2]
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
    
    /**
     * Filter points by confidence threshold
     */
    fun filterByConfidence(points: List<FloatArray>, threshold: Float = 0.5f): List<FloatArray> {
        return points.filter { it.size > 3 && it[3] >= threshold }
    }
    
    /**
     * Downsample point cloud by taking every nth point
     */
    fun downsample(points: List<FloatArray>, step: Int = 2): List<FloatArray> {
        return points.filterIndexed { index, _ -> index % step == 0 }
    }
}
