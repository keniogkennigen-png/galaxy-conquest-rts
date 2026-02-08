package com.galaxycommand.rts.core

import com.galaxycommand.rts.entities.Entity
import com.galaxycommand.rts.entities.Unit
import com.galaxycommand.rts.entities.Building
import com.galaxycommand.rts.entities.Resource
import com.galaxycommand.rts.factions.Faction
import com.galaxycommand.rts.factions.FactionType
import com.galaxycommand.rts.systems.AIEngine
import com.galaxycommand.rts.systems.PathFinder
import com.galaxycommand.rts.systems.CombatSystem
import com.galaxycommand.rts.systems.FogOfWarManager
import com.galaxycommand.rts.ui.HUDManager
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * Main Game Engine that orchestrates all game systems.
 * This is the central hub for all game logic and entity management.
 */
class GameEngine private constructor() {

    companion object {
        @Volatile
        private var instance: GameEngine? = null

        fun getInstance(): GameEngine {
            return instance ?: synchronized(this) {
                instance ?: GameEngine().also { instance = it }
            }
        }

        // Map dimensions
        const val MAP_WIDTH = 2000f
        const val MAP_HEIGHT = 2000f
        const val TILE_SIZE = 32f
    }

    // Game State
    private var gameState = GameState()
    private var isRunning = false
    private var lastUpdateTime = 0L

    // Subsystems
    val pathFinder = PathFinder()
    val combatSystem = CombatSystem()
    val fogOfWar = FogOfWarManager()
    val aiEngine = AIEngine()

    // Entity collections
    private val entities = ConcurrentHashMap<Long, Entity>()
    private val units = ConcurrentHashMap<Long, Unit>()
    private val buildings = ConcurrentHashMap<Long, Building>()
    private val resources = ConcurrentHashMap<Long, Resource>()

    // Entity and game managers
    val entityManager = EntityManager()
    lateinit var camera: Camera
    lateinit var gameMap: GameMap
    var hudManager: HUDManager? = null

    // Entity ID counter
    private var nextEntityId = 1L

    /**
     * Initialize the game with player faction and map settings
     */
    fun initialize(
        playerFaction: FactionType,
        mapSeed: Int = System.currentTimeMillis().toInt(),
        difficulty: AIEngine.Difficulty = AIEngine.Difficulty.MEDIUM
    ) {
        gameState = GameState(
            playerFaction = playerFaction,
            mapSeed = mapSeed,
            difficulty = difficulty,
            isPaused = false
        )

        // Reset subsystems
        pathFinder.initialize()
        combatSystem.initialize()
        fogOfWar.initialize()
        aiEngine.initialize(this, difficulty)

        // Clear existing entities
        entities.clear()
        units.clear()
        buildings.clear()
        resources.clear()
        entityManager.clear()

        // Generate map and initial entities
        generateMap()

        isRunning = true
        lastUpdateTime = System.currentTimeMillis()
    }

    /**
     * Initialize the game with screen dimensions for camera and HUD
     */
    fun initializeWithScreen(
        playerFaction: FactionType,
        screenWidth: Int,
        screenHeight: Int,
        mapSeed: Int = System.currentTimeMillis().toInt(),
        difficulty: AIEngine.Difficulty = AIEngine.Difficulty.MEDIUM
    ) {
        initialize(playerFaction, mapSeed, difficulty)
        
        // Initialize camera
        camera = Camera(MAP_WIDTH, MAP_HEIGHT)
        camera.setViewportSize(screenWidth, screenHeight)
        
        // Initialize HUD
        hudManager = HUDManager(gameMap, gameState, camera, entityManager)
        hudManager?.initialize(screenWidth, screenHeight)
    }

    /**
     * Handle screen size changes (rotation)
     */
    fun onScreenSizeChanged(screenWidth: Int, screenHeight: Int) {
        if (::camera.isInitialized) {
            camera.resize(screenWidth, screenHeight)
        }
        hudManager?.onOrientationChanged(screenWidth, screenHeight)
    }

    /**
     * Generate the game map with terrain, resources, and starting positions
     */
    private fun generateMap() {
        val random = java.util.Random(gameState.mapSeed.toLong())

        // Create player base
        val playerFaction = Faction.createFaction(gameState.playerFaction)
        val playerBasePos = Vector2.new(200f, MAP_HEIGHT / 2)
        createBase(playerFaction, playerBasePos, PlayerType.HUMAN)

        // Create enemy base (opposite side)
        val enemyFactionType = getEnemyFaction(gameState.playerFaction)
        val enemyFaction = Faction.createFaction(enemyFactionType)
        val enemyBasePos = Vector2.new(MAP_WIDTH - 200f, MAP_HEIGHT / 2)
        createBase(enemyFaction, enemyBasePos, PlayerType.AI)

        // Generate mineral fields
        generateMineralFields(random)

        // Generate terrain obstacles
        generateTerrain(random)
    }

    private fun createBase(faction: Faction, position: Vector2, playerType: PlayerType) {
        // Add player to game state first to get player ID
        val playerId = (gameState.players.size + 1).toLong()
        gameState.addPlayer(faction.factionType, playerType, position)

        // Create main base building
        val baseBuilding = faction.createBaseBuilding(position)
        addEntity(baseBuilding)
        buildings[baseBuilding.id] = baseBuilding

        // Create initial workers
        for (i in 0 until 4) {
            val workerPos = position + Vector2.new(100f + i * 30f, (i % 2) * 60f - 30f)
            val worker = faction.createWorker(workerPos, playerId, playerType)
            addEntity(worker)
            units[worker.id] = worker
        }

        // Create initial combat units
        for (i in 0 until 2) {
            val unitPos = position + Vector2.new(150f + i * 40f, (i % 2) * 80f - 40f)
            val combatUnit = faction.createCombatUnit(unitPos, playerId, playerType)
            addEntity(combatUnit)
            units[combatUnit.id] = combatUnit
        }
    }

    private fun generateMineralFields(random: java.util.Random) {
        // Player mineral field
        addMineralField(Vector2.new(400f, MAP_HEIGHT / 2), 1000)
        addMineralField(Vector2.new(400f, MAP_HEIGHT / 2 + 100), 1000)

        // Enemy mineral field
        addMineralField(Vector2.new(MAP_WIDTH - 400f, MAP_HEIGHT / 2), 1000)
        addMineralField(Vector2.new(MAP_WIDTH - 400f, MAP_HEIGHT / 2 - 100), 1000)

        // Neutral mineral fields in middle
        for (i in 0 until 3) {
            for (j in 0 until 2) {
                val x = MAP_WIDTH / 2 - 150 + i * 150 + random.nextFloat() * 50
                val y = MAP_HEIGHT / 2 - 100 + j * 200 + random.nextFloat() * 50
                addMineralField(Vector2.new(x, y), 800 + random.nextInt(400))
            }
        }
    }

    private fun generateTerrain(random: java.util.Random) {
        // Create terrain tiles
        // This would generate actual terrain in a full implementation
    }

    private fun addMineralField(position: Vector2, amount: Int) {
        val resource = Resource(
            id = nextEntityId++,
            type = "Mineral",
            position = position,
            amount = amount,
            maxAmount = amount,
            radius = 40f
        )
        resources[resource.id] = resource
        entities[resource.id] = resource
    }

    private fun getEnemyFaction(playerFaction: FactionType): FactionType {
        return when (playerFaction) {
            FactionType.VANGUARD -> FactionType.SWARM
            FactionType.SWARM -> FactionType.SYNODE
            FactionType.SYNODE -> FactionType.VANGUARD
        }
    }

    /**
     * Main game update loop - processes all game logic
     */
    fun update(deltaTime: Float) {
        if (isPaused()) return

        val currentTime = System.currentTimeMillis()
        val dt = (currentTime - lastUpdateTime) / 1000f
        lastUpdateTime = currentTime

        // Update game time
        gameState.gameTime += dt

        // Update all units
        units.values.forEach { unit ->
            updateUnit(unit, dt)
        }

        // Update AI
        aiEngine.update(dt)

        // Update fog of war
        fogOfWar.update(dt, units.values.toList(), buildings.values.toList())

        // Process combat
        combatSystem.update(dt, units.values.toList(), this)

        // Update HUD
        hudManager?.update()

        // Check win/lose conditions
        checkGameEndConditions()
    }

    private fun updateUnit(unit: Unit, deltaTime: Float) {
        // Skip dead units
        if (!unit.isAlive) return

        // Process unit state machine
        when (unit.state) {
            Unit.UnitState.IDLE -> processIdleState(unit)
            Unit.UnitState.MOVING -> processMovingState(unit, deltaTime)
            Unit.UnitState.ATTACKING -> processAttackingState(unit, deltaTime)
            Unit.UnitState.GATHERING -> processGatheringState(unit, deltaTime)
            Unit.UnitState.RETURNING -> processReturningState(unit, deltaTime)
            Unit.UnitState.BUILDING -> processBuildingState(unit, deltaTime)
            Unit.UnitState.PATROLLING -> processPatrollingState(unit, deltaTime)
            Unit.UnitState.HOLDING_POSITION -> processHoldingPositionState(unit)
            Unit.UnitState.DEAD -> { /* Do nothing for dead units */ }
        }

        // Regenerate shields/health if applicable
        unit.regenerate(deltaTime)
    }

    private fun processIdleState(unit: Unit) {
        // Auto-attack nearby enemies if combat unit
        if (unit.isCombatUnit) {
            val nearbyEnemy = findNearestEnemy(unit)
            if (nearbyEnemy != null && unit.position.distanceTo(nearbyEnemy.position) <= unit.attackRange) {
                unit.state = Unit.UnitState.ATTACKING
                unit.targetUnit = nearbyEnemy
            }
        }
    }

    private fun processMovingState(unit: Unit, deltaTime: Float) {
        unit.target?.let { target ->
            val direction = (target - unit.position).normalize()
            val movement = direction.times(unit.speed * deltaTime)
            unit.position = unit.position + movement

            // Check if reached target
            if (unit.position.distanceTo(target) < 5f) {
                unit.state = Unit.UnitState.IDLE
                unit.target = null
            }
        } ?: run {
            unit.state = Unit.UnitState.IDLE
        }
    }

    private fun processAttackingState(unit: Unit, deltaTime: Float) {
        unit.target?.let { target ->
            val distance = unit.position.distanceTo(target)

            if (distance > unit.attackRange) {
                // Move toward target
                val direction = (target - unit.position).normalize()
                unit.position += direction * unit.speed * deltaTime
            } else if (target is Unit && target.isAlive) {
                // Attack
                combatSystem.attack(unit, target)
            } else {
                unit.state = Unit.UnitState.IDLE
                unit.target = null
            }
        } ?: run {
            unit.state = Unit.UnitState.IDLE
        }
    }

    private fun processGatheringState(unit: Unit, deltaTime: Float) {
        unit.targetResource?.let { resource ->
            val distance = unit.position.distanceTo(resource.position)

            if (distance > 50f) {
                // Move toward resource
                val direction = (resource.position - unit.position).normalize()
                unit.position += direction * unit.speed * deltaTime
            } else if (resource.amount > 0 && unit.carrying < unit.maxCarry) {
                // Harvest
                val harvestAmount = (5 * deltaTime).toInt().coerceAtMost(resource.amount)
                resource.amount -= harvestAmount
                unit.carrying += harvestAmount

                if (unit.carrying >= unit.maxCarry || resource.amount <= 0) {
                    // Return to base
                    unit.state = Unit.UnitState.RETURNING
                    unit.target = findNearestBase(unit)
                    unit.targetResource = null
                }
            }
        }
    }

    private fun processReturningState(unit: Unit, deltaTime: Float) {
        unit.target?.let { base ->
            val distance = unit.position.distanceTo(base.position)

            if (distance > 60f) {
                val direction = (base.position - unit.position).normalize()
                unit.position += direction * unit.speed * deltaTime
            } else {
                // Deposit resources
                gameState.addMinerals(unit.carrying)
                unit.carrying = 0

                // Go back to gathering if we have a target resource
                unit.targetResource?.let { resource ->
                    if (resource.amount > 0) {
                        unit.state = Unit.UnitState.GATHERING
                        unit.target = resource.position
                    } else {
                        unit.state = Unit.UnitState.IDLE
                    }
                } ?: run {
                    unit.state = Unit.UnitState.IDLE
                }
                unit.target = null
            }
        }
    }

    private fun processBuildingState(unit: Unit, deltaTime: Float) {
        // Building construction logic
    }

    private fun processPatrollingState(unit: Unit, deltaTime: Float) {
        // Patrol between waypoints
        unit.target?.let { target ->
            val direction = (target - unit.position).normalize()
            val movement = direction * unit.speed * 0.5f * deltaTime
            unit.position = unit.position + movement

            if (unit.position.distanceTo(target) < 10f) {
                unit.target = null
                unit.state = Unit.UnitState.IDLE
            }
        } ?: run {
            unit.state = Unit.UnitState.IDLE
        }
    }

    private fun processHoldingPositionState(unit: Unit) {
        // Hold position - do nothing unless enemy is very close
        val nearbyEnemy = findNearestEnemy(unit)
        if (nearbyEnemy != null && unit.position.distanceTo(nearbyEnemy.position) <= unit.attackRange) {
            unit.state = Unit.UnitState.ATTACKING
            unit.targetUnit = nearbyEnemy
        }
    }

    private fun findNearestEnemy(unit: Unit): Unit? {
        var nearest: Unit? = null
        var nearestDist = Float.MAX_VALUE

        units.values.forEach { other ->
            if (other.ownerId != unit.ownerId && other.isAlive) {
                val dist = unit.position.distanceTo(other.position)
                if (dist < nearestDist && dist < unit.sightRange) {
                    nearest = other
                    nearestDist = dist
                }
            }
        }

        return nearest
    }

    private fun findNearestBase(unit: Unit): Building? {
        var nearest: Building? = null
        var nearestDist = Float.MAX_VALUE

        buildings.values.forEach { building ->
            if (building.ownerId == unit.ownerId && building.isMainBase) {
                val dist = unit.position.distanceTo(building.position)
                if (dist < nearestDist) {
                    nearest = building
                    nearestDist = dist
                }
            }
        }

        return nearest
    }

    /**
     * Issue a move command to selected units
     */
    fun issueMoveCommand(unitIds: List<Long>, targetPosition: Vector2) {
        unitIds.forEach { id ->
            units[id]?.let { unit ->
                if (unit.ownerId == gameState.currentPlayerId && unit.isAlive) {
                    // Find path to target
                    val path = pathFinder.findPath(unit.position, targetPosition)
                    unit.path = path
                    unit.target = targetPosition
                    unit.state = Unit.UnitState.MOVING
                }
            }
        }
    }

    /**
     * Issue an attack command to selected units
     */
    fun issueAttackCommand(unitIds: List<Long>, target: Unit) {
        unitIds.forEach { id ->
            units[id]?.let { unit ->
                if (unit.ownerId == gameState.currentPlayerId && unit.isAlive && unit.isCombatUnit) {
                    unit.target = target.position
                    unit.state = Unit.UnitState.ATTACKING
                }
            }
        }
    }

    /**
     * Issue a gather command to selected workers
     */
    fun issueGatherCommand(unitIds: List<Long>, resource: Resource) {
        unitIds.forEach { id ->
            units[id]?.let { unit ->
                if (unit.ownerId == gameState.currentPlayerId && unit.isAlive && unit.isWorker) {
                    unit.targetResource = resource
                    unit.state = Unit.UnitState.GATHERING
                }
            }
        }
    }

    /**
     * Add a new entity to the game
     */
    fun addEntity(entity: Entity) {
        entity.id = nextEntityId++
        entities[entity.id] = entity
        entityManager.addEntity(entity)

        when (entity) {
            is Unit -> units[entity.id] = entity
            is Building -> buildings[entity.id] = entity
            is Resource -> resources[entity.id] = entity
        }
    }

    /**
     * Remove an entity from the game
     */
    fun removeEntity(entityId: Long) {
        entities.remove(entityId)
        units.remove(entityId)
        buildings.remove(entityId)
        resources.remove(entityId)
    }

    /**
     * Get entity by ID
     */
    fun getEntity(id: Long): Entity? = entities[id]

    /**
     * Get all units within a rectangular area
     */
    fun getUnitsInArea(topLeft: Vector2, bottomRight: Vector2, playerId: Long? = null): List<Unit> {
        return units.values.filter { unit ->
            val inArea = unit.position.x >= topLeft.x &&
                        unit.position.x <= bottomRight.x &&
                        unit.position.y >= topLeft.y &&
                        unit.position.y <= bottomRight.y

            if (playerId != null) {
                inArea && unit.ownerId == playerId && unit.isAlive
            } else {
                inArea && unit.isAlive
            }
        }
    }

    /**
     * Get unit at position
     */
    fun getUnitAtPosition(position: Vector2, radius: Float = 20f): Unit? {
        return units.values.find { unit ->
            unit.isAlive && unit.position.distanceTo(position) < radius
        }
    }

    /**
     * Get resource at position
     */
    fun getResourceAtPosition(position: Vector2, radius: Float = 40f): Resource? {
        return resources.values.find { resource ->
            resource.amount > 0 && resource.position.distanceTo(position) < radius
        }
    }

    /**
     * Check if position is valid for unit placement
     */
    fun isValidPlacement(position: Vector2, radius: Float): Boolean {
        // Check bounds
        if (position.x < radius || position.x > MAP_WIDTH - radius ||
            position.y < radius || position.y > MAP_HEIGHT - radius) {
            return false
        }

        // Check collision with other units
        units.values.forEach { unit ->
            if (unit.isAlive && unit.position.distanceTo(position) < radius + 20f) {
                return false
            }
        }

        // Check collision with buildings
        buildings.values.forEach { building ->
            if (building.position.distanceTo(position) < radius + building.radius) {
                return false
            }
        }

        return true
    }

    /**
     * Check win/lose conditions
     */
    private fun checkGameEndConditions() {
        val playerBuildings = buildings.values.filter {
            it.ownerId == gameState.currentPlayerId && it.isAlive
        }

        val enemyBuildings = buildings.values.filter {
            it.ownerId != gameState.currentPlayerId && it.isAlive
        }

        val playerUnits = units.values.filter {
            it.ownerId == gameState.currentPlayerId && it.isAlive
        }

        val enemyUnits = units.values.filter {
            it.ownerId != gameState.currentPlayerId && it.isAlive
        }

        when {
            playerBuildings.isEmpty() && playerUnits.isEmpty() -> {
                endGame(GameResult.DEFEAT)
            }
            enemyBuildings.isEmpty() && enemyUnits.isEmpty() -> {
                endGame(GameResult.VICTORY)
            }
        }
    }

    private fun endGame(result: GameResult) {
        isRunning = false
        gameState.gameResult = result
    }

    /**
     * Pause the game
     */
    fun pause() {
        gameState.isPaused = true
    }

    /**
     * Resume the game
     */
    fun resume() {
        gameState.isPaused = false
        lastUpdateTime = System.currentTimeMillis()
    }

    /**
     * Check if game is paused
     */
    fun isPaused(): Boolean = gameState.isPaused

    /**
     * Get current game state
     */
    fun getGameState(): GameState = gameState

    /**
     * Get all units
     */
    fun getAllUnits(): Map<Long, Unit> = units.toMap()

    /**
     * Get all buildings
     */
    fun getAllBuildings(): Map<Long, Building> = buildings.toMap()

    /**
     * Get all resources
     */
    fun getAllResources(): Map<Long, Resource> = resources.toMap()

    /**
     * Get game map dimensions
     */
    fun getMapSize(): Vector2 = Vector2.new(MAP_WIDTH, MAP_HEIGHT)

    /**
     * Get the entity manager
     */
    fun getEntityManager(): EntityManager = entityManager

    /**
     * Handle touch events for HUD and game input
     * @param event The motion event
     * @return true if the event was handled
     */
    fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        // First check if HUD handles the touch
        hudManager?.let { hud ->
            if (hud.onTouchEvent(event)) {
                return true
            }
        }
        return false
    }

    /**
     * Draw the HUD overlay
     */
    fun drawHUD(canvas: android.graphics.Canvas) {
        hudManager?.draw(canvas)
    }

    /**
     * Check if touch is within UI area
     */
    fun isTouchInUI(x: Float, y: Float): Boolean {
        return hudManager?.isTouchInUI(x, y) ?: false
    }

    /**
     * Check if game is running
     */
    fun isGameRunning(): Boolean = isRunning

    /**
     * Stop the game
     */
    fun stop() {
        isRunning = false
    }

    enum class PlayerType {
        HUMAN, AI
    }

    enum class GameResult {
        VICTORY, DEFEAT
    }
}
