package com.kaleaon.spacemaker

import com.google.ar.core.Plane
import kotlin.math.sqrt

/**
 * Represents a detected planar surface in AR space
 * Used for PGSR (Planar-based Gaussian Splatting Reconstruction)
 */
data class DetectedPlane(
    val id: String,
    val centerPose: FloatArray,  // 4x4 transformation matrix
    val extentX: Float,
    val extentZ: Float,
    val planeType: PlaneType,
    val normalVector: FloatArray,  // 3D normal vector
    val polygon: List<FloatArray>,  // Boundary polygon points
    val confidence: Float = 1.0f
) {
    enum class PlaneType {
        HORIZONTAL_UPWARD_FACING,  // Floor
        HORIZONTAL_DOWNWARD_FACING, // Ceiling
        VERTICAL,                   // Wall
        UNKNOWN
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DetectedPlane

        if (id != other.id) return false
        if (!centerPose.contentEquals(other.centerPose)) return false
        if (extentX != other.extentX) return false
        if (extentZ != other.extentZ) return false
        if (planeType != other.planeType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + centerPose.contentHashCode()
        result = 31 * result + extentX.hashCode()
        result = 31 * result + extentZ.hashCode()
        result = 31 * result + planeType.hashCode()
        return result
    }
}

/**
 * Utility for processing planar surfaces from ARCore
 */
object PlaneProcessor {
    
    /**
     * Convert ARCore Plane to DetectedPlane
     */
    fun fromARCorePlane(plane: Plane): DetectedPlane {
        val centerPose = FloatArray(16)
        plane.centerPose.toMatrix(centerPose, 0)
        
        val normal = FloatArray(3)
        plane.centerPose.getTransformedAxis(1, 1.0f, normal, 0)
        
        val planeType = when (plane.type) {
            Plane.Type.HORIZONTAL_UPWARD_FACING -> DetectedPlane.PlaneType.HORIZONTAL_UPWARD_FACING
            Plane.Type.HORIZONTAL_DOWNWARD_FACING -> DetectedPlane.PlaneType.HORIZONTAL_DOWNWARD_FACING
            Plane.Type.VERTICAL -> DetectedPlane.PlaneType.VERTICAL
            else -> DetectedPlane.PlaneType.UNKNOWN
        }
        
        val polygonBuffer = plane.polygon.asReadOnlyBuffer()
        val polygonFloats = FloatArray(polygonBuffer.remaining())
        polygonBuffer.get(polygonFloats)
        val polygon = polygonFloats.toList().chunked(2).map { 
            floatArrayOf(it[0], it[1])
        }
        
        return DetectedPlane(
            id = plane.hashCode().toString(),
            centerPose = centerPose,
            extentX = plane.extentX,
            extentZ = plane.extentZ,
            planeType = planeType,
            normalVector = normal,
            polygon = polygon
        )
    }
    
    /**
     * Filter planes by minimum size
     */
    fun filterBySize(planes: List<DetectedPlane>, minArea: Float = 0.1f): List<DetectedPlane> {
        return planes.filter { (it.extentX * it.extentZ) >= minArea }
    }
    
    /**
     * Merge nearby parallel planes
     */
    fun mergePlanes(planes: List<DetectedPlane>, angleThreshold: Float = 0.1f): List<DetectedPlane> {
        val merged = mutableListOf<DetectedPlane>()
        val processed = mutableSetOf<String>()
        
        for (plane in planes) {
            if (plane.id in processed) continue
            
            val similar = planes.filter { other ->
                other.id != plane.id && 
                other.id !in processed &&
                areParallel(plane.normalVector, other.normalVector, angleThreshold) &&
                plane.planeType == other.planeType
            }
            
            if (similar.isEmpty()) {
                merged.add(plane)
            } else {
                // For now, just take the larger plane
                val largest = (listOf(plane) + similar).maxByOrNull { it.extentX * it.extentZ }
                largest?.let { merged.add(it) }
                processed.addAll(similar.map { it.id })
            }
            
            processed.add(plane.id)
        }
        
        return merged
    }
    
    /**
     * Check if two normal vectors are parallel
     */
    private fun areParallel(n1: FloatArray, n2: FloatArray, threshold: Float): Boolean {
        val dot = n1[0] * n2[0] + n1[1] * n2[1] + n1[2] * n2[2]
        return Math.abs(Math.abs(dot) - 1.0f) < threshold
    }
    
    /**
     * Calculate plane equation coefficients (ax + by + cz + d = 0)
     */
    fun getPlaneEquation(plane: DetectedPlane): FloatArray {
        val a = plane.normalVector[0]
        val b = plane.normalVector[1]
        val c = plane.normalVector[2]
        
        // Get plane center point
        val cx = plane.centerPose[12]
        val cy = plane.centerPose[13]
        val cz = plane.centerPose[14]
        
        // d = -(ax + by + cz)
        val d = -(a * cx + b * cy + c * cz)
        
        return floatArrayOf(a, b, c, d)
    }
    
    /**
     * Calculate distance from point to plane
     */
    fun pointToPlaneDistance(point: FloatArray, planeEq: FloatArray): Float {
        val numerator = Math.abs(
            planeEq[0] * point[0] + 
            planeEq[1] * point[1] + 
            planeEq[2] * point[2] + 
            planeEq[3]
        )
        val denominator = sqrt(
            planeEq[0] * planeEq[0] + 
            planeEq[1] * planeEq[1] + 
            planeEq[2] * planeEq[2]
        )
        return numerator / denominator
    }
    
    /**
     * Assign points to nearest plane
     */
    fun assignPointsToPlanes(
        points: List<FloatArray>,
        planes: List<DetectedPlane>,
        maxDistance: Float = 0.05f
    ): Map<String, List<FloatArray>> {
        val assignments = mutableMapOf<String, MutableList<FloatArray>>()
        
        // Initialize lists for each plane
        planes.forEach { plane ->
            assignments[plane.id] = mutableListOf()
        }
        
        // Unassigned points
        assignments["unassigned"] = mutableListOf()
        
        for (point in points) {
            var nearestPlane: DetectedPlane? = null
            var minDistance = Float.MAX_VALUE
            
            for (plane in planes) {
                val planeEq = getPlaneEquation(plane)
                val distance = pointToPlaneDistance(point, planeEq)
                
                if (distance < minDistance) {
                    minDistance = distance
                    nearestPlane = plane
                }
            }
            
            if (nearestPlane != null && minDistance <= maxDistance) {
                assignments[nearestPlane.id]?.add(point)
            } else {
                assignments["unassigned"]?.add(point)
            }
        }
        
        return assignments
    }
    
    /**
     * Get dominant plane orientation (for room layout)
     */
    fun getDominantOrientation(planes: List<DetectedPlane>): Map<DetectedPlane.PlaneType, List<DetectedPlane>> {
        return planes.groupBy { it.planeType }
    }
}
