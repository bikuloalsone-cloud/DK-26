
package com.dk26.football

import android.app.Activity
import android.content.pm.ActivityInfo
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Bundle
import android.view.View
import android.view.WindowManager
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
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        gameView = GLSurfaceView(this).apply {
            setEGLContextClientVersion(2)
            setRenderer(DK26Renderer())
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }

        setContentView(gameView)
    }

    override fun onPause() {
        super.onPause()
        if (::gameView.isInitialized) gameView.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (::gameView.isInitialized) gameView.onResume()
    }
}

private class DK26Renderer : GLSurfaceView.Renderer {

    private var program = 0
    private var positionHandle = 0
    private var matrixHandle = 0
    private var colorHandle = 0

    private val projection = FloatArray(16)
    private val view = FloatArray(16)
    private val viewProjection = FloatArray(16)

    private val vertexShader = """
        attribute vec3 aPosition;
        uniform mat4 uMVP;
        void main() {
            gl_Position = uMVP * vec4(aPosition, 1.0);
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
        GLES20.glClearColor(0.025f, 0.12f, 0.045f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)

        program = createProgram(vertexShader, fragmentShader)

        if (program != 0) {
            positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
            matrixHandle = GLES20.glGetUniformLocation(program, "uMVP")
            colorHandle = GLES20.glGetUniformLocation(program, "uColor")
        }
    }

    override fun onSurfaceChanged(
        gl: GL10?,
        width: Int,
        height: Int
    ) {
        GLES20.glViewport(0, 0, width, height)

        val aspect = width.toFloat() / height.coerceAtLeast(1)

        Matrix.perspectiveM(
            projection,
            0,
            48f,
            aspect,
            1f,
            250f
        )

        Matrix.setLookAtM(
            view,
            0,
            0f, 66f, 112f,
            0f, 0f, 0f,
            0f, 1f, 0f
        )

        Matrix.multiplyMM(
            viewProjection,
            0,
            projection,
            0,
            view,
            0
        )
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(
            GLES20.GL_COLOR_BUFFER_BIT or
            GLES20.GL_DEPTH_BUFFER_BIT
        )

        if (program == 0) return

        GLES20.glUseProgram(program)

        drawPitch()
        drawPitchMarkings()
        drawGoals()
    }

    private fun drawPitch() {
        // Dimensions du terrain : 105 m x 68 m.
        drawRectangle(
            -34f, -52.5f,
            34f, 52.5f,
            0f,
            floatArrayOf(0.035f, 0.29f, 0.085f, 1f)
        )

        // Bandes de gazon créées par géométrie.
        val stripeColors = arrayOf(
            floatArrayOf(0.045f, 0.34f, 0.105f, 1f),
            floatArrayOf(0.035f, 0.29f, 0.085f, 1f)
        )

        for (i in 0 until 10) {
            val left = -34f + i * 6.8f
            val right = left + 6.8f

            drawRectangle(
                left, -52.5f,
                right, 52.5f,
                0.015f,
                stripeColors[i % 2]
            )
        }
    }

    private fun drawPitchMarkings() {
        val lines = ArrayList<Float>()

        val y = 0.045f

        // Contour extérieur.
        addLine(lines, -34f, y, -52.5f, 34f, y, -52.5f)
        addLine(lines, 34f, y, -52.5f, 34f, y, 52.5f)
        addLine(lines, 34f, y, 52.5f, -34f, y, 52.5f)
        addLine(lines, -34f, y, 52.5f, -34f, y, -52.5f)

        // Ligne centrale.
        addLine(lines, -34f, y, 0f, 34f, y, 0f)

        // Cercle central.
        addCircle(lines, 0f, y, 0f, 9.15f)

        // Surface de réparation.
        addLine(lines, -20.16f, y, -52.5f, -20.16f, y, -36f)
        addLine(lines, 20.16f, y, -52.5f, 20.16f, y, -36f)
        addLine(lines, -20.16f, y, -36f, 20.16f, y, -36f)

        addLine(lines, -20.16f, y, 52.5f, -20.16f, y, 36f)
        addLine(lines, 20.16f, y, 52.5f, 20.16f, y, 36f)
        addLine(lines, -20.16f, y, 36f, 20.16f, y, 36f)

        // Surface de but.
        addLine(lines, -9.16f, y, -52.5f, -9.16f, y, -47f)
        addLine(lines, 9.16f, y, -52.5f, 9.16f, y, -47f)
        addLine(lines, -9.16f, y, -47f, 9.16f, y, -47f)

        addLine(lines, -9.16f, y, 52.5f, -9.16f, y, 47f)
        addLine(lines, 9.16f, y, 52.5f, 9.16f, y, 47f)
        addLine(lines, -9.16f, y, 47f, 9.16f, y, 47f)

        // Points de penalty.
        addCircle(lines, 0f, y, -41.5f, 0.18f)
        addCircle(lines, 0f, y, 41.5f, 0.18f)

        drawLines(
            lines.toFloatArray(),
            floatArrayOf(0.96f, 0.97f, 0.92f, 1f)
        )
    }

    private fun drawGoals() {
        val goals = ArrayList<Float>()

        val halfWidth = 3.66f
        val goalHeight = 2.44f

        // But côté nord.
        addLine(goals, -halfWidth, 0f, -52.5f,
            -halfWidth, goalHeight, -52.5f)
        addLine(goals, halfWidth, 0f, -52.5f,
            halfWidth, goalHeight, -52.5f)
        addLine(goals, -halfWidth, goalHeight, -52.5f,
            halfWidth, goalHeight, -52.5f)

        // Profondeur du filet.
        addLine(goals, -halfWidth, 0f, -52.5f,
            -halfWidth, 0f, -55f)
        addLine(goals, halfWidth, 0f, -52.5f,
            halfWidth, 0f, -55f)
        addLine(goals, -halfWidth, goalHeight, -52.5f,
            -halfWidth, goalHeight, -55f)
        addLine(goals, halfWidth, goalHeight, -52.5f,
            halfWidth, goalHeight, -55f)
        addLine(goals, -halfWidth, goalHeight, -55f,
            halfWidth, goalHeight, -55f)
        addLine(goals, -halfWidth, 0f, -55f,
            halfWidth, 0f, -55f)

        // But côté sud.
        addLine(goals, -halfWidth, 0f, 52.5f,
            -halfWidth, goalHeight, 52.5f)
        addLine(goals, halfWidth, 0f, 52.5f,
            halfWidth, goalHeight, 52.5f)
        addLine(goals, -halfWidth, goalHeight, 52.5f,
            halfWidth, goalHeight, 52.5f)

        addLine(goals, -halfWidth, 0f, 52.5f,
            -halfWidth, 0f, 55f)
        addLine(goals, halfWidth, 0f, 52.5f,
            halfWidth, 0f, 55f)
        addLine(goals, -halfWidth, goalHeight, 52.5f,
            -halfWidth, goalHeight, 55f)
        addLine(goals, halfWidth, goalHeight, 52.5f,
            halfWidth, goalHeight, 55f)
        addLine(goals, -halfWidth, goalHeight, 55f,
            halfWidth, goalHeight, 55f)
        addLine(goals, -halfWidth, 0f, 55f,
            halfWidth, 0f, 55f)

        drawLines(
            goals.toFloatArray(),
            floatArrayOf(0.98f, 0.98f, 0.95f, 1f)
        )
    }

    private fun drawRectangle(
        left: Float,
        nearZ: Float,
        right: Float,
        farZ: Float,
        y: Float,
        color: FloatArray
    ) {
        val vertices = floatArrayOf(
            left, y, nearZ,
            right, y, nearZ,
            left, y, farZ,

            right, y, nearZ,
            right, y, farZ,
            left, y, farZ
        )

        draw(
            vertices,
            GLES20.GL_TRIANGLES,
            color
        )
    }

    private fun addLine(
        list: MutableList<Float>,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float
    ) {
        list.add(x1)
        list.add(y1)
        list.add(z1)

        list.add(x2)
        list.add(y2)
        list.add(z2)
    }

    private fun addCircle(
        list: MutableList<Float>,
        cx: Float,
        y: Float,
        cz: Float,
        radius: Float
    ) {
        val segments = 96

        for (i in 0 until segments) {
            val a1 = 2.0 * Math.PI * i / segments
            val a2 = 2.0 * Math.PI * (i + 1) / segments

            addLine(
                list,
                cx + radius * cos(a1).toFloat(),
                y,
                cz + radius * sin(a1).toFloat(),
                cx + radius * cos(a2).toFloat(),
                y,
                cz + radius * sin(a2).toFloat()
            )
        }
    }

    private fun drawLines(
        vertices: FloatArray,
        color: FloatArray
    ) {
        draw(vertices, GLES20.GL_LINES, color)
    }

    private fun draw(
        vertices: FloatArray,
        mode: Int,
        color: FloatArray
    ) {
        if (vertices.isEmpty()) return

        val buffer: FloatBuffer = ByteBuffer
            .allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        buffer.put(vertices)
        buffer.position(0)

        GLES20.glEnableVertexAttribArray(positionHandle)

        GLES20.glVertexAttribPointer(
            positionHandle,
            3,
            GLES20.GL_FLOAT,
            false,
            0,
            buffer
        )

        GLES20.glUniformMatrix4fv(
            matrixHandle,
            1,
            false,
            viewProjection,
            0
        )

        GLES20.glUniform4fv(
            colorHandle,
            1,
            color,
            0
        )

        GLES20.glLineWidth(2.2f)

        GLES20.glDrawArrays(
            mode,
            0,
            vertices.size / 3
        )

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

        GLES20.glDeleteShader(vertex)
        GLES20.glDeleteShader(fragment)

        if (status[0] == 0) {
            GLES20.glDeleteProgram(result)
            return 0
        }

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
