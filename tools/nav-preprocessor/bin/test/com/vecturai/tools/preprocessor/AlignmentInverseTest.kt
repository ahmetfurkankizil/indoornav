package com.Vectura AI.tools.preprocessor

import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.*

/**
 * Tests for AlignmentTransform inverse (AR-world → building-local).
 */
class AlignmentInverseTest {

    data class Transform(
        val offsetX: Double = 0.0, val offsetY: Double = 0.0,
        val offsetZ: Double = 0.0, val rotationYDeg: Double = 0.0,
    ) {
        fun forward(bx: Double, by: Double, bz: Double): Triple<Double, Double, Double> {
            val rad = Math.toRadians(rotationYDeg)
            val c = cos(rad); val s = sin(rad)
            return Triple(bx * c + bz * s + offsetX, by + offsetY, -bx * s + bz * c + offsetZ)
        }

        fun inverse(ax: Double, ay: Double, az: Double): Triple<Double, Double, Double> {
            val rad = Math.toRadians(-rotationYDeg)
            val c = cos(rad); val s = sin(rad)
            val tx = ax - offsetX; val tz = az - offsetZ
            return Triple(tx * c + tz * s, ay - offsetY, -tx * s + tz * c)
        }
    }

    @Test
    fun `inverse of identity is identity`() {
        val t = Transform()
        val (x, y, z) = t.inverse(5.0, 1.0, 3.0)
        assertEquals(5.0, x, 0.001); assertEquals(1.0, y, 0.001); assertEquals(3.0, z, 0.001)
    }

    @Test
    fun `inverse undoes forward transform`() {
        val t = Transform(offsetX = 3.0, offsetY = -1.0, offsetZ = 7.0, rotationYDeg = 45.0)
        val (ax, ay, az) = t.forward(5.0, 2.0, 8.0)
        val (bx, by, bz) = t.inverse(ax, ay, az)
        assertEquals(5.0, bx, 0.001); assertEquals(2.0, by, 0.001); assertEquals(8.0, bz, 0.001)
    }

    @Test
    fun `inverse undoes 90-degree rotation`() {
        val t = Transform(rotationYDeg = 90.0)
        val (ax, _, az) = t.forward(1.0, 0.0, 0.0)
        val (bx, _, bz) = t.inverse(ax, 0.0, az)
        assertEquals(1.0, bx, 0.001); assertEquals(0.0, bz, 0.001)
    }

    @Test
    fun `inverse undoes translation only`() {
        val t = Transform(offsetX = 10.0, offsetZ = -5.0)
        val (ax, ay, az) = t.forward(2.0, 0.0, 3.0)
        val (bx, _, bz) = t.inverse(ax, ay, az)
        assertEquals(2.0, bx, 0.001); assertEquals(3.0, bz, 0.001)
    }

    @Test
    fun `inverse with realistic marker alignment`() {
        val t = Transform(offsetX = -3.5, offsetY = -1.2, offsetZ = -2.0, rotationYDeg = 12.0)
        // A point that was at building (0,0,0) should roundtrip
        val (ax, ay, az) = t.forward(0.0, 0.0, 0.0)
        val (bx, by, bz) = t.inverse(ax, ay, az)
        assertEquals(0.0, bx, 0.001); assertEquals(0.0, by, 0.001); assertEquals(0.0, bz, 0.001)
    }

    @Test
    fun `inverse preserves route points after alignment`() {
        val t = Transform(offsetX = 1.0, offsetZ = -1.0, rotationYDeg = 30.0)
        // Transform building point → AR, then inverse should return original
        val routePoints = listOf(
            Triple(0.0, 0.0, 0.0), Triple(3.0, 0.0, 0.0),
            Triple(6.0, 0.0, 0.0), Triple(6.0, 0.0, 4.0),
        )
        for ((bx, by, bz) in routePoints) {
            val (ax, ay, az) = t.forward(bx, by, bz)
            val (rx, ry, rz) = t.inverse(ax, ay, az)
            assertEquals(bx, rx, 0.001, "X mismatch for ($bx,$bz)")
            assertEquals(bz, rz, 0.001, "Z mismatch for ($bx,$bz)")
        }
    }
}
