package com.galaxycommand.rts.entities

import com.galaxycommand.rts.core.Vector2
import com.galaxycommand.rts.factions.FactionType

/**
 * Base class for all game entities.
 * Provides common properties and methods for units, buildings, and resources.
 */
abstract class Entity(
    open var id: Long = 0,
    open var type: String = "Entity",
    open var position: Vector2 = Vector2.ZERO,
    open var ownerId: Long = 0,
    open var radius: Float = 10f,
    open var isAlive: Boolean = true
) {

    /**
     * Get the display name for this entity
     */
    open fun getDisplayName(): String = type

    /**
     * Get entity type category
     */
    abstract fun getCategory(): EntityCategory

    /**
     * Update entity logic
     */
    abstract fun update(deltaTime: Float)

    /**
     * Get entity health percentage (0-1)
     */
    open fun getHealthPercent(): Float = 1f

    /**
     * Check if entity is visible to a player
     */
    open fun isVisibleTo(playerId: Long): Boolean {
        // Show all alive entities (for debugging/playability)
        // TODO: Implement proper fog of war
        return isAlive
    }

    /**
     * Check if entity is visible on minimap
     */
    open fun showsOnMinimap(): Boolean = isAlive && ownerId != 0L

    /**
     * Entity categories for filtering and grouping
     */
    enum class EntityCategory {
        UNIT,
        BUILDING,
        RESOURCE,
        TERRAIN,
        PROJECTILE
    }

    /**
     * Check collision with another entity
     */
    fun collidesWith(other: Entity): Boolean {
        if (!isAlive || !other.isAlive) return false
        return position.distanceTo(other.position) < radius + other.radius
    }

    /**
     * Check if point is within entity radius
     */
    fun containsPoint(point: Vector2): Boolean {
        return position.distanceTo(point) <= radius
    }

    /**
     * Destroy the entity
     */
    open fun destroy() {
        isAlive = false
    }

    /**
     * Get visual size (for rendering)
     */
    open fun getVisualSize(): Float = radius * 2

    /**
     * Get render order (z-index)
     */
    open fun getRenderOrder(): Int = 0

    /**
     * Serialize entity state for network/save
     */
    abstract fun serialize(): String

    /**
     * Deserialize entity state
     */
    companion object {
        fun deserialize(data: String): Entity {
            throw UnsupportedOperationException("deserialize must be implemented by subclass")
        }
    }
}
