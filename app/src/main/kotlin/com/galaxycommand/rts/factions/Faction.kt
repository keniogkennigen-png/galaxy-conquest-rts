package com.galaxycommand.rts.factions

import com.galaxycommand.rts.core.Vector2
import com.galaxycommand.rts.entities.Building
import com.galaxycommand.rts.entities.Unit

/**
 * Base Faction interface and factory for creating faction-specific units and buildings.
 */
sealed class Faction(
    open val factionType: FactionType
) {

    /**
     * Create a worker unit
     */
    abstract fun createWorker(position: Vector2, ownerId: Long, playerType: com.galaxycommand.rts.core.GameEngine.PlayerType): Unit

    /**
     * Create a combat unit
     */
    abstract fun createCombatUnit(position: Vector2, ownerId: Long, playerType: com.galaxycommand.rts.core.GameEngine.PlayerType): Unit

    /**
     * Create base building
     */
    abstract fun createBaseBuilding(position: Vector2): Building

    /**
     * Create production building
     */
    abstract fun createProductionBuilding(position: Vector2, ownerId: Long): Building

    /**
     * Get faction color for UI
     */
    abstract fun getFactionColor(): Int

    /**
     * Get faction accent color
     */
    abstract fun getFactionAccentColor(): Int

    /**
     * Get available units for this faction
     */
    abstract fun getAvailableUnits(): List<Building.UnitType>

    /**
     * Get unit production buildings
     */
    abstract fun getProductionBuildings(): List<String>

    /**
     * Faction factory
     */
    companion object {
        fun createFaction(factionType: FactionType): Faction {
            return when (factionType) {
                FactionType.VANGUARD -> VanguardFaction()
                FactionType.SWARM -> SwarmFaction()
                FactionType.SYNODE -> SynodFaction()
                else -> throw IllegalArgumentException("Unknown faction type: $factionType")
            }
        }
    }
}

/**
 * Terran-equivalent faction: Industrial, balanced, repairable
 */
class VanguardFaction : Faction(FactionType.VANGUARD) {

    override fun createWorker(position: Vector2, ownerId: Long, playerType: com.galaxycommand.rts.core.GameEngine.PlayerType): Unit {
        return Unit.createWorker(position, ownerId, FactionType.VANGUARD)
    }

    override fun createCombatUnit(position: Vector2, ownerId: Long, playerType: com.galaxycommand.rts.core.GameEngine.PlayerType): Unit {
        return Unit.createCombatUnit(position, ownerId, FactionType.VANGUARD)
    }

    override fun createBaseBuilding(position: Vector2): Building {
        return Building.createBaseBuilding(position, FactionType.VANGUARD)
    }

    override fun createProductionBuilding(position: Vector2, ownerId: Long): Building {
        return Building.createProductionBuilding(position, ownerId, FactionType.VANGUARD, getAvailableUnits())
    }

    override fun getFactionColor(): Int = 0xFF546E7A.toInt() // Blue-grey

    override fun getFactionAccentColor(): Int = 0xFF29B6F6.toInt() // Light blue

    override fun getAvailableUnits(): List<Building.UnitType> = listOf(
        Building.UnitType.MARINE,
        Building.UnitType.REAPER,
        Building.UnitType.GHOST,
        Building.UnitType.HELLION,
        Building.UnitType.MARAUDER,
        Building.UnitType.TANK,
        Building.UnitType.THOR,
        Building.UnitType.MEDIVAC,
        Building.UnitType.VIKING,
        Building.UnitType.BANSHEE,
        Building.UnitType.RAVEN,
        Building.UnitType.BATTLECRUISER
    )

    override fun getProductionBuildings(): List<String> = listOf(
        "Barracks",
        "Factory",
        "Starport"
    )

    override fun toString(): String = "Vanguard"
}

/**
 * Zerg-equivalent faction: Biological, fast, swarm-based, regenerative
 */
class SwarmFaction : Faction(FactionType.SWARM) {

    override fun createWorker(position: Vector2, ownerId: Long, playerType: com.galaxycommand.rts.core.GameEngine.PlayerType): Unit {
        return Unit.createWorker(position, ownerId, FactionType.SWARM)
    }

    override fun createCombatUnit(position: Vector2, ownerId: Long, playerType: com.galaxycommand.rts.core.GameEngine.PlayerType): Unit {
        return Unit.createCombatUnit(position, ownerId, FactionType.SWARM)
    }

    override fun createBaseBuilding(position: Vector2): Building {
        return Building.createBaseBuilding(position, FactionType.SWARM)
    }

    override fun createProductionBuilding(position: Vector2, ownerId: Long): Building {
        return Building.createProductionBuilding(position, ownerId, FactionType.SWARM, getAvailableUnits())
    }

    override fun getFactionColor(): Int = 0xFF4E342E.toInt() // Brown

    override fun getFactionAccentColor(): Int = 0xFFAB47BC.toInt() // Purple

    override fun getAvailableUnits(): List<Building.UnitType> = listOf(
        Building.UnitType.DRONE,
        Building.UnitType.ZERGLING,
        Building.UnitType.ROACH,
        Building.UnitType.HYDRA,
        Building.UnitType.INFESTOR,
        Building.UnitType.ULTRALISK,
        Building.UnitType.MUTALISK,
        Building.UnitType.CORRUPTOR,
        Building.UnitType.BLORG,
        Building.UnitType.OVERLORD,
        Building.UnitType.OVERSEER,
        Building.UnitType.QUEEN
    )

    override fun getProductionBuildings(): List<String> = listOf(
        "Spawning Pool",
        "Roach Warren",
        "Hydralisk Den",
        "Infestation Pit",
        "Ultralisk Cavern",
        "Spire",
        "Greater Spire"
    )

    override fun toString(): String = "Swarm"
}

/**
 * Protoss-equivalent faction: Advanced technology, shielded, powerful but expensive
 */
class SynodFaction : Faction(FactionType.SYNODE) {

    override fun createWorker(position: Vector2, ownerId: Long, playerType: com.galaxycommand.rts.core.GameEngine.PlayerType): Unit {
        return Unit.createWorker(position, ownerId, FactionType.SYNODE)
    }

    override fun createCombatUnit(position: Vector2, ownerId: Long, playerType: com.galaxycommand.rts.core.GameEngine.PlayerType): Unit {
        return Unit.createCombatUnit(position, ownerId, FactionType.SYNODE)
    }

    override fun createBaseBuilding(position: Vector2): Building {
        return Building.createBaseBuilding(position, FactionType.SYNODE)
    }

    override fun createProductionBuilding(position: Vector2, ownerId: Long): Building {
        return Building.createProductionBuilding(position, ownerId, FactionType.SYNODE, getAvailableUnits())
    }

    override fun getFactionColor(): Int = 0xFFFFCA28.toInt() // Gold

    override fun getFactionAccentColor(): Int = 0xFF4DD0E1.toInt() // Cyan

    override fun getAvailableUnits(): List<Building.UnitType> = listOf(
        Building.UnitType.PROBE,
        Building.UnitType.ZEALOT,
        Building.UnitType.STALKER,
        Building.UnitType.ADEPT,
        Building.UnitType.SENTRY,
        Building.UnitType.IMMORTAL,
        Building.UnitType.COLOSSUS,
        Building.UnitType.HIGHTEMPLAR,
        Building.UnitType.DARKTEMPLAR,
        Building.UnitType.ORACLE,
        Building.UnitType.PHOENIX,
        Building.UnitType.VOIDRAY,
        Building.UnitType.TEMPEST,
        Building.UnitType.CARRIER,
        Building.UnitType.MOTHERSHIP
    )

    override fun getProductionBuildings(): List<String> = listOf(
        "Gateway",
        "Robotics Bay",
        "Twilight Council",
        "Robotics Facility",
        "Stargate",
        "Fleet Beacon",
        "Templar Archives",
        "Dark Shrine"
    )

    override fun toString(): String = "Synod"
}
