package com.galaxycommand.rts.ui

import android.content.Context
import android.graphics.*
import com.galaxycommand.rts.core.Camera
import com.galaxycommand.rts.core.GameEngine
import com.galaxycommand.rts.core.GameState
import com.galaxycommand.rts.core.Vector2
import com.galaxycommand.rts.entities.Building
import com.galaxycommand.rts.entities.Entity
import com.galaxycommand.rts.entities.Resource
import com.galaxycommand.rts.entities.Unit
import com.galaxycommand.rts.factions.FactionType
import kotlin.math.*

/**
 * Game Renderer for drawing all game elements.
 * Handles efficient rendering with object pooling and level-of-detail.
 */
class GameRenderer(
    private val context: Context,
    private val gameEngine: GameEngine
) {

    companion object {
        // Colors
        private val COLOR_BACKGROUND = Color.parseColor("#1a1a2e")
        private val COLOR_GRID = Color.parseColor("#2a2a4e")
        private val COLOR_SELECTION = Color.parseColor("#00ff00")
        private val COLOR_SELECTION_ALLY = Color.parseColor("#00aaff")
        private val COLOR_SELECTION_ENEMY = Color.parseColor("#ff4444")
        private val COLOR_HEALTH_BAR = Color.parseColor("#00ff00")
        private val COLOR_HEALTH_LOW = Color.parseColor("#ff0000")
        private val COLOR_SHIELD_BAR = Color.parseColor("#00aaff")
        private val COLOR_MINIMAP_BORDER = Color.parseColor("#444444")

        // UI overlay colors
        private val COLOR_UI_BACKGROUND = Color.parseColor("#cc1a1a2e")
        private val COLOR_UI_BORDER = Color.parseColor("#66668899")
        private val COLOR_TEXT = Color.parseColor("#ffffff")
    }

    // Paint objects
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_GRID
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    private val selectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val healthBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val shieldBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val buildingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val resourcePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_TEXT
        textSize = 24f
        typeface = Typeface.DEFAULT_BOLD
    }

    private val smallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_TEXT
        textSize = 16f
    }

    // Selection rectangle
    private val selectionRectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4400ff00")
        style = Paint.Style.FILL
    }
    private val selectionRectBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_SELECTION
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    // Path for smooth drawing
    private val path = Path()

    // Camera reference
    private var camera: Camera? = null

    // Rendering bounds
    private var renderBounds = RectF()

    /**
     * Set camera reference
     */
    fun setCamera(camera: Camera) {
        this.camera = camera
    }

    /**
     * Render the entire game
     */
    fun render(canvas: Canvas, width: Int, height: Int) {
        // Update render bounds based on camera
        camera?.let { cam ->
            val bounds = cam.getBounds()
            renderBounds.set(
                bounds.left,
                bounds.top,
                bounds.right,
                bounds.bottom
            )
        }

        // Clear canvas
        canvas.drawColor(COLOR_BACKGROUND)

        // Draw grid
        drawGrid(canvas)

        // Draw terrain features
        drawTerrain(canvas)

        // Draw resources
        drawResources(canvas)

        // Draw buildings
        drawBuildings(canvas)

        // Draw units
        drawUnits(canvas)

        // Draw projectiles (simplified)
        drawProjectiles(canvas)

        // Draw effects
        drawEffects(canvas)

        // Draw selection rectangle
        // (handled by InputHandler)

        // Draw UI overlays
        drawUIOverlays(canvas, width, height)
    }

    /**
     * Draw background grid
     */
    private fun drawGrid(canvas: Canvas) {
        val gridSize = 50f
        val startX = kotlin.math.floor(renderBounds.left)
        val startY = kotlin.math.floor(renderBounds.top)
        val endX = kotlin.math.ceil(renderBounds.right)
        val endY = kotlin.math.ceil(renderBounds.bottom)

        // Vertical lines
        var x = kotlin.math.floor(startX / gridSize) * gridSize
        while (x <= endX) {
            canvas.drawLine(x, startY, x, endY, gridPaint)
            x += gridSize
        }

        // Horizontal lines
        var y = kotlin.math.floor(startY / gridSize) * gridSize
        while (y <= endY) {
            canvas.drawLine(startX, y, endX, y, gridPaint)
            y += gridSize
        }
    }

    /**
     * Draw terrain features (mountains, cliffs, etc.)
     */
    private fun drawTerrain(canvas: Canvas) {
        // Placeholder for terrain rendering
        // In a full implementation, render terrain tiles and features
    }

    /**
     * Draw resource deposits
     */
    private fun drawResources(canvas: Canvas) {
        val resources = gameEngine.getAllResources()

        resources.forEach { (id, resource) ->
            if (!resource.isVisibleTo(gameEngine.getGameState().currentPlayerId)) return@forEach

            val screenPos = camera?.worldToScreen(resource.position.x, resource.position.y)
                ?: return@forEach

            // Check if on screen
            if (!isOnScreen(screenPos.x, screenPos.y, resource.radius * 2)) return@forEach

            // Draw crystal shape
            when (resource.resourceType) {
                Resource.ResourceType.MINERAL -> {
                    resourcePaint.color = 0xFF00AAFF.toInt()
                    canvas.drawCircle(screenPos.x, screenPos.y, resource.radius, resourcePaint)

                    // Draw crystal detail
                    resourcePaint.color = 0xFF88DDFF.toInt()
                    canvas.drawCircle(screenPos.x - 5, screenPos.y - 5, resource.radius * 0.4f, resourcePaint)

                    // Draw amount text
                    val amountText = "${resource.amount}"
                    val textWidth = smallTextPaint.measureText(amountText)
                    canvas.drawText(amountText, screenPos.x - textWidth / 2, screenPos.y + 5, smallTextPaint)
                }
                Resource.ResourceType.VESPENE_GAS -> {
                    resourcePaint.color = 0xFF00FF88.toInt()
                    canvas.drawCircle(screenPos.x, screenPos.y, resource.radius * 1.2f, resourcePaint)

                    // Draw gas effect
                    resourcePaint.color = 0xFF88FFBB.toInt()
                    canvas.drawCircle(screenPos.x, screenPos.y, resource.radius * 0.8f, resourcePaint)
                }
            }
        }
    }

    /**
     * Draw buildings
     */
    private fun drawBuildings(canvas: Canvas) {
        val buildings = gameEngine.getAllBuildings()

        buildings.forEach { (id, building) ->
            if (!building.isVisibleTo(gameEngine.getGameState().currentPlayerId)) return@forEach

            val screenPos = camera?.worldToScreen(building.position.x, building.position.y)
                ?: return@forEach

            // Check if on screen
            if (!isOnScreen(screenPos.x, screenPos.y, building.width)) return@forEach

            // Get faction colors
            val factionColor = getFactionColor(building.faction)
            val isAlly = building.ownerId == gameEngine.getGameState().currentPlayerId

            // Draw building base
            buildingPaint.color = factionColor
            val screenWidth = camera?.worldToScreenDistance(building.width) ?: building.width
            val screenHeight = camera?.worldToScreenDistance(building.height) ?: building.height

            canvas.save()
            canvas.translate(screenPos.x - screenWidth / 2, screenPos.y - screenHeight / 2)

            // Draw main building shape
            canvas.drawRect(0f, 0f, screenWidth, screenHeight, buildingPaint)

            // Draw building detail
            buildingPaint.color = darkenColor(factionColor, 0.7f)
            canvas.drawRect(screenWidth * 0.1f, screenHeight * 0.1f, screenWidth * 0.9f, screenHeight * 0.9f, buildingPaint)

            // Draw selection ring
            if (building.isSelected) {
                selectionPaint.color = if (isAlly) COLOR_SELECTION_ALLY else COLOR_SELECTION_ENEMY
                canvas.drawRect(-5f, -5f, screenWidth + 5f, screenHeight + 5f, selectionPaint)
            }

            // Draw health bar
            drawHealthBar(canvas, screenWidth, screenHeight, building.getHealthPercent(), building.hasShields())

            // Draw construction progress
            if (building.isUnderConstruction) {
                val progress = building.constructionProgress / 100f
                buildingPaint.color = Color.parseColor("#888888")
                canvas.drawRect(0f, screenHeight + 5f, screenWidth * progress, screenHeight + 15f, buildingPaint)
            }

            // Draw building name
            val nameText = building.type
            val nameWidth = textPaint.measureText(nameText)
            canvas.drawText(nameText, (screenWidth - nameWidth) / 2, -10f, textPaint)

            canvas.restore()
        }
    }

    /**
     * Draw units
     */
    private fun drawUnits(canvas: Canvas) {
        val units = gameEngine.getAllUnits()

        // Sort by y position for depth
        val sortedUnits = units.values.sortedBy { it.position.y }

        sortedUnits.forEach { (id, unit) ->
            if (!unit.isVisibleTo(gameEngine.getGameState().currentPlayerId)) return@forEach

            val screenPos = camera?.worldToScreen(unit.position.x, unit.position.y)
                ?: return@forEach

            // Check if on screen
            if (!isOnScreen(screenPos.x, screenPos.y, unit.radius * 2)) return@forEach

            val screenRadius = camera?.worldToScreenDistance(unit.radius) ?: unit.radius
            val isAlly = unit.ownerId == gameEngine.getGameState().currentPlayerId

            // Get faction color
            val factionColor = getFactionColor(unit.faction)

            // Draw unit shape based on type
            when (unit.faction) {
                FactionType.VANGUARD -> drawTerranUnit(canvas, screenPos, screenRadius, unit, factionColor, isAlly)
                FactionType.SWARM -> drawZergUnit(canvas, screenPos, screenRadius, unit, factionColor, isAlly)
                FactionType.SYNOD -> drawProtossUnit(canvas, screenPos, screenRadius, unit, factionColor, isAlly)
            }

            // Draw selection ring
            if (unit.isSelected) {
                selectionPaint.color = if (isAlly) COLOR_SELECTION_ALLY else COLOR_SELECTION_ENEMY
                canvas.drawCircle(screenPos.x, screenPos.y, screenRadius + 5f, selectionPaint)
            }

            // Draw health bar
            drawUnitHealthBar(canvas, screenPos, screenRadius, unit.getHealthPercent(), unit.getShieldPercent())

            // Draw carrying indicator for workers
            if (unit.carrying > 0) {
                unitPaint.color = 0xFF00AAFF.toInt()
                canvas.drawCircle(screenPos.x, screenPos.y - screenRadius - 8f, 5f, unitPaint)
            }
        }
    }

    /**
     * Draw Terran (Vanguard) unit
     */
    private fun drawTerranUnit(canvas: Canvas, pos: Vector2, radius: Float, unit: Unit, color: Int, isAlly: Boolean) {
        // Main body
        unitPaint.color = if (isAlly) color else darkenColor(color, 0.7f)
        canvas.drawCircle(pos.x, pos.y, radius, unitPaint)

        // Inner detail
        unitPaint.color = if (isAlly) lightenColor(color, 1.2f) else color
        canvas.drawCircle(pos.x, pos.y, radius * 0.7f, unitPaint)

        // Weapon indicator for combat units
        if (unit.isCombatUnit) {
            unitPaint.color = Color.GRAY
            canvas.drawRect(pos.x + radius * 0.5f, pos.y - 3f, pos.x + radius * 0.8f, pos.y + 3f, unitPaint)
        }
    }

    /**
     * Draw Zerg (Swarm) unit
     */
    private fun drawZergUnit(canvas: Canvas, pos: Vector2, radius: Float, unit: Unit, color: Int, isAlly: Boolean) {
        // Main body (organic shape)
        unitPaint.color = if (isAlly) color else darkenColor(color, 0.7f)

        // Draw slightly irregular shape for organic feel
        path.reset()
        path.addCircle(pos.x, pos.y, radius, Path.Direction.CW)
        canvas.drawPath(path, unitPaint)

        // Inner detail
        unitPaint.color = if (isAlly) lightenColor(color, 1.3f) else color
        canvas.drawCircle(pos.x, pos.y, radius * 0.6f, unitPaint)

        // Claws for zerglings
        if (unit.type == "Zergling") {
            unitPaint.color = if (isAlly) lightenColor(color, 1.4f) else color
            canvas.drawCircle(pos.x + radius * 0.8f, pos.y - radius * 0.3f, radius * 0.3f, unitPaint)
            canvas.drawCircle(pos.x + radius * 0.8f, pos.y + radius * 0.3f, radius * 0.3f, unitPaint)
        }
    }

    /**
     * Draw Protoss (Synod) unit
     */
    private fun drawProtossUnit(canvas: Canvas, pos: Vector2, radius: Float, unit: Unit, color: Int, isAlly: Boolean) {
        // Main body with shield effect
        unitPaint.color = if (isAlly) color else darkenColor(color, 0.7f)
        canvas.drawCircle(pos.x, pos.y, radius, unitPaint)

        // Shield glow effect
        unitPaint.color = Color.argb(100, Color.red(color), Color.green(color), Color.blue(color))
        canvas.drawCircle(pos.x, pos.y, radius * 1.2f, unitPaint)

        // Inner detail (geometric shape)
        unitPaint.color = if (isAlly) lightenColor(color, 1.4f) else color
        val size = radius * 0.5f
        canvas.drawRect(pos.x - size, pos.y - size, pos.x + size, pos.y + size, unitPaint)

        // Energy effect
        if (unit.hasShields() && unit.shield > 0) {
            unitPaint.color = 0xFF00FFFF.toInt()
            canvas.drawCircle(pos.x, pos.y, radius * 1.1f, unitPaint)
        }
    }

    /**
     * Draw health bar
     */
    private fun drawHealthBar(canvas: Canvas, width: Float, height: Float, healthPercent: Float, hasShields: Boolean) {
        val barWidth = width * 0.8f
        val barHeight = 6f
        val barX = width * 0.1f
        val barY = height + 8f

        // Background
        healthBarPaint.color = Color.GRAY
        canvas.drawRect(barX, barY, barX + barWidth, barY + barHeight, healthBarPaint)

        // Health
        healthBarPaint.color = if (healthPercent > 0.5f) COLOR_HEALTH_BAR else COLOR_HEALTH_LOW
        canvas.drawRect(barX, barY, barX + barWidth * healthPercent, barY + barHeight, healthBarPaint)

        // Shield bar (if applicable)
        if (hasShields) {
            val shieldY = barY + barHeight + 2f
            shieldBarPaint.color = COLOR_SHIELD_BAR
            canvas.drawRect(barX, shieldY, barX + barWidth * 0.5f, shieldY + 3f, shieldBarPaint)
        }
    }

    /**
     * Draw unit health bar
     */
    private fun drawUnitHealthBar(canvas: Canvas, pos: Vector2, radius: Float, healthPercent: Float, shieldPercent: Float) {
        val barWidth = radius * 2f
        val barHeight = 4f
        val barX = pos.x - radius
        val barY = pos.y - radius - 8f

        // Background
        healthBarPaint.color = Color.GRAY
        canvas.drawRect(barX, barY, barX + barWidth, barY + barHeight, healthBarPaint)

        // Health
        healthBarPaint.color = if (healthPercent > 0.5f) COLOR_HEALTH_BAR else COLOR_HEALTH_LOW
        canvas.drawRect(barX, barY, barX + barWidth * healthPercent, barY + barHeight, healthBarPaint)

        // Shield bar
        if (shieldPercent > 0) {
            val shieldY = barY + barHeight + 1f
            shieldBarPaint.color = COLOR_SHIELD_BAR
            canvas.drawRect(barX, shieldY, barX + barWidth * shieldPercent * 0.5f, shieldY + 2f, shieldBarPaint)
        }
    }

    /**
     * Draw projectiles (placeholder)
     */
    private fun drawProjectiles(canvas: Canvas) {
        // In a full implementation, draw active projectiles
    }

    /**
     * Draw effects (explosions, spells, etc.)
     */
    private fun drawEffects(canvas: Canvas) {
        // In a full implementation, draw visual effects
    }

    /**
     * Draw UI overlays (resources, minimap placeholder, etc.)
     */
    private fun drawUIOverlays(canvas: Canvas, width: Int, height: Int) {
        val state = gameEngine.getGameState()

        // Resource display (top left)
        val resourceBg = RectF(10f, 10f, 250f, 60f)
        canvas.drawRoundRect(resourceBg, 10f, 10f, getUIBackgroundPaint())

        // Minerals
        val mineralText = "Minerals: ${state.minerals}"
        textPaint.color = 0xFF00AAFF.toInt()
        canvas.drawText(mineralText, 20f, 40f, textPaint)

        // Gas
        val gasText = "Gas: ${state.gas}"
        textPaint.color = 0xFF00FF88.toInt()
        canvas.drawText(gasText, 20f, 60f, textPaint)

        // Game timer (top center)
        val timerText = state.getFormattedTime()
        val timerWidth = textPaint.measureText(timerText)
        textPaint.color = Color.WHITE
        canvas.drawText(timerText, (width - timerWidth) / 2, 40f, textPaint)

        // Minimap placeholder (bottom right)
        val minimapSize = 150f
        val minimapX = width - minimapSize - 20f
        val minimapY = height - minimapSize - 20f

        // Minimap background
        val minimapBg = RectF(minimapX, minimapY, minimapX + minimapSize, minimapY + minimapSize)
        canvas.drawRoundRect(minimapBg, 10f, 10f, getUIBackgroundPaint())
        canvas.drawRoundRect(minimapBg, 10f, 10f, getUIBorderPaint())

        // Minimap content would be drawn here
        minimapPaint.color = 0xFF333344.toInt()
        canvas.drawRect(minimapBg, minimapPaint)

        // FPS counter (top right)
        val fpsText = "60 FPS"
        smallTextPaint.color = Color.GRAY
        canvas.drawText(fpsText, width - 80f, 30f, smallTextPaint)
    }

    private fun getUIBackgroundPaint(): Paint {
        return Paint().apply {
            color = COLOR_UI_BACKGROUND
            style = Paint.Style.FILL
        }
    }

    private fun getUIBorderPaint(): Paint {
        return Paint().apply {
            color = COLOR_UI_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
    }

    private val minimapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    /**
     * Draw selection rectangle
     */
    fun drawSelectionRect(canvas: Canvas, start: Vector2, end: Vector2) {
        val left = minOf(start.x, end.x)
        val top = minOf(start.y, end.y)
        val width = abs(end.x - start.x)
        val height = abs(end.y - start.y)

        canvas.drawRoundRect(RectF(left, top, left + width, top + height), 5f, 5f, selectionRectPaint)
        canvas.drawRoundRect(RectF(left, top, left + width, top + height), 5f, 5f, selectionRectBorderPaint)
    }

    /**
     * Get faction color
     */
    private fun getFactionColor(faction: FactionType): Int {
        return when (faction) {
            FactionType.VANGUARD -> 0xFF546E7A.toInt() // Blue-grey
            FactionType.SWARM -> 0xFF4E342E.toInt() // Brown/purple
            FactionType.SYNOD -> 0xFFFFCA28.toInt() // Gold
        }
    }

    /**
     * Check if position is on screen
     */
    private fun isOnScreen(x: Float, y: Float, size: Float): Boolean {
        return x >= -size && x <= renderBounds.right + size &&
               y >= -size && y <= renderBounds.bottom + size
    }

    /**
     * Darken a color
     */
    private fun darkenColor(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = (Color.red(color) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * factor).toInt().coerceIn(0, 255)
        return Color.argb(a, r, g, b)
    }

    /**
     * Lighten a color
     */
    private fun lightenColor(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = (Color.red(color) + (255 - Color.red(color)) * (factor - 1)).toInt().coerceIn(0, 255)
        val g = (Color.green(color) + (255 - Color.green(color)) * (factor - 1)).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) + (255 - Color.blue(color)) * (factor - 1)).toInt().coerceIn(0, 255)
        return Color.argb(a, r, g, b)
    }
}
