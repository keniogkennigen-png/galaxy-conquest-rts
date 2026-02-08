package com.galaxycommand.rts.core

import kotlin.math.*

/**
 * 2D Vector class for game mathematics.
 * Provides common vector operations used throughout the game engine.
 */
data class Vector2(
    var x: Float,
    var y: Float
) {

    companion object {
        val ZERO = Vector2(0f, 0f)
        val ONE = Vector2(1f, 1f)
        val UP = Vector2(0f, -1f)
        val DOWN = Vector2(0f, 1f)
        val LEFT = Vector2(-1f, 0f)
        val RIGHT = Vector2(1f, 0f)

        /**
         * Calculate distance between two points
         */
        fun distance(a: Vector2, b: Vector2): Float {
            val dx = b.x - a.x
            val dy = b.y - a.y
            return sqrt(dx * dx + dy * dy)
        }

        /**
         * Calculate squared distance (faster, no sqrt)
         */
        fun distanceSquared(a: Vector2, b: Vector2): Float {
            val dx = b.x - a.x
            val dy = b.y - a.y
            return dx * dx + dy * dy
        }

        /**
         * Linear interpolation between two vectors
         */
        fun lerp(a: Vector2, b: Vector2, t: Float): Vector2 {
            return Vector2(
                a.x + (b.x - a.x) * t,
                a.y + (b.y - a.y) * t
            )
        }

        /**
         * Get direction from a to b
         */
        fun direction(from: Vector2, to: Vector2): Vector2 {
            return (to - from).normalized()
        }

        /**
         * Calculate angle between two vectors
         */
        fun angle(a: Vector2, b: Vector2): Float {
            return atan2(b.y - a.y.toDouble(), b.x - a.x.toDouble()).toFloat()
        }

        /**
         * Clamp vector to bounds
         */
        fun clamp(value: Vector2, min: Vector2, max: Vector2): Vector2 {
            return Vector2(
                value.x.coerceIn(min.x, max.x),
                value.y.coerceIn(min.y, max.y)
            )
        }

        /**
         * Get random point in rectangle
         */
        fun randomInRect(topLeft: Vector2, bottomRight: Vector2): Vector2 {
            val random = java.util.Random()
            return Vector2(
                topLeft.x + random.nextFloat() * (bottomRight.x - topLeft.x),
                topLeft.y + random.nextFloat() * (bottomRight.y - topLeft.y)
            )
        }

        /**
         * Create a new vector from Cartesian coordinates (x, y)
         */
        fun new(x: Float, y: Float): Vector2 {
            return Vector2(x, y)
        }

        /**
         * Create a new vector from polar coordinates (length, angle in radians)
         */
        fun fromPolar(length: Float, angleInRadians: Float): Vector2 {
            return Vector2(
                length * cos(angleInRadians),
                length * sin(angleInRadians)
            )
        }
    }

    /**
     * Get the length of the vector
     */
    val length: Float
        get() = sqrt(x * x + y * y)

    /**
     * Get squared length (faster)
     */
    val lengthSquared: Float
        get() = x * x + y * y

    /**
     * Get normalized copy of vector
     */
    val normalized: Vector2
        get() {
            val len = length
            return if (len > 0) Vector2(x / len, y / len) else ZERO
        }

    /**
     * Get perpendicular vector (rotated 90 degrees counter-clockwise)
     */
    val perpendicular: Vector2
        get() = Vector2(-y, x)

    /**
     * Check if vector is zero
     */
    val isZero: Boolean
        get() = x == 0f && y == 0f

    /**
     * Check if vector is approximately zero
     */
    fun isNearlyZero(threshold: Float = 0.001f): Boolean {
        return abs(x) < threshold && abs(y) < threshold
    }

    /**
     * Set vector components
     */
    fun set(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    /**
     * Set from another vector
     */
    fun set(other: Vector2) {
        this.x = other.x
        this.y = other.y
    }

    /**
     * Add another vector
     */
    operator fun plus(other: Vector2): Vector2 {
        return Vector2(x + other.x, y + other.y)
    }

    /**
     * Add vector in-place
     */
    fun add(other: Vector2) {
        x += other.x
        y += other.y
    }

    /**
     * Subtract another vector
     */
    operator fun minus(other: Vector2): Vector2 {
        return Vector2(x - other.x, y - other.y)
    }

    /**
     * Subtract vector in-place
     */
    fun sub(other: Vector2) {
        x -= other.x
        y -= other.y
    }

    /**
     * Multiply by scalar
     */
    operator fun times(scalar: Float): Vector2 {
        return Vector2(x * scalar, y * scalar)
    }

    /**
     * Multiply by another vector (component-wise)
     */
    operator fun times(other: Vector2): Vector2 {
        return Vector2(x * other.x, y * other.y)
    }

    /**
     * Multiply in-place
     */
    fun scale(scalar: Float) {
        x *= scalar
        y *= scalar
    }

    /**
     * Divide by scalar
     */
    operator fun div(scalar: Float): Vector2 {
        return Vector2(x / scalar, y / scalar)
    }

    /**
     * Divide in-place
     */
    fun divide(scalar: Float) {
        x /= scalar
        y /= scalar
    }

    /**
     * Calculate dot product with another vector
     */
    fun dot(other: Vector2): Float {
        return x * other.x + y * other.y
    }

    /**
     * Calculate cross product (2D)
     */
    fun cross(other: Vector2): Float {
        return x * other.y - y * other.x
    }

    /**
     * Normalize this vector
     */
    fun normalize() {
        val len = length
        if (len > 0) {
            x /= len
            y /= len
        }
    }

    /**
     * Get normalized copy of vector
     */
    fun normalized(): Vector2 {
        val len = length
        return if (len > 0) Vector2(x / len, y / len) else Vector2.ZERO
    }

    /**
     * Get distance to another point
     */
    fun distanceTo(other: Vector2): Float {
        return distance(this, other)
    }

    /**
     * Get squared distance to another point
     */
    fun distanceSquaredTo(other: Vector2): Float {
        return distanceSquared(this, other)
    }

    /**
     * Get squared distance to another point (alias for distanceSquaredTo)
     */
    fun distanceToSquared(other: Vector2): Float {
        return distanceSquared(this, other)
    }

    /**
     * Get angle to another point
     */
    fun angleTo(other: Vector2): Float {
        return angle(this, other)
    }

    /**
     * Rotate by angle
     */
    fun rotate(angle: Float): Vector2 {
        val cos = cos(angle)
        val sin = sin(angle)
        return Vector2(
            x * cos - y * sin,
            x * sin + y * cos
        )
    }

    /**
     * Rotate in-place
     */
    fun rotateInPlace(angle: Float) {
        val cos = cos(angle)
        val sin = sin(angle)
        val newX = x * cos - y * sin
        val newY = x * sin + y * cos
        x = newX
        y = newY
    }

    /**
     * Clamp to bounds
     */
    fun clamp(min: Vector2, max: Vector2) {
        x = x.coerceIn(min.x, max.x)
        y = y.coerceIn(min.y, max.y)
    }

    /**
     * Get absolute values
     */
    fun abs(): Vector2 {
        return Vector2(kotlin.math.abs(x), kotlin.math.abs(y))
    }

    /**
     * Floor values
     */
    fun floor(): Vector2 {
        return Vector2(kotlin.math.floor(x), kotlin.math.floor(y))
    }

    /**
     * Ceil values
     */
    fun ceil(): Vector2 {
        return Vector2(kotlin.math.ceil(x), kotlin.math.ceil(y))
    }

    override fun toString(): String {
        return "Vector2(x=%.2f, y=%.2f)".format(x, y)
    }
}
