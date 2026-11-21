
package com.dk26.football

import android.app.Activity
import android.content.pm.ActivityInfo
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Bundle
import android.view.View
import android.view.MotionEvent
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
    private lateinit var renderer: DK26Renderer
    private var touchStartX = 0f
    private var touchStartY = 0f

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
            renderer = DK26Renderer()
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }

        setContentView(gameView)
    }

    /**
     * Un glissement sur l'écran déclenche un tir.
     * La direction horizontale règle l'angle ; la longueur du geste règle la puissance.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.x
                touchStartY = event.y
                return true
            }

            MotionEvent.ACTION_UP -> {
                val dx = (event.x - touchStartX) / gameView.width.coerceAtLeast(1)
                val dy = (event.y - touchStartY) / gameView.height.coerceAtLeast(1)
                val distance = kotlin.math.sqrt(dx * dx + dy * dy)

                if (distance > 0.025f) {
                    val horizontalAim = (dx * 2.4f).coerceIn(-0.85f, 0.85f)
                    val power = (0.30f + distance * 1.8f).coerceIn(0.30f, 1f)
                    gameView.queueEvent {
                        renderer.shootBall(horizontalAim, power)
                    }
                }
                return true
            }
        }
        return true
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
    private var ballBuffer = 0
    private var ballVertexCount = 0

    private val ballPhysics = BallPhysics()
    private val ballColor = floatArrayOf(0.96f, 0.96f, 0.91f, 1f)
    private val ballModelMatrix = FloatArray(16)
    private val ballMvpMatrix = FloatArray(16)
    private val goalFrames = listOf(
        BallGoalFrame(goalLineZ = -52.5f),
        BallGoalFrame(goalLineZ = 52.5f)
    )
    private val playerColliders = mutableListOf<BallPlayerCollider>()

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
        val parts: List<PlayerGeometryPart>,
        val bindPositions: Map<PlayerBone, BonePosition>
    )

    private data class CpuPlayerBatch(
        val color: FloatArray,
        val vertices: FloatArrayBuilder
    )

    private data class BonePartTransform(
        val bindPosition: BonePosition,
        val animatedBone: AnimatedBone
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
        GLES20.glClearColor(0.025f, 0.12f, 0.045f, 1f)

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

        val ballVertices = buildBallSphere()
        ballVertexCount = ballVertices.size / 3
        ballBuffer = uploadStaticGeometry(ballVertices)
        ballPhysics.reset(BallVector3(0f, 0.12f, 0f))

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

        updateAnimatedPlayers(deltaSeconds)
        updateBall(deltaSeconds)

        GLES20.glUseProgram(program)

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

        GLES20.glLineWidth(2f)

        drawStaticBuffer(
            linesBuffer,
            linesVertexCount,
            GLES20.GL_LINES,
            lineColor
        )

        for (batch in playerBatches) {
            drawStaticBuffer(
                batch.bufferId,
                batch.vertexCount,
                GLES20.GL_TRIANGLES,
                batch.color
            )
        }

        drawBall()

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

                model.setAction(PlayerAction.RUN)

                model.update(
                    deltaSeconds = animationOffset * 0.003f,
                    movementSpeed = 0.85f
                )

                val bindPositions = buildBindPositions(
                    model.skeleton.bones
                )

                animatedPlayers.add(
                    AnimatedPlayer(
                        matchPlayer = matchPlayer,
                        parts = model.geometry.parts,
                        bindPositions = bindPositions
                    )
                )

                playerColliders.add(
                    BallPlayerCollider(
                        position = BallVector3(
                            matchPlayer.x,
                            0.42f,
                            matchPlayer.z
                        ),
                        radius = 0.43f
                    )
                )

                animationOffset++
            }
        }
    }

    // -------------------------------------------------
    // CALCUL DES POSITIONS DE REPOS DES OS
    // -------------------------------------------------

    private fun buildBindPositions(
        definitions: List<BoneDefinition>
    ): Map<PlayerBone, BonePosition> {

        val positions = HashMap<PlayerBone, BonePosition>(
            definitions.size
        )

        // Les définitions du squelette sont ordonnées
        // du parent vers les enfants.
        for (definition in definitions) {

            val local = definition.bindPosition
            val parent = definition.parent

            val worldPosition = if (parent == null) {

                BonePosition(
                    local.x,
                    local.y,
                    local.z
                )

            } else {

                val parentPosition = positions[parent]
                    ?: continue

                BonePosition(
                    parentPosition.x + local.x,
                    parentPosition.y + local.y,
                    parentPosition.z + local.z
                )
            }

            positions[definition.bone] = worldPosition
        }

        return positions
    }

    // -------------------------------------------------
    // ANIMATION DES 22 JOUEURS
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

            val animatedBones = model.animatedBones
            if (animatedBones.isEmpty()) continue

            val redTeam = matchPlayer.team == TeamSide.RED

            for (part in animatedPlayer.parts) {

                val vertices = part.vertices
                val color = part.color

                if (color.size < 4 || vertices.size < 9) continue

                val boneTransform = getPartBoneTransform(
                    part.name,
                    animatedPlayer.bindPositions,
                    animatedBones
                )

                val key = color.joinToString("|") {
                    java.lang.Float.floatToIntBits(it).toString()
                }

                val batch = groups.getOrPut(key) {
                    CpuPlayerBatch(
                        color = color.copyOf(),
                        vertices = FloatArrayBuilder()
                    )
                }

                val transformed = FloatArray(3)

                var index = 0

                while (index + 2 < vertices.size) {

                    val originalX = vertices[index]
                    val originalY = vertices[index + 1]
                    val originalZ = vertices[index + 2]

                    if (boneTransform != null) {
                        transformVertex(
                            originalX,
                            originalY,
                            originalZ,
                            boneTransform,
                            transformed
                        )
                    } else {
                        transformed[0] = originalX
                        transformed[1] = originalY
                        transformed[2] = originalZ
                    }

                    val worldX = if (redTeam) {
                        matchPlayer.x - transformed[0]
                    } else {
                        matchPlayer.x + transformed[0]
                    }

                    val worldZ = if (redTeam) {
                        matchPlayer.z - transformed[2]
                    } else {
                        matchPlayer.z + transformed[2]
                    }

                    batch.vertices.add(
                        worldX,
                        transformed[1] + 0.025f,
                        worldZ
                    )

                    index += 3
                }
            }
        }

        updatePlayerGpuBuffers(groups)
    }

    // -------------------------------------------------
    // ASSOCIATION DES PARTIES DU CORPS AUX ARTICULATIONS
    // -------------------------------------------------

    private fun getPartBoneTransform(
        partName: String,
        bindPositions: Map<PlayerBone, BonePosition>,
        animatedBones: List<AnimatedBone>
    ): BonePartTransform? {

        val name = partName.lowercase(Locale.ROOT)

        val bone = when {

            // BRAS GAUCHE
            name.contains("leftforearm") ->
                PlayerBone.LEFT_FOREARM

            name.contains("lefthand") ->
                PlayerBone.LEFT_HAND

            name.contains("leftupperarm") ||
                name.contains("leftsleeve") ->
                PlayerBone.LEFT_UPPER_ARM

            name.contains("leftshoulder") ||
                name.contains("leftarm") ->
                PlayerBone.LEFT_SHOULDER

            // BRAS DROIT
            name.contains("rightforearm") ->
                PlayerBone.RIGHT_FOREARM

            name.contains("righthand") ->
                PlayerBone.RIGHT_HAND

            name.contains("rightupperarm") ||
                name.contains("rightsleeve") ->
                PlayerBone.RIGHT_UPPER_ARM

            name.contains("rightshoulder") ||
                name.contains("rightarm") ->
                PlayerBone.RIGHT_SHOULDER

            // JAMBE GAUCHE
            name.contains("leftfoot") ||
                name.contains("leftboot") ->
                PlayerBone.LEFT_FOOT

            name.contains("leftcalf") ||
                name.contains("leftsock") ->
                PlayerBone.LEFT_CALF

            name.contains("leftthigh") ||
                name.contains("leftleg") ->
                PlayerBone.LEFT_THIGH

            // JAMBE DROITE
            name.contains("rightfoot") ||
                name.contains("rightboot") ->
                PlayerBone.RIGHT_FOOT

            name.contains("rightcalf") ||
                name.contains("rightsock") ->
                PlayerBone.RIGHT_CALF

            name.contains("rightthigh") ||
                name.contains("rightleg") ->
                PlayerBone.RIGHT_THIGH

            // TETE ET VISAGE
            name.contains("neck") ->
                PlayerBone.NECK

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
                name.contains("chin") ->
                PlayerBone.HEAD

            // TRONC ET BASSIN
            name.contains("torso") ||
                name.contains("chest") ||
                name.contains("jersey") ->
                PlayerBone.CHEST

            name.contains("shorts") ||
                name.contains("pelvis") ->
                PlayerBone.PELVIS

            else -> null
        } ?: return null

        val bindPosition = bindPositions[bone]
            ?: return null

        val animatedBone = animatedBones.firstOrNull {
            it.bone == bone
        } ?: return null

        return BonePartTransform(
            bindPosition = bindPosition,
            animatedBone = animatedBone
        )
    }

    // -------------------------------------------------
    // TRANSFORMATION DES SOMMETS AVEC LES OS
    // -------------------------------------------------

    private fun transformVertex(
        x: Float,
        y: Float,
        z: Float,
        transform: BonePartTransform,
        output: FloatArray
    ) {

        val bind = transform.bindPosition
        val animated = transform.animatedBone

        // Coordonnées locales du sommet par rapport
        // à la position globale de repos de son articulation.
        val localX = x - bind.x
        val localY = y - bind.y
        val localZ = z - bind.z

        val rotation = animated.rotation

        val cx = cos(rotation.x)
        val sx = sin(rotation.x)

        val cy = cos(rotation.y)
        val sy = sin(rotation.y)

        val cz = cos(rotation.z)
        val sz = sin(rotation.z)

        // Rotation autour de X.
        val x1 = localX
        val y1 = localY * cx - localZ * sx
        val z1 = localY * sx + localZ * cx

        // Rotation autour de Y.
        val x2 = x1 * cy + z1 * sy
        val y2 = y1
        val z2 = -x1 * sy + z1 * cy

        // Rotation autour de Z.
        val x3 = x2 * cz - y2 * sz
        val y3 = x2 * sz + y2 * cz

        // Retour dans l'espace local du joueur.
        output[0] = animated.position.x + x3
        output[1] = animated.position.y + y3
        output[2] = animated.position.z + z2
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
    // BALLON : PHYSIQUE, COMMANDES ET RENDU 3D
    // -------------------------------------------------

    /**
     * Appelée depuis le thread OpenGL après un geste tactile.
     * Le ballon est frappé vers le but opposé, avec une visée latérale.
     */
    fun shootBall(horizontalAim: Float, power: Float) {
        val direction = BallVector3(
            horizontalAim.coerceIn(-0.85f, 0.85f),
            0f,
            -1f
        ).normalized()

        ballPhysics.shoot(
            direction = direction,
            power = power.coerceIn(0.25f, 1f),
            lift = 0.12f
        )
    }

    private fun updateBall(deltaSeconds: Float) {
        val events = ballPhysics.update(
            deltaSeconds = deltaSeconds,
            players = playerColliders,
            goals = goalFrames
        )

        if (events.any { it.type == BallEventType.GOAL }) {
            // Remise au centre après un but. Le comptage du score
            // sera relié au futur gestionnaire de match.
            ballPhysics.reset(BallVector3(0f, 0.12f, 0f))
        }
    }

    /**
     * Sphère triangulée légère : 10 x 14 segments.
     */
    private fun buildBallSphere(): FloatArray {
        val output = FloatArrayBuilder(10 * 14 * 18)
        val latitudeSegments = 10
        val longitudeSegments = 14

        fun vertex(theta: Double, phi: Double): FloatArray {
            val st = sin(theta)
            return floatArrayOf(
                (st * cos(phi)).toFloat(),
                cos(theta).toFloat(),
                (st * sin(phi)).toFloat()
            )
        }

        for (lat in 0 until latitudeSegments) {
            val theta1 = Math.PI * lat / latitudeSegments
            val theta2 = Math.PI * (lat + 1) / latitudeSegments

            for (lon in 0 until longitudeSegments) {
                val phi1 = 2.0 * Math.PI * lon / longitudeSegments
                val phi2 = 2.0 * Math.PI * (lon + 1) / longitudeSegments

                val a = vertex(theta1, phi1)
                val b = vertex(theta2, phi1)
                val c = vertex(theta2, phi2)
                val d = vertex(theta1, phi2)

                output.add(a[0], a[1], a[2], b[0], b[1], b[2], c[0], c[1], c[2])
                output.add(a[0], a[1], a[2], c[0], c[1], c[2], d[0], d[1], d[2])
            }
        }
        return output.toArray()
    }

    private fun drawBall() {
        if (ballBuffer == 0 || ballVertexCount == 0) return

        val ballPosition = ballPhysics.position
        Matrix.setIdentityM(ballModelMatrix, 0)
        Matrix.translateM(
            ballModelMatrix, 0,
            ballPosition.x,
            ballPosition.y,
            ballPosition.z
        )
        Matrix.scaleM(
            ballModelMatrix, 0,
            0.11f, 0.11f, 0.11f
        )
        Matrix.multiplyMM(
            ballMvpMatrix, 0,
            viewProjection, 0,
            ballModelMatrix, 0
        )

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, ballBuffer)
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(
            positionHandle, 3, GLES20.GL_FLOAT, false, 0, 0
        )
        GLES20.glUniformMatrix4fv(
            matrixHandle, 1, false, ballMvpMatrix, 0
        )
        GLES20.glUniform4fv(colorHandle, 1, ballColor, 0)
        GLES20.glDrawArrays(
            GLES20.GL_TRIANGLES, 0, ballVertexCount
        )
        GLES20.glDisableVertexAttribArray(positionHandle)
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
