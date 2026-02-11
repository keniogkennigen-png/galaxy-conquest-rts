package com.galaxycommand.rts.ui

import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.galaxycommand.rts.R
import com.galaxycommand.rts.core.*
import com.galaxycommand.rts.factions.FactionType
import com.galaxycommand.rts.systems.AIEngine
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Game Activity - Main game screen with SurfaceView rendering.
 * Handles all game input, rendering, and gameplay logic.
 */
class GameActivity : AppCompatActivity(), InputHandler.InputCallback {

    // Game components
    private lateinit var gameEngine: GameEngine
    private lateinit var gameLoop: GameLoop
    private lateinit var camera: Camera
    private lateinit var inputHandler: InputHandler
    private lateinit var renderer: GameRenderer

    // UI Elements
    private lateinit var surfaceView: SurfaceView
    private lateinit var resourcesText: TextView
    private lateinit var timerText: TextView
    private lateinit var fpsText: TextView
    private lateinit var btnPause: Button

    // Game state
    private var playerFaction: FactionType = FactionType.VANGUARD
    private var difficulty: AIEngine.Difficulty = AIEngine.Difficulty.MEDIUM
    private var mapSeed: Int = 0

    // Thread safety
    private val isRunning = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreen immersive mode
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )

        setContentView(R.layout.activity_game)

        // Initialize SurfaceView
        surfaceView = findViewById(R.id.surfaceView)
        if (surfaceView == null) {
            throw IllegalStateException("SurfaceView not found in layout")
        }

        // Get intent extras
        intent.getStringExtra("faction")?.let {
            playerFaction = FactionType.fromName(it) ?: FactionType.VANGUARD
        }
        intent.getStringExtra("difficulty")?.let {
            difficulty = when (it.uppercase()) {
                "EASY" -> AIEngine.Difficulty.EASY
                "MEDIUM" -> AIEngine.Difficulty.MEDIUM
                "HARD" -> AIEngine.Difficulty.HARD
                "INSANE" -> AIEngine.Difficulty.INSANE
                else -> AIEngine.Difficulty.MEDIUM
            }
        }
        mapSeed = intent.getIntExtra("mapSeed", System.currentTimeMillis().toInt())

        // Initialize components
        initGameEngine()
        initCamera()
        initRenderer()
        initInput()
        initUI()

        // Start game loop
        startGameLoop()
    }

    private fun initGameEngine() {
        gameEngine = GameEngine.getInstance()
        
        // Get screen dimensions - use default values if not yet measured
        var screenWidth = window.decorView.width
        var screenHeight = window.decorView.height
        
        // Fallback to common mobile resolution if dimensions are 0
        if (screenWidth <= 0) screenWidth = 1080
        if (screenHeight <= 0) screenHeight = 1920
        
        // Initialize with screen dimensions for camera and HUD
        gameEngine.initializeWithScreen(playerFaction, screenWidth, screenHeight, mapSeed, difficulty)
    }

    private fun initCamera() {
        // Use the camera from game engine (already initialized with viewport size)
        camera = gameEngine.camera

        // Update viewport size based on surface if available
        surfaceView?.let {
            if (it.width > 0 && it.height > 0) {
                camera.setViewportSize(it.width, it.height)
            }
        }
    }

    private fun initRenderer() {
        renderer = GameRenderer(this, gameEngine)
        renderer.setCamera(camera)
    }

    private fun initInput() {
        inputHandler = InputHandler(surfaceView, camera, this)
        surfaceView.setOnTouchListener { view, event ->
            // First check if game engine handles the touch (for HUD)
            if (gameEngine.onTouchEvent(event)) {
                return@setOnTouchListener true
            }
            // Otherwise use the normal input handler
            inputHandler.onTouch(view, event)
        }
    }

    private fun initUI() {
        resourcesText = findViewById(R.id.textResources) 
        timerText = findViewById(R.id.textTimer)
        fpsText = findViewById(R.id.textFps)
        btnPause = findViewById(R.id.btnPause)

        // Verify all views are found
        if (resourcesText == null || timerText == null || fpsText == null || btnPause == null) {
            throw IllegalStateException("Failed to find required UI views in activity_game.xml")
        }

        btnPause.setOnClickListener {
            togglePause()
        }

        updateUI()
    }

    private fun startGameLoop() {
        isRunning.set(true)

        gameLoop = GameLoop(surfaceView, gameEngine)
        gameLoop.renderCallback = { alpha ->
            renderFrame(alpha)
        }
        gameLoop.fpsCallback = { fps ->
            runOnUiThread {
                fpsText.text = "$fps FPS"
            }
        }

        gameLoop.startLoop()
    }

    private fun renderFrame(alpha: Float) {
        try {
            val holder = surfaceView.holder
            if (!holder.surface.isValid) return

            val canvas = holder.lockCanvas() ?: return

            try {
                // Update camera
                camera.update(1f / 60f)

                // Render game
                renderer.render(canvas, surfaceView.width, surfaceView.height)
                
                // Render HUD on top of game
                gameEngine.drawHUD(canvas)
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateUI() {
        val state = gameEngine.getGameState()
        runOnUiThread {
            resourcesText.text = "M: ${state.minerals} | G: ${state.gas}"
            timerText.text = state.getFormattedTime()
        }
    }

    private fun togglePause() {
        if (gameEngine.isPaused()) {
            gameLoop.resumeLoop()
        } else {
            gameLoop.pauseLoop()
            showPauseMenu()
        }
    }

    private fun showPauseMenu() {
        // Show pause dialog
        android.app.AlertDialog.Builder(this)
            .setTitle("Paused")
            .setItems(arrayOf("Resume", "Restart", "Exit")) { _, which ->
                when (which) {
                    0 -> gameLoop.resumeLoop()
                    1 -> restartGame()
                    2 -> finish()
                }
            }
            .setOnCancelListener { gameLoop.resumeLoop() }
            .show()
    }

    private fun restartGame() {
        gameEngine.stop()
        gameLoop.stopLoop()

        // Reinitialize
        initGameEngine()
        initCamera()
        startGameLoop()
    }

    // InputHandler.InputCallback implementations
    override fun onSingleTap(worldPosition: Vector2) {
        // Check if touch was in UI area (don't select units behind UI)
        if (gameEngine.isTouchInUI(
                worldPosition.x + camera.position.x,
                worldPosition.y + camera.position.y
            )
        ) {
            return
        }
        
        // Try to select unit at position
        val unit = gameEngine.getUnitAtPosition(worldPosition)
        if (unit != null) {
            // Select unit
            gameEngine.getAllUnits().values.forEach { it.isSelected = false }
            unit.isSelected = true
        } else {
            // Check for resource
            val resource = gameEngine.getResourceAtPosition(worldPosition)
            if (resource != null) {
                // Show resource info or assign workers
            } else {
                // Move selected units
                val selectedUnits = gameEngine.getAllUnits().values.filter { it.isSelected }
                if (selectedUnits.isNotEmpty()) {
                    val unitIds = selectedUnits.map { it.id }
                    gameEngine.issueMoveCommand(unitIds, worldPosition)
                }
            }
        }
        updateUI()
    }

    override fun onDoubleTap(worldPosition: Vector2) {
        // Select all units of same type on screen
        val firstUnit = gameEngine.getUnitAtPosition(worldPosition)
        if (firstUnit != null) {
            val selectedUnits = gameEngine.getAllUnits().values.filter {
                it.isAlive &&
                it.ownerId == gameEngine.getGameState().currentPlayerId &&
                it.type == firstUnit.type
            }
            gameEngine.getAllUnits().values.forEach { it.isSelected = false }
            selectedUnits.forEach { it.isSelected = true }
        }
        updateUI()
    }

    override fun onSelectionStart(screenPosition: Vector2) {
        // Start selection rectangle
    }

    override fun onSelectionUpdate(screenStart: Vector2, screenEnd: Vector2) {
        // Update selection rectangle
    }

    override fun onSelectionEnd(selectedUnitIds: List<Long>) {
        // Finalize selection
    }

    override fun onMoveCommand(worldPosition: Vector2) {
        val selectedUnits = gameEngine.getAllUnits().values.filter { it.isSelected }
        if (selectedUnits.isNotEmpty()) {
            val unitIds = selectedUnits.map { it.id }
            gameEngine.issueMoveCommand(unitIds, worldPosition)
        }
    }

    override fun onAttackCommand(worldPosition: Vector2, targetUnitId: Long?) {
        val selectedUnits = gameEngine.getAllUnits().values.filter { it.isSelected && it.isCombatUnit }
        if (selectedUnits.isNotEmpty()) {
            targetUnitId?.let { targetId ->
                gameEngine.getEntity(targetId)?.let { target ->
                    if (target is com.galaxycommand.rts.entities.Unit) {
                        val unitIds = selectedUnits.map { it.id }
                        gameEngine.issueAttackCommand(unitIds, target)
                    }
                }
            }
        }
    }

    override fun onGatherCommand(worldPosition: Vector2, resourceId: Long) {
        val selectedUnits = gameEngine.getAllUnits().values.filter { it.isSelected && it.isWorker }
        if (selectedUnits.isNotEmpty()) {
            val resource = gameEngine.getEntity(resourceId) as? com.galaxycommand.rts.entities.Resource
            if (resource != null) {
                val unitIds = selectedUnits.map { it.id }
                // Note: This would call a gather command in GameEngine
            }
        }
    }

    override fun onBuildCommand(buildingType: String, worldPosition: Vector2) {
        // Handle building placement
    }

    override fun onAbilityCommand(abilityIndex: Int, worldPosition: Vector2) {
        // Handle unit abilities
    }

    override fun onCameraPan(deltaX: Float, deltaY: Float) {
        camera.pan(deltaX, deltaY)
    }

    override fun onZoom(factor: Float) {
        camera.zoomBy(factor)
    }

    override fun onMiniMapTap(screenPosition: Vector2) {
        // Center camera on minimap tap
    }

    override fun onResume() {
        super.onResume()
        if (!gameEngine.isPaused() && isRunning.get()) {
            gameLoop.resumeLoop()
        }
    }

    override fun onPause() {
        super.onPause()
        if (isRunning.get()) {
            gameLoop.pauseLoop()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning.set(false)
        gameEngine.stop()
        gameLoop.stopLoop()
    }
}
