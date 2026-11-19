
package com.dk26.football

import android.app.Activity
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.content.pm.ActivityInfo
import android.view.View
import android.view.WindowManager
import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : Activity() {

    private lateinit var gameView: GLSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)

        gameView = GLSurfaceView(this).apply {
            setEGLContextClientVersion(2)
            setRenderer(FootballRenderer())
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }

        setContentView(gameView)
    }

    override fun onPause() {
        super.onPause()
        gameView.onPause()
    }

    override fun onResume() {
        super.onResume()
        gameView.onResume()
    }
}

private class FootballRenderer : GLSurfaceView.Renderer {

    private var program = 0
    private var positionHandle = 0
    private var colorHandle = 0

    private val vertexShader = """
        attribute vec2 aPosition;
        void main() {
            gl_Position = vec4(aPosition, 0.0, 1.0);
        }
    """.trimIndent()

    private val fragmentShader = """
        precision mediump float;
        uniform vec4 uColor;
        void main() {
            gl_FragColor = uColor;
        }
    """.trimIndent()

    override fun onSurfaceCreated(
        gl: GL10?,
        config: EGLConfig?
    ) {
        GLES20.glClearColor(0.04f, 0.30f, 0.12f, 1f)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)

        program = createProgram(vertexShader, fragmentShader)
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        colorHandle = GLES20.glGetUniformLocation(program, "uColor")
    }

    override fun onSurfaceChanged(
        gl: GL10?,
        width: Int,
        height: Int
    ) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        if (program == 0) return

        GLES20.glUseProgram(program)

        drawRectangle(
            -0.92f, -0.82f, 0.92f, 0.82f,
            floatArrayOf(0.04f, 0.38f, 0.13f, 1f)
        )

        val white = floatArrayOf(0.94f, 0.97f, 0.92f, 1f)

        drawLine(-0.92f, -0.82f, 0.92f, -0.82f, white)
        drawLine(0.92f, -0.82f, 0.92f, 0.82f, white)
        drawLine(0.92f, 0.82f, -0.92f, 0.82f, white)
        drawLine(-0.92f, 0.82f, -0.92f, -0.82f, white)

        drawLine(0f, -0.82f, 0f, 0.82f, white)

        drawCircle(0f, 0f, 0.19f, white)

        drawRectangleOutline(
            -0.92f, -0.42f, -0.62f, 0.42f, white
        )

        drawRectangleOutline(
            0.62f, -0.42f, 0.92f, 0.42f, white
        )

        drawRectangleOutline(
            -0.92f, -0.20f, -0.79f, 0.20f, white
        )

        drawRectangleOutline(
            0.79f, -0.20f, 0.92f, 0.20f, white
        )

        drawCircle(-0.73f, 0f, 0.012f, white)
        drawCircle(0.73f, 0f, 0.012f, white)
        drawCircle(0f, 0f, 0.012f, white)
    }

    private fun drawRectangle(
        left: Float,
        bottom: Float,
        right: Float,
        top: Float,
        color: FloatArray
    ) {
        val vertices = floatArrayOf(
            left, bottom,
            right, bottom,
            left, top,
            right, top
        )
        draw(vertices, GLES20.GL_TRIANGLE_STRIP, color)
    }

    private fun drawRectangleOutline(
        left: Float,
        bottom: Float,
        right: Float,
        top: Float,
        color: FloatArray
    ) {
        val vertices = floatArrayOf(
            left, bottom,
            right, bottom,
            right, top,
            left, top,
            left, bottom
        )
        draw(vertices, GLES20.GL_LINE_STRIP, color)
    }

    private fun drawLine(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        color: FloatArray
    ) {
        draw(
            floatArrayOf(x1, y1, x2, y2),
            GLES20.GL_LINES,
            color
        )
    }

    private fun drawCircle(
        cx: Float,
        cy: Float,
        radius: Float,
        color: FloatArray
    ) {
        val vertices = ArrayList<Float>()
        for (i in 0..80) {
            val angle = 2.0 * Math.PI * i / 80.0
            vertices.add(cx + radius * cos(angle).toFloat())
            vertices.add(cy + radius * sin(angle).toFloat())
        }
        draw(vertices.toFloatArray(), GLES20.GL_LINE_STRIP, color)
    }

    private fun draw(
        vertices: FloatArray,
        mode: Int,
        color: FloatArray
    ) {
        val buffer: FloatBuffer = ByteBuffer
            .allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        buffer.put(vertices)
        buffer.position(0)

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(
            positionHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            0,
            buffer
        )

        GLES20.glUniform4fv(colorHandle, 1, color, 0)
        GLES20.glLineWidth(2f)
        GLES20.glDrawArrays(mode, 0, vertices.size / 2)
        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    private fun createProgram(
        vertexSource: String,
        fragmentSource: String
    ): Int {
        val vertex = compileShader(
            GLES20.GL_VERTEX_SHADER,
            vertexSource
        )
        val fragment = compileShader(
            GLES20.GL_FRAGMENT_SHADER,
            fragmentSource
        )

        if (vertex == 0 || fragment == 0) return 0

        val result = GLES20.glCreateProgram()
        GLES20.glAttachShader(result, vertex)
        GLES20.glAttachShader(result, fragment)
        GLES20.glLinkProgram(result)

        val status = IntArray(1)
        GLES20.glGetProgramiv(
            result,
            GLES20.GL_LINK_STATUS,
            status,
            0
        )

        if (status[0] == 0) {
            GLES20.glDeleteProgram(result)
            return 0
        }

        GLES20.glDeleteShader(vertex)
        GLES20.glDeleteShader(fragment)

        return result
    }

    private fun compileShader(
        type: Int,
        source: String
    ): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)

        val status = IntArray(1)
        GLES20.glGetShaderiv(
            shader,
            GLES20.GL_COMPILE_STATUS,
            status,
            0
        )

        if (status[0] == 0) {
            GLES20.glDeleteShader(shader)
            return 0
        }

        return shader
    }
}
