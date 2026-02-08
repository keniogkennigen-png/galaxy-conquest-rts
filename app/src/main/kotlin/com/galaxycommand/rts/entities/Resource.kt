package com.galaxycommand.rts.entities

import com.galaxycommand.rts.core.Vector2

/**
 * Resource class for mineral and gas extraction.
 */
data class Resource(
    override var id: Long = 0,
    override var type: String = "Resource",
    override var position: Vector2 = Vector2.ZERO,
    override var ownerId: Long = 0,

    // Resource properties
    val resourceType: ResourceType = ResourceType.MINERAL,
    var amount: Int = 1000,
    var maxAmount: Int = 1000,

    override var radius: Float = 40f,

    // Visual
    var crystalColor: Int = 0xFF00AAFF.toInt()
) : Entity(id, type, position, ownerId, radius, isAlive) {

    /**
     * Resource type enum
     */
    enum class ResourceType(
        val displayName: String,
        val harvestAmount: Int = 8
    ) {
        MINERAL("Mineral Field", 8),
        VESPENE_GAS("Vespene Geyser", 4)
    }

    override fun getCategory(): Entity.EntityCategory = Entity.EntityCategory.RESOURCE

    override fun getDisplayName(): String = "${resourceType.displayName} (${amount})"

    /**
     * Check if resource is depleted
     */
    val isDepleted: Boolean
        get() = amount <= 0

    /**
     * Harvest resource
     */
    fun harvest(harvesterCount: Int = 1): Int {
        if (isDepleted) return 0

        val harvestAmount = resourceType.harvestAmount * harvesterCount
        val actualHarvest = amount.coerceAtMost(harvestAmount)
        amount -= actualHarvest

        if (amount <= 0) {
            destroy()
        }

        return actualHarvest
    }

    /**
     * Get remaining percentage
     */
    fun getRemainingPercent(): Float {
        return if (maxAmount > 0) amount.toFloat() / maxAmount else 0f
    }

    /**
     * Check if visible to player
     */
    override fun isVisibleTo(playerId: Long): Boolean {
        // Resources are neutral, visible to all
        return true
    }

    /**
     * Check if shows on minimap
     */
    override fun showsOnMinimap(): Boolean = !isDepleted

    override fun destroy() {
        super.destroy()
        amount = 0
    }

    override fun getRenderOrder(): Int {
        return position.y.toInt()
    }

    override fun serialize(): String {
        return "${id}|${resourceType.name}|${position.x}|${position.y}|${amount}|${maxAmount}"
    }

    companion object {
        fun deserialize(data: String): Resource {
            val parts = data.split("|")
            return Resource(
                id = parts[0].toLong(),
                type = parts[1],
                position = Vector2.new(parts[2].toFloat(), parts[3].toFloat()),
                amount = parts[4].toInt(),
                maxAmount = parts[5].toInt()
            )
        }
    }
}
