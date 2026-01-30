package com.kaleaon.spacemaker

import android.content.Context
import android.view.Surface
import android.view.SurfaceView
import com.google.android.filament.*
import com.google.android.filament.android.UiHelper
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * High-quality 3D renderer using Google Filament
 * Provides real-time visualization of captured point clouds and meshes
 */
class FilamentRenderer(private val context: Context) {
    
    private lateinit var engine: Engine
    private lateinit var renderer: Renderer
    private lateinit var scene: Scene
    private lateinit var view: View
    private lateinit var camera: Camera
    private lateinit var swapChain: SwapChain
    private lateinit var uiHelper: UiHelper
    
    private var pointCloudEntity: Int = 0
    private var initialized = false
    
    /**
     * Initialize Filament rendering engine
     */
    fun initialize(surfaceView: SurfaceView) {
        try {
            // Create Filament engine
            engine = Engine.create()
            renderer = engine.createRenderer()
            scene = engine.createScene()
            view = engine.createView()
            camera = engine.createCamera(EntityManager.get().create())
            
            // Configure view
            view.scene = scene
            view.camera = camera
            
            // Set up post-processing for high quality
            view.isPostProcessingEnabled = true
            view.antiAliasing = View.AntiAliasing.FXAA
            view.sampleCount = 4
            view.ambientOcclusion = View.AmbientOcclusion.SSAO
            
            // Set up camera
            val aspect = surfaceView.width.toDouble() / surfaceView.height.toDouble()
            camera.setProjection(45.0, aspect, 0.1, 100.0, Camera.Fov.VERTICAL)
            camera.lookAt(
                0.0, 1.5, 3.0,  // eye position
                0.0, 0.0, 0.0,  // center
                0.0, 1.0, 0.0   // up vector
            )
            
            // Set up UI helper for surface management
            uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK)
            uiHelper.renderCallback = object : UiHelper.RendererCallback {
                override fun onNativeWindowChanged(surface: Surface) {
                    if (::swapChain.isInitialized) {
                        engine.destroySwapChain(swapChain)
                    }
                    swapChain = engine.createSwapChain(surface)
                }
                
                override fun onDetachedFromSurface() {
                    if (::swapChain.isInitialized) {
                        engine.destroySwapChain(swapChain)
                    }
                }
                
                override fun onResized(width: Int, height: Int) {
                    view.viewport = Viewport(0, 0, width, height)
                    val aspect = width.toDouble() / height.toDouble()
                    camera.setProjection(45.0, aspect, 0.1, 100.0, Camera.Fov.VERTICAL)
                }
            }
            
            uiHelper.attachTo(surfaceView)
            
            // Set up lighting
            setupLighting()
            
            initialized = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Set up environmental lighting for realistic rendering
     */
    private fun setupLighting() {
        // Create a simple directional light
        val sunlight = EntityManager.get().create()
        
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(1.0f, 1.0f, 1.0f)
            .intensity(100000.0f)
            .direction(0.0f, -1.0f, 0.0f)
            .castShadows(true)
            .build(engine, sunlight)
        
        scene.addEntity(sunlight)
        
        // Add ambient light
        val indirectLight = IndirectLight.Builder()
            .intensity(30000.0f)
            .build(engine)
        
        scene.indirectLight = indirectLight
    }
    
    /**
     * Render a point cloud with Filament
     */
    fun renderPointCloud(points: List<FloatArray>) {
        if (!initialized || points.isEmpty()) return
        
        try {
            // Remove old point cloud if exists
            if (pointCloudEntity != 0) {
                engine.destroyEntity(pointCloudEntity)
                pointCloudEntity = 0
            }
            
            // Create vertex buffer
            val vertexCount = points.size
            val vertexBuffer = VertexBuffer.Builder()
                .vertexCount(vertexCount)
                .bufferCount(1)
                .attribute(
                    VertexBuffer.VertexAttribute.POSITION,
                    0,
                    VertexBuffer.AttributeType.FLOAT3,
                    0,
                    12
                )
                .build(engine)
            
            // Fill vertex buffer with point data
            val vertexData = ByteBuffer.allocateDirect(vertexCount * 12)
                .order(ByteOrder.nativeOrder())
            
            for (point in points) {
                vertexData.putFloat(point[0])
                vertexData.putFloat(point[1])
                vertexData.putFloat(point[2])
            }
            
            vertexData.flip()
            vertexBuffer.setBufferAt(engine, 0, vertexData)
            
            // Create index buffer (for points, each point is its own primitive)
            val indexBuffer = IndexBuffer.Builder()
                .indexCount(vertexCount)
                .bufferType(IndexBuffer.Builder.IndexType.UINT)
                .build(engine)
            
            val indexData = ByteBuffer.allocateDirect(vertexCount * 4)
                .order(ByteOrder.nativeOrder())
            
            for (i in 0 until vertexCount) {
                indexData.putInt(i)
            }
            
            indexData.flip()
            indexBuffer.setBuffer(engine, indexData)
            
            // Create material for points
            val material = createPointCloudMaterial()
            
            // Create renderable entity
            pointCloudEntity = EntityManager.get().create()
            
            RenderableManager.Builder(1)
                .boundingBox(Box(0.0f, 0.0f, 0.0f, 10.0f, 10.0f, 10.0f))
                .geometry(0, RenderableManager.PrimitiveType.POINTS, vertexBuffer, indexBuffer)
                .material(0, material.defaultInstance)
                .build(engine, pointCloudEntity)
            
            scene.addEntity(pointCloudEntity)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Create material for point cloud rendering
     */
    private fun createPointCloudMaterial(): Material {
        // Simple unlit material for point cloud
        val materialData = """
            material {
                name : PointCloudMaterial,
                shadingModel : unlit,
                parameters : [
                    { type : float3, name : baseColor }
                ]
            }
            
            fragment {
                void material(inout MaterialInputs material) {
                    prepareMaterial(material);
                    material.baseColor.rgb = materialParams.baseColor;
                }
            }
        """.trimIndent()
        
        // In production, compile this with matc tool and load as binary
        // For now, return a default material
        return Material.Builder()
            .payload(ByteBuffer.allocate(0), 0)
            .build(engine)
    }
    
    /**
     * Update camera position (for user interaction)
     */
    fun updateCamera(eyeX: Double, eyeY: Double, eyeZ: Double) {
        if (!initialized) return
        
        camera.lookAt(
            eyeX, eyeY, eyeZ,
            0.0, 0.0, 0.0,
            0.0, 1.0, 0.0
        )
    }
    
    /**
     * Render a frame
     */
    fun render() {
        if (!initialized || !::swapChain.isInitialized) return
        
        try {
            // Begin frame
            if (renderer.beginFrame(swapChain)) {
                renderer.render(view)
                renderer.endFrame()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Clean up resources
     */
    fun destroy() {
        if (!initialized) return
        
        try {
            uiHelper.detach()
            
            if (pointCloudEntity != 0) {
                engine.destroyEntity(pointCloudEntity)
            }
            
            engine.destroyRenderer(renderer)
            engine.destroyView(view)
            engine.destroyScene(scene)
            engine.destroyCamera(camera)
            
            if (::swapChain.isInitialized) {
                engine.destroySwapChain(swapChain)
            }
            
            engine.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
