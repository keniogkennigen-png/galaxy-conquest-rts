package com.galaxycommand.rts.ui

import android.graphics.Canvas
import android.view.MotionEvent
import com.galaxycommand.rts.core.Camera
import com.galaxycommand.rts.core.EntityManager
import com.galaxycommand.rts.core.GameMap
import com.galaxycommand.rts.core.GameState

/**
 * Manager class that coordinates all HUD/UI components in the game.
 * Handles initialization, updating, rendering, and input distribution
 * for all UI elements including the minimap and resource bar.
 */
class HUDManager(
    private val gameMap: GameMap,
    private val gameState: GameState,
    private val camera: Camera,
    private val entityManager: EntityManager
) {
    
    /**
     * List of all UI components managed by this HUD manager.
     */
    private val components: MutableList<UIComponent> = mutableListOf()
    
    /**
     * Reference to the minimap component for quick access.
     */
    val minimap: Minimap
    
    /**
     * Reference to the resource bar component for quick access.
     */
    val resourceBar: ResourceBar
    
    /**
     * Current screen dimensions.
     */
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    
    /**
     * Whether the HUD is currently enabled and visible.
     */
    var isEnabled: Boolean = true
    
    /**
     * Whether any UI component is currently handling a touch event.
     */
    var isTouchHandled: Boolean = false
        private set
    
    init {
        // Create and register all UI components
        resourceBar = ResourceBar(gameState)
        components.add(resourceBar)
        
        minimap = Minimap(gameMap, gameState, camera, entityManager)
        components.add(minimap)
    }
    
    /**
     * Initialize all UI components with the current screen dimensions.
     * Should be called when the game surface is created or resized.
     */
    fun initialize(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
        
        // Center the camera on the map initially
        centerCameraOnSpawn()
        
        // Initialize all components
        for (component in components) {
            component.initialize(width, height)
        }
    }
    
    /**
     * Center the camera on the player's spawn position.
     */
    private fun centerCameraOnSpawn() {
        val spawn = gameMap.getPlayerSpawn(gameState.playerFaction)
        if (spawn != null) {
            val spawnX = spawn.x
            val spawnY = spawn.y
            val tileSize = 32f // Standard tile size for the game
            camera.setPosition(
                (spawnX * tileSize) - (camera.viewportWidth / 2),
                (spawnY * tileSize) - (camera.viewportHeight / 2)
            )
            
            // Clamp to map boundaries
            val maxX = (gameMap.width * tileSize) - camera.viewportWidth
            val maxY = (gameMap.height * tileSize) - camera.viewportHeight
            camera.setPosition(
                camera.position.x.coerceIn(0f, maxX.coerceAtLeast(0f)),
                camera.position.y.coerceIn(0f, maxY.coerceAtLeast(0f))
            )
        }
    }
    
    /**
     * Update all UI components.
     * Should be called once per frame before rendering.
     */
    fun update() {
        if (!isEnabled) return
        
        for (component in components) {
            component.update()
        }
    }
    
    /**
     * Render all UI components to the canvas.
     * Should be called after world rendering to ensure UI appears on top.
     */
    fun draw(canvas: Canvas) {
        if (!isEnabled) return
        
        for (component in components) {
            if (component.isVisible) {
                component.draw(canvas)
            }
        }
    }
    
    /**
     * Process touch events and distribute them to UI components.
     * @param event The motion event to process
     * @return true if the event was handled by a UI component
     */
    fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        
        isTouchHandled = false
        
        // Check each component in reverse order (topmost first)
        // UI components should be checked before game world input
        for (component in components.reversed()) {
            if (component.isEnabled && component.isVisible) {
                if (component.onTouchEvent(event)) {
                    isTouchHandled = true
                    return true
                }
            }
        }
        
        return false
    }
    
    /**
     * Show or hide all UI components.
     */
    fun setVisible(visible: Boolean) {
        for (component in components) {
            component.isVisible = visible
        }
    }
    
    /**
     * Enable or disable all UI components.
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        for (component in components) {
            component.isEnabled = enabled
        }
    }
    
    /**
     * Get the minimap component.
     */
    fun getMinimap(): Minimap = minimap
    
    /**
     * Get the resource bar component.
     */
    fun getResourceBar(): ResourceBar = resourceBar
    
    /**
     * Check if the given screen coordinates are within any UI component.
     */
    fun isTouchInUI(x: Float, y: Float): Boolean {
        for (component in components) {
            if (component.isVisible && component.containsPoint(x, y)) {
                return true
            }
        }
        return false
    }
    
    /**
     * Handle orientation change by reinitializing components.
     */
    fun onOrientationChanged(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
        
        for (component in components) {
            component.initialize(width, height)
        }
    }
}
