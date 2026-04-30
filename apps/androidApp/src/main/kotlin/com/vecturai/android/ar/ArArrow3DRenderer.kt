package com.vecturai.android.ar

import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.cos

internal class ArArrow3DRenderer {
    private var arrowProgram = 0
    private var shadowProgram = 0
    private var programsReady = false

    private var aPosition = -1
    private var aNormal = -1
    private var aGradient = -1
    private var uMvp = -1
    private var uModel = -1
    private var uColorTop = -1
    private var uColorBottom = -1
    private var uAlpha = -1
    private var uCameraPosition = -1
    private var uLightDir = -1

    private var sAPosition = -1
    private var sUMvp = -1
    private var sUAlpha = -1

    private val arrowVertices = floatBufferOf(Arrow3DGeometry.arrowVertices)
    private val arrowIndices = shortBufferOf(Arrow3DGeometry.arrowIndices)
    private val shadowVertices = floatBufferOf(Arrow3DGeometry.shadowVertices)

    private val model = FloatArray(16)
    private val viewModel = FloatArray(16)
    private val mvp = FloatArray(16)
    private val inverseView = FloatArray(16)
    private val cameraPosition = FloatArray(3)

    fun onSurfaceCreated() {
        arrowProgram = createProgram(ARROW_VERTEX_SHADER, ARROW_FRAGMENT_SHADER)
        shadowProgram = createProgram(SHADOW_VERTEX_SHADER, SHADOW_FRAGMENT_SHADER)
        if (arrowProgram == 0 || shadowProgram == 0) {
            Log.e(TAG, "3D arrow renderer disabled because a shader program failed to compile.")
            programsReady = false
            return
        }

        aPosition = GLES20.glGetAttribLocation(arrowProgram, "a_Position")
        aNormal = GLES20.glGetAttribLocation(arrowProgram, "a_Normal")
        aGradient = GLES20.glGetAttribLocation(arrowProgram, "a_Gradient")
        uMvp = GLES20.glGetUniformLocation(arrowProgram, "u_MVP")
        uModel = GLES20.glGetUniformLocation(arrowProgram, "u_Model")
        uColorTop = GLES20.glGetUniformLocation(arrowProgram, "u_ColorTop")
        uColorBottom = GLES20.glGetUniformLocation(arrowProgram, "u_ColorBottom")
        uAlpha = GLES20.glGetUniformLocation(arrowProgram, "u_Alpha")
        uCameraPosition = GLES20.glGetUniformLocation(arrowProgram, "u_CameraPosition")
        uLightDir = GLES20.glGetUniformLocation(arrowProgram, "u_LightDir")

        sAPosition = GLES20.glGetAttribLocation(shadowProgram, "a_Position")
        sUMvp = GLES20.glGetUniformLocation(shadowProgram, "u_MVP")
        sUAlpha = GLES20.glGetUniformLocation(shadowProgram, "u_Alpha")

        programsReady = listOf(
            aPosition,
            aNormal,
            aGradient,
            uMvp,
            uModel,
            uColorTop,
            uColorBottom,
            uAlpha,
            uCameraPosition,
            uLightDir,
            sAPosition,
            sUMvp,
            sUAlpha,
        ).all { it >= 0 }

        if (!programsReady) {
            Log.e(TAG, "3D arrow renderer disabled because shader locations could not be resolved.")
        }
    }

    fun draw(
        arrows: List<ArRouteRenderer.RenderableArrow3D>,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
        frameTimeMs: Long,
    ) {
        if (arrows.isEmpty() || !programsReady) return

        if (Matrix.invertM(inverseView, 0, viewMatrix, 0)) {
            cameraPosition[0] = inverseView[12]
            cameraPosition[1] = inverseView[13]
            cameraPosition[2] = inverseView[14]
        } else {
            cameraPosition[0] = 0f
            cameraPosition[1] = 0f
            cameraPosition[2] = 0f
        }

        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glDisable(GLES20.GL_CULL_FACE)

        drawShadows(arrows, viewMatrix, projectionMatrix, frameTimeMs)
        drawArrowMeshes(arrows, viewMatrix, projectionMatrix, frameTimeMs)

        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glDepthMask(true)
    }

    private fun drawShadows(
        arrows: List<ArRouteRenderer.RenderableArrow3D>,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
        frameTimeMs: Long,
    ) {
        GLES20.glUseProgram(shadowProgram)
        GLES20.glDepthMask(false)
        shadowVertices.position(0)
        GLES20.glEnableVertexAttribArray(sAPosition)
        GLES20.glVertexAttribPointer(sAPosition, 3, GLES20.GL_FLOAT, false, 0, shadowVertices)

        arrows.forEach { arrow ->
            if (arrow.alpha <= 0.01f || arrow.scale <= 0.01f) return@forEach
            val pulseScale = pulseScale(arrow, frameTimeMs)
            buildModelMatrix(arrow, isShadow = true, pulseScale = pulseScale)
            updateMvp(viewMatrix, projectionMatrix)
            GLES20.glUniformMatrix4fv(sUMvp, 1, false, mvp, 0)
            GLES20.glUniform1f(sUAlpha, (arrow.alpha * 0.28f).coerceIn(0f, 0.28f))
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, Arrow3DGeometry.shadowVertexCount)
        }

        GLES20.glDisableVertexAttribArray(sAPosition)
        GLES20.glDepthMask(true)
    }

    private fun drawArrowMeshes(
        arrows: List<ArRouteRenderer.RenderableArrow3D>,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
        frameTimeMs: Long,
    ) {
        GLES20.glUseProgram(arrowProgram)
        GLES20.glUniform3f(uLightDir, 0.3f, 1.0f, 0.4f)
        GLES20.glUniform3fv(uCameraPosition, 1, cameraPosition, 0)

        arrowVertices.position(Arrow3DGeometry.POSITION_OFFSET)
        GLES20.glEnableVertexAttribArray(aPosition)
        GLES20.glVertexAttribPointer(
            aPosition,
            3,
            GLES20.GL_FLOAT,
            false,
            Arrow3DGeometry.STRIDE_BYTES,
            arrowVertices,
        )

        arrowVertices.position(Arrow3DGeometry.NORMAL_OFFSET)
        GLES20.glEnableVertexAttribArray(aNormal)
        GLES20.glVertexAttribPointer(
            aNormal,
            3,
            GLES20.GL_FLOAT,
            false,
            Arrow3DGeometry.STRIDE_BYTES,
            arrowVertices,
        )

        arrowVertices.position(Arrow3DGeometry.GRADIENT_OFFSET)
        GLES20.glEnableVertexAttribArray(aGradient)
        GLES20.glVertexAttribPointer(
            aGradient,
            1,
            GLES20.GL_FLOAT,
            false,
            Arrow3DGeometry.STRIDE_BYTES,
            arrowVertices,
        )

        arrows.forEach { arrow ->
            if (arrow.alpha <= 0.01f || arrow.scale <= 0.01f) return@forEach
            val pulseScale = pulseScale(arrow, frameTimeMs)
            val colors = Arrow3DGeometry.colorsFor(arrow.type)
            buildModelMatrix(arrow, isShadow = false, pulseScale = pulseScale)
            updateMvp(viewMatrix, projectionMatrix)

            GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0)
            GLES20.glUniformMatrix4fv(uModel, 1, false, model, 0)
            GLES20.glUniform3fv(uColorTop, 1, colors.top, 0)
            GLES20.glUniform3fv(uColorBottom, 1, colors.bottom, 0)
            GLES20.glUniform1f(uAlpha, arrow.alpha.coerceIn(0f, 1f))

            arrowIndices.position(0)
            GLES20.glDrawElements(
                GLES20.GL_TRIANGLES,
                Arrow3DGeometry.arrowIndices.size,
                GLES20.GL_UNSIGNED_SHORT,
                arrowIndices,
            )
        }

        GLES20.glDisableVertexAttribArray(aPosition)
        GLES20.glDisableVertexAttribArray(aNormal)
        GLES20.glDisableVertexAttribArray(aGradient)
    }

    private fun pulseScale(arrow: ArRouteRenderer.RenderableArrow3D, frameTimeMs: Long): Float {
        if (!arrow.isImminent) return 1f
        val phase = (frameTimeMs % PULSE_DURATION_MS).toFloat() / PULSE_DURATION_MS.toFloat()
        val pulse = (1f - cos((phase * Math.PI * 2.0)).toFloat()) * 0.5f
        return 1f + 0.08f * pulse
    }

    private fun buildModelMatrix(
        arrow: ArRouteRenderer.RenderableArrow3D,
        isShadow: Boolean,
        pulseScale: Float,
    ) {
        Matrix.setIdentityM(model, 0)
        Matrix.translateM(
            model,
            0,
            arrow.worldX,
            if (isShadow) arrow.floorWorldY + 0.002f else arrow.worldY,
            arrow.worldZ,
        )
        Matrix.rotateM(model, 0, Math.toDegrees(arrow.headingRad.toDouble()).toFloat(), 0f, 1f, 0f)
        val scale = arrow.scale * pulseScale
        if (isShadow) {
            Matrix.scaleM(model, 0, scale * 1.45f, 1f, scale * 1.65f)
        } else {
            Matrix.scaleM(model, 0, scale, scale, scale)
        }
    }

    private fun updateMvp(viewMatrix: FloatArray, projectionMatrix: FloatArray) {
        Matrix.multiplyMM(viewModel, 0, viewMatrix, 0, model, 0)
        Matrix.multiplyMM(mvp, 0, projectionMatrix, 0, viewModel, 0)
    }

    private fun createProgram(vertexShaderCode: String, fragmentShaderCode: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        if (vertexShader == 0) return 0
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        if (fragmentShader == 0) {
            GLES20.glDeleteShader(vertexShader)
            return 0
        }

        val program = GLES20.glCreateProgram()
        if (program == 0) {
            GLES20.glDeleteShader(vertexShader)
            GLES20.glDeleteShader(fragmentShader)
            return 0
        }
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Program link failed: ${GLES20.glGetProgramInfoLog(program)}")
            GLES20.glDeleteProgram(program)
            GLES20.glDeleteShader(vertexShader)
            GLES20.glDeleteShader(fragmentShader)
            return 0
        }
        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)
        return program
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        if (shader == 0) return 0
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)

        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            Log.e(TAG, "Shader compile failed: ${GLES20.glGetShaderInfoLog(shader)}")
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    private fun floatBufferOf(values: FloatArray): FloatBuffer =
        ByteBuffer.allocateDirect(values.size * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(values)
                position(0)
            }

    private fun shortBufferOf(values: ShortArray): ShortBuffer =
        ByteBuffer.allocateDirect(values.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .apply {
                put(values)
                position(0)
            }

    private companion object {
        private const val TAG = "Arrow3D"
        private const val PULSE_DURATION_MS = 1_200L

        private const val ARROW_VERTEX_SHADER = """
            uniform mat4 u_MVP;
            uniform mat4 u_Model;
            attribute vec4 a_Position;
            attribute vec3 a_Normal;
            attribute float a_Gradient;
            varying vec3 v_WorldNormal;
            varying vec3 v_WorldPosition;
            varying float v_Gradient;
            void main() {
                vec4 worldPosition = u_Model * a_Position;
                gl_Position = u_MVP * a_Position;
                v_WorldPosition = worldPosition.xyz;
                v_WorldNormal = normalize((u_Model * vec4(a_Normal, 0.0)).xyz);
                v_Gradient = a_Gradient;
            }
        """

        private const val ARROW_FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec3 u_ColorTop;
            uniform vec3 u_ColorBottom;
            uniform float u_Alpha;
            uniform vec3 u_CameraPosition;
            uniform vec3 u_LightDir;
            varying vec3 v_WorldNormal;
            varying vec3 v_WorldPosition;
            varying float v_Gradient;
            void main() {
                vec3 base = mix(u_ColorBottom, u_ColorTop, clamp(v_Gradient, 0.0, 1.0));
                vec3 normal = normalize(v_WorldNormal);
                vec3 light = normalize(u_LightDir);
                vec3 viewDir = normalize(u_CameraPosition - v_WorldPosition);
                float diffuse = max(0.0, dot(normal, light));
                float rim = pow(1.0 - max(0.0, dot(normal, viewDir)), 2.0);
                vec3 color = base * (0.35 + 0.65 * diffuse) + rim * 0.25 * u_ColorTop;
                gl_FragColor = vec4(color, u_Alpha);
            }
        """

        private const val SHADOW_VERTEX_SHADER = """
            uniform mat4 u_MVP;
            attribute vec4 a_Position;
            varying float v_Radius;
            void main() {
                gl_Position = u_MVP * a_Position;
                v_Radius = clamp(length(a_Position.xz) / 0.18, 0.0, 1.0);
            }
        """

        private const val SHADOW_FRAGMENT_SHADER = """
            precision mediump float;
            uniform float u_Alpha;
            varying float v_Radius;
            void main() {
                float falloff = 1.0 - smoothstep(0.35, 1.0, v_Radius);
                gl_FragColor = vec4(0.0, 0.0, 0.0, u_Alpha * falloff);
            }
        """
    }
}
