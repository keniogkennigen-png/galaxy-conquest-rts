package com.galaxycommand.rts.core

import com.galaxycommand.rts.factions.FactionType

/**
 * Data class representing a tile on the game map.
 */
data class Tile(
    val x: Int,
    val y: Int,
    val terrainType: Int, // 0=Ground, 1=Rock, 2=Water, 3=Dirt
    val isWalkable: Boolean = terrainType != 2,
    val height: Float = 0f
)

/**
 * Data class representing a resource node on the map.
 */
data class ResourceNode(
    val x: Float,
    val y: Float,
    val type: ResourceType,
    val amount: Int
)

enum class ResourceType {
    MINERAL,
    ENERGY,
    GAS
}

/**
 * Data class representing a spawn point for a faction.
 */
data class SpawnPoint(
    val faction: FactionType,
    val x: Float,
    val y: Float
)

/**
 * Data class containing all information about a game map.
 */
data class GameMap(
    val id: String,
    val name: String,
    val description: String,
    val width: Int,
    val height: Int,
    val tiles: Array<Array<Tile>>,
    val resourceNodes: List<ResourceNode>,
    val spawnPoints: List<SpawnPoint>,
    val theme: MapTheme = MapTheme.DEFAULT
) {
    fun getTile(x: Int, y: Int): Tile {
        return if (x in 0 until width && y in 0 until height) {
            tiles[x][y]
        } else {
            Tile(x, y, 1, false)
        }
    }
    
    fun getPlayerSpawn(faction: FactionType): SpawnPoint? {
        return spawnPoints.find { it.faction == faction }
    }
    
    fun getRandomSpawn(): SpawnPoint {
        return spawnPoints.random()
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as GameMap
        return id == other.id
    }
    
    override fun hashCode(): Int {
        return id.hashCode()
    }
}

/**
 * Enum for map visual themes.
 */
enum class MapTheme(val displayName: String) {
    DEFAULT("Default"),
    DESERT("Desert"),
    ICE("Ice"),
    VOLCANIC("Volcanic"),
    ALIEN("Alien")
}

/**
 * Manager object that holds all available game maps.
 */
object MapManager {
    
    val maps: List<GameMap> by lazy {
        listOf(
            createTwinGorges(),
            createObsidianCitadel(),
            createAsteroidBelt()
        )
    }
    
    fun getMapById(id: String): GameMap? {
        return maps.find { it.id == id }
    }
    
    fun getDefaultMap(): GameMap {
        return maps.first()
    }
    
    private fun createTwinGorges(): GameMap {
        val width = 64
        val height = 64
        val tiles = Array(width) { x ->
            Array(height) { y ->
                val distFromCenter = kotlin.math.sqrt(
                    ((x - width/2f) * (x - width/2f) + 
                     (y - height/2f) * (y - height/2f)) / (width * height)
                )
                val isCenter = distFromCenter < 0.15f
                val isEdge = x < 4 || x >= width - 4 || y < 4 || y >= height - 4
                
                when {
                    isCenter -> Tile(x, y, 2, false) // Water in center
                    isEdge -> Tile(x, y, 1, false)   // Rock edges
                    else -> Tile(x, y, 0, true)       // Ground elsewhere
                }
            }
        }
        
        val resources = mutableListOf<ResourceNode>()
        // Add mineral fields on left side (player side)
        resources.add(ResourceNode(15f, 25f, ResourceType.MINERAL, 1500))
        resources.add(ResourceNode(18f, 35f, ResourceType.MINERAL, 1500))
        resources.add(ResourceNode(15f, 45f, ResourceType.MINERAL, 1500))
        
        // Add mineral fields on right side (enemy side)
        resources.add(ResourceNode(55f, 20f, ResourceType.MINERAL, 1500))
        resources.add(ResourceNode(58f, 32f, ResourceType.MINERAL, 1500))
        resources.add(ResourceNode(55f, 42f, ResourceType.MINERAL, 1500))
        
        // Add neutral minerals in middle area
        resources.add(ResourceNode(35f, 25f, ResourceType.MINERAL, 800))
        resources.add(ResourceNode(38f, 32f, ResourceType.MINERAL, 800))
        resources.add(ResourceNode(35f, 40f, ResourceType.MINERAL, 800))
        
        val spawnPoints = listOf(
            SpawnPoint(FactionType.VANGUARD, 8f, 32f),
            SpawnPoint(FactionType.SWARM, 56f, 32f),
            SpawnPoint(FactionType.SYNODE, 32f, 8f)
        )
        
        return GameMap(
            id = "twin_gorges",
            name = "Twin Gorges",
            description = "A classic balanced map with a river dividing the center. Control the middle to deny resources.",
            width = width,
            height = height,
            tiles = tiles,
            resourceNodes = resources,
            spawnPoints = spawnPoints,
            theme = MapTheme.DEFAULT
        )
    }
    
    private fun createObsidianCitadel(): GameMap {
        val width = 80
        val height = 80
        val tiles = Array(width) { x ->
            Array(height) { y ->
                val distFromCenterX = kotlin.math.abs((x - width/2f) / (width/2f))
                val distFromCenterY = kotlin.math.abs((y - height/2f) / (height/2f))
                
                when {
                    distFromCenterX > 0.8f || distFromCenterY > 0.8f -> Tile(x, y, 1, false)
                    distFromCenterX > 0.6f || distFromCenterY > 0.6f -> Tile(x, y, 3, true)
                    else -> Tile(x, y, 0, true)
                }
            }
        }
        
        val resources = mutableListOf<ResourceNode>()
        // Create mineral clusters at each corner
        for (cornerX in listOf(10, 70)) {
            for (cornerY in listOf(10, 70)) {
                resources.add(ResourceNode(cornerX.toFloat(), cornerY.toFloat(), ResourceType.MINERAL, 2000))
                resources.add(ResourceNode((cornerX + 5).toFloat(), cornerY.toFloat(), ResourceType.MINERAL, 1500))
                resources.add(ResourceNode(cornerX.toFloat(), (cornerY + 5).toFloat(), ResourceType.MINERAL, 1500))
            }
        }
        
        // Center neutral zone
        resources.add(ResourceNode(40f, 40f, ResourceType.MINERAL, 1000))
        resources.add(ResourceNode(40f, 45f, ResourceType.MINERAL, 1000))
        resources.add(ResourceNode(45f, 40f, ResourceType.MINERAL, 1000))
        resources.add(ResourceNode(45f, 45f, ResourceType.MINERAL, 1000))
        
        val spawnPoints = listOf(
            SpawnPoint(FactionType.VANGUARD, 12f, 12f),
            SpawnPoint(FactionType.SWARM, 68f, 68f),
            SpawnPoint(FactionType.SYNODE, 68f, 12f)
        )
        
        return GameMap(
            id = "obsidian_citadel",
            name = "Obsidian Citadel",
            description = "A large map with four corner bases. Expand quickly to secure the rich mineral deposits.",
            width = width,
            height = height,
            tiles = tiles,
            resourceNodes = resources,
            spawnPoints = spawnPoints,
            theme = MapTheme.DEFAULT
        )
    }
    
    private fun createAsteroidBelt(): GameMap {
        val width = 48
        val height = 48
        val tiles = Array(width) { x ->
            Array(height) { y ->
                // More rock tiles for asteroid field effect
                val random = (x * 7 + y * 13) % 17
                when {
                    random < 3 -> Tile(x, y, 1, false) // Asteroid obstacles
                    random < 5 -> Tile(x, y, 2, false) // Some water pockets
                    else -> Tile(x, y, 0, true)
                }
            }
        }
        
        val resources = mutableListOf<ResourceNode>()
        // Scattered mineral fields
        for (i in 0 until 8) {
            val x = 10 + (i % 4) * 8
            val y = 10 + (i / 4) * 24
            resources.add(ResourceNode(x.toFloat(), y.toFloat(), ResourceType.MINERAL, 1200))
        }
        
        // Center choke point resources
        resources.add(ResourceNode(24f, 20f, ResourceType.MINERAL, 600))
        resources.add(ResourceNode(24f, 28f, ResourceType.MINERAL, 600))
        
        val spawnPoints = listOf(
            SpawnPoint(FactionType.VANGUARD, 6f, 24f),
            SpawnPoint(FactionType.SWARM, 42f, 24f),
            SpawnPoint(FactionType.SYNODE, 24f, 6f)
        )
        
        return GameMap(
            id = "asteroid_belt",
            name = "Asteroid Belt",
            description = "A tight map with scattered obstacles. Maneuver through the asteroid field to victory.",
            width = width,
            height = height,
            tiles = tiles,
            resourceNodes = resources,
            spawnPoints = spawnPoints,
            theme = MapTheme.VOLCANIC
        )
    }
}
