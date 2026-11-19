
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
    private var positionHandle = -1
    private var matrixHandle = -1
    private var colorHandle = -1

    private var pitchBuffer = 0
    private var linesBuffer = 0

    private var pitchVertexCount = 0
    private var linesVertexCount = 0

    private val projection = FloatArray(16)
    private val view = FloatArray(16)
    private val viewProjection = FloatArray(16)

    private val pitchColor =
        floatArrayOf(0.035f, 0.29f, 0.085f, 1f)

    private val lineColor =
        floatArrayOf(0.96f, 0.97f, 0.92f, 1f)

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
        GLES20.glClearColor(
            0.025f, 0.12f, 0.045f, 1f
        )

        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)

        GLES20.glDisable(GLES20.GL_CULL_FACE)

        program = createProgram(
            vertexShader,
            fragmentShader
        )

        if (program == 0) return

        positionHandle =
            GLES20.glGetAttribLocation(program, "aPosition")

        matrixHandle =
            GLES20.glGetUniformLocation(program, "uMVP")

        colorHandle =
            GLES20.glGetUniformLocation(program, "uColor")

        if (positionHandle < 0 ||
            matrixHandle < 0 ||
            colorHandle < 0
        ) {
            program = 0
            return
        }

        // Construction unique des géométries fixes.
        val pitch = buildPitch()
        val lines = buildPitchLines()

        pitchVertexCount = pitch.size / 3
        linesVertexCount = lines.size / 3

        // Transfert des géométries vers la mémoire GPU.
        pitchBuffer = uploadStaticGeometry(pitch)
        linesBuffer = uploadStaticGeometry(lines)
    }

    override fun onSurfaceChanged(
        gl: GL10?,
        width: Int,
        height: Int
    ) {
        val safeWidth = width.coerceAtLeast(1)
        val safeHeight = height.coerceAtLeast(1)

        GLES20.glViewport(
            0, 0, safeWidth, safeHeight
        )

        val aspect =
            safeWidth.toFloat() / safeHeight.toFloat()

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

        if (program == 0 ||
            pitchBuffer == 0 ||
            linesBuffer == 0
        ) return

        GLES20.glUseProgram(program)

        // Terrain : un seul appel graphique.
        drawStaticBuffer(
            pitchBuffer,
            pitchVertexCount,
            GLES20.GL_TRIANGLES,
            pitchColor
        )

        // Marquages et buts : un seul appel graphique.
        GLES20.glLineWidth(2f)

        drawStaticBuffer(
            linesBuffer,
            linesVertexCount,
            GLES20.GL_LINES,
            lineColor
        )

        GLES20.glBindBuffer(
            GLES20.GL_ARRAY_BUFFER,
            0
        )
    }

    // -------------------------------------------------
    // TERRAIN
    // -------------------------------------------------

    private fun buildPitch(): FloatArray {

        val vertices = FloatArrayBuilder()

        // Dix bandes de gazon.
        for (i in 0 until 10) {

            val left = -34f + i * 6.8f
            val right = left + 6.8f

            addRectangle(
                vertices,
                left,
                -52.5f,
                right,
                52.5f,
                0.015f
            )
        }

        return vertices.toArray()
    }

    private fun addRectangle(
        list: FloatArrayBuilder,
        left: Float,
        nearZ: Float,
        right: Float,
        farZ: Float,
        y: Float
    ) {

        list.add(
            left, y, nearZ,
            right, y, nearZ,
            left, y, farZ,

            right, y, nearZ,
            right, y, farZ,
            left, y, farZ
        )
    }

    // -------------------------------------------------
    // LIGNES ET BUTS
    // -------------------------------------------------

    private fun buildPitchLines(): FloatArray {

        val lines = FloatArrayBuilder()
        val y = 0.045f

        // Contour du terrain.
        addLine(lines, -34f, y, -52.5f, 34f, y, -52.5f)
        addLine(lines, 34f, y, -52.5f, 34f, y, 52.5f)
        addLine(lines, 34f, y, 52.5f, -34f, y, 52.5f)
        addLine(lines, -34f, y, 52.5f, -34f, y, -52.5f)

        // Ligne centrale et cercle central.
        addLine(lines, -34f, y, 0f, 34f, y, 0f)
        addCircle(lines, 0f, y, 0f, 9.15f)

        // Surfaces de réparation.
        addLine(lines, -20.16f, y, -52.5f, -20.16f, y, -36f)
        addLine(lines, 20.16f, y, -52.5f, 20.16f, y, -36f)
        addLine(lines, -20.16f, y, -36f, 20.16f, y, -36f)

        addLine(lines, -20.16f, y, 52.5f, -20.16f, y, 36f)
        addLine(lines, 20.16f, y, 52.5f, 20.16f, y, 36f)
        addLine(lines, -20.16f, y, 36f, 20.16f, y, 36f)

        // Surfaces de but.
        addLine(lines, -9.16f, y, -52.5f, -9.16f, y, -47f)
        addLine(lines, 9.16f, y, -52.5f, 9.16f, y, -47f)
        addLine(lines, -9.16f, y, -47f, 9.16f, y, -47f)

        addLine(lines, -9.16f, y, 52.5f, -9.16f, y, 47f)
        addLine(lines, 9.16f, y, 52.5f, 9.16f, y, 47f)
        addLine(lines, -9.16f, y, 47f, 9.16f, y, 47f)

        // Points de penalty.
        addCircle(lines, 0f, y, -41.5f, 0.18f)
        addCircle(lines, 0f, y, 41.5f, 0.18f)

        // Deux buts.
        addGoal(lines, -52.5f)
        addGoal(lines, 52.5f)

        return lines.toArray()
    }

    private fun addGoal(
        lines: FloatArrayBuilder,
        z: Float
    ) {

        val halfWidth = 3.66f
        val height = 2.44f
        val direction = if (z < 0f) -1f else 1f
        val backZ = z + direction * 2.5f

        // Montants et barre transversale.
        addLine(lines, -halfWidth, 0f, z,
            -halfWidth, height, z)

        addLine(lines, halfWidth, 0f, z,
            halfWidth, height, z)

        addLine(lines, -halfWidth, height, z,
            halfWidth, height, z)

        // Profondeur du filet.
        addLine(lines, -halfWidth, 0f, z,
            -halfWidth, 0f, backZ)

        addLine(lines, halfWidth, 0f, z,
            halfWidth, 0f, backZ)

        addLine(lines, -halfWidth, height, z,
            -halfWidth, height, backZ)

        addLine(lines, halfWidth, height, z,
            halfWidth, height, backZ)

        addLine(lines, -halfWidth, height, backZ,
            halfWidth, height, backZ)

        addLine(lines, -halfWidth, 0f, backZ,
            halfWidth, 0f, backZ)
    }

    private fun addLine(
        list: FloatArrayBuilder,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float
    ) {
        list.add(
            x1, y1, z1,
            x2, y2, z2
        )
    }

    private fun addCircle(
        list: FloatArrayBuilder,
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

    // -------------------------------------------------
    // GPU : STOCKAGE DES GEOMETRIES
    // -------------------------------------------------

    private fun uploadStaticGeometry(
        vertices: FloatArray
    ): Int {

        if (vertices.isEmpty()) return 0

        val ids = IntArray(1)

        GLES20.glGenBuffers(1, ids, 0)

        val buffer = ByteBuffer
            .allocateDirect(
                vertices.size * Float.SIZE_BYTES
            )
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        buffer.put(vertices)
        buffer.position(0)

        GLES20.glBindBuffer(
            GLES20.GL_ARRAY_BUFFER,
            ids[0]
        )

        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            vertices.size * Float.SIZE_BYTES,
            buffer,
            GLES20.GL_STATIC_DRAW
        )

        GLES20.glBindBuffer(
            GLES20.GL_ARRAY_BUFFER,
            0
        )

        return ids[0]
    }

    private fun drawStaticBuffer(
        bufferId: Int,
        vertexCount: Int,
        mode: Int,
        color: FloatArray
    ) {

        GLES20.glBindBuffer(
            GLES20.GL_ARRAY_BUFFER,
            bufferId
        )

        GLES20.glEnableVertexAttribArray(
            positionHandle
        )

        GLES20.glVertexAttribPointer(
            positionHandle,
            3,
            GLES20.GL_FLOAT,
            false,
            0,
            0
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

        GLES20.glDrawArrays(
            mode,
            0,
            vertexCount
        )

        GLES20.glDisableVertexAttribArray(
            positionHandle
        )
    }

    // -------------------------------------------------
    // SHADERS
    // -------------------------------------------------

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

        if (vertex == 0 || fragment == 0) {
            if (vertex != 0) GLES20.glDeleteShader(vertex)
            if (fragment != 0) GLES20.glDeleteShader(fragment)
            return 0
        }

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

// -----------------------------------------------------
// CONSTRUCTEUR DE TABLEAUX : UTILISE A LA CONSTRUCTION
// -----------------------------------------------------

private class FloatArrayBuilder(initialCapacity: Int = 1024) {

    private var values = FloatArray(initialCapacity)
    private var count = 0

    fun add(vararg data: Float) {

        ensureCapacity(data.size)

        for (value in data) {
            values[count++] = value
        }
    }

    private fun ensureCapacity(extra: Int) {

        val required = count + extra

        if (required > values.size) {
            var newSize = values.size * 2

            while (newSize < required) {
                newSize *= 2
            }

            values = values.copyOf(newSize)
        }
    }

    fun toArray(): FloatArray {
        return values.copyOf(count)
    }
}
