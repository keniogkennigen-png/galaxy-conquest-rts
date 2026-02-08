package com.galaxycommand.rts.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.galaxycommand.rts.core.GameEngine
import com.galaxycommand.rts.core.GameState
import com.galaxycommand.rts.core.Vector2
import com.galaxycommand.rts.factions.FactionType
import com.galaxycommand.rts.R

/**
 * Main Activity for the Galaxy Command RTS game.
 * Handles game initialization and main menu.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize game engine
        initGameEngine()

        // Setup UI
        setupUI()
    }

    private fun initGameEngine() {
        // Game engine is initialized on-demand when game starts
    }

    private fun setupUI() {
        // Start game button
        findViewById<android.widget.Button>(R.id.btnStartGame)?.setOnClickListener {
            startNewGame()
        }

        // Settings button
        findViewById<android.widget.Button>(R.id.btnSettings)?.setOnClickListener {
            showSettings()
        }

        // About button
        findViewById<android.widget.Button>(R.id.btnAbout)?.setOnClickListener {
            showAbout()
        }
    }

    private fun startNewGame() {
        // Get selected faction (default to Vanguard)
        val selectedFaction = intent.getStringExtra("faction")?.let {
            FactionType.fromName(it)
        } ?: FactionType.VANGUARD

        // Get difficulty (default to Medium)
        val difficulty = intent.getStringExtra("difficulty")?.let {
            when (it.lowercase()) {
                "easy" -> GameEngine.AIEngine.Difficulty.EASY
                "hard" -> GameEngine.AIEngine.Difficulty.HARD
                "insane" -> GameEngine.AIEngine.Difficulty.INSANE
                else -> GameEngine.AIEngine.Difficulty.MEDIUM
            }
        } ?: GameEngine.AIEngine.Difficulty.MEDIUM

        // Start game activity
        val intent = android.content.Intent(this, GameActivity::class.java).apply {
            putExtra("faction", selectedFaction.name)
            putExtra("difficulty", difficulty.name)
            putExtra("mapSeed", System.currentTimeMillis().toInt())
        }
        startActivity(intent)
    }

    private fun showSettings() {
        // Show settings dialog
        val intent = android.content.Intent(this, MenuActivity::class.java).apply {
            putExtra("menu_type", "settings")
        }
        startActivity(intent)
    }

    private fun showAbout() {
        // Show about dialog
        android.app.AlertDialog.Builder(this)
            .setTitle("Galaxy Command RTS")
            .setMessage("A StarCraft-inspired real-time strategy game for Android.\n\nFeatures:\n- 3 playable factions\n- Real-time combat\n- Resource management\n- AI opponent\n- Touch controls")
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        // Resume game if active
    }

    override fun onPause() {
        super.onPause()
        // Pause game if active
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cleanup
    }
}
