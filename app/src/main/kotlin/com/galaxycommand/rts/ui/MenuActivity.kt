package com.galaxycommand.rts.ui

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.RadioButton
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.galaxycommand.rts.R
import com.galaxycommand.rts.factions.FactionType

/**
 * Menu Activity for settings and faction selection.
 */
class MenuActivity : AppCompatActivity() {

    private var menuType: String = "main"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        menuType = intent.getStringExtra("menu_type") ?: "main"

        when (menuType) {
            "main" -> setContentView(R.layout.activity_menu)
            "settings" -> setContentView(R.layout.activity_settings)
            "faction" -> setContentView(R.layout.activity_faction_select)
            else -> setContentView(R.layout.activity_menu)
        }

        setupUI()
    }

    private fun setupUI() {
        when (menuType) {
            "main" -> setupMainMenu()
            "settings" -> setupSettings()
            "faction" -> setupFactionSelect()
        }
    }

    private fun setupMainMenu() {
        findViewById<View>(R.id.btnNewGame)?.setOnClickListener {
            val intent = android.content.Intent(this, GameActivity::class.java).apply {
                putExtra("faction", FactionType.VANGUARD.name)
                putExtra("difficulty", "MEDIUM")
                putExtra("mapSeed", System.currentTimeMillis().toInt())
            }
            startActivity(intent)
        }

        findViewById<View>(R.id.btnCampaign)?.setOnClickListener {
            // Show campaign selection
        }

        findViewById<View>(R.id.btnMultiplayer)?.setOnClickListener {
            // Show multiplayer options
        }

        findViewById<View>(R.id.btnSettings)?.setOnClickListener {
            val intent = android.content.Intent(this, MenuActivity::class.java).apply {
                putExtra("menu_type", "settings")
            }
            startActivity(intent)
        }

        findViewById<View>(R.id.btnExit)?.setOnClickListener {
            finish()
        }
    }

    private fun setupSettings() {
        // Setup faction selection spinner
        val factionSpinner = findViewById<Spinner>(R.id.spinnerFaction)
        val factions = FactionType.getAll().map { it.displayName }
        factionSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, factions)

        // Setup difficulty spinner
        val difficultySpinner = findViewById<Spinner>(R.id.spinnerDifficulty)
        val difficulties = listOf("Easy", "Medium", "Hard", "Insane")
        difficultySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, difficulties)

        // Setup graphics quality spinner
        val qualitySpinner = findViewById<Spinner>(R.id.spinnerQuality)
        val qualities = listOf("Low", "Medium", "High")
        qualitySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, qualities)

        // Sound volume slider
        // Music volume slider
        // Fullscreen checkbox

        findViewById<View>(R.id.btnBack)?.setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.btnApply)?.setOnClickListener {
            // Apply settings
            finish()
        }
    }

    private fun setupFactionSelect() {
        val vanguardBtn = findViewById<RadioButton>(R.id.radioVanguard)
        val swarmBtn = findViewById<RadioButton>(R.id.radioSwarm)
        val synodBtn = findViewById<RadioButton>(R.id.radioSynod)

        vanguardBtn?.setOnClickListener { selectFaction(FactionType.VANGUARD) }
        swarmBtn?.setOnClickListener { selectFaction(FactionType.SWARM) }
        synodBtn?.setOnClickListener { selectFaction(FactionType.SYNOD) }

        findViewById<View>(R.id.btnConfirm)?.setOnClickListener {
            val selectedFaction = when {
                vanguardBtn?.isChecked == true -> FactionType.VANGUARD
                swarmBtn?.isChecked == true -> FactionType.SWARM
                synodBtn?.isChecked == true -> FactionType.SYNOD
                else -> FactionType.VANGUARD
            }

            val intent = android.content.Intent(this, GameActivity::class.java).apply {
                putExtra("faction", selectedFaction.name)
                putExtra("difficulty", "MEDIUM")
                putExtra("mapSeed", System.currentTimeMillis().toInt())
            }
            startActivity(intent)
        }

        findViewById<View>(R.id.btnBack)?.setOnClickListener {
            finish()
        }
    }

    private fun selectFaction(faction: FactionType) {
        // Update UI to show faction details
        val descriptionText = findViewById<android.widget.TextView>(R.id.textFactionDescription)
        descriptionText?.text = faction.description
    }

    override fun onBackPressed() {
        finish()
    }
}
