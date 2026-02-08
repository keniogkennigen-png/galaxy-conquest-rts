package com.galaxycommand.rts.core

import com.galaxycommand.rts.entities.Building
import com.galaxycommand.rts.entities.Entity
import com.galaxycommand.rts.entities.Resource
import com.galaxycommand.rts.entities.Unit
import com.galaxycommand.rts.factions.FactionType
import java.util.concurrent.ConcurrentHashMap

/**
 * Manager class for all game entities including units, buildings, and resources.
 * Provides efficient lookup and iteration methods for game systems.
 */
class EntityManager {
    
    // Entity collections using ConcurrentHashMap for thread safety
    private val entities = ConcurrentHashMap<Long, Entity>()
    private val units = ConcurrentHashMap<Long, Unit>()
    private val buildings = ConcurrentHashMap<Long, Building>()
    private val resources = ConcurrentHashMap<Long, Resource>()
    
    // Entity ID counter
    private var nextEntityId = 1L
    
    /**
     * Add an entity to the manager.
     * Automatically categorizes based on entity type.
     */
    fun addEntity(entity: Entity): Long {
        val id = nextEntityId++
        entity.id = id
        entities[id] = entity
        
        when (entity) {
            is Unit -> units[id] = entity
            is Building -> buildings[id] = entity
            is Resource -> resources[id] = entity
        }
        
        return id
    }
    
    /**
     * Remove an entity from the manager.
     */
    fun removeEntity(entityId: Long) {
        entities.remove(entityId)
        units.remove(entityId)
        buildings.remove(entityId)
        resources.remove(entityId)
    }
    
    /**
     * Get an entity by ID.
     */
    fun getEntity(entityId: Long): Entity? {
        return entities[entityId]
    }
    
    /**
     * Get a unit by ID.
     */
    fun getUnit(entityId: Long): Unit? {
        return units[entityId]
    }
    
    /**
     * Get a building by ID.
     */
    fun getBuilding(entityId: Long): Building? {
        return buildings[entityId]
    }
    
    /**
     * Get a resource by ID.
     */
    fun getResource(entityId: Long): Resource? {
        return resources[entityId]
    }
    
    /**
     * Get all entities.
     */
    fun getAllEntities(): Collection<Entity> {
        return entities.values
    }
    
    /**
     * Get all units.
     */
    fun getAllUnits(): Collection<Unit> {
        return units.values
    }
    
    /**
     * Get all buildings.
     */
    fun getAllBuildings(): Collection<Building> {
        return buildings.values
    }
    
    /**
     * Get all resources.
     */
    fun getAllResources(): Collection<Resource> {
        return resources.values
    }
    
    /**
     * Get all alive units.
     */
    fun getAliveUnits(): List<Unit> {
        return units.values.filter { it.isAlive }
    }
    
    /**
     * Get all alive buildings.
     */
    fun getAliveBuildings(): List<Building> {
        return buildings.values.filter { it.isAlive }
    }
    
    /**
     * Get units within a specified radius of a position.
     */
    fun getUnitsInRadius(position: Vector2, radius: Float): List<Unit> {
        val radiusSquared = radius * radius
        return units.values.filter { unit ->
            unit.isAlive && unit.position.distanceToSquared(position) <= radiusSquared
        }
    }
    
    /**
     * Get buildings within a specified radius of a position.
     */
    fun getBuildingsInRadius(position: Vector2, radius: Float): List<Building> {
        val radiusSquared = radius * radius
        return buildings.values.filter { building ->
            building.isAlive && building.position.distanceToSquared(position) <= radiusSquared
        }
    }
    
    /**
     * Get resources within a specified radius of a position.
     */
    fun getResourcesInRadius(position: Vector2, radius: Float): List<Resource> {
        val radiusSquared = radius * radius
        return resources.values.filter { resource ->
            resource.amount > 0 && resource.position.distanceToSquared(position) <= radiusSquared
        }
    }
    
    /**
     * Get the unit at a specific position (for selection).
     */
    fun getUnitAtPosition(position: Vector2, radius: Float = 20f): Unit? {
        return units.values.find { unit ->
            unit.isAlive && unit.position.distanceTo(position) <= radius
        }
    }
    
    /**
     * Get the building at a specific position (for selection).
     */
    fun getBuildingAtPosition(position: Vector2, radius: Float = 40f): Building? {
        return buildings.values.find { building ->
            building.isAlive && building.position.distanceTo(position) <= radius
        }
    }
    
    /**
     * Get the resource at a specific position.
     */
    fun getResourceAtPosition(position: Vector2, radius: Float = 40f): Resource? {
        return resources.values.find { resource ->
            resource.amount > 0 && resource.position.distanceTo(position) <= radius
        }
    }
    
    /**
     * Get units belonging to a specific faction.
     */
    fun getUnitsByFaction(faction: FactionType): List<Unit> {
        return units.values.filter { it.faction == faction && it.isAlive }
    }
    
    /**
     * Get buildings belonging to a specific faction.
     */
    fun getBuildingsByFaction(faction: FactionType): List<Building> {
        return buildings.values.filter { it.faction == faction && it.isAlive }
    }
    
    /**
     * Get enemy units visible to a faction (considering fog of war).
     * This is a simplified version - the full implementation would check visibility.
     */
    fun getVisibleEnemies(faction: FactionType, viewPosition: Vector2, viewRadius: Float): List<Unit> {
        val radiusSquared = viewRadius * viewRadius
        return units.values.filter { unit ->
            unit.isAlive && 
            unit.faction != faction &&
            unit.position.distanceToSquared(viewPosition) <= radiusSquared
        }
    }
    
    /**
     * Clear all entities (for game reset).
     */
    fun clear() {
        entities.clear()
        units.clear()
        buildings.clear()
        resources.clear()
        nextEntityId = 1L
    }
    
    /**
     * Get total entity count.
     */
    fun getEntityCount(): Int {
        return entities.size
    }
    
    /**
     * Get unit count.
     */
    fun getUnitCount(): Int {
        return units.size
    }
    
    /**
     * Get building count.
     */
    fun getBuildingCount(): Int {
        return buildings.size
    }
    
    /**
     * Get resource count.
     */
    fun getResourceCount(): Int {
        return resources.size
    }
}
