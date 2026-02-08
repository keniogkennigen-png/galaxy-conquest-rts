package com.galaxycommand.rts.core

import com.galaxycommand.rts.factions.FactionType
import com.galaxycommand.rts.systems.AIEngine

/**
 * Data class holding the complete game state.
 * This is used for save/load functionality and game management.
 */
data class GameState(
    var playerFaction: FactionType = FactionType.VANGUARD,
    var mapSeed: Int = 0,
    var difficulty: AIEngine.Difficulty = AIEngine.Difficulty.MEDIUM,
    var gameTime: Float = 0f,
    var isPaused: Boolean = false,
    var gameResult: GameEngine.GameResult? = null,

    // Player resources
    var minerals: Int = 50,
    var energy: Int = 50,
    var maxEnergy: Int = 100,
    var gas: Int = 0,
    var supply: Int = 0,
    var maxSupply: Int = 200,

    // Player tracking
    var currentPlayerId: Long = 1L,

    // Game flags
    var isGameOver: Boolean = false,
    var isVictory: Boolean = false,
    var isDefeat: Boolean = false,

    // Player information
    val players: MutableList<PlayerInfo> = mutableListOf()
) {

    /**
     * Add minerals to player resources
     */
    fun addMinerals(amount: Int) {
        minerals += amount
    }

    /**
     * Spend minerals
     */
    fun spendMinerals(amount: Int): Boolean {
        return if (minerals >= amount) {
            minerals -= amount
            true
        } else {
            false
        }
    }

    /**
     * Add gas to player resources
     */
    fun addGas(amount: Int) {
        gas += amount
    }

    /**
     * Spend gas
     */
    fun spendGas(amount: Int): Boolean {
        return if (gas >= amount) {
            gas -= amount
            true
        } else {
            false
        }
    }

    /**
     * Check if player has enough resources
     */
    fun hasResources(mineralCost: Int = 0, gasCost: Int = 0): Boolean {
        return minerals >= mineralCost && gas >= gasCost
    }

    /**
     * Check if player has enough supply
     */
    fun hasSupply(): Boolean {
        return supply < maxSupply
    }

    /**
     * Add a player to the game
     */
    fun addPlayer(factionType: FactionType, playerType: GameEngine.PlayerType, basePosition: Vector2) {
        val playerId = (players.size + 1).toLong()
        players.add(
            PlayerInfo(
                id = playerId,
                faction = factionType,
                playerType = playerType,
                basePosition = basePosition,
                isAlive = true
            )
        )
    }

    /**
     * Update player alive status
     */
    fun setPlayerAlive(playerId: Long, alive: Boolean) {
        players.find { it.id == playerId }?.isAlive = alive
    }

    /**
     * Get player info by ID
     */
    fun getPlayer(playerId: Long): PlayerInfo? {
        return players.find { it.id == playerId }
    }

    /**
     * Get all AI players
     */
    fun getAIPlayers(): List<PlayerInfo> {
        return players.filter { it.playerType == GameEngine.PlayerType.AI && it.isAlive }
    }

    /**
     * Get all human players
     */
    fun getHumanPlayers(): List<PlayerInfo> {
        return players.filter { it.playerType == GameEngine.PlayerType.HUMAN && it.isAlive }
    }

    /**
     * Get player minerals (for HUD display)
     */
    fun getPlayerMinerals(): Int {
        return minerals
    }

    /**
     * Get player energy (for HUD display)
     */
    fun getPlayerEnergy(): Int {
        return energy
    }

    /**
     * Get energy capacity (for HUD display)
     */
    fun getEnergyCapacity(): Int {
        return maxEnergy
    }

    /**
     * Calculate and get income rate per minute
     */
    fun getIncomeRate(): Int {
        // Simplified income calculation - could be enhanced with worker tracking
        val workers = players.firstOrNull { it.id == currentPlayerId }?.let {
            it.unitsBuilt / 10 // Rough estimate based on units built
        } ?: 5
        return workers * 10 // 10 minerals per worker per minute
    }

    /**
     * Check if fog of war is cleared at position (for minimap)
     */
    fun isFogOfWarCleared(x: Float, y: Float): Boolean {
        // Simplified implementation - should be connected to FogOfWarManager
        return true // All units visible for now
    }

    /**
     * Get player faction type
     */
    fun getPlayerFaction(): FactionType {
        return playerFaction
    }

    /**
     * Get current player info
     */
    fun getCurrentPlayer(): PlayerInfo? {
        return players.find { it.id == currentPlayerId }
    }

    /**
     * Check if game has ended
     */
    fun hasEnded(): Boolean {
        return isGameOver || isVictory || isDefeat
    }

    /**
     * Set victory
     */
    fun setVictory() {
        isGameOver = true
        isVictory = true
        gameResult = GameEngine.GameResult.VICTORY
    }

    /**
     * Set defeat
     */
    fun setDefeat() {
        isGameOver = true
        isDefeat = true
        gameResult = GameEngine.GameResult.DEFEAT
    }

    /**
     * Get formatted game time
     */
    fun getFormattedTime(): String {
        val totalSeconds = gameTime.toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    /**
     * Reset the game state
     */
    fun reset() {
        gameTime = 0f
        isPaused = false
        gameResult = null
        minerals = 50
        gas = 0
        supply = 0
        isGameOver = false
        isVictory = false
        isDefeat = false
        players.clear()
    }

    /**
     * Serialize game state to JSON (for save games)
     */
    fun toJson(): String {
        return com.google.gson.GsonBuilder().create().toJson(this)
    }

    /**
     * Deserialize game state from JSON
     */
    companion object {
        fun fromJson(json: String): GameState {
            return com.google.gson.GsonBuilder().create().fromJson(json, GameState::class.java)
        }
    }

    /**
     * Data class for player information
     */
    data class PlayerInfo(
        val id: Long,
        val faction: FactionType,
        val playerType: GameEngine.PlayerType,
        val basePosition: Vector2,
        var isAlive: Boolean,
        var minerals: Int = 50,
        var gas: Int = 0,
        var unitsBuilt: Int = 0,
        var buildingsBuilt: Int = 0,
        var unitsKilled: Int = 0,
        var buildingsDestroyed: Int = 0
    )
}
