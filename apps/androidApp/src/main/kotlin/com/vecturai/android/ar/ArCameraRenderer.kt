package com.vecturai.android.ar

import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import com.google.ar.core.Session
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Minimal OpenGL renderer that displays the ARCore camera background.
 *
 * Renders the live camera feed from ARCore's session as a full-screen
 * textured quad using GL_TEXTURE_EXTERNAL_OES.
 */
class ArCameraRenderer(
    private val sessionProvider: () -> Session?,
) : GLSurfaceView.Renderer {

    private var cameraTextureId = -1
    private var shaderProgram = 0
    private var quadVertexBuffer: FloatBuffer? = null
    private var texCoordBuffer: FloatBuffer? = null

    private var viewportWidth = 0
    private var viewportHeight = 0

    // Current frame from ARCore (set after session.update())
    var currentFrame: Frame? = null
        private set

    companion object {
        private val QUAD_VERTICES = floatArrayOf(
            -1f, -1f, +1f, -1f, -1f, +1f, +1f, +1f,
        )

        private val QUAD_TEX_COORDS = floatArrayOf(
            0f, 1f, 1f, 1f, 0f, 0f, 1f, 0f,
        )

        private const val VERTEX_SHADER = """
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = aPosition;
                vTexCoord = aTexCoord;
            }
        """

        private const val FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            varying vec2 vTexCoord;
            uniform samplerExternalOES uTexture;
            void main() {
                gl_FragColor = texture2D(uTexture, vTexCoord);
            }
        """
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // Create camera texture
        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)
        cameraTextureId = textureIds[0]

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        // Tell ARCore to use this texture
        sessionProvider()?.setCameraTextureName(cameraTextureId)

        // Compile shader program
        shaderProgram = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)

        // Quad vertex buffer
        quadVertexBuffer = ByteBuffer.allocateDirect(QUAD_VERTICES.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(QUAD_VERTICES)
                rewind()
            }

        // Tex coord buffer
        texCoordBuffer = ByteBuffer.allocateDirect(QUAD_TEX_COORDS.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(QUAD_TEX_COORDS)
                rewind()
            }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        viewportWidth = width
        viewportHeight = height

        sessionProvider()?.setDisplayGeometry(0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        val session = sessionProvider() ?: return

        try {
            // Make sure texture is bound to the session
            if (cameraTextureId != -1) {
                session.setCameraTextureName(cameraTextureId)
            }

            val frame = session.update()
            currentFrame = frame

            // Update display geometry
            if (viewportWidth > 0 && viewportHeight > 0) {
                session.setDisplayGeometry(0, viewportWidth, viewportHeight)
            }

            // Get transformed tex coords from ARCore
            val transformedTexCoords = FloatBuffer.allocate(8)
            frame.transformCoordinates2d(
                Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
                quadVertexBuffer,
                Coordinates2d.TEXTURE_NORMALIZED,
                transformedTexCoords,
            )
            transformedTexCoords.rewind()

            // Draw camera background
            GLES20.glDisable(GLES20.GL_DEPTH_TEST)
            GLES20.glDepthMask(false)

            GLES20.glUseProgram(shaderProgram)

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId)
            GLES20.glUniform1i(GLES20.glGetUniformLocation(shaderProgram, "uTexture"), 0)

            val posAttrib = GLES20.glGetAttribLocation(shaderProgram, "aPosition")
            GLES20.glVertexAttribPointer(posAttrib, 2, GLES20.GL_FLOAT, false, 0, quadVertexBuffer)
            GLES20.glEnableVertexAttribArray(posAttrib)

            val texAttrib = GLES20.glGetAttribLocation(shaderProgram, "aTexCoord")
            GLES20.glVertexAttribPointer(texAttrib, 2, GLES20.GL_FLOAT, false, 0, transformedTexCoords)
            GLES20.glEnableVertexAttribArray(texAttrib)

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

            GLES20.glDisableVertexAttribArray(posAttrib)
            GLES20.glDisableVertexAttribArray(texAttrib)

            GLES20.glDepthMask(true)
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        } catch (e: Exception) {
            // Session not ready or frame unavailable
            println("[ArCameraRenderer] Frame error: ${e.message}")
        }
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        return program
    }

    private fun loadShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        return shader
    }
}
