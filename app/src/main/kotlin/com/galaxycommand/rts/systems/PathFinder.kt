package com.galaxycommand.rts.systems

import com.galaxycommand.rts.core.Vector2
import java.util.*
import kotlin.collections.ArrayDeque
import kotlin.collections.HashMap
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * A* Pathfinding implementation for unit navigation.
 * Uses a grid-based approach optimized for mobile performance.
 */
class PathFinder {

    companion object {
        // Grid cell size
        const val CELL_SIZE = 32f

        // Heuristic weights
        const val MOVE_COST = 10f
        const val DIAGONAL_COST = 14f

        // Maximum path length
        const val MAX_PATH_LENGTH = 500

        // Update interval for grid (in milliseconds)
        private const val GRID_UPDATE_INTERVAL = 1000L
    }

    // Grid dimensions
    private var gridWidth = 0
    private var gridHeight = 0

    // Grid data
    private var grid: Array<Array<GridCell>> = emptyArray()

    // Obstacles
    private val dynamicObstacles = HashMap<Long, ObstacleInfo>()
    private var lastGridUpdate = 0L

    // Path cache
    private val pathCache = HashMap<PathKey, List<Vector2>>()
    private var cacheHitCount = 0
    private var cacheMissCount = 0

    data class GridCell(
        var x: Int,
        var y: Int,
        var isWalkable: Boolean = true,
        var g: Float = Float.MAX_VALUE,
        var h: Float = 0f,
        var f: Float = Float.MAX_VALUE,
        var parent: GridCell? = null,
        var terrainCost: Float = 1f
    )

    data class ObstacleInfo(
        val position: Vector2,
        val radius: Float,
        var isDynamic: Boolean = false,
        var lastUpdate: Long = System.currentTimeMillis()
    )

    data class PathKey(
        val startX: Int,
        val startY: Int,
        val endX: Int,
        val endY: Int
    )

    /**
     * Initialize pathfinder with map dimensions
     */
    fun initialize() {
        val gameEngine = com.galaxycommand.rts.core.GameEngine.getInstance()
        val mapSize = gameEngine.getMapSize()

        gridWidth = (mapSize.x / CELL_SIZE).toInt() + 1
        gridHeight = (mapSize.y / CELL_SIZE).toInt() + 1

        // Create grid
        grid = Array(gridWidth) { x ->
            Array(gridHeight) { y ->
                GridCell(x, y)
            }
        }

        // Initialize all cells as walkable
        for (x in 0 until gridWidth) {
            for (y in 0 until gridHeight) {
                grid[x][y] = GridCell(x, y)
            }
        }

        pathCache.clear()
        dynamicObstacles.clear()
    }

    /**
     * Add a static obstacle to the grid
     */
    fun addStaticObstacle(position: Vector2, radius: Float) {
        val centerCell = worldToGrid(position)
        val cellRadius = (radius / CELL_SIZE).toInt() + 1

        for (x in (centerCell.x - cellRadius - 1)..(centerCell.x + cellRadius + 1)) {
            for (y in (centerCell.y - cellRadius - 1)..(centerCell.y + cellRadius + 1)) {
                if (isValidCell(x, y)) {
                    val cellPos = gridToWorld(x, y)
                    if (position.distanceTo(cellPos) <= radius + CELL_SIZE) {
                        grid[x][y].isWalkable = false
                    }
                }
            }
        }
    }

    /**
     * Add a dynamic obstacle
     */
    fun addDynamicObstacle(id: Long, position: Vector2, radius: Float) {
        dynamicObstacles[id] = ObstacleInfo(position, radius, true, System.currentTimeMillis())
        updateDynamicObstacles()
    }

    /**
     * Remove a dynamic obstacle
     */
    fun removeDynamicObstacle(id: Long) {
        dynamicObstacles.remove(id)
        updateDynamicObstacles()
    }

    /**
     * Update dynamic obstacle positions
     */
    fun updateDynamicObstacle(id: Long, position: Vector2) {
        dynamicObstacles[id]?.let {
            dynamicObstacles[id] = it.copy(position = position, lastUpdate = System.currentTimeMillis())
        }
    }

    /**
     * Update all dynamic obstacle positions
     */
    private fun updateDynamicObstacles() {
        // For performance, only update periodically
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastGridUpdate < GRID_UPDATE_INTERVAL) return

        lastGridUpdate = currentTime

        // Clear previous dynamic obstacles from grid
        for (row in grid) {
            for (cell in row) {
                if (cell.terrainCost < 1f) {
                    cell.isWalkable = true
                    cell.terrainCost = 1f
                }
            }
        }

        // Re-apply dynamic obstacles
        dynamicObstacles.values.forEach { obstacle ->
            val centerCell = worldToGrid(obstacle.position)
            val cellRadius = (obstacle.radius / CELL_SIZE).toInt() + 1

            for (x in (centerCell.x - cellRadius)..(centerCell.x + cellRadius)) {
                for (y in (centerCell.y - cellRadius)..(centerCell.y + cellRadius)) {
                    if (isValidCell(x, y)) {
                        val cellPos = gridToWorld(x, y)
                        if (obstacle.position.distanceTo(cellPos) <= obstacle.radius) {
                            grid[x][y].isWalkable = false
                            grid[x][y].terrainCost = 100f
                        }
                    }
                }
            }
        }
    }

    /**
     * Find path from start to end position
     */
    fun findPath(start: Vector2, end: Vector2): List<Vector2> {
        // Check cache first
        val startCell = worldToGrid(start)
        val endCell = worldToGrid(end)
        val cacheKey = PathKey(startCell.x, startCell.y, endCell.x, endCell.y)

        pathCache[cacheKey]?.let { cachedPath ->
            cacheHitCount++
            return cachedPath
        }

        cacheMissCount++

        // Check if destination is walkable
        if (!isValidCell(endCell.x, endCell.y) || !grid[endCell.x][endCell.y].isWalkable) {
            // Find nearest walkable cell
            val nearest = findNearestWalkableCell(endCell)
            if (nearest != null) {
                return findPath(start, gridToWorld(nearest.x, nearest.y))
            }
            return emptyList()
        }

        // Run A* algorithm
        val path = runAStar(startCell, endCell)

        // Cache the result
        if (path.size <= MAX_PATH_LENGTH) {
            pathCache[cacheKey] = path
        }

        return path
    }

    /**
     * A* pathfinding implementation
     */
    private fun runAStar(start: GridCell, end: GridCell): List<Vector2> {
        // Reset grid
        for (x in 0 until gridWidth) {
            for (y in 0 until gridHeight) {
                grid[x][y].g = Float.MAX_VALUE
                grid[x][y].h = 0f
                grid[x][y].f = Float.MAX_VALUE
                grid[x][y].parent = null
            }
        }

        val openSet = PriorityQueue<GridCell>(compareBy { it.f })
        val closedSet = HashSet<GridCell>()

        // Initialize start cell
        start.g = 0f
        start.h = heuristic(start, end)
        start.f = start.h
        openSet.add(start)

        var iterations = 0

        while (openSet.isNotEmpty() && iterations < MAX_PATH_LENGTH) {
            iterations++

            // Get cell with lowest f
            val current = openSet.poll() ?: break

            // Check if reached destination
            if (current.x == end.x && current.y == end.y) {
                return reconstructPath(current)
            }

            closedSet.add(current)

            // Check neighbors
            val neighbors = getNeighbors(current)

            for (neighbor in neighbors) {
                if (closedSet.contains(neighbor) || !neighbor.isWalkable) {
                    continue
                }

                val tentativeG = current.g + getMovementCost(current, neighbor)

                if (tentativeG < neighbor.g) {
                    neighbor.parent = current
                    neighbor.g = tentativeG
                    neighbor.h = heuristic(neighbor, end)
                    neighbor.f = neighbor.g + neighbor.h

                    if (!openSet.contains(neighbor)) {
                        openSet.add(neighbor)
                    }
                }
            }
        }

        // No path found
        return emptyList()
    }

    /**
     * Get movement cost between two cells
     */
    private fun getMovementCost(from: GridCell, to: GridCell): Float {
        val baseCost = if (from.x != to.x && from.y != to.y) {
            DIAGONAL_COST
        } else {
            MOVE_COST
        }
        return baseCost * to.terrainCost
    }

    /**
     * Heuristic function (Manhattan distance)
     */
    private fun heuristic(a: GridCell, b: GridCell): Float {
        val dx = abs(a.x - b.x)
        val dy = abs(a.y - b.y)
        return (dx + dy) * MOVE_COST
    }

    /**
     * Get neighboring cells
     */
    private fun getNeighbors(cell: GridCell): List<GridCell> {
        val neighbors = mutableListOf<GridCell>()

        // 8-directional movement
        val directions = listOf(
            Pair(0, -1),   // North
            Pair(1, -1),   // Northeast
            Pair(1, 0),    // East
            Pair(1, 1),    // Southeast
            Pair(0, 1),    // South
            Pair(-1, 1),   // Southwest
            Pair(-1, 0),   // West
            Pair(-1, -1)   // Northwest
        )

        for ((dx, dy) in directions) {
            val nx = cell.x + dx
            val ny = cell.y + dy

            if (isValidCell(nx, ny)) {
                neighbors.add(grid[nx][ny])
            }
        }

        return neighbors
    }

    /**
     * Reconstruct path from end to start
     */
    private fun reconstructPath(end: GridCell): List<Vector2> {
        val path = mutableListOf<Vector2>()
        var current: GridCell? = end

        while (current != null) {
            path.add(0, gridToWorld(current.x, current.y))
            current = current.parent
        }

        return path
    }

    /**
     * Find nearest walkable cell to target
     */
    private fun findNearestWalkableCell(target: GridCell): GridCell? {
        var bestCell: GridCell? = null
        var bestDist = Float.MAX_VALUE

        for (x in 0 until gridWidth) {
            for (y in 0 until gridHeight) {
                if (grid[x][y].isWalkable) {
                    val dist = sqrt(((x - target.x) * (x - target.x) + (y - target.y) * (y - target.y)).toFloat())
                    if (dist < bestDist) {
                        bestDist = dist
                        bestCell = grid[x][y]
                    }
                }
            }
        }

        return bestCell
    }

    /**
     * Convert world position to grid cell
     */
    fun worldToGrid(position: Vector2): GridCell {
        val x = (position.x / CELL_SIZE).toInt().coerceIn(0, gridWidth - 1)
        val y = (position.y / CELL_SIZE).toInt().coerceIn(0, gridHeight - 1)
        return grid[x][y]
    }

    /**
     * Convert grid cell to world position
     */
    fun gridToWorld(x: Int, y: Int): Vector2 {
        return Vector2.new(x * CELL_SIZE + CELL_SIZE / 2, y * CELL_SIZE + CELL_SIZE / 2)
    }

    /**
     * Check if cell coordinates are valid
     */
    fun isValidCell(x: Int, y: Int): Boolean {
        return x >= 0 && x < gridWidth && y >= 0 && y < gridHeight
    }

    /**
     * Check if a world position is walkable
     */
    fun isWalkable(position: Vector2): Boolean {
        val cell = worldToGrid(position)
        return isValidCell(cell.x, cell.y) && grid[cell.x][cell.y].isWalkable
    }

    /**
     * Get pathfinding statistics
     */
    fun getStats(): PathFinderStats {
        return PathFinderStats(
            cacheSize = pathCache.size,
            cacheHits = cacheHitCount,
            cacheMisses = cacheMissCount,
            hitRate = if (cacheHitCount + cacheMissCount > 0) {
                cacheHitCount.toFloat() / (cacheHitCount + cacheMissCount)
            } else 0f
        )
    }

    /**
     * Clear path cache
     */
    fun clearCache() {
        pathCache.clear()
        cacheHitCount = 0
        cacheMissCount = 0
    }

    /**
     * Clear all data
     */
    fun clear() {
        grid = emptyArray()
        dynamicObstacles.clear()
        pathCache.clear()
    }

    data class PathFinderStats(
        val cacheSize: Int,
        val cacheHits: Int,
        val cacheMisses: Int,
        val hitRate: Float
    )
}
