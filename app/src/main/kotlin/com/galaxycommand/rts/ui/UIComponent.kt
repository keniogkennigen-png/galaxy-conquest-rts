package com.galaxycommand.rts.ui

import android.graphics.Canvas
import android.graphics.RectF

/**
 * Abstract base class for all UI components in the game HUD.
 * Provides common functionality for positioning, rendering, and touch handling.
 */
abstract class UIComponent {
    
    /**
     * The bounding box of this UI component on screen.
     */
    val boundingBox: RectF = RectF()
    
    /**
     * Whether this component is currently visible and should be rendered.
     */
    var isVisible: Boolean = true
    
    /**
     * Whether this component is currently enabled and responding to input.
     */
    var isEnabled: Boolean = true
    
    /**
     * Initialize the component with screen dimensions.
     * Called during HUD setup.
     */
    abstract fun initialize(screenWidth: Int, screenHeight: Int)
    
    /**
     * Update component state.
     * Called every frame before rendering.
     */
    abstract fun update()
    
    /**
     * Render the component to the canvas.
     * @param canvas The canvas to draw on
     */
    abstract fun draw(canvas: Canvas)
    
    /**
     * Handle touch events.
     * @param event The motion event to process
     * @return true if the event was handled by this component
     */
    abstract fun onTouchEvent(event: android.view.MotionEvent): Boolean
    
    /**
     * Check if a point is within this component's bounds.
     * @param x The x coordinate to check
     * @param y The y coordinate to check
     * @return true if the point is inside this component
     */
    fun containsPoint(x: Float, y: Float): Boolean {
        return boundingBox.contains(x, y)
    }
    
    /**
     * Set the position and size of this component.
     * @param left Left edge position
     * @param top Top edge position
     * @param right Right edge position
     * @param bottom Bottom edge position
     */
    protected fun setBounds(left: Float, top: Float, right: Float, bottom: Float) {
        boundingBox.set(left, top, right, bottom)
    }
    
    /**
     * Calculate relative position based on screen dimensions.
     * @param screenWidth Current screen width
     * @param screenHeight Current screen height
     * @param relativeX X position as fraction (0.0 to 1.0)
     * @param relativeY Y position as fraction (0.0 to 1.0)
     * @param width Component width
     * @param height Component height
     * @param anchorX Horizontal anchor (0=left, 0.5=center, 1=right)
     * @param anchorY Vertical anchor (0=top, 0.5=center, 1=bottom)
     */
    protected fun calculatePosition(
        screenWidth: Int,
        screenHeight: Int,
        relativeX: Float,
        relativeY: Float,
        width: Float,
        height: Float,
        anchorX: Float = 0f,
        anchorY: Float = 0f
    ): RectF {
        val x = relativeX * screenWidth
        val y = relativeY * screenHeight
        
        val left = x - (width * anchorX)
        val top = y - (height * anchorY)
        val right = left + width
        val bottom = top + height
        
        return RectF(left, top, right, bottom)
    }
}
