package com.galaxycommand.rts.systems

import com.galaxycommand.rts.core.Vector2
import com.galaxycommand.rts.entities.Building
import com.galaxycommand.rts.entities.Entity
import com.galaxycommand.rts.entities.Unit

/**
 * Fog of War Manager for visibility and stealth mechanics.
 * Manages three visibility states: Visible, Explored, and Hidden.
 */
class FogOfWarManager {

    companion object {
        // Visibility radii
        const val SIGHT_RADIUS = 150f
        const val DETECTION_RADIUS = 200f
        const val VISION_UPDATE_INTERVAL = 0.1f

        // Resolution for visibility grid (lower = more efficient)
        const val GRID_SIZE = 50f
    }

    // Visibility grid
    private var gridWidth = 0
    private var gridHeight = 0
    private var visibilityGrid: Array<Array<VisibilityState>> = emptyArray()

    // Player visibility data
    private val playerVisibility = mutableMapOf<Long, VisibilityData>()

    // Temporary tracking
    private var lastVisionUpdate = 0f

    /**
     * Visibility states for each tile
     */
    enum class VisibilityState {
        HIDDEN,      // Never seen or last seen too long ago
        EXPLORED,    // Seen but not currently visible
        VISIBLE      // Currently visible
    }

    /**
     * Visibility data for a player
     */
    data class VisibilityData(
        val playerId: Long,
        val visibleTiles: MutableSet<Pair<Int, Int>> = mutableSetOf(),
        val exploredTiles: MutableSet<Pair<Int, Int>> = mutableSetOf(),
        val lastSeen: MutableMap<Pair<Int, Int>, Float> = mutableMapOf()
    )

    /**
     * Initialize fog of war system
     */
    fun initialize() {
        val gameEngine = com.galaxycommand.rts.core.GameEngine.getInstance()
        val mapSize = gameEngine.getMapSize()

        gridWidth = (mapSize.x / GRID_SIZE).toInt() + 1
        gridHeight = (mapSize.y / GRID_SIZE).toInt() + 1

        // Create visibility grid
        visibilityGrid = Array(gridWidth) { x ->
            Array(gridHeight) { y ->
                VisibilityState.HIDDEN
            }
        }

        playerVisibility.clear()
    }

    /**
     * Add a player to visibility tracking
     */
    fun addPlayer(playerId: Long) {
        if (!playerVisibility.containsKey(playerId)) {
            playerVisibility[playerId] = VisibilityData(playerId)
        }
    }

    /**
     * Update visibility for all players
     */
    fun update(deltaTime: Float, units: List<Unit>, buildings: List<Building>) {
        lastVisionUpdate += deltaTime
        if (lastVisionUpdate < VISION_UPDATE_INTERVAL) return

        lastVisionUpdate = 0f

        // Update visibility for each player
        playerVisibility.forEach { (playerId, visibilityData) ->
            // Calculate visible tiles from units
            val playerUnits = units.filter { it.ownerId == playerId && it.isAlive }
            val playerBuildings = buildings.filter { it.ownerId == playerId && it.isAlive }

            // Reset visibility for this player
            val newlyVisible = mutableSetOf<Pair<Int, Int>>()

            // Process unit sight
            playerUnits.forEach { unit ->
                val sightTiles = getTilesInRadius(unit.position, unit.sightRange)
                newlyVisible.addAll(sightTiles)
            }

            // Process building sight
            playerBuildings.forEach { building ->
                val sightTiles = getTilesInRadius(building.position, SIGHT_RADIUS * 1.5f)
                newlyVisible.addAll(sightTiles)
            }

            // Update visibility data
            newlyVisible.forEach { tile ->
                // Mark as visible
                visibilityGrid[tile.first][tile.second] = VisibilityState.VISIBLE

                // Add to visible set
                visibilityData.visibleTiles.add(tile)

                // Add to explored set
                visibilityData.exploredTiles.add(tile)

                // Update last seen time
                visibilityData.lastSeen[tile] = System.currentTimeMillis() / 1000f
            }

            // Remove tiles that are no longer visible
            val toRemove = visibilityData.visibleTiles.filter { it !in newlyVisible }
            toRemove.forEach { tile ->
                visibilityData.visibleTiles.remove(tile)
                // Keep explored for some time
                visibilityData.lastSeen[tile]?.let { lastSeen ->
                    if (System.currentTimeMillis() / 1000f - lastSeen > 60f) {
                        // After 60 seconds, mark as explored (not hidden)
                        visibilityGrid[tile.first][tile.second] = VisibilityState.EXPLORED
                    }
                }
            }
        }

        // Check for cloaked units
        detectCloakedUnits(units, buildings)
    }

    /**
     * Get tiles within radius of a position
     */
    private fun getTilesInRadius(position: Vector2, radius: Float): Set<Pair<Int, Int>> {
        val tiles = mutableSetOf<Pair<Int, Int>>()

        val minTileX = ((position.x - radius) / GRID_SIZE).toInt().coerceAtLeast(0)
        val maxTileX = ((position.x + radius) / GRID_SIZE).toInt().coerceAtMost(gridWidth - 1)
        val minTileY = ((position.y - radius) / GRID_SIZE).toInt().coerceAtLeast(0)
        val maxTileY = ((position.y + radius) / GRID_SIZE).toInt().coerceAtMost(gridHeight - 1)

        for (x in minTileX..maxTileX) {
            for (y in minTileY..maxTileY) {
                val tileCenter = Vector2.new(
                    x * GRID_SIZE + GRID_SIZE / 2,
                    y * GRID_SIZE + GRID_SIZE / 2
                )
                if (position.distanceTo(tileCenter) <= radius) {
                    tiles.add(Pair(x, y))
                }
            }
        }

        return tiles
    }

    /**
     * Check for cloaked units and detect them if appropriate units are nearby
     */
    private fun detectCloakedUnits(units: List<Unit>, buildings: List<Building>) {
        units.forEach { unit ->
            if (unit.isAlive) {
                // Check if unit is cloaked
                val isCloaked = isUnitCloaked(unit)

                // Check if detected by nearby detectors
                val isDetected = isDetectedByPlayer(unit, units, buildings)

                // Update visibility based on detection
                if (isDetected) {
                    // Unit is visible to its owner even if cloaked
                    playerVisibility[unit.ownerId]?.let { visibilityData ->
                        // Unit position tiles
                        val tileX = (unit.position.x / GRID_SIZE).toInt().coerceIn(0, gridWidth - 1)
                        val tileY = (unit.position.y / GRID_SIZE).toInt().coerceIn(0, gridHeight - 1)
                        val tile = Pair(tileX, tileY)

                        visibilityData.visibleTiles.add(tile)
                        visibilityData.exploredTiles.add(tile)
                        visibilityGrid[tileX][tileY] = VisibilityState.VISIBLE
                    }
                }
            }
        }
    }

    /**
     * Check if a unit is cloaked
     */
    private fun isUnitCloaked(unit: Unit): Boolean {
        // Check for cloak ability (simplified)
        return false // In a full implementation, check unit abilities
    }

    /**
     * Check if a cloaked unit is detected by nearby detectors
     */
    private fun isDetectedByPlayer(unit: Unit, units: List<Unit>, buildings: List<Building>): Boolean {
        val detectorRange = DETECTION_RADIUS

        // Check for nearby detection sources from same player
        val nearbyDetectors = units.filter {
            it.ownerId == unit.ownerId &&
            it.isAlive &&
            it.position.distanceTo(unit.position) <= detectorRange &&
            hasDetectionAbility(it)
        }

        if (nearbyDetectors.isNotEmpty()) return true

        // Check for detection buildings
        val nearbyDetectorsBuildings = buildings.filter {
            it.ownerId == unit.ownerId &&
            it.isAlive &&
            it.position.distanceTo(unit.position) <= detectorRange * 1.5f &&
            hasBuildingDetection(it)
        }

        return nearbyDetectorsBuildings.isNotEmpty()
    }

    /**
     * Check if unit has detection ability
     */
    private fun hasDetectionAbility(unit: Unit): Boolean {
        // In a full implementation, check unit abilities
        return unit.type in listOf("Observer", "Overseer", "Raven")
    }

    /**
     * Check if building has detection
     */
    private fun hasBuildingDetection(building: Building): Boolean {
        // In a full implementation, check building types
        return building.type in listOf("Photon Cannon", "Spine Crawler", "Spore Crawler")
    }

    /**
     * Check if a position is visible to a player
     */
    fun isVisibleTo(playerId: Long, position: Vector2): Boolean {
        val tileX = (position.x / GRID_SIZE).toInt().coerceIn(0, gridWidth - 1)
        val tileY = (position.y / GRID_SIZE).toInt().coerceIn(0, gridHeight - 1)

        return visibilityGrid[tileX][tileY] == VisibilityState.VISIBLE
    }

    /**
     * Check if a position is explored by a player
     */
    fun isExploredBy(playerId: Long, position: Vector2): Boolean {
        val tileX = (position.x / GRID_SIZE).toInt().coerceIn(0, gridWidth - 1)
        val tileY = (position.y / GRID_SIZE).toInt().coerceIn(0, gridHeight - 1)
        val tile = Pair(tileX, tileY)

        return playerVisibility[playerId]?.exploredTiles?.contains(tile) == true ||
               visibilityGrid[tileX][tileY] != VisibilityState.HIDDEN
    }

    /**
     * Check if an entity is visible to a player
     */
    fun isEntityVisibleTo(playerId: Long, entity: Entity): Boolean {
        // Enemy entities are only visible if currently visible
        // Friendly entities are always visible (if alive)

        if (entity.ownerId == playerId) {
            return entity.isAlive
        }

        return isVisibleTo(playerId, entity.position)
    }

    /**
     * Reveal an area for a player
     */
    fun revealArea(playerId: Long, position: Vector2, radius: Float) {
        val tiles = getTilesInRadius(position, radius)
        val visibilityData = playerVisibility[playerId] ?: return

        tiles.forEach { tile ->
            visibilityData.visibleTiles.add(tile)
            visibilityData.exploredTiles.add(tile)
            if (tile.first < gridWidth && tile.second < gridHeight) {
                visibilityGrid[tile.first][tile.second] = VisibilityState.VISIBLE
            }
        }
    }

    /**
     * Hide an area (remove visibility)
     */
    fun hideArea(playerId: Long, position: Vector2, radius: Float) {
        val tiles = getTilesInRadius(position, radius)
        val visibilityData = playerVisibility[playerId] ?: return

        tiles.forEach { tile ->
            visibilityData.visibleTiles.remove(tile)
            if (tile.first < gridWidth && tile.second < gridHeight) {
                visibilityGrid[tile.first][tile.second] = VisibilityState.EXPLORED
            }
        }
    }

    /**
     * Get visibility grid for rendering
     */
    fun getVisibilityGrid(): Array<Array<VisibilityState>> = visibilityGrid

    /**
     * Get visibility statistics
     */
    fun getStats(): FogOfWarStats {
        var visibleCount = 0
        var exploredCount = 0
        var hiddenCount = 0

        for (x in 0 until gridWidth) {
            for (y in 0 until gridHeight) {
                when (visibilityGrid[x][y]) {
                    VisibilityState.VISIBLE -> visibleCount++
                    VisibilityState.EXPLORED -> exploredCount++
                    VisibilityState.HIDDEN -> hiddenCount++
                }
            }
        }

        return FogOfWarStats(
            visibleTiles = visibleCount,
            exploredTiles = exploredCount,
            hiddenTiles = hiddenCount,
            totalTiles = gridWidth * gridHeight,
            visiblePercent = if (gridWidth * gridHeight > 0) visibleCount.toFloat() / (gridWidth * gridHeight) else 0f
        )
    }

    /**
     * Clear all visibility data
     */
    fun clear() {
        visibilityGrid = emptyArray()
        playerVisibility.clear()
    }

    /**
     * Fog of War statistics
     */
    data class FogOfWarStats(
        val visibleTiles: Int,
        val exploredTiles: Int,
        val hiddenTiles: Int,
        val totalTiles: Int,
        val visiblePercent: Float
    )
}
