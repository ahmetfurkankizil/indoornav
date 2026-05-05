package com.vecturai.tools.preprocessor

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.*

/**
 * Tests for the AlignmentTransform coordinate transformation.
 *
 * Mirrors the shared AlignmentTransform logic to verify
 * building-local → AR-world coordinate mapping.
 */
class AlignmentTransformTest {

    data class Transform(
        val offsetX: Double = 0.0,
        val offsetY: Double = 0.0,
        val offsetZ: Double = 0.0,
        val rotationYDeg: Double = 0.0,
    ) {
        fun transformPoint(bx: Double, by: Double, bz: Double): Triple<Double, Double, Double> {
            val rad = Math.toRadians(rotationYDeg)
            val cosR = cos(rad)
            val sinR = sin(rad)
            val rx = bx * cosR + bz * sinR
            val rz = -bx * sinR + bz * cosR
            return Triple(rx + offsetX, by + offsetY, rz + offsetZ)
        }

        companion object {
            fun fromMarker(
                bX: Double, bY: Double, bZ: Double, bRot: Double,
                aX: Double, aY: Double, aZ: Double, aRot: Double,
            ): Transform {
                val rotDeg = aRot - bRot
                val rad = Math.toRadians(rotDeg)
                val cosR = cos(rad)
                val sinR = sin(rad)
                val rbX = bX * cosR + bZ * sinR
                val rbZ = -bX * sinR + bZ * cosR
                return Transform(
                    offsetX = aX - rbX,
                    offsetY = aY - bY,
                    offsetZ = aZ - rbZ,
                    rotationYDeg = rotDeg,
                )
            }
        }
    }

    @Test
    fun `identity transform passes through`() {
        val t = Transform()
        val (x, y, z) = t.transformPoint(5.0, 0.0, 3.0)
        assertEquals(5.0, x, 0.001)
        assertEquals(0.0, y, 0.001)
        assertEquals(3.0, z, 0.001)
    }

    @Test
    fun `pure translation`() {
        val t = Transform(offsetX = 10.0, offsetY = 1.0, offsetZ = -5.0)
        val (x, y, z) = t.transformPoint(3.0, 0.0, 2.0)
        assertEquals(13.0, x, 0.001)
        assertEquals(1.0, y, 0.001)
        assertEquals(-3.0, z, 0.001)
    }

    @Test
    fun `90-degree Y rotation`() {
        val t = Transform(rotationYDeg = 90.0)
        val (x, y, z) = t.transformPoint(1.0, 0.0, 0.0)
        // Rotating (1,0,0) by 90 degrees Y: x→z, z→-x
        // cos90=0, sin90=1 → rotX = 1*0 + 0*1 = 0, rotZ = -1*1 + 0*0 = -1
        assertEquals(0.0, x, 0.001)
        assertEquals(0.0, y, 0.001)
        assertEquals(-1.0, z, 0.001)
    }

    @Test
    fun `marker at origin with identity alignment`() {
        val t = Transform.fromMarker(
            bX = 0.0, bY = 1.2, bZ = 0.0, bRot = 0.0,
            aX = 0.0, aY = 0.0, aZ = -1.0, aRot = 0.0,
        )
        // Marker is at building (0,1.2,0), detected at AR (0,0,-1)
        // offsetX = 0, offsetY = -1.2, offsetZ = -1
        assertEquals(0.0, t.offsetX, 0.001)
        assertEquals(-1.2, t.offsetY, 0.001)
        assertEquals(-1.0, t.offsetZ, 0.001)
        assertEquals(0.0, t.rotationYDeg, 0.001)
    }

    @Test
    fun `marker position maps exactly to detected position`() {
        // Building marker at (5, 1.2, 3), detected at AR (2, 0, -4)
        val t = Transform.fromMarker(
            bX = 5.0, bY = 1.2, bZ = 3.0, bRot = 0.0,
            aX = 2.0, aY = 0.0, aZ = -4.0, aRot = 0.0,
        )
        // offsets: (2-5, 0-1.2, -4-3) = (-3, -1.2, -7)
        val (rx, ry, rz) = t.transformPoint(5.0, 1.2, 3.0)
        assertEquals(2.0, rx, 0.001)
        assertEquals(0.0, ry, 0.001)
        assertEquals(-4.0, rz, 0.001)
    }

    @Test
    fun `rotation alignment preserves marker position`() {
        val t = Transform.fromMarker(
            bX = 0.0, bY = 0.0, bZ = 0.0, bRot = 0.0,
            aX = 0.0, aY = 0.0, aZ = 0.0, aRot = 90.0,
        )
        // Marker at origin in both frames, 90-degree rotation
        val (rx, _, rz) = t.transformPoint(0.0, 0.0, 0.0)
        assertEquals(0.0, rx, 0.001)
        assertEquals(0.0, rz, 0.001)
    }

    @Test
    fun `direction transform has no translation`() {
        val t = Transform(offsetX = 100.0, offsetZ = 200.0)
        // Direction (1,0,0) should not be affected by translation
        val rad = Math.toRadians(t.rotationYDeg)
        val dx = 1.0 * cos(rad) + 0.0 * sin(rad)
        val dz = -1.0 * sin(rad) + 0.0 * cos(rad)
        assertEquals(1.0, dx, 0.001) // unchanged since rotation is 0
        assertEquals(0.0, dz, 0.001)
    }

    @Test
    fun `negative rotation`() {
        val t = Transform(rotationYDeg = -45.0)
        val (x, _, z) = t.transformPoint(1.0, 0.0, 0.0)
        val expected_x = cos(Math.toRadians(-45.0))
        val expected_z = -sin(Math.toRadians(-45.0))
        assertEquals(expected_x, x, 0.001)
        assertEquals(expected_z, z, 0.001)
    }
}
