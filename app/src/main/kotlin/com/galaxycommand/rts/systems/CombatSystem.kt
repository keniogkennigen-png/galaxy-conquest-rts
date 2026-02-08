package com.galaxycommand.rts.systems

import com.galaxycommand.rts.core.GameEngine
import com.galaxycommand.rts.core.Vector2
import com.galaxycommand.rts.entities.Unit

/**
 * Combat system for handling unit combat, damage calculation, and attack cooldowns.
 */
class CombatSystem {

    companion object {
        // Combat constants
        const val DEFAULT_ATTACK_RANGE = 100f
        const val DEFAULT_ATTACK_COOLDOWN = 1f
        const val MIN_DAMAGE = 1f

        // Splash damage constants
        const val SPLASH_RADIUS = 50f
        const val SPLASH_DAMAGE_PERCENT = 0.5f

        // Critical hit constants
        const val CRITICAL_CHANCE = 0.05f
        const val CRITICAL_MULTIPLIER = 2f
    }

    // Combat log
    private val combatLog = mutableListOf<CombatEvent>()
    private var maxCombatLogSize = 100

    // Damage modifiers by unit type
    private val unitTypeModifiers = mapOf(
        "Marine" to mapOf("Light" to 1.5f, "Armored" to 0.5f),
        "Firebat" to mapOf("Light" to 2f, "Biological" to 1.5f),
        "Marauder" to mapOf("Armored" to 1.5f, "Mechanical" to 1.5f),
        "Ghost" to mapOf("Light" to 1.25f, "Heroic" to 2f),
        "Hellion" to mapOf("Light" to 1.5f),
        "Tank" to mapOf("Armored" to 1.5f),
        "Thor" to mapOf("Light" to 2f, "Massive" to 1.5f),
        "Reaper" to mapOf("Light" to 1.5f),
        "Viking" to mapOf("Air" to 1.5f, "Armored" to 1.5f),
        "Banshee" to mapOf("Light" to 1.5f),
        "Raven" to mapOf("Mechanical" to 1.5f),
        "Battlecruiser" to mapOf("Massive" to 1.5f),

        // Zerg
        "Zergling" to mapOf("Light" to 1.5f),
        "Roach" to mapOf("Armored" to 1.5f),
        "Hydralisk" to mapOf("Light" to 1.5f, "Armored" to 1.5f),
        "Ultralisk" to mapOf("Massive" to 1.5f),
        "Mutalisk" to mapOf("Light" to 1.5f),
        "Corruptor" to mapOf("Massive" to 1.5f),
        "BroodLord" to mapOf("Massive" to 1.5f),
        "Infestor" to mapOf("Biological" to 1.5f),
        "Queen" to mapOf("Light" to 1.5f),

        // Protoss
        "Zealot" to mapOf("Light" to 1.5f),
        "Stalker" to mapOf("Armored" to 1.5f),
        "Adept" to mapOf("Light" to 1.5f),
        "Sentry" to mapOf("Light" to 1.5f),
        "Immortal" to mapOf("Armored" to 1.5f, "Massive" to 2f),
        "Colossus" to mapOf("Light" to 1.5f),
        "HighTemplar" to mapOf("Light" to 1.5f, "Biological" to 2f),
        "DarkTemplar" to mapOf("Light" to 2f, "Heroic" to 3f),
        "Oracle" to mapOf("Light" to 1.5f),
        "Tempest" to mapOf("Air" to 1.5f, "Massive" to 1.5f),
        "Voidray" to mapOf("Armored" to 1.5f),
        "Phoenix" to mapOf("Light" to 1.5f),
        "Carrier" to mapOf("Air" to 1.5f),
        "Mothership" to mapOf("Everything" to 1.5f)
    )

    /**
     * Initialize combat system
     */
    fun initialize() {
        combatLog.clear()
    }

    /**
     * Update combat state
     */
    fun update(deltaTime: Float, units: List<Unit>, gameEngine: GameEngine) {
        // Check for unit deaths and remove them
        val deadUnits = units.filter { !it.isAlive && it.state != Unit.UnitState.DEAD }
        deadUnits.forEach { unit ->
            unit.state = Unit.UnitState.DEAD
            logCombatEvent(CombatEvent(CombatEvent.EventType.DEATH, unit.id, null, unit.health))
        }
    }

    /**
     * Execute attack from attacker to target
     */
    fun attack(attacker: Unit, target: Unit) {
        if (!attacker.canAttack || !target.isAlive) return

        val currentTime = System.currentTimeMillis() / 1000f
        if (!attacker.isReadyToAttack(currentTime)) return

        // Calculate damage
        val baseDamage = attacker.getEffectiveDamage()
        val damage = calculateDamage(attacker, target)

        // Apply critical hit chance
        val finalDamage = if (Math.random() < CRITICAL_CHANCE) {
            logCombatEvent(CombatEvent(CombatEvent.EventType.CRITICAL, attacker.id, target.id, damage))
            (damage * CRITICAL_MULTIPLIER).toInt()
        } else {
            damage.toInt()
        }

        // Apply damage to target
        val actualDamage = target.takeDamage(finalDamage.toFloat())

        // Log the attack
        logCombatEvent(CombatEvent(CombatEvent.EventType.ATTACK, attacker.id, target.id, actualDamage))

        // Perform attack cooldown
        attacker.performAttack(currentTime)

        // Check if target died
        if (!target.isAlive) {
            logCombatEvent(CombatEvent(CombatEvent.EventType.KILL, attacker.id, target.id, 0f))
        }
    }

    /**
     * Calculate damage with all modifiers
     */
    fun calculateDamage(attacker: Unit, target: Unit): Float {
        var damage = attacker.getEffectiveDamage()

        // Apply unit type modifiers
        val attackerModifiers = unitTypeModifiers[attacker.type]
        if (attackerModifiers != null) {
            // Check if target matches any modifier type
            val targetType = getTargetType(target)
            val modifier = attackerModifiers[targetType] ?: attackerModifiers.values.firstOrNull()
            if (modifier != null) {
                damage *= modifier
            }
        }

        // Apply armor reduction
        damage = (damage - target.armor).coerceAtLeast(MIN_DAMAGE)

        // Apply shield reduction first (handled in Unit.takeDamage)

        return damage
    }

    /**
     * Get target type for damage calculation
     */
    private fun getTargetType(target: Unit): String {
        // Simplified target typing
        return when {
            target.maxShield > 0 -> "Shielded"
            target.type in listOf("Marine", "Firebat", "Reaper", "Ghost", "Zergling", "Zealot", "Probe", "Drone", "SCV") -> "Light"
            target.type in listOf("Tank", "Marauder", "Roach", "Hydralisk", "Stalker", "Immortal", "Voidray", "Viking") -> "Armored"
            target.type in listOf("Ultralisk", "Thor", "Colossus", "Tempest", "Battlecruiser", "BroodLord", "Carrier", "Mothership") -> "Massive"
            target.type in listOf("Infestor", "Queen", "HighTemplar", "Medivac", "Overlord", "Overseer", "Mothership") -> "Biological"
            target.type in listOf("Hellion", "Raven", "Banshee", "Mutalisk", "Corruptor", "Oracle", "Phoenix") -> "Mechanical"
            target.type in listOf("Banshee", "Viking", "Mutalisk", "Corruptor", "Phoenix", "Voidray", "Tempest", "Carrier") -> "Air"
            else -> "Standard"
        }
    }

    /**
     * Execute area attack (splash damage)
     */
    fun areaAttack(attacker: Unit, center: Vector2, radius: Float, units: List<Unit>) {
        if (!attacker.canAttack) return

        val currentTime = System.currentTimeMillis() / 1000f
        if (!attacker.isReadyToAttack(currentTime)) return

        // Find all units in area
        val targets = units.filter {
            it.isAlive && it.position.distanceTo(center) <= radius && it.ownerId != attacker.ownerId
        }

        if (targets.isEmpty()) return

        // Calculate base damage
        val baseDamage = attacker.getEffectiveDamage()

        // Apply splash damage to each target
        targets.forEach { target ->
            val distance = target.position.distanceTo(center)
            val distanceFactor = 1f - (distance / radius).coerceIn(0f, 1f)
            val damage = (baseDamage * SPLASH_DAMAGE_PERCENT * distanceFactor).toInt()

            if (damage > 0) {
                target.takeDamage(damage.toFloat())
                logCombatEvent(CombatEvent(CombatEvent.EventType.ATTACK, attacker.id, target.id, damage.toFloat()))
            }
        }

        // Perform attack cooldown
        attacker.performAttack(currentTime)
    }

    /**
     * Log a combat event
     */
    private fun logCombatEvent(event: CombatEvent) {
        combatLog.add(event)
        if (combatLog.size > maxCombatLogSize) {
            combatLog.removeAt(0)
        }
    }

    /**
     * Get recent combat events
     */
    fun getRecentEvents(count: Int = 10): List<CombatEvent> {
        val startIndex = (combatLog.size - count).coerceAtLeast(0)
        return combatLog.subList(startIndex, combatLog.size)
    }

    /**
     * Get all combat events
     */
    fun getAllEvents(): List<CombatEvent> = combatLog.toList()

    /**
     * Clear combat log
     */
    fun clearLog() {
        combatLog.clear()
    }

    /**
     * Check if two units can attack each other
     */
    fun canAttackEachOther(attacker: Unit, target: Unit): Boolean {
        if (!attacker.canAttack || !target.isAlive) return false

        // Check if target is in range
        return attacker.position.distanceTo(target.position) <= attacker.attackRange
    }

    /**
     * Find best target for attacker
     */
    fun findBestTarget(attacker: Unit, enemies: List<Unit>): Unit? {
        var bestTarget: Unit? = null
        var bestScore = Float.MAX_VALUE

        enemies.forEach { target ->
            if (target.isAlive) {
                val distance = attacker.position.distanceTo(target.position)
                if (distance <= attacker.attackRange) {
                    // Score based on threat (closer units score higher)
                    val score = distance - target.health
                    if (score < bestScore) {
                        bestScore = score
                        bestTarget = target
                    }
                }
            }
        }

        return bestTarget
    }

    /**
     * Get combat statistics
     */
    fun getStats(): CombatStats {
        val attacks = combatLog.filter { it.type == CombatEvent.EventType.ATTACK }
        val kills = combatLog.filter { it.type == CombatEvent.EventType.KILL }
        val deaths = combatLog.filter { it.type == CombatEvent.EventType.DEATH }

        return CombatStats(
            totalAttacks = attacks.size,
            totalKills = kills.size,
            totalDeaths = deaths.size,
            totalDamageDealt = attacks.sumOf { it.value.toDouble() }.toFloat(),
            avgDamagePerAttack = if (attacks.isNotEmpty()) attacks.sumOf { it.value.toDouble() }.toFloat() / attacks.size else 0f
        )
    }

    /**
     * Data class for combat events
     */
    data class CombatEvent(
        val type: EventType,
        val attackerId: Long,
        val targetId: Long?,
        val value: Float,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        enum class EventType {
            ATTACK,
            KILL,
            DEATH,
            CRITICAL
        }
    }

    /**
     * Combat statistics
     */
    data class CombatStats(
        val totalAttacks: Int,
        val totalKills: Int,
        val totalDeaths: Int,
        val totalDamageDealt: Float,
        val avgDamagePerAttack: Float
    )
}
