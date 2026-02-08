package com.galaxycommand.rts.core

import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Input Handler for touch-based mobile controls.
 * Handles selection, movement, and gesture recognition.
 */
class InputHandler(
    private val gameView: View,
    private val camera: Camera,
    private val callback: InputCallback
) : View.OnTouchListener {

    companion object {
        // Selection thresholds
        const val TAP_THRESHOLD = 20f
        const val DRAG_THRESHOLD = 30f
        const val DOUBLE_TAP_TIME = 300L

        // Multi-touch constants
        const val PINCH_THRESHOLD = 50f
    }

    // Touch state
    private var activePointers = mutableMapOf<Int, PointerInfo>()
    private var primaryPointerId = -1
    private var secondaryPointerId = -1

    // Selection state
    private var selectionStart = Vector2.ZERO
    private var isSelecting = false
    private var selectedUnits = mutableListOf<Long>()

    // Gesture state
    private var isPinchZooming = false
    private var pinchStartDistance = 0f

    // Double tap detection
    private var lastTapTime = 0L
    private var lastTapPosition = Vector2.ZERO

    // Callback interface
    interface InputCallback {
        fun onSingleTap(worldPosition: Vector2)
        fun onDoubleTap(worldPosition: Vector2)
        fun onSelectionStart(screenPosition: Vector2)
        fun onSelectionUpdate(screenStart: Vector2, screenEnd: Vector2)
        fun onSelectionEnd(selectedUnitIds: List<Long>)
        fun onMoveCommand(worldPosition: Vector2)
        fun onAttackCommand(worldPosition: Vector2, targetUnitId: Long?)
        fun onGatherCommand(worldPosition: Vector2, resourceId: Long)
        fun onBuildCommand(buildingType: String, worldPosition: Vector2)
        fun onAbilityCommand(abilityIndex: Int, worldPosition: Vector2)
        fun onCameraPan(deltaX: Float, deltaY: Float)
        fun onZoom(factor: Float)
        fun onMiniMapTap(screenPosition: Vector2)
    }

    data class PointerInfo(
        val id: Int,
        var x: Float,
        var y: Float,
        var startX: Float,
        var startY: Float
    )

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                handleActionDown(event)
            }
            MotionEvent.ACTION_MOVE -> {
                handleActionMove(event)
            }
            MotionEvent.ACTION_UP -> {
                handleActionUp(event)
            }
            MotionEvent.ACTION_CANCEL -> {
                handleActionCancel(event)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                handlePointerDown(event)
            }
            MotionEvent.ACTION_POINTER_UP -> {
                handlePointerUp(event)
            }
        }
        return true
    }

    private fun handleActionDown(event: MotionEvent) {
        val pointerId = event.getPointerId(event.actionIndex)
        val x = event.getX(event.actionIndex)
        val y = event.getY(event.actionIndex)

        val pointer = PointerInfo(
            id = pointerId,
            x = x,
            y = y,
            startX = x,
            startY = y
        )
        activePointers[pointerId] = pointer

        if (primaryPointerId == -1) {
            primaryPointerId = pointerId
            camera.onTouchDown(x, y)
            selectionStart = Vector2(x, y)
            isSelecting = false
            selectedUnits.clear()
        } else if (secondaryPointerId == -1) {
            secondaryPointerId = pointerId
            // Start pinch zoom
            if (activePointers.size == 2) {
                val pointers = activePointers.values.toList()
                val p1 = pointers[0]
                val p2 = pointers[1]
                camera.startPinch(p1.x, p1.y, p2.x, p2.y)
                isPinchZooming = true
            }
        }
    }

    private fun handleActionMove(event: MotionEvent) {
        // Handle primary pointer movement
        if (primaryPointerId != -1) {
            val pointerIndex = event.findPointerIndex(primaryPointerId)
            if (pointerIndex != -1) {
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)
                val pointer = activePointers[primaryPointerId]!!

                // Check for camera pan (edge drag or single finger drag on edge)
                if (isEdgeDrag(x, y)) {
                    camera.onTouchMove(x, y)
                    callback.onCameraPan(
                        camera.screenToWorldDistance(x - pointer.x),
                        camera.screenToWorldDistance(y - pointer.y)
                    )
                }
                // Check for selection drag
                else if (!isPinchZooming) {
                    val deltaX = abs(x - selectionStart.x)
                    val deltaY = abs(y - selectionStart.y)

                    if (deltaX > DRAG_THRESHOLD || deltaY > DRAG_THRESHOLD) {
                        isSelecting = true
                        callback.onSelectionStart(selectionStart)
                        callback.onSelectionUpdate(selectionStart, Vector2(x, y))
                    }
                }

                pointer.x = x
                pointer.y = y
            }
        }

        // Handle pinch zoom
        if (isPinchZooming && activePointers.size >= 2) {
            val pointers = activePointers.values.toList()
            if (pointers.size >= 2) {
                val p1 = pointers[0]
                val p2 = pointers[1]
                camera.updatePinch(p1.x, p1.y, p2.x, p2.y)
            }
        }
    }

    private fun handleActionUp(event: MotionEvent) {
        val pointerId = event.getPointerId(event.actionIndex)
        activePointers.remove(pointerId)

        if (pointerId == primaryPointerId) {
            camera.onTouchUp()

            val wasDragging = isSelecting

            // Process tap/click
            if (!wasDragging) {
                val pointer = activePointers.values.firstOrNull()
                if (pointer != null) {
                    val worldPos = camera.screenToWorld(pointer.x, pointer.y)
                    handleTap(worldPos, pointer.startX, pointer.startY)
                }
            } else {
                // Selection ended
                val pointer = activePointers.values.firstOrNull()
                if (pointer != null) {
                    val screenEnd = Vector2(pointer.x, pointer.y)
                    finishSelection(selectionStart, screenEnd)
                }
            }

            isSelecting = false
            selectedUnits.clear()
            primaryPointerId = if (secondaryPointerId != -1) {
                secondaryPointerId.also { secondaryPointerId = -1 }
            } else {
                -1
            }
        } else if (pointerId == secondaryPointerId) {
            secondaryPointerId = -1
            isPinchZooming = false
        }
    }

    private fun handleActionCancel(event: MotionEvent) {
        activePointers.clear()
        isSelecting = false
        isPinchZooming = false
        primaryPointerId = -1
        secondaryPointerId = -1
    }

    private fun handlePointerDown(event: MotionEvent) {
        // Already handled in ACTION_DOWN for multi-touch
    }

    private fun handlePointerUp(event: MotionEvent) {
        val pointerId = event.getPointerId(event.actionIndex)

        if (pointerId == secondaryPointerId) {
            secondaryPointerId = -1
            isPinchZooming = false

            // Promote next pointer to secondary
            if (activePointers.isNotEmpty()) {
                secondaryPointerId = activePointers.keys.first()
            }
        }
    }

    private fun handleTap(worldPosition: Vector2, screenStartX: Float, screenStartY: Float) {
        val currentTime = System.currentTimeMillis()
        val distFromLastTap = Vector2.distance(
            Vector2(screenStartX, screenStartY),
            lastTapPosition
        )

        // Check for double tap
        if (currentTime - lastTapTime < DOUBLE_TAP_TIME && distFromLastTap < TAP_THRESHOLD * 2) {
            callback.onDoubleTap(worldPosition)
            lastTapTime = 0
        } else {
            callback.onSingleTap(worldPosition)
            lastTapTime = currentTime
            lastTapPosition = Vector2(screenStartX, screenStartY)
        }
    }

    private fun finishSelection(screenStart: Vector2, screenEnd: Vector2) {
        val worldStart = camera.screenToWorld(screenStart.x, screenStart.y)
        val worldEnd = camera.screenToWorld(screenEnd.x, screenEnd.y)

        // Calculate selection bounds
        val minX = minOf(worldStart.x, worldEnd.x)
        val maxX = maxOf(worldStart.x, worldEnd.x)
        val minY = minOf(worldStart.y, worldEnd.y)
        val maxY = maxOf(worldStart.y, worldEnd.y)

        // Get units in selection area
        val selectedUnitIds = callback.onSelectionEnd(emptyList())

        // If single point selection, select unit under tap
        if (abs(screenEnd.x - screenStart.x) < TAP_THRESHOLD &&
            abs(screenEnd.y - screenStart.y) < TAP_THRESHOLD) {
            callback.onSingleTap(worldStart)
        }
    }

    private fun isEdgeDrag(x: Float, y: Float): Boolean {
        val edgeThreshold = 50f
        return x < edgeThreshold || x > gameView.width - edgeThreshold ||
               y < edgeThreshold || y > gameView.height - edgeThreshold
    }

    /**
     * Check if multi-touch is active
     */
    fun isMultiTouching(): Boolean = activePointers.size >= 2

    /**
     * Check if pinching
     */
    fun isPinching(): Boolean = isPinchZooming

    /**
     * Get current selection rectangle
     */
    fun getSelectionRect(): Pair<Vector2, Vector2>? {
        return if (isSelecting) {
            Pair(selectionStart, Vector2(
                activePointers[primaryPointerId]?.x ?: selectionStart.x,
                activePointers[primaryPointerId]?.y ?: selectionStart.y
            ))
        } else {
            null
        }
    }

    /**
     * Clear selection
     */
    fun clearSelection() {
        selectedUnits.clear()
        isSelecting = false
    }

    /**
     * Process keyboard commands (for optional keyboard support)
     */
    fun onKeyDown(keyCode: Int): Boolean {
        return when (keyCode) {
            android.view.KeyEvent.KEYCODE_A -> {
                // Select all visible units
                true
            }
            android.view.KeyEvent.KEYCODE_S -> {
                // Stop all units
                true
            }
            android.view.KeyEvent.KEYCODE_H -> {
                // Hold position
                true
            }
            android.view.KeyEvent.KEYCODE_P -> {
                // Pause game
                true
            }
            else -> false
        }
    }
}
