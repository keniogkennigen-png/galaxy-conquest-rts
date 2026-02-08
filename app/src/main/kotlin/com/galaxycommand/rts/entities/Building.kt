package com.galaxycommand.rts.entities

import com.galaxycommand.rts.core.Vector2
import com.galaxycommand.rts.factions.FactionType

/**
 * Building class representing static game entities.
 * Includes bases, production facilities, and defensive structures.
 */
data class Building(
    override var id: Long = 0,
    override var type: String = "Building",
    override var position: Vector2 = Vector2.ZERO,
    override var ownerId: Long = 0,

    // Building stats
    var health: Float = 500f,
    var maxHealth: Float = 500f,
    var shield: Float = 0f,
    var maxShield: Float = 0f,
    var armor: Float = 1f,

    // Size and shape
    var width: Float = 100f,
    var height: Float = 80f,

    // Production queue
    val productionQueue: MutableList<ProductionItem> = mutableListOf(),
    var isProducing: Boolean = false,
    var currentProductionTime: Float = 0f,
    var currentProduct: UnitType? = null,

    // Resource storage
    var storedMinerals: Int = 0,
    var storedGas: Int = 0,
    var maxStorage: Int = 200,

    // State
    var isUnderConstruction: Boolean = false,
    var constructionProgress: Float = 0f,
    var builderUnitId: Long? = null,

    // Type classification
    val buildingType: BuildingType = BuildingType.BASE,

    // Special properties
    var spawnPoint: Vector2 = position,
    var rallyPoint: Vector2 = position + Vector2.new(100f, 0f),

    // Faction
    var faction: FactionType = FactionType.VANGUARD,

    override var radius: Float = 50f
) : Entity(id, type, position, ownerId, radius, isAlive) {

    /**
     * Building type classification
     */
    enum class BuildingType {
        BASE,           // Main base (Command Center, Hatchery, Nexus)
        PRODUCTION,     // Unit production (Barracks, Spawning Pool, Gateway)
        TECHNOLOGY,     // Tech buildings (Engineering Bay, Evolution Chamber, Cybernetics)
        DEFENSE,        // Turrets, Cannons, Spines
        ECONOMY,        // Refineries, Extractors, Assimilators
        SPECIAL         // Special buildings
    }

    /**
     * Production queue item
     */
    data class ProductionItem(
        val unitType: UnitType,
        val mineralCost: Int,
        val gasCost: Int,
        val productionTime: Float,
        var remainingTime: Float = productionTime
    )

    /**
     * Unit types that can be produced
     */
    enum class UnitType(
        val displayName: String,
        val mineralCost: Int,
        val gasCost: Int,
        val productionTime: Float
    ) {
        MARINE("Marine", 50, 0, 25f),
        SCV("SCV", 50, 0, 20f),
        FIREBAT("Firebat", 75, 0, 30f),
        MARAUDER("Marauder", 100, 25, 35f),
        REAPER("Reaper", 50, 50, 30f),
        GHOST("Ghost", 150, 100, 50f),
        HELLION("Hellion", 100, 0, 30f),
        TANK("Siege Tank", 150, 100, 50f),
        THOR("Thor", 300, 200, 80f),
        MEDIVAC("Medivac", 100, 100, 40f),
        VIKING("Viking", 120, 70, 40f),
        BANSHEE("Banshee", 150, 100, 60f),
        RAVEN("Raven", 140, 100, 50f),
        BATTLECRUISER("Battlecruiser", 400, 300, 100f),

        ZERGLING("Zergling", 25, 0, 20f),
        ROACH("Roach", 75, 0, 30f),
        HYDRA("Hydralisk", 100, 50, 35f),
        MUTALISK("Mutalisk", 100, 100, 40f),
        CORRUPTOR("Corruptor", 150, 100, 45f),
        ULTRALISK("Ultralisk", 200, 100, 60f),
        INFESTOR("Infestor", 150, 150, 50f),
        BLORG("Brood Lord", 300, 250, 80f),
        OVERLORD("Overlord", 100, 0, 50f),
        OVERSEER("Overseer", 150, 50, 30f),
        QUEEN("Queen", 150, 0, 40f),
        DRONE("Drone", 50, 0, 20f),

        ZEALOT("Zealot", 100, 0, 35f),
        STALKER("Stalker", 125, 50, 35f),
        ADEPT("Adept", 100, 25, 30f),
        SENTRY("Sentry", 150, 100, 35f),
        IMMORTAL("Immortal", 200, 100, 50f),
        COLOSSUS("Colossus", 300, 200, 60f),
        HIGHTEMPLAR("High Templar", 150, 150, 50f),
        DARKTEMPLAR("Dark Templar", 125, 100, 45f),
        ORACLE("Oracle", 150, 150, 40f),
        TEMPEST("Tempest", 250, 175, 55f),
        CARRIER("Carrier", 350, 250, 80f),
        MOTHERSHIP("Mothership", 400, 400, 100f),
        PROBE("Probe", 50, 0, 20f),
        PHOENIX("Phoenix", 150, 100, 40f),
        VOIDRAY("Voidray", 200, 150, 50f)
    }

    override fun getCategory(): Entity.EntityCategory = Entity.EntityCategory.BUILDING

    override fun getDisplayName(): String = type

    override fun getHealthPercent(): Float {
        return if (maxHealth > 0) (health / maxHealth).coerceIn(0f, 1f) else 0f
    }

    /**
     * Check if main base
     */
    val Boolean
        get() = buildingType isMainBase: == BuildingType.BASE

    /**
     * Check if can produce units
     */
    val canProduce: Boolean
        get() = buildingType == BuildingType.PRODUCTION ||
                buildingType == BuildingType.BASE ||
                buildingType == BuildingType.SPECIAL

    /**
     * Check if production facility
     */
    val isProductionFacility: Boolean
        get() = buildingType == BuildingType.PRODUCTION

    /**
     * Check if under construction
     */
    val isConstructionComplete: Boolean
        get() = !isUnderConstruction || constructionProgress >= 100f

    /**
     * Take damage
     */
    fun takeDamage(amount: Float): Float {
        var damage = amount - armor

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
     * Add to production queue
     */
    fun addToQueue(unitType: UnitType): Boolean {
        if (!canProduce || !isConstructionComplete) return false

        productionQueue.add(
            ProductionItem(
                unitType = unitType,
                mineralCost = unitType.mineralCost,
                gasCost = unitType.gasCost,
                productionTime = unitType.productionTime
            )
        )
        return true
    }

    /**
     * Update production
     */
    fun updateProduction(deltaTime: Float) {
        if (productionQueue.isEmpty()) {
            isProducing = false
            currentProduct = null
            return
        }

        val currentItem = productionQueue.first()
        currentItem.remainingTime -= deltaTime
        currentProductionTime += deltaTime
        isProducing = true
        currentProduct = currentItem.unitType

        if (currentItem.remainingTime <= 0) {
            productionQueue.removeAt(0)
            currentProductionTime = 0f
        }
    }

    /**
     * Get current production progress (0-1)
     */
    fun getProductionProgress(): Float {
        if (productionQueue.isEmpty()) return 0f
        val currentItem = productionQueue.first()
        return 1f - (currentItem.remainingTime / currentItem.productionTime)
    }

    /**
     * Get current queue count
     */
    fun getQueueCount(): Int = productionQueue.size

    /**
     * Cancel production
     */
    fun cancelProduction(): Boolean {
        if (productionQueue.isEmpty()) return false
        productionQueue.removeAt(0)
        currentProductionTime = 0f
        return true
    }

    /**
     * Clear production queue
     */
    fun clearQueue() {
        productionQueue.clear()
        currentProductionTime = 0f
        isProducing = false
    }

    /**
     * Get spawn position for produced units
     */
    fun getSpawnPosition(): Vector2 {
        return spawnPoint
    }

    /**
     * Set rally point
     */
    fun setRallyPoint(point: Vector2) {
        rallyPoint = point
    }

    /**
     * Start construction
     */
    fun startConstruction() {
        isUnderConstruction = true
        constructionProgress = 0f
    }

    /**
     * Continue construction
     */
    fun construct(amount: Float) {
        if (!isUnderConstruction) return
        constructionProgress = (constructionProgress + amount).coerceAtMost(100f)

        if (constructionProgress >= 100f) {
            isUnderConstruction = false
            health = maxHealth
        }
    }

    /**
     * Store minerals
     */
    fun storeMinerals(amount: Int): Int {
        val canStore = (maxStorage - storedMinerals).coerceAtLeast(0)
        val stored = amount.coerceAtMost(canStore)
        storedMinerals += stored
        return stored
    }

    /**
     * Withdraw minerals
     */
    fun withdrawMinerals(amount: Int): Int {
        val withdraw = amount.coerceAtMost(storedMinerals)
        storedMinerals -= withdraw
        return withdraw
    }

    /**
     * Check if unit can spawn here
     */
    fun canSpawnUnit(): Boolean {
        // Check for space and no units blocking spawn point
        return isConstructionComplete && isAlive
    }

    override fun destroy() {
        super.destroy()
        clearQueue()
    }

    override fun getRenderOrder(): Int {
        return (position.y + height).toInt()
    }

    override fun serialize(): String {
        return "${id}|${type}|${position.x}|${position.y}|${ownerId}|${health}|${maxHealth}|${isUnderConstruction}|${constructionProgress}|${productionQueue.size}"
    }

    companion object {
        fun deserialize(data: String): Building {
            val parts = data.split("|")
            return Building(
                id = parts[0].toLong(),
                type = parts[1],
                position = Vector2.new(parts[2].toFloat(), parts[3].toFloat()),
                ownerId = parts[4].toLong(),
                health = parts[5].toFloat(),
                maxHealth = parts[6].toFloat(),
                isUnderConstruction = parts[7].toBoolean(),
                constructionProgress = parts[8].toFloat()
            )
        }

        /**
         * Create base building
         */
        fun createBaseBuilding(position: Vector2, faction: FactionType): Building {
            return when (faction) {
                FactionType.VANGUARD -> Building(
                    type = "Command Center",
                    position = position,
                    health = 1500f,
                    maxHealth = 1500f,
                    armor = 3f,
                    width = 160f,
                    height = 120f,
                    buildingType = BuildingType.BASE,
                    spawnPoint = position + Vector2.new(0f, 80f),
                    faction = faction,
                    radius = 80f,
                    maxStorage = 500
                )
                FactionType.SWARM -> Building(
                    type = "Hatchery",
                    position = position,
                    health = 1250f,
                    maxHealth = 1250f,
                    armor = 1f,
                    width = 140f,
                    height = 120f,
                    buildingType = BuildingType.BASE,
                    spawnPoint = position + Vector2.new(0f, 80f),
                    faction = faction,
                    radius = 70f,
                    maxStorage = 400
                )
                FactionType.SYNOD -> Building(
                    type = "Nexus",
                    position = position,
                    health = 1000f,
                    maxHealth = 1000f,
                    shield = 500f,
                    maxShield = 500f,
                    armor = 2f,
                    width = 120f,
                    height = 100f,
                    buildingType = BuildingType.BASE,
                    spawnPoint = position + Vector2.new(0f, 70f),
                    faction = faction,
                    radius = 60f,
                    maxStorage = 400
                )
            }
        }

        /**
         * Create production building
         */
        fun createProductionBuilding(position: Vector2, ownerId: Long, faction: FactionType, unitTypes: List<UnitType>): Building {
            val building = when (faction) {
                FactionType.VANGUARD -> Building(
                    type = "Barracks",
                    position = position,
                    ownerId = ownerId,
                    health = 1000f,
                    maxHealth = 1000f,
                    armor = 2f,
                    width = 120f,
                    height = 100f,
                    buildingType = BuildingType.PRODUCTION,
                    spawnPoint = position + Vector2.new(0f, 60f),
                    faction = faction,
                    radius = 60f
                )
                FactionType.SWARM -> Building(
                    type = "Spawning Pool",
                    position = position,
                    ownerId = ownerId,
                    health = 750f,
                    maxHealth = 750f,
                    armor = 1f,
                    width = 100f,
                    height = 90f,
                    buildingType = BuildingType.PRODUCTION,
                    spawnPoint = position + Vector2.new(0f, 60f),
                    faction = faction,
                    radius = 50f
                )
                FactionType.SYNOD -> Building(
                    type = "Gateway",
                    position = position,
                    ownerId = ownerId,
                    health = 800f,
                    maxHealth = 800f,
                    shield = 400f,
                    maxShield = 400f,
                    armor = 2f,
                    width = 110f,
                    height = 90f,
                    buildingType = BuildingType.PRODUCTION,
                    spawnPoint = position + Vector2.new(0f, 60f),
                    faction = faction,
                    radius = 55f
                )
            }

            // Note: In a full implementation, we'd set up available unit types
            return building
        }
    }
}
