package com.galaxycommand.rts.systems

import com.galaxycommand.rts.core.GameEngine
import com.galaxycommand.rts.core.Vector2
import com.galaxycommand.rts.entities.Building
import com.galaxycommand.rts.entities.Unit
import com.galaxycommand.rts.factions.FactionType
import java.util.Random

/**
 * AI Engine for enemy opponent behavior.
 * Uses a Finite State Machine (FSM) for decision making.
 */
class AIEngine {

    companion object {
        // AI Update interval
        private const val AI_UPDATE_INTERVAL = 0.5f // seconds

        // Resource thresholds
        private const val WORKER_LIMIT = 16
        private const val ARMY_LIMIT = 40
        private const val EXPAND_THRESHOLD = 800
    }

    private var gameEngine: GameEngine? = null
    private var difficulty: Difficulty = Difficulty.MEDIUM
    private var lastUpdateTime = 0f
    private var random = Random()

    // AI State
    private var currentState: AIState = AIState.IDLE
    private var stateTimer = 0f
    private var expandRequested = false
    private var attackRequested = false

    // Economy tracking
    private var workersBuilt = 0
    private var lastResourceCheck = 0f

    // Unit production tracking
    private var combatUnitsBuilt = 0

    /**
     * AI difficulty levels
     */
    enum class Difficulty {
        EASY,      // Random decisions, slow reactions
        MEDIUM,    // Balanced behavior
        HARD,      // Optimal decisions, fast reactions
        INSANE     // Perfect play
    }

    /**
     * AI States
     */
    enum class AIState {
        IDLE,
        ECONOMY,   // Focus on economy
        EXPAND,    // Expand to new bases
        ARMY,      // Build army
        ATTACK,    // Launch attack
        DEFEND,    // Defend base
        RETREAT    // Retreat damaged units
    }

    /**
     * Initialize AI with difficulty
     */
    fun initialize(engine: GameEngine, difficulty: Difficulty) {
        this.gameEngine = engine
        this.difficulty = difficulty
        this.currentState = AIState.ECONOMY
        this.stateTimer = 0f
        this.workersBuilt = 0
        this.combatUnitsBuilt = 0
    }

    /**
     * Update AI behavior
     */
    fun update(deltaTime: Float) {
        if (gameEngine?.isPaused() == true) return

        lastUpdateTime += deltaTime
        if (lastUpdateTime < AI_UPDATE_INTERVAL) return

        lastUpdateTime = 0f
        stateTimer += deltaTime

        // Get current game state
        val state = gameEngine?.getGameState() ?: return
        val minerals = state.minerals
        val gas = state.gas

        // Check for threats
        val nearbyEnemies = checkForEnemies()

        // State machine transition
        when (currentState) {
            AIState.IDLE -> {
                currentState = AIState.ECONOMY
            }
            AIState.ECONOMY -> {
                // Build workers until limit
                if (workersBuilt < WORKER_LIMIT && minerals >= 50) {
                    queueUnitProduction("worker")
                    workersBuilt++
                    minerals -= 50
                }
                // Expand when rich
                else if (minerals > EXPAND_THRESHOLD && !expandRequested) {
                    currentState = AIState.EXPAND
                    expandRequested = true
                }
                // Start building army
                else if (combatUnitsBuilt < 10 && minerals > 300) {
                    currentState = AIState.ARMY
                }
            }
            AIState.EXPAND -> {
                if (stateTimer > 5f || expandRequested) {
                    queueBuilding("expansion")
                    expandRequested = false
                    currentState = AIState.ECONOMY
                }
            }
            AIState.ARMY -> {
                // Build combat units
                if (combatUnitsBuilt < ARMY_LIMIT) {
                    queueUnitProduction("combat")
                    combatUnitsBuilt++
                }

                // Launch attack when army is ready
                if (combatUnitsBuilt >= ARMY_LIMIT || (nearbyEnemies.size >= 5 && stateTimer > 10f)) {
                    attackRequested = true
                    currentState = AIState.ATTACK
                }

                // Defend if enemies nearby
                if (nearbyEnemies.size >= 3) {
                    currentState = AIState.DEFEND
                }
            }
            AIState.ATTACK -> {
                // Issue attack command
                if (attackRequested) {
                    issueAttackCommand()
                    attackRequested = false
                }

                // Return to economy after attack
                if (stateTimer > 15f) {
                    currentState = AIState.ECONOMY
                    stateTimer = 0f
                }
            }
            AIState.DEFEND -> {
                // Rally units to defend
                if (nearbyEnemies.isNotEmpty()) {
                    issueDefendCommand(nearbyEnemies)
                }

                // Return to army building if no threat
                if (stateTimer > 8f) {
                    currentState = AIState.ARMY
                    stateTimer = 0f
                }
            }
            AIState.RETREAT -> {
                // Retreat damaged units
                issueRetreatCommand()

                if (stateTimer > 5f) {
                    currentState = AIState.ECONOMY
                    stateTimer = 0f
                }
            }
        }

        // Random events based on difficulty
        handleRandomEvents()
    }

    /**
     * Check for enemies near AI base
     */
    private fun checkForEnemies(): List<Unit> {
        val engine = gameEngine ?: return emptyList()
        val basePos = engine.getGameState().getPlayer(engine.getAllBuildings().values.firstOrNull { it.isMainBase }?.ownerId ?: 0L)?.basePosition
            ?: return emptyList()

        return engine.getAllUnits().values.filter {
            it.ownerId != engine.getGameState().currentPlayerId &&
            it.position.distanceTo(basePos) < 400f
        }
    }

    /**
     * Queue unit production
     */
    private fun queueUnitProduction(type: String) {
        val engine = gameEngine ?: return

        // Find production buildings
        val productionBuildings = engine.getAllBuildings().values.filter {
            it.buildingType == Building.BuildingType.PRODUCTION ||
            it.buildingType == Building.BuildingType.BASE
        }

        if (productionBuildings.isNotEmpty()) {
            // Random production building
            val building = productionBuildings.random()
            val unitType = when (type) {
                "worker" -> getWorkerType(engine.getGameState().playerFaction)
                "combat" -> getCombatType(engine.getGameState().playerFaction)
                else -> getCombatType(engine.getGameState().playerFaction)
            }
            building.addToQueue(unitType)
        }
    }

    /**
     * Queue building construction
     */
    private fun queueBuilding(type: String) {
        // AI building logic would go here
        // For simplicity, this just places random buildings
    }

    /**
     * Get worker unit type for faction
     */
    private fun getWorkerType(faction: FactionType): Building.UnitType {
        return when (faction) {
            FactionType.VANGUARD -> Building.UnitType.SCV
            FactionType.SWARM -> Building.UnitType.DRONE
            FactionType.SYNOD -> Building.UnitType.PROBE
        }
    }

    /**
     * Get combat unit type for faction (random selection)
     */
    private fun getCombatType(faction: FactionType): Building.UnitType {
        return when (faction) {
            FactionType.VANGUARD -> listOf(
                Building.UnitType.MARINE,
                Building.UnitType.REAPER,
                Building.UnitType.HELLION,
                Building.UnitType.MARAUDER
            ).random()
            FactionType.SWARM -> listOf(
                Building.UnitType.ZERGLING,
                Building.UnitType.ROACH,
                Building.UnitType.HYDRA
            ).random()
            FactionType.SYNOD -> listOf(
                Building.UnitType.ZEALOT,
                Building.UnitType.STALKER,
                Building.UnitType.ADEPT
            ).random()
        }
    }

    /**
     * Issue attack command to all combat units
     */
    private fun issueAttackCommand() {
        val engine = gameEngine ?: return

        // Get all combat units
        val combatUnits = engine.getAllUnits().values.filter {
            it.isCombatUnit && it.ownerId != engine.getGameState().currentPlayerId
        }

        if (combatUnits.isEmpty()) return

        // Find enemy base position
        val enemyBase = engine.getGameState().getAIPlayers().firstOrNull()?.basePosition
            ?: engine.getGameState().getCurrentPlayer()?.basePosition
            ?: Vector2.new(100f, 100f)

        // Issue move command to enemy base
        combatUnits.forEach { unit ->
            unit.target = enemyBase
            unit.state = Unit.UnitState.ATTACKING
        }
    }

    /**
     * Issue defend command against nearby enemies
     */
    private fun issueDefendCommand(enemies: List<Unit>) {
        val engine = gameEngine ?: return

        // Get defending units
        val defendingUnits = engine.getAllUnits().values.filter {
            it.isCombatUnit && it.ownerId != engine.getGameState().currentPlayerId
        }

        defendingUnits.forEach { unit ->
            // Find nearest enemy
            val nearestEnemy = enemies.minByOrNull { unit.position.distanceTo(it.position) }
            nearestEnemy?.let {
                unit.target = it.position
                unit.state = Unit.UnitState.ATTACKING
            }
        }
    }

    /**
     * Issue retreat command for damaged units
     */
    private fun issueRetreatCommand() {
        val engine = gameEngine ?: return

        // Get damaged combat units
        val damagedUnits = engine.getAllUnits().values.filter {
            it.isCombatUnit &&
            it.ownerId != engine.getGameState().currentPlayerId &&
            it.getHealthPercent() < 0.3f
        }

        // Get base position
        val basePos = engine.getGameState().getAIPlayers().firstOrNull()?.basePosition
            ?: return

        damagedUnits.forEach { unit ->
            unit.target = basePos
            unit.state = Unit.UnitState.MOVING
        }
    }

    /**
     * Handle random events based on difficulty
     */
    private fun handleRandomEvents() {
        val chance = when (difficulty) {
            Difficulty.EASY -> 0.02f
            Difficulty.MEDIUM -> 0.05f
            Difficulty.HARD -> 0.1f
            Difficulty.INSANE -> 0.2f
        }

        if (random.nextFloat() < chance) {
            when (random.nextInt(5)) {
                0 -> currentState = AIState.ARMY
                1 -> currentState = AIState.EXPAND
                2 -> currentState = AIState.DEFEND
                3 -> currentState = AIState.RETREAT
                4 -> attackRequested = true
            }
        }
    }

    /**
     * Force immediate attack
     */
    fun forceAttack() {
        currentState = AIState.ATTACK
        attackRequested = true
    }

    /**
     * Set difficulty level
     */
    fun setDifficulty(newDifficulty: Difficulty) {
        difficulty = newDifficulty
    }

    /**
     * Get current difficulty
     */
    fun getDifficulty(): Difficulty = difficulty

    /**
     * Get current state
     */
    fun getCurrentState(): AIState = currentState

    /**
     * Get AI statistics
     */
    fun getStats(): AIStats {
        return AIStats(
            currentState = currentState,
            workersBuilt = workersBuilt,
            combatUnitsBuilt = combatUnitsBuilt,
            attackRequested = attackRequested
        )
    }

    data class AIStats(
        val currentState: AIState,
        val workersBuilt: Int,
        val combatUnitsBuilt: Int,
        val attackRequested: Boolean
    )
}
