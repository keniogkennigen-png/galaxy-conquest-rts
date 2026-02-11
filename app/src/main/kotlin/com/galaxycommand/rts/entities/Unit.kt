package com.galaxycommand.rts.entities

import com.galaxycommand.rts.core.Vector2
import com.galaxycommand.rts.factions.FactionType

/**
 * Unit class representing mobile game entities.
 * Includes workers, combat units, and specialized units.
 */
data class Unit(
    override var id: Long = 0,
    override var type: String = "Unit",
    override var position: Vector2 = Vector2.ZERO,
    override var ownerId: Long = 0,

    // Combat stats
    var health: Float = 100f,
    var maxHealth: Float = 100f,
    var shield: Float = 0f,
    var maxShield: Float = 0f,
    var armor: Float = 0f,

    // Combat properties
    var attackDamage: Float = 10f,
    var attackRange: Float = 100f,
    var attackSpeed: Float = 1f,
    var lastAttackTime: Float = 0f,

    // Movement
    var speed: Float = 100f,
    var target: Vector2? = null,
    var path: List<Vector2> = emptyList(),

    // State machine
    var state: UnitState = UnitState.IDLE,
    var targetUnit: Unit? = null,

    // Selection and UI
    var isSelected: Boolean = false,

    // Unit type flags
    val unitType: UnitType = UnitType.COMBAT,

    // Worker properties
    var isWorker: Boolean = false,
    var carrying: Int = 0,
    var maxCarry: Int = 10,
    var targetResource: Resource? = null,

    // Gathering
    var gatherTarget: Vector2? = null,
    var returnTarget: Building? = null,

    // Building construction
    var isBuilder: Boolean = false,
    var buildingTarget: Building? = null,
    var buildProgress: Float = 0f,
    var buildRate: Float = 20f,

    // Vision and detection
    var sightRange: Float = 150f,

    // Faction specific
    var faction: FactionType = FactionType.VANGUARD,

    // Visual properties
    override var radius: Float = 15f
) : Entity(id, type, position, ownerId, radius, true) {

    /**
     * Get unit state enum
     */
    enum class UnitState {
        IDLE,
        MOVING,
        ATTACKING,
        GATHERING,
        RETURNING,
        BUILDING,
        PATROLLING,
        HOLDING_POSITION,
        DEAD
    }

    /**
     * Unit type classification
     */
    enum class UnitType {
        WORKER,
        COMBAT,
        HERO,
        AIR
    }

    override fun getCategory(): Entity.EntityCategory = Entity.EntityCategory.UNIT

    /**
     * Update unit logic
     */
    override fun update(deltaTime: Float) {
        if (!isAlive) return

        // Regenerate shields if applicable
        if (hasShields() && shield < maxShield) {
            shield = (shield + 5f * deltaTime).coerceAtMost(maxShield)
        }
    }

    override fun getHealthPercent(): Float {
        return if (maxHealth > 0) (health / maxHealth).coerceIn(0f, 1f) else 0f
    }

    /**
     * Get shield percentage
     */
    fun getShieldPercent(): Float {
        return if (maxShield > 0) (shield / maxShield).coerceIn(0f, 1f) else 0f
    }

    /**
     * Check if unit has shield
     */
    fun hasShields(): Boolean = maxShield > 0

    /**
     * Take damage
     */
    fun takeDamage(amount: Float): Float {
        var damage = amount - armor

        // Apply shield first
        if (hasShields() && shield > 0) {
            if (shield >= damage) {
                shield -= damage
                damage = 0f
            } else {
                damage -= shield
                shield = 0f
            }
        }

        health -= damage.coerceAtLeast(0f)

        if (health <= 0) {
            health = 0f
            destroy()
        }

        return damage
    }

    /**
     * Heal unit
     */
    fun heal(amount: Float) {
        health = (health + amount).coerceAtMost(maxHealth)
    }

    /**
     * Add shield
     */
    fun addShield(amount: Float) {
        if (hasShields()) {
            shield = (shield + amount).coerceAtMost(maxShield)
        }
    }

    /**
     * Regenerate health/shields
     */
    fun regenerate(deltaTime: Float) {
        // Health regeneration (Zerg-style)
        if (faction == FactionType.SWARM && health < maxHealth) {
            health = (health + 0.5f * deltaTime).coerceAtMost(maxHealth)
        }

        // Shield regeneration (Protoss-style)
        if (hasShields() && shield < maxShield) {
            shield = (shield + 1f * deltaTime).coerceAtMost(maxShield)
        }
    }

    /**
     * Check if combat unit
     */
    val isCombatUnit: Boolean
        get() = unitType == UnitType.COMBAT || unitType == UnitType.AIR

    /**
     * Check if can attack
     */
    val canAttack: Boolean
        get() = isCombatUnit && attackDamage > 0

    /**
     * Check if ready to attack
     */
    fun isReadyToAttack(currentTime: Float): Boolean {
        return canAttack && (currentTime - lastAttackTime) >= (1f / attackSpeed)
    }

    /**
     * Perform attack cooldown
     */
    fun performAttack(currentTime: Float) {
        lastAttackTime = currentTime
    }

    /**
     * Move toward target position
     */
    fun moveToward(targetPos: Vector2, deltaTime: Float): Boolean {
        val direction = (targetPos - position).normalized()
        position = position.plus(direction.times(speed * deltaTime))

        // Check if reached target
        return position.distanceTo(targetPos) < 5f
    }

    /**
     * Stop unit
     */
    fun stop() {
        state = UnitState.IDLE
        target = null
        path = emptyList()
    }

    /**
     * Hold position
     */
    fun holdPosition() {
        state = UnitState.HOLDING_POSITION
        target = null
        path = emptyList()
    }

    /**
     * Patrol between points
     */
    fun patrol(positions: List<Vector2>) {
        state = UnitState.PATROLLING
        // Patrol logic would cycle through positions
    }

    /**
     * Get effective range (including bonus)
     */
    fun getEffectiveRange(): Float = attackRange

    /**
     * Get effective damage (including bonuses)
     */
    fun getEffectiveDamage(): Float = attackDamage

    /**
     * Check if unit can attack target
     */
    fun canAttackTarget(target: Unit): Boolean {
        if (!canAttack || !target.isAlive) return false
        return position.distanceTo(target.position) <= attackRange
    }

    /**
     * Check if target is in range
     */
    fun isTargetInRange(targetPos: Vector2): Boolean {
        return position.distanceTo(targetPos) <= attackRange
    }

    override fun destroy() {
        super.destroy()
        state = UnitState.DEAD
    }

    /**
     * Select unit
     */
    fun select() {
        isSelected = true
    }

    /**
     * Deselect unit
     */
    fun deselect() {
        isSelected = false
    }

    /**
     * Toggle selection
     */
    fun toggleSelection() {
        isSelected = !isSelected
    }

    override fun getRenderOrder(): Int {
        return position.y.toInt()
    }

    override fun serialize(): String {
        return "${id}|${type}|${position.x}|${position.y}|${ownerId}|${health}|${maxHealth}|${state.ordinal}|${isSelected}"
    }

    companion object {
        fun deserialize(data: String): Unit {
            val parts = data.split("|")
            return Unit(
                id = parts[0].toLong(),
                type = parts[1],
                position = Vector2.new(parts[2].toFloat(), parts[3].toFloat()),
                ownerId = parts[4].toLong(),
                health = parts[5].toFloat(),
                maxHealth = parts[6].toFloat(),
                state = UnitState.entries[parts[7].toInt()],
                isSelected = parts[8].toBoolean()
            )
        }

        /**
         * Create a basic worker unit
         */
        fun createWorker(
            position: Vector2,
            ownerId: Long,
            faction: FactionType
        ): Unit {
            return when (faction) {
                FactionType.VANGUARD -> Unit(
                    type = "SCV",
                    position = position,
                    ownerId = ownerId,
                    health = 50f,
                    maxHealth = 50f,
                    speed = 120f,
                    sightRange = 100f,
                    unitType = UnitType.WORKER,
                    isWorker = true,
                    maxCarry = 8,
                    faction = faction,
                    radius = 12f
                )
                FactionType.SWARM -> Unit(
                    type = "Drone",
                    position = position,
                    ownerId = ownerId,
                    health = 40f,
                    maxHealth = 40f,
                    speed = 140f,
                    sightRange = 90f,
                    unitType = UnitType.WORKER,
                    isWorker = true,
                    maxCarry = 8,
                    faction = faction,
                    radius = 12f
                )
                FactionType.SYNODE -> Unit(
                    type = "Probe",
                    position = position,
                    ownerId = ownerId,
                    health = 60f,
                    maxHealth = 60f,
                    speed = 100f,
                    sightRange = 110f,
                    unitType = UnitType.WORKER,
                    isWorker = true,
                    maxCarry = 10,
                    faction = faction,
                    radius = 12f
                )
                else -> throw IllegalArgumentException("Unknown faction type: $faction")
            }
        }

        /**
         * Create a basic combat unit
         */
        fun createCombatUnit(
            position: Vector2,
            ownerId: Long,
            faction: FactionType
        ): Unit {
            return when (faction) {
                FactionType.VANGUARD -> Unit(
                    type = "Marine",
                    position = position,
                    ownerId = ownerId,
                    health = 45f,
                    maxHealth = 45f,
                    armor = 1f,
                    attackDamage = 6f,
                    attackRange = 150f,
                    attackSpeed = 1f,
                    speed = 90f,
                    sightRange = 130f,
                    unitType = UnitType.COMBAT,
                    isWorker = false,
                    faction = faction,
                    radius = 10f
                )
                FactionType.SWARM -> Unit(
                    type = "Zergling",
                    position = position,
                    ownerId = ownerId,
                    health = 35f,
                    maxHealth = 35f,
                    armor = 0f,
                    attackDamage = 5f,
                    attackRange = 20f,
                    attackSpeed = 1.5f,
                    speed = 180f,
                    sightRange = 100f,
                    unitType = UnitType.COMBAT,
                    isWorker = false,
                    faction = faction,
                    radius = 8f
                )
                FactionType.SYNODE -> Unit(
                    type = "Zealot",
                    position = position,
                    ownerId = ownerId,
                    health = 100f,
                    maxHealth = 100f,
                    shield = 50f,
                    maxShield = 50f,
                    armor = 1f,
                    attackDamage = 16f,
                    attackRange = 30f,
                    attackSpeed = 0.8f,
                    speed = 130f,
                    sightRange = 140f,
                    unitType = UnitType.COMBAT,
                    isWorker = false,
                    faction = faction,
                    radius = 14f
                )
                else -> throw IllegalArgumentException("Unknown faction type: $faction")
            }
        }
    }
}
