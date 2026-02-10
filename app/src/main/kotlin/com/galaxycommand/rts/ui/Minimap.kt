package com.galaxycommand.rts.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import com.galaxycommand.rts.core.Camera
import com.galaxycommand.rts.core.EntityManager
import com.galaxycommand.rts.core.GameMap
import com.galaxycommand.rts.core.GameState
import com.galaxycommand.rts.factions.FactionType
import com.galaxycommand.rts.entities.Unit

/**
 * Minimap component for overview navigation and tactical awareness.
 * Displays the entire game map with unit positions and camera viewport.
 * Supports touch interaction for quick navigation.
 */
class Minimap(
    private val gameMap: GameMap,
    private val gameState: GameState,
    private val camera: Camera,
    private val entityManager: EntityManager
) : UIComponent() {
    
    // Paint objects for rendering
    private val backgroundPaint = Paint().apply {
        color = Color.parseColor("#CC000000")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val borderPaint = Paint().apply {
        color = Color.parseColor("#00D4FF")
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }
    
    private val viewportPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }
    
    private val playerUnitPaint = Paint().apply {
        color = Color.parseColor("#00FF00")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val enemyUnitPaint = Paint().apply {
        color = Color.parseColor("#FF0000")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val neutralUnitPaint = Paint().apply {
        color = Color.parseColor("#FFFF00")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val resourcePaint = Paint().apply {
        color = Color.parseColor("#00BFFF")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val fogPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
        isAntiAlias = false
    }
    
    // Component dimensions
    private var minimapSize = 180f
    private var padding = 4f
    private var innerSize = 0f
    private var scaleFactor = 1f
    
    // Cached terrain bitmap for performance
    private var terrainBitmap: Bitmap? = null
    private var terrainCanvas: Canvas? = null
    
    // Touch handling
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    
    override fun initialize(screenWidth: Int, screenHeight: Int) {
        // Scale minimap size based on screen dimensions
        val minDimension = minOf(screenWidth, screenHeight)
        minimapSize = (minDimension * 0.25f).coerceIn(150f, 250f)
        padding = minimapSize * 0.02f
        innerSize = minimapSize - (padding * 2)
        
        // Calculate scale factor to fit map into minimap
        val mapMaxDimension = maxOf(gameMap.width, gameMap.height)
        scaleFactor = innerSize / mapMaxDimension
        
        // Position in bottom-right corner
        val rightMargin = padding * 2
        val bottomMargin = padding * 2
        
        setBounds(
            screenWidth - minimapSize - rightMargin,
            screenHeight - minimapSize - bottomMargin,
            screenWidth - rightMargin,
            screenHeight - bottomMargin
        )
        
        // Pre-render terrain bitmap
        createTerrainBitmap()
    }
    
    /**
     * Create a cached bitmap of the static terrain elements.
     * This significantly improves performance by avoiding terrain redraw every frame.
     */
    private fun createTerrainBitmap() {
        try {
            terrainBitmap = Bitmap.createBitmap(
                innerSize.toInt(),
                innerSize.toInt(),
                Bitmap.Config.ARGB_8888
            )
            terrainCanvas = Canvas(terrainBitmap!!)
            
            // Draw terrain grid
            val tileSize = innerSize / gameMap.width
            
            for (y in 0 until gameMap.height) {
                for (x in 0 until gameMap.width) {
                    val tile = gameMap.getTile(x, y)
                    val drawX = x * tileSize
                    val drawY = y * tileSize
                    
                    // Draw tile background based on terrain type
                    val tilePaint = Paint().apply {
                        when (tile.terrainType) {
                            0 -> color = Color.parseColor("#2D5016") // Grass/Ground
                            1 -> color = Color.parseColor("#4A4A4A") // Rock
                            2 -> color = Color.parseColor("#1E3A5F") // Water
                            3 -> color = Color.parseColor("#5C4033") // Dirt
                            else -> color = Color.parseColor("#2D5016")
                        }
                        style = Paint.Style.FILL
                        isAntiAlias = false
                    }
                    
                    terrainCanvas?.drawRect(
                        drawX,
                        drawY,
                        drawX + tileSize,
                        drawY + tileSize,
                        tilePaint
                    )
                    
                    // Draw grid lines
                    val gridPaint = Paint().apply {
                        color = Color.parseColor("#331A1A1D")
                        style = Paint.Style.STROKE
                        strokeWidth = 0.5f
                    }
                    
                    terrainCanvas?.drawRect(drawX, drawY, drawX + tileSize, drawY + tileSize, gridPaint)
                }
            }
        } catch (e: OutOfMemoryError) {
            // Handle memory issues gracefully
            terrainBitmap?.recycle()
            terrainBitmap = null
        }
    }
    
    override fun update() {
        // Minimap doesn't need per-frame updates for static elements
        // Could add pulsing effects for selected units here if desired
    }
    
    override fun draw(canvas: Canvas) {
        if (!isVisible) return
        
        val innerLeft = boundingBox.left + padding
        val innerTop = boundingBox.top + padding
        
        // Draw minimap background
        canvas.drawRoundRect(boundingBox, 8f, 8f, backgroundPaint)
        
        // Draw cached terrain
        terrainBitmap?.let { bitmap ->
            canvas.drawBitmap(
                bitmap,
                innerLeft,
                innerTop,
                null
            )
        }
        
        // Draw resource deposits
        drawResources(canvas, innerLeft, innerTop)
        
        // Draw units
        drawUnits(canvas, innerLeft, innerTop)
        
        // Draw viewport rectangle (shows current camera view)
        drawViewport(canvas, innerLeft, innerTop)
        
        // Draw border on top
        canvas.drawRoundRect(boundingBox, 8f, 8f, borderPaint)
    }
    
    /**
     * Draw resource deposits on the minimap.
     */
    private fun drawResources(canvas: Canvas, offsetX: Float, offsetY: Float) {
        for (resource in gameMap.resourceNodes) {
            val x = offsetX + (resource.x * scaleFactor)
            val y = offsetY + (resource.y * scaleFactor)
            val size = maxOf(3f, 6f * scaleFactor)
            
            canvas.drawRect(
                x - size,
                y - size,
                x + size,
                y + size,
                resourcePaint
            )
        }
    }
    
    /**
     * Draw all visible units on the minimap.
     */
    private fun drawUnits(canvas: Canvas, offsetX: Float, offsetY: Float) {
        val units = entityManager.getAllUnits()
        
        for (unit in units) {
            if (!unit.isAlive) continue
            
            val screenX = offsetX + (unit.position.x * scaleFactor)
            val screenY = offsetY + (unit.position.y * scaleFactor)
            
            // Determine if unit should be visible on minimap
            val shouldShow = when {
                unit.faction == gameState.playerFaction -> true
                gameState.isFogOfWarCleared(unit.position.x, unit.position.y) -> true
                else -> false
            }
            
            if (shouldShow) {
                val paint = when (unit.faction) {
                    gameState.playerFaction -> playerUnitPaint
                    FactionType.NEUTRAL -> neutralUnitPaint
                    else -> enemyUnitPaint
                }
                
                val dotSize = 2.5f
                
                canvas.drawCircle(screenX, screenY, dotSize, paint)
            }
        }
    }
    
    /**
     * Draw the camera viewport rectangle on the minimap.
     */
    private fun drawViewport(canvas: Canvas, offsetX: Float, offsetY: Float) {
        val camLeft = offsetX + (camera.position.x * scaleFactor)
        val camTop = offsetY + (camera.position.y * scaleFactor)
        val camRight = camLeft + (camera.viewportWidth * scaleFactor)
        val camBottom = camTop + (camera.viewportHeight * scaleFactor)
        
        canvas.drawRect(camLeft, camTop, camRight, camBottom, viewportPaint)
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled || !isVisible) return false
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (containsPoint(event.x, event.y)) {
                    lastTouchX = event.x
                    lastTouchY = event.y
                    handleMinimapTouch(event.x, event.y)
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (containsPoint(event.x, event.y)) {
                    // Optionally implement continuous tracking
                    // For now, just handle initial tap
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                if (containsPoint(event.x, event.y)) {
                    handleMinimapTouch(event.x, event.y)
                    return true
                }
            }
        }
        return false
    }
    
    /**
     * Handle touch input on the minimap to navigate the camera.
     */
    private fun handleMinimapTouch(touchX: Float, touchY: Float) {
        val innerLeft = boundingBox.left + padding
        val innerTop = boundingBox.top + padding
        
        // Convert minimap coordinates back to world coordinates
        val worldX = (touchX - innerLeft) / scaleFactor
        val worldY = (touchY - innerTop) / scaleFactor
        
        // Center the camera on the touched position
        val targetX = worldX - (camera.viewportWidth / 2)
        val targetY = worldY - (camera.viewportHeight / 2)
        
        // Clamp to map boundaries
        val tileSize = 32f // Standard tile size for the game
        val clampedX = targetX.coerceIn(0f, (gameMap.width * tileSize) - camera.viewportWidth)
        val clampedY = targetY.coerceIn(0f, (gameMap.height * tileSize) - camera.viewportHeight)
        
        // Move camera to new position
        camera.setPosition(clampedX, clampedY)
    }
    
    /**
     * Update the terrain bitmap (call if map changes).
     */
    fun updateTerrain() {
        terrainBitmap?.recycle()
        createTerrainBitmap()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        terrainBitmap?.recycle()
        terrainBitmap = null
    }
}
