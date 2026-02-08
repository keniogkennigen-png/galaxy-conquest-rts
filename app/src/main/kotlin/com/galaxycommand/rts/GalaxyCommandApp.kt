package com.galaxycommand.rts

import android.app.Application
import android.content.Context

/**
 * Main Application class for Galaxy Command RTS.
 * Handles application-wide initialization and provides global access to game state.
 */
class GalaxyCommandApp : Application() {

    companion object {
        private lateinit var instance: GalaxyCommandApp

        fun getInstance(): GalaxyCommandApp = instance
        fun getAppContext(): Context = instance.applicationContext
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize global game configurations
        initializeGameConfigs()
    }

    private fun initializeGameConfigs() {
        // Game configuration constants are initialized here
        // These can be adjusted based on device capabilities
        System.setProperty("java.awt.headless", "true")
    }

    /**
     * Check if device meets minimum requirements for smooth gameplay
     */
    fun isLowEndDevice(): Boolean {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory() / (1024 * 1024)
        return maxMemory < 128 // Less than 128MB available
    }

    /**
     * Adjust game quality settings based on device capabilities
     */
    fun getQualityLevel(): QualityLevel {
        return when {
            isLowEndDevice() -> QualityLevel.LOW
            else -> QualityLevel.HIGH
        }
    }

    enum class QualityLevel {
        LOW,
        MEDIUM,
        HIGH
    }
}
