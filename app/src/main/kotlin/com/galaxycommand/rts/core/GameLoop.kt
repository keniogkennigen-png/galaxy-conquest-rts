package com.galaxycommand.rts.core

import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.abs

/**
 * Main Game Loop that manages the update and render cycles.
 * Uses a fixed timestep with interpolation for smooth gameplay.
 */
class GameLoop(
    private val surfaceView: SurfaceView,
    private val gameEngine: GameEngine
) : Thread(), SurfaceHolder.Callback {

    companion object {
        // Target FPS
        const val TARGET_FPS = 60
        const val TARGET_FRAME_TIME = 1000L / TARGET_FPS

        // Fixed timestep for physics (60 updates per second)
        const val PHYSICS_STEP = 1f / 60f
    }

    // Thread control
    @Volatile
    private var isRunning = false
    private var isPaused = false

    // Lock object for wait/notify (using Object for Java synchronization)
    private val pauseLock = java.lang.Object()

    // Time tracking
    private var previousTime = 0L
    private var accumulator = 0f
    private var currentTime = 0L
    private var frameCount = 0
    private var lastFpsUpdate = 0L
    private var currentFps = 0

    // Interpolation for smooth rendering
    private var alpha = 0f

    // Callbacks
    var renderCallback: ((Float) -> Unit)? = null
    var fpsCallback: ((Int) -> Unit)? = null

    // Surface holder for drawing
    private val surfaceHolder: SurfaceHolder = surfaceView.holder

    init {
        surfaceHolder.addCallback(this)
        isRunning = false
    }

    override fun run() {
        previousTime = System.currentTimeMillis()
        lastFpsUpdate = previousTime

        while (isRunning) {
            if (isPaused) {
                synchronized(pauseLock) {
                    while (isPaused && isRunning) {
                        pauseLock.wait(100)
                    }
                }
                previousTime = System.currentTimeMillis()
                continue
            }

            currentTime = System.currentTimeMillis()
            val frameTime = (currentTime - previousTime) / 1000f
            previousTime = currentTime

            // Prevent spiral of death from large frame times
            if (frameTime > 0.25f) {
                accumulator = 0f
            } else {
                accumulator += frameTime
            }

            // Fixed timestep update
            while (accumulator >= PHYSICS_STEP) {
                gameEngine.update(PHYSICS_STEP)
                accumulator -= PHYSICS_STEP
            }

            // Calculate interpolation factor
            alpha = accumulator / PHYSICS_STEP

            // Render
            renderCallback?.invoke(alpha)

            // FPS calculation
            frameCount++
            if (currentTime - lastFpsUpdate >= 1000) {
                currentFps = frameCount
                frameCount = 0
                lastFpsUpdate = currentTime
                fpsCallback?.invoke(currentFps)
            }

            // Frame rate limiting
            val sleepTime = TARGET_FRAME_TIME - (System.currentTimeMillis() - currentTime)
            if (sleepTime > 0) {
                try {
                    sleep(sleepTime)
                } catch (e: InterruptedException) {
                    // Ignore
                }
            }
        }
    }

    /**
     * Start the game loop
     */
    fun startLoop() {
        if (!isRunning) {
            isRunning = true
            isPaused = false
            start()
        }
    }

    /**
     * Stop the game loop
     */
    fun stopLoop() {
        isRunning = false
        interrupt()
    }

    /**
     * Pause the game loop
     */
    fun pauseLoop() {
        isPaused = true
        gameEngine.pause()
    }

    /**
     * Resume the game loop
     */
    fun resumeLoop() {
        isPaused = false
        gameEngine.resume()
        synchronized(pauseLock) {
            pauseLock.notifyAll()
        }
    }

    /**
     * Check if game loop is running
     */
    fun isGameRunning(): Boolean = isRunning

    /**
     * Check if game loop is paused
     */
    fun isGamePaused(): Boolean = isPaused

    /**
     * Get current FPS
     */
    fun getCurrentFps(): Int = currentFps

    /**
     * Get interpolation alpha
     */
    fun getAlpha(): Float = alpha

    // SurfaceHolder.Callback implementations
    override fun surfaceCreated(holder: SurfaceHolder) {
        // Surface created, can start drawing
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Surface size changed, update viewport if needed
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // Surface destroyed, pause the loop
        pauseLoop()
    }
}
