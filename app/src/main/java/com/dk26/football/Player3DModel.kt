
package com.dk26.football

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Modèle 3D assemblé d'un joueur de DK 26.
 * Les géométries sont générées par le code, sans modèle externe.
 */
class Player3DModel private constructor(
    val player: FootballPlayer,
    val geometry: GeneratedPlayerModel,
    val skeleton: PlayerSkeleton,
    val animationController: PlayerAnimationController
) {
    var currentPose: PlayerPose? = null
        private set

    var animatedBones: List<AnimatedBone> = emptyList()
        private set

    fun setAction(action: PlayerAction) {
        animationController.setAction(action)
    }

    fun update(
        deltaSeconds: Float,
        movementSpeed: Float = 1f,
        direction: Float = 0f
    ): List<AnimatedBone> {
        val pose = animationController.update(
            deltaSeconds,
            movementSpeed,
            direction
        )

        currentPose = pose
        animatedBones = skeleton.applyPose(pose)

        return animatedBones
    }

    companion object {

        fun create(
            player: FootballPlayer,
            shirtColor: FloatArray = floatArrayOf(0.08f, 0.20f, 0.75f, 1f),
            shortsColor: FloatArray = floatArrayOf(0.08f, 0.08f, 0.12f, 1f),
            socksColor: FloatArray = floatArrayOf(0.92f, 0.92f, 0.92f, 1f),
            bootsColor: FloatArray = floatArrayOf(0.85f, 0.12f, 0.06f, 1f)
        ): Player3DModel {

            require(shirtColor.size == 4)
            require(shortsColor.size == 4)
            require(socksColor.size == 4)
            require(bootsColor.size == 4)

            val parts = PlayerAppearanceGenerator
                .generate(player)
                .parts
                .toMutableList()

            val heightScale = player.appearance.heightCm / 175f
            val bodyScale = when (player.appearance.bodyType) {
                0 -> 0.88f
                1 -> 1f
                2 -> 1.15f
                else -> 1f
            }

            // Maillot principal
            addEllipsoid(
                parts, "Jersey",
                0f, 1.15f * heightScale, -0.008f,
                0.258f * bodyScale,
                0.345f * heightScale,
                0.151f * bodyScale,
                shirtColor
            )

            // Manches
            addEllipsoid(
                parts, "LeftSleeve",
                -0.255f * bodyScale, 1.36f * heightScale, 0f,
                0.095f, 0.12f * heightScale, 0.105f,
                shirtColor
            )

            addEllipsoid(
                parts, "RightSleeve",
                0.255f * bodyScale, 1.36f * heightScale, 0f,
                0.095f, 0.12f * heightScale, 0.105f,
                shirtColor
            )

            // Short de football
            addEllipsoid(
                parts, "Shorts",
                0f, 0.82f * heightScale, 0f,
                0.225f * bodyScale,
                0.15f * heightScale,
                0.13f * bodyScale,
                shortsColor
            )

            // Chaussettes
            addEllipsoid(
                parts, "LeftSock",
                -0.115f, 0.36f * heightScale, 0f,
                0.075f, 0.19f * heightScale, 0.08f,
                socksColor
            )

            addEllipsoid(
                parts, "RightSock",
                0.115f, 0.36f * heightScale, 0f,
                0.075f, 0.19f * heightScale, 0.08f,
                socksColor
            )

            // Chaussures
            addEllipsoid(
                parts, "LeftBoot",
                -0.115f, 0.055f * heightScale, 0.065f,
                0.085f, 0.055f, 0.15f,
                bootsColor
            )

            addEllipsoid(
                parts, "RightBoot",
                0.115f, 0.055f * heightScale, 0.065f,
                0.085f, 0.055f, 0.15f,
                bootsColor
            )

            val generated = GeneratedPlayerModel(
                playerId = player.id,
                parts = parts
            )

            return Player3DModel(
                player = player,
                geometry = generated,
                skeleton = PlayerSkeleton.create(),
                animationController = PlayerAnimationController()
            )
        }

        private fun addEllipsoid(
            parts: MutableList<PlayerGeometryPart>,
            name: String,
            cx: Float,
            cy: Float,
            cz: Float,
            rx: Float,
            ry: Float,
            rz: Float,
            color: FloatArray
        ) {
            parts.add(
                PlayerGeometryPart(
                    name = name,
                    vertices = createEllipsoid(
                        cx, cy, cz, rx, ry, rz
                    ),
                    color = color.copyOf()
                )
            )
        }

        private fun createEllipsoid(
            cx: Float,
            cy: Float,
            cz: Float,
            rx: Float,
            ry: Float,
            rz: Float
        ): FloatArray {
            val output = ArrayList<Float>()
            val latitudeSegments = 12
            val longitudeSegments = 16

            for (lat in 0 until latitudeSegments) {
                val theta1 = PI * lat / latitudeSegments
                val theta2 = PI * (lat + 1) / latitudeSegments

                for (lon in 0 until longitudeSegments) {
                    val phi1 = 2.0 * PI * lon / longitudeSegments
                    val phi2 = 2.0 * PI * (lon + 1) / longitudeSegments

                    val a = point(cx, cy, cz, rx, ry, rz, theta1, phi1)
                    val b = point(cx, cy, cz, rx, ry, rz, theta2, phi1)
                    val c = point(cx, cy, cz, rx, ry, rz, theta2, phi2)
                    val d = point(cx, cy, cz, rx, ry, rz, theta1, phi2)

                    addTriangle(output, a, b, c)
                    addTriangle(output, a, c, d)
                }
            }

            return output.toFloatArray()
        }

        private fun point(
            cx: Float,
            cy: Float,
            cz: Float,
            rx: Float,
            ry: Float,
            rz: Float,
            theta: Double,
            phi: Double
        ): FloatArray {
            return floatArrayOf(
                cx + rx * (sin(theta) * cos(phi)).toFloat(),
                cy + ry * cos(theta).toFloat(),
                cz + rz * (sin(theta) * sin(phi)).toFloat()
            )
        }

        private fun addTriangle(
            output: MutableList<Float>,
            a: FloatArray,
            b: FloatArray,
            c: FloatArray
        ) {
            for (vertex in arrayOf(a, b, c)) {
                output.add(vertex[0])
                output.add(vertex[1])
                output.add(vertex[2])
            }
        }
    }
}
