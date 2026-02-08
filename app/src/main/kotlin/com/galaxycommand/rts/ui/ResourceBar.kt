package com.galaxycommand.rts.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import com.galaxycommand.rts.core.GameState

/**
 * Resource bar component that displays current player resources and income rates.
 * Shows minerals and energy with real-time updates.
 */
class ResourceBar(private val gameState: GameState) : UIComponent() {
    
    // Paint objects for rendering
    private val backgroundPaint = Paint().apply {
        color = Color.parseColor("#CC1A1A1D")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val borderPaint = Paint().apply {
        color = Color.parseColor("#00D4FF")
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }
    
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 32f
        typeface = Typeface.MONOSPACE
        isAntiAlias = true
    }
    
    private val incomePaint = Paint().apply {
        color = Color.parseColor("#4CAF50")
        textSize = 24f
        typeface = Typeface.MONOSPACE
        isAntiAlias = true
    }
    
    private val mineralIconPaint = Paint().apply {
        color = Color.parseColor("#00BFFF")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val energyIconPaint = Paint().apply {
        color = Color.parseColor("#FFD700")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    // Component dimensions
    private var componentWidth = 300f
    private var componentHeight = 80f
    private var iconSize = 24f
    private var padding = 16f
    private var textY = 0f
    
    // Reusable string buffers to avoid garbage collection
    private val mineralText = StringBuilder(32)
    private val energyText = StringBuilder(32)
    private val incomeText = StringBuilder(32)
    
    override fun initialize(screenWidth: Int, screenHeight: Int) {
        // Scale dimensions based on screen size
        val scaleFactor = minOf(screenWidth / 1080f, screenHeight / 1920f).coerceIn(0.6f, 1.5f)
        
        componentWidth = 320f * scaleFactor
        componentHeight = 72f * scaleFactor
        iconSize = 20f * scaleFactor
        padding = 12f * scaleFactor
        textPaint.textSize = 28f * scaleFactor
        incomePaint.textSize = 20f * scaleFactor
        
        // Position in top-left corner with margin
        val margin = padding
        setBounds(
            margin,
            margin,
            margin + componentWidth,
            margin + componentHeight
        )
        
        // Calculate text baseline position
        textY = boundingBox.centerY() + (textPaint.textSize / 3)
    }
    
    override fun update() {
        // Resource bar is mostly static, but could animate income indicators
        // if we wanted pulsing effects when resources increase
    }
    
    override fun draw(canvas: Canvas) {
        if (!isVisible) return
        
        // Draw background panel
        canvas.drawRoundRect(boundingBox, 8f, 8f, backgroundPaint)
        
        // Draw border
        canvas.drawRoundRect(boundingBox, 8f, 8f, borderPaint)
        
        // Draw resource icons and values
        var currentX = boundingBox.left + padding
        
        // Draw Minerals Icon (Diamond shape)
        drawDiamondIcon(canvas, currentX, boundingBox.centerY(), iconSize, mineralIconPaint)
        
        // Draw Minerals Value
        mineralText.clear()
        val minerals = gameState.getPlayerMinerals()
        mineralText.append(String.format("%,d", minerals))
        canvas.drawText(mineralText.toString(), currentX + iconSize + padding, textY, textPaint)
        
        currentX += componentWidth * 0.45f
        
        // Draw Energy Icon (Lightning bolt simplified as triangle)
        drawTriangleIcon(canvas, currentX, boundingBox.centerY(), iconSize, energyIconPaint)
        
        // Draw Energy Value (current/capacity format)
        energyText.clear()
        val energy = gameState.getPlayerEnergy()
        val energyCap = gameState.getEnergyCapacity()
        energyText.append(String.format("%d/%d", energy, energyCap))
        canvas.drawText(energyText.toString(), currentX + iconSize + padding, textY, textPaint)
        
        // Draw income rate at bottom
        val incomeY = boundingBox.bottom - padding * 0.5f
        incomeText.clear()
        val incomeRate = gameState.getIncomeRate()
        if (incomeRate > 0) {
            incomeText.append("+").append(incomeRate).append("/min")
            canvas.drawText(incomeText.toString(), currentX + iconSize + padding, incomeY, incomePaint)
        } else if (incomeRate < 0) {
            incomeText.append(incomeRate).append("/min")
            incomePaint.color = Color.parseColor("#FF5555")
            canvas.drawText(incomeText.toString(), currentX + iconSize + padding, incomeY, incomePaint)
            incomePaint.color = Color.parseColor("#4CAF50")
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Resource bar doesn't respond to touch - it's display only
        return false
    }
    
    /**
     * Draw a diamond-shaped icon for minerals.
     */
    private fun drawDiamondIcon(canvas: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
        val half = size / 2
        val path = android.graphics.Path().apply {
            moveTo(cx, cy - half)      // Top
            lineTo(cx + half, cy)      // Right
            lineTo(cx, cy + half)      // Bottom
            lineTo(cx - half, cy)      // Left
            close()
        }
        canvas.drawPath(path, paint)
    }
    
    /**
     * Draw a triangle-shaped icon for energy.
     */
    private fun drawTriangleIcon(canvas: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
        val half = size / 2
        val path = android.graphics.Path().apply {
            moveTo(cx, cy - half)      // Top
            lineTo(cx + half, cy + half * 0.7f)  // Bottom Right
            lineTo(cx - half, cy + half * 0.7f)  // Bottom Left
            close()
        }
        canvas.drawPath(path, paint)
    }
}
