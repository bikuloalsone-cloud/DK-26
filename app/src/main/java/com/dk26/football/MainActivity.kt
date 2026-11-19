
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
import java.util.Locale
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

    private var pitchLightBuffer = 0
    private var pitchDarkBuffer = 0
    private var linesBuffer = 0

    private var pitchLightCount = 0
    private var pitchDarkCount = 0
    private var linesVertexCount = 0

    private data class PlayerBatch(
        val key: String,
        val bufferId: Int,
        var vertexCount: Int,
        val color: FloatArray
    )

    private data class AnimatedPlayer(
        val matchPlayer: MatchPlayer,
        val parts: List<PlayerGeometryPart>
    )

    private data class CpuPlayerBatch(
        val color: FloatArray,
        val vertices: FloatArrayBuilder
    )

    private val playerBatches = mutableListOf<PlayerBatch>()
    private val animatedPlayers = mutableListOf<AnimatedPlayer>()

    private val projection = FloatArray(16)
    private val view = FloatArray(16)
    private val viewProjection = FloatArray(16)

    private var lastFrameNanos = 0L

    private val pitchLightColor =
        floatArrayOf(0.035f, 0.31f, 0.09f, 1f)

    private val pitchDarkColor =
        floatArrayOf(0.025f, 0.25f, 0.065f, 1f)

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

        program = createProgram(vertexShader, fragmentShader)

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

        val pitch = buildPitch()

        pitchLightCount = pitch.first.size / 3
        pitchDarkCount = pitch.second.size / 3

        pitchLightBuffer = uploadStaticGeometry(pitch.first)
        pitchDarkBuffer = uploadStaticGeometry(pitch.second)

        val lines = buildPitchLines()

        linesVertexCount = lines.size / 3
        linesBuffer = uploadStaticGeometry(lines)

        buildPlayers()

        lastFrameNanos = System.nanoTime()
    }

    override fun onSurfaceChanged(
        gl: GL10?,
        width: Int,
        height: Int
    ) {
        val safeWidth = width.coerceAtLeast(1)
        val safeHeight = height.coerceAtLeast(1)

        GLES20.glViewport(0, 0, safeWidth, safeHeight)

        val aspect = safeWidth.toFloat() / safeHeight.toFloat()

        Matrix.perspectiveM(
            projection, 0, 48f, aspect, 1f, 250f
        )

        Matrix.setLookAtM(
            view,
            0,
            0f, 66f, 112f,
            0f, 0f, 0f,
            0f, 1f, 0f
        )

        Matrix.multiplyMM(
            viewProjection, 0,
            projection, 0,
            view, 0
        )
    }

    override fun onDrawFrame(gl: GL10?) {

        GLES20.glClear(
            GLES20.GL_COLOR_BUFFER_BIT or
                GLES20.GL_DEPTH_BUFFER_BIT
        )

        if (program == 0) return

        val now = System.nanoTime()

        val deltaSeconds = if (lastFrameNanos == 0L) {
            0.016f
        } else {
            ((now - lastFrameNanos) / 1_000_000_000.0)
                .toFloat()
                .coerceIn(0f, 0.1f)
        }

        lastFrameNanos = now

        // Actualisation des animations individuelles.
        updateAnimatedPlayers(deltaSeconds)

        GLES20.glUseProgram(program)

        // Terrain.
        drawStaticBuffer(
            pitchLightBuffer,
            pitchLightCount,
            GLES20.GL_TRIANGLES,
            pitchLightColor
        )

        drawStaticBuffer(
            pitchDarkBuffer,
            pitchDarkCount,
            GLES20.GL_TRIANGLES,
            pitchDarkColor
        )

        // Lignes et buts.
        GLES20.glLineWidth(2f)

        drawStaticBuffer(
            linesBuffer,
            linesVertexCount,
            GLES20.GL_LINES,
            lineColor
        )

        // Joueurs animés.
        for (batch in playerBatches) {
            drawStaticBuffer(
                batch.bufferId,
                batch.vertexCount,
                GLES20.GL_TRIANGLES,
                batch.color
            )
        }

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    // -------------------------------------------------
    // CREATION DES JOUEURS
    // -------------------------------------------------

    private fun buildPlayers() {

        animatedPlayers.clear()

        val teams = MatchTeamSetup.createMatch()

        var animationOffset = 0

        for (team in teams) {
            for (matchPlayer in team.players) {

                val model = matchPlayer.model

                // Animation de course pour cette version.
                model.setAction(PlayerAction.RUN)

                // Décalage initial des phases d'animation.
                model.update(
                    deltaSeconds = animationOffset * 0.003f,
                    movementSpeed = 0.85f
                )

                animatedPlayers.add(
                    AnimatedPlayer(
                        matchPlayer = matchPlayer,
                        parts = model.geometry.parts
                    )
                )

                animationOffset++
            }
        }
    }

    // -------------------------------------------------
    // ANIMATION ET MISE A JOUR DES GEOMETRIES
    // -------------------------------------------------

    private fun updateAnimatedPlayers(deltaSeconds: Float) {

        val groups = LinkedHashMap<String, CpuPlayerBatch>()

        for (animatedPlayer in animatedPlayers) {

            val matchPlayer = animatedPlayer.matchPlayer
            val model = matchPlayer.model

            model.update(
                deltaSeconds = deltaSeconds,
                movementSpeed = 0.85f
            )

            val pose = model.currentPose ?: continue
            val redTeam = matchPlayer.team == TeamSide.RED

            for (part in animatedPlayer.parts) {

                val vertices = part.vertices
                val color = part.color

                if (color.size < 4 || vertices.size < 9) continue

                val key = color.joinToString("|") {
                    java.lang.Float.floatToIntBits(it).toString()
                }

                val batch = groups.getOrPut(key) {
                    CpuPlayerBatch(
                        color = color.copyOf(),
                        vertices = FloatArrayBuilder()
                    )
                }

                val transform = getPartTransform(part.name, pose)

                var index = 0

                while (index + 2 < vertices.size) {

                    var x = vertices[index]
                    var y = vertices[index + 1]
                    var z = vertices[index + 2]

                    if (transform != null) {
                        val result = animateVertex(
                            x, y, z,
                            transform.first,
                            transform.second
                        )

                        x = result[0]
                        y = result[1]
                        z = result[2]
                    }

                    val worldX = if (redTeam) {
                        matchPlayer.x - x
                    } else {
                        matchPlayer.x + x
                    }

                    val worldZ = if (redTeam) {
                        matchPlayer.z - z
                    } else {
                        matchPlayer.z + z
                    }

                    batch.vertices.add(
                        worldX,
                        y + 0.025f,
                        worldZ
                    )

                    index += 3
                }
            }
        }

        updatePlayerGpuBuffers(groups)
    }

    // -------------------------------------------------
    // ASSOCIATION DES PARTIES DU CORPS AUX ANIMATIONS
    // -------------------------------------------------

    private fun getPartTransform(
        partName: String,
        pose: PlayerPose
    ): Pair<JointTransform, FloatArray>? {

        val name = partName.lowercase(Locale.ROOT)

        return when {

            name.contains("leftarm") ||
            name.contains("leftforearm") ||
            name.contains("lefthand") ||
            name.contains("leftsleeve") -> {
                Pair(
                    pose.leftArm,
                    floatArrayOf(-0.25f, 1.36f, 0f)
                )
            }

            name.contains("rightarm") ||
            name.contains("rightforearm") ||
            name.contains("righthand") ||
            name.contains("rightsleeve") -> {
                Pair(
                    pose.rightArm,
                    floatArrayOf(0.25f, 1.36f, 0f)
                )
            }

            name.contains("leftleg") ||
            name.contains("leftsock") ||
            name.contains("leftboot") ||
            name.contains("leftthigh") ||
            name.contains("leftcalf") -> {
                Pair(
                    pose.leftLeg,
                    floatArrayOf(-0.115f, 0.88f, 0f)
                )
            }

            name.contains("rightleg") ||
            name.contains("rightsock") ||
            name.contains("rightboot") ||
            name.contains("rightthigh") ||
            name.contains("rightcalf") -> {
                Pair(
                    pose.rightLeg,
                    floatArrayOf(0.115f, 0.88f, 0f)
                )
            }

            name.contains("head") ||
            name.contains("jaw") ||
            name.contains("ear") ||
            name.contains("nose") ||
            name.contains("eye") ||
            name.contains("iris") ||
            name.contains("pupil") ||
            name.contains("eyebrow") ||
            name.contains("mouth") ||
            name.contains("lip") ||
            name.contains("hair") ||
            name.contains("beard") ||
            name.contains("cheek") ||
            name.contains("chin") -> {
                Pair(
                    pose.head,
                    floatArrayOf(0f, 1.65f, 0f)
                )
            }

            name.contains("torso") ||
            name.contains("jersey") -> {
                Pair(
                    pose.torso,
                    floatArrayOf(0f, 1.1f, 0f)
                )
            }

            else -> null
        }
    }

    // -------------------------------------------------
    // ROTATION DES VERTICES AUTOUR DES ARTICULATIONS
    // -------------------------------------------------

    private fun animateVertex(
        originalX: Float,
        originalY: Float,
        originalZ: Float,
        transform: JointTransform,
        pivot: FloatArray
    ): FloatArray {

        val px = pivot[0]
        val py = pivot[1]
        val pz = pivot[2]

        var x = originalX - px
        var y = originalY - py
        var z = originalZ - pz

        // Rotation X.
        val cx = cos(transform.rotationX)
        val sx = sin(transform.rotationX)

        val y1 = y * cx - z * sx
        val z1 = y * sx + z * cx

        y = y1
        z = z1

        // Rotation Y.
        val cy = cos(transform.rotationY)
        val sy = sin(transform.rotationY)

        val x2 = x * cy + z * sy
        val z2 = -x * sy + z * cy

        x = x2
        z = z2

        // Rotation Z.
        val cz = cos(transform.rotationZ)
        val sz = sin(transform.rotationZ)

        val x3 = x * cz - y * sz
        val y3 = x * sz + y * cz

        x = x3
        y = y3

        return floatArrayOf(
            x + px + transform.offsetX,
            y + py + transform.offsetY,
            z + pz + transform.offsetZ
        )
    }

    // -------------------------------------------------
    // MISE A JOUR DES BUFFERS GPU
    // -------------------------------------------------

    private fun updatePlayerGpuBuffers(
        groups: LinkedHashMap<String, CpuPlayerBatch>
    ) {

        for (batch in playerBatches) {
            batch.vertexCount = 0
        }

        for ((key, cpuBatch) in groups) {

            val data = cpuBatch.vertices.toArray()

            var gpuBatch = playerBatches.find {
                it.key == key
            }

            if (gpuBatch == null) {

                val bufferId = uploadDynamicGeometry(data, 0)

                if (bufferId != 0) {
                    gpuBatch = PlayerBatch(
                        key = key,
                        bufferId = bufferId,
                        vertexCount = data.size / 3,
                        color = cpuBatch.color
                    )

                    playerBatches.add(gpuBatch)
                }

            } else {

                uploadDynamicGeometry(data, gpuBatch.bufferId)
                gpuBatch.vertexCount = data.size / 3
            }
        }
    }

    private fun uploadDynamicGeometry(
        vertices: FloatArray,
        existingBuffer: Int
    ): Int {

        if (vertices.isEmpty()) return existingBuffer

        var bufferId = existingBuffer

        if (bufferId == 0) {
            val ids = IntArray(1)
            GLES20.glGenBuffers(1, ids, 0)
            bufferId = ids[0]
        }

        if (bufferId == 0) return 0

        val buffer = ByteBuffer
            .allocateDirect(vertices.size * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        buffer.put(vertices)
        buffer.position(0)

        GLES20.glBindBuffer(
            GLES20.GL_ARRAY_BUFFER,
            bufferId
        )

        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            vertices.size * Float.SIZE_BYTES,
            buffer,
            GLES20.GL_DYNAMIC_DRAW
        )

        GLES20.glBindBuffer(
            GLES20.GL_ARRAY_BUFFER,
            0
        )

        return bufferId
    }

    // -------------------------------------------------
    // TERRAIN
    // -------------------------------------------------

    private fun buildPitch(): Pair<FloatArray, FloatArray> {

        val light = FloatArrayBuilder()
        val dark = FloatArrayBuilder()

        for (i in 0 until 10) {

            val left = -34f + i * 6.8f
            val right = left + 6.8f

            val destination = if (i % 2 == 0) light else dark

            addRectangle(
                destination,
                left, -52.5f,
                right, 52.5f,
                0.015f
            )
        }

        return Pair(light.toArray(), dark.toArray())
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

        addLine(lines, -34f, y, -52.5f, 34f, y, -52.5f)
        addLine(lines, 34f, y, -52.5f, 34f, y, 52.5f)
        addLine(lines, 34f, y, 52.5f, -34f, y, 52.5f)
        addLine(lines, -34f, y, 52.5f, -34f, y, -52.5f)

        addLine(lines, -34f, y, 0f, 34f, y, 0f)
        addCircle(lines, 0f, y, 0f, 9.15f)

        addLine(lines, -20.16f, y, -52.5f, -20.16f, y, -36f)
        addLine(lines, 20.16f, y, -52.5f, 20.16f, y, -36f)
        addLine(lines, -20.16f, y, -36f, 20.16f, y, -36f)

        addLine(lines, -20.16f, y, 52.5f, -20.16f, y, 36f)
        addLine(lines, 20.16f, y, 52.5f, 20.16f, y, 36f)
        addLine(lines, -20.16f, y, 36f, 20.16f, y, 36f)

        addLine(lines, -9.16f, y, -52.5f, -9.16f, y, -47f)
        addLine(lines, 9.16f, y, -52.5f, 9.16f, y, -47f)
        addLine(lines, -9.16f, y, -47f, 9.16f, y, -47f)

        addLine(lines, -9.16f, y, 52.5f, -9.16f, y, 47f)
        addLine(lines, 9.16f, y, 52.5f, 9.16f, y, 47f)
        addLine(lines, -9.16f, y, 47f, 9.16f, y, 47f)

        addCircle(lines, 0f, y, -41.5f, 0.18f)
        addCircle(lines, 0f, y, 41.5f, 0.18f)

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

        addLine(lines, -halfWidth, 0f, z, -halfWidth, height, z)
        addLine(lines, halfWidth, 0f, z, halfWidth, height, z)
        addLine(lines, -halfWidth, height, z, halfWidth, height, z)

        addLine(lines, -halfWidth, 0f, z, -halfWidth, 0f, backZ)
        addLine(lines, halfWidth, 0f, z, halfWidth, 0f, backZ)

        addLine(lines, -halfWidth, height, z, -halfWidth, height, backZ)
        addLine(lines, halfWidth, height, z, halfWidth, height, backZ)

        addLine(lines, -halfWidth, height, backZ, halfWidth, height, backZ)
        addLine(lines, -halfWidth, 0f, backZ, halfWidth, 0f, backZ)
    }

    private fun addLine(
        list: FloatArrayBuilder,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float
    ) {
        list.add(x1, y1, z1, x2, y2, z2)
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
    // GPU STATIQUE
    // -------------------------------------------------

    private fun uploadStaticGeometry(vertices: FloatArray): Int {

        if (vertices.isEmpty()) return 0

        val ids = IntArray(1)
        GLES20.glGenBuffers(1, ids, 0)

        if (ids[0] == 0) return 0

        val buffer = ByteBuffer
            .allocateDirect(vertices.size * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        buffer.put(vertices)
        buffer.position(0)

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, ids[0])

        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            vertices.size * Float.SIZE_BYTES,
            buffer,
            GLES20.GL_STATIC_DRAW
        )

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

        return ids[0]
    }

    // -------------------------------------------------
    // RENDU
    // -------------------------------------------------

    private fun drawStaticBuffer(
        bufferId: Int,
        vertexCount: Int,
        mode: Int,
        color: FloatArray
    ) {

        if (bufferId == 0 || vertexCount == 0) return

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferId)

        GLES20.glEnableVertexAttribArray(positionHandle)

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

        GLES20.glUniform4fv(colorHandle, 1, color, 0)

        GLES20.glDrawArrays(mode, 0, vertexCount)

        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    // -------------------------------------------------
    // SHADERS
    // -------------------------------------------------

    private fun createProgram(
        vertexSource: String,
        fragmentSource: String
    ): Int {

        val vertex = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragment = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)

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
// CONSTRUCTEUR DE TABLEAUX
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
