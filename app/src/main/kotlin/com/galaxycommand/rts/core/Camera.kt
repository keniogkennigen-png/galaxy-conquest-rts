package com.galaxycommand.rts.core

import android.view.MotionEvent
import kotlin.math.abs

/**
 * Camera class for viewport management.
 * Handles panning, zooming, and coordinate transformations.
 */
class Camera(
    private val mapWidth: Float,
    private val mapHeight: Float
) {

    companion object {
        // Default zoom levels
        const val MIN_ZOOM = 0.3f
        const val MAX_ZOOM = 2.0f
        const val DEFAULT_ZOOM = 0.8f

        // Default camera bounds
        const val MIN_X = 0f
        const val MIN_Y = 0f
    }

    // Camera position (top-left corner of viewport)
    var position = Vector2.new(0f, 0f)
        private set

    // Camera zoom level
    var zoom = DEFAULT_ZOOM
        private set

    // Viewport dimensions (set by surface size)
    var viewportWidth = 1920f
        private set
    var viewportHeight = 1080f
        private set

    // Target position for smooth camera movement
    private var targetPosition = Vector2.new(0f, 0f)
    private var isFollowingTarget = false

    // Touch gesture state
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false

    // Pinch zoom state
    private var initialPinchDistance = 0f
    private var initialZoom = DEFAULT_ZOOM

    // Camera bounds (considering zoom)
    val minX: Float
        get() = MIN_X

    val maxX: Float
        get() = (mapWidth * zoom) - viewportWidth

    val maxY: Float
        get() = (mapHeight * zoom) - viewportHeight

    /**
     * Set viewport size
     */
    fun setViewportSize(width: Int, height: Int) {
        viewportWidth = width.toFloat()
        viewportHeight = height.toFloat()

        // Clamp camera position to new bounds
        clampPosition()
    }

    /**
     * Convert screen coordinates to world coordinates
     */
    fun screenToWorld(screenX: Float, screenY: Float): Vector2 {
        return Vector2(
            position.x + screenX / zoom,
            position.y + screenY / zoom
        )
    }

    /**
     * Convert world coordinates to screen coordinates
     */
    fun worldToScreen(worldX: Float, worldY: Float): Vector2 {
        return Vector2(
            (worldX - position.x) * zoom,
            (worldY - position.y) * zoom
        )
    }

    /**
     * Convert world distance to screen distance
     */
    fun worldToScreenDistance(distance: Float): Float {
        return distance * zoom
    }

    /**
     * Convert screen distance to world distance
     */
    fun screenToWorldDistance(distance: Float): Float {
        return distance / zoom
    }

    /**
     * Center camera on a world position
     */
    fun centerOn(worldX: Float, worldY: Float, smooth: Boolean = true) {
        val targetX = worldX - (viewportWidth / 2) / zoom
        val targetY = worldY - (viewportHeight / 2) / zoom

        if (smooth) {
            targetPosition = Vector2.new(targetX, targetY)
            isFollowingTarget = true
        } else {
            position.x = targetX
            position.y = targetY
            clampPosition()
        }
    }

    /**
     * Center camera on a vector
     */
    fun centerOn(position: Vector2, smooth: Boolean = true) {
        centerOn(position.x, position.y, smooth)
    }

    /**
     * Pan camera by a world offset
     */
    fun pan(offsetX: Float, offsetY: Float) {
        position.x -= offsetX / zoom
        position.y -= offsetY / zoom
        clampPosition()
    }

    /**
     * Set camera position directly
     */
    fun setPosition(x: Float, y: Float) {
        position.x = x
        position.y = y
        clampPosition()
    }

    /**
     * Zoom in/out by a factor
     */
    fun zoomBy(factor: Float) {
        val newZoom = (zoom * factor).coerceIn(MIN_ZOOM, MAX_ZOOM)
        setZoom(newZoom)
    }

    /**
     * Set zoom level directly
     */
    fun setZoom(newZoom: Float) {
        // Calculate center point before zoom
        val centerX = position.x + viewportWidth / 2 / zoom
        val centerY = position.y + viewportHeight / 2 / zoom

        // Apply new zoom
        zoom = newZoom.coerceIn(MIN_ZOOM, MAX_ZOOM)

        // Adjust position to keep center point stable
        position.x = centerX - viewportWidth / 2 / zoom
        position.y = centerY - viewportHeight / 2 / zoom

        clampPosition()
    }

    /**
     * Handle touch down event
     */
    fun onTouchDown(x: Float, y: Float) {
        lastTouchX = x
        lastTouchY = y
        isDragging = false
    }

    /**
     * Handle touch move event
     */
    fun onTouchMove(x: Float, y: Float): Boolean {
        val deltaX = x - lastTouchX
        val deltaY = y - lastTouchY

        // Check if this is a drag gesture
        if (abs(deltaX) > 10 || abs(deltaY) > 10) {
            isDragging = true
        }

        if (isDragging) {
            pan(deltaX, deltaY)
        }

        lastTouchX = x
        lastTouchY = y
        return isDragging
    }

    /**
     * Handle pinch gesture for zooming
     */
    fun onPinch(startX1: Float, startY1: Float, startX2: Float, startY2: Float,
                currentX1: Float, currentY1: Float, currentX2: Float, currentY2: Float): Float {
        val initialDist = calculateDistance(startX1, startY1, startX2, startY2)
        val currentDist = calculateDistance(currentX1, currentY1, currentX2, currentY2)

        if (initialDist > 0) {
            val zoomFactor = currentDist / initialDist
            zoomBy(zoomFactor)
        }

        return zoom
    }

    /**
     * Start pinch zoom
     */
    fun startPinch(x1: Float, y1: Float, x2: Float, y2: Float) {
        initialPinchDistance = calculateDistance(x1, y1, x2, y2)
        initialZoom = zoom
    }

    /**
     * Continue pinch zoom
     */
    fun updatePinch(x1: Float, y1: Float, x2: Float, y2: Float) {
        val currentDistance = calculateDistance(x1, y1, x2, y2)
        if (initialPinchDistance > 0) {
            val newZoom = initialZoom * (currentDistance / initialPinchDistance)
            setZoom(newZoom)
        }
    }

    private fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    /**
     * Handle touch up event
     */
    fun onTouchUp(): Boolean {
        val wasDragging = isDragging
        isDragging = false
        return wasDragging
    }

    /**
     * Check if a world position is visible on screen
     */
    fun isWorldPositionVisible(worldX: Float, worldY: Float): Boolean {
        return worldX >= position.x &&
               worldX <= position.x + viewportWidth / zoom &&
               worldY >= position.y &&
               worldY <= position.y + viewportHeight / zoom
    }

    /**
     * Check if world rectangle is visible
     */
    fun isWorldRectangleVisible(x: Float, y: Float, width: Float, height: Float): Boolean {
        return x + width >= position.x &&
               x <= position.x + viewportWidth / zoom &&
               y + height >= position.y &&
               y <= position.y + viewportHeight / zoom
    }

    /**
     * Clamp camera position to valid bounds
     */
    private fun clampPosition() {
        position.x = position.x.coerceIn(MIN_X, maxX.coerceAtLeast(MIN_X))
        position.y = position.y.coerceIn(MIN_Y, maxY.coerceAtLeast(MIN_Y))
    }

    /**
     * Update camera (for smooth following)
     */
    fun update(deltaTime: Float) {
        if (isFollowingTarget) {
            // Smooth interpolation to target position
            val smoothing = 5f * deltaTime
            position.x += (targetPosition.x - position.x) * smoothing
            position.y += (targetPosition.y - position.y) * smoothing

            // Stop following when close enough
            if (Vector2.distance(position, targetPosition) < 5f) {
                isFollowingTarget = false
            }
        }
    }

    /**
     * Reset camera to default position
     */
    fun reset() {
        position = Vector2.new(0f, 0f)
        zoom = DEFAULT_ZOOM
        isFollowingTarget = false
    }

    /**
     * Get camera bounds rectangle
     */
    fun getBounds(): CameraBounds {
        return CameraBounds(
            left = position.x,
            top = position.y,
            right = position.x + viewportWidth / zoom,
            bottom = position.y + viewportHeight / zoom
        )
    }

    /**
     * Data class for camera bounds
     */
    data class CameraBounds(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    ) {
        val width: Float get() = right - left
        val height: Float get() = bottom - top
    }
}
