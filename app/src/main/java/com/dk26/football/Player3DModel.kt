
package com.dk26.football

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Modèle 3D procédural d'un joueur de DK 26.
 * Géométrie générée en Kotlin, sans moteur externe ni modèle téléchargé.
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

        val safeDelta = deltaSeconds.coerceIn(0f, 0.05f)
        val safeSpeed = movementSpeed.coerceIn(0f, 2.5f)

        val pose = animationController.update(
            safeDelta,
            safeSpeed,
            direction
        )

        currentPose = pose
        animatedBones = skeleton.applyPose(pose)

        return animatedBones
    }

    companion object {

        private const val LATITUDE_SEGMENTS = 12
        private const val LONGITUDE_SEGMENTS = 16

        fun create(
            player: FootballPlayer,
            shirtColor: FloatArray = floatArrayOf(
                0.08f, 0.20f, 0.75f, 1f
            ),
            shortsColor: FloatArray = floatArrayOf(
                0.08f, 0.08f, 0.12f, 1f
            ),
            socksColor: FloatArray = floatArrayOf(
                0.92f, 0.92f, 0.92f, 1f
            ),
            bootsColor: FloatArray = floatArrayOf(
                0.85f, 0.12f, 0.06f, 1f
            )
        ): Player3DModel {

            require(shirtColor.size == 4)
            require(shortsColor.size == 4)
            require(socksColor.size == 4)
            require(bootsColor.size == 4)

            val appearance = player.appearance

            val heightScale = (
                appearance.heightCm / 175f
            ).coerceIn(0.8f, 1.26f)

            val bodyScale = when (appearance.bodyType) {
                0 -> 0.88f
                1 -> 1f
                2 -> 1.15f
                else -> 1f
            }

            // Corps et anatomie générés procéduralement.
            val parts = PlayerAppearanceGenerator
                .generate(player)
                .parts
                .toMutableList()

            // Détails faciaux procéduraux.
            parts.addAll(PlayerFace3D.generate(player))

            // Maillot.
            addEllipsoid(
                parts,
                "Jersey",
                0f,
                1.15f * heightScale,
                -0.008f,
                0.258f * bodyScale,
                0.345f * heightScale,
                0.151f * bodyScale,
                shirtColor
            )

            // Manches.
            addEllipsoid(
                parts,
                "LeftSleeve",
                -0.255f * bodyScale,
                1.36f * heightScale,
                0f,
                0.095f,
                0.12f * heightScale,
                0.105f,
                shirtColor
            )

            addEllipsoid(
                parts,
                "RightSleeve",
                0.255f * bodyScale,
                1.36f * heightScale,
                0f,
                0.095f,
                0.12f * heightScale,
                0.105f,
                shirtColor
            )

            // Short.
            addEllipsoid(
                parts,
                "Shorts",
                0f,
                0.82f * heightScale,
                0f,
                0.225f * bodyScale,
                0.15f * heightScale,
                0.13f * bodyScale,
                shortsColor
            )

            // Chaussette gauche.
            addEllipsoid(
                parts,
                "LeftSock",
                -0.115f * bodyScale,
                0.36f * heightScale,
                0f,
                0.075f,
                0.19f * heightScale,
                0.08f,
                socksColor
            )

            // Chaussette droite.
            addEllipsoid(
                parts,
                "RightSock",
                0.115f * bodyScale,
                0.36f * heightScale,
                0f,
                0.075f,
                0.19f * heightScale,
                0.08f,
                socksColor
            )

            // Chaussure gauche.
            addEllipsoid(
                parts,
                "LeftBoot",
                -0.115f * bodyScale,
                0.055f * heightScale,
                0.065f,
                0.085f,
                0.055f,
                0.15f,
                bootsColor
            )

            // Chaussure droite.
            addEllipsoid(
                parts,
                "RightBoot",
                0.115f * bodyScale,
                0.055f * heightScale,
                0.065f,
                0.085f,
                0.055f,
                0.15f,
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
                        cx, cy, cz,
                        rx, ry, rz
                    ),
                    color = color.copyOf()
                )
            )
        }

        /**
         * Génération optimisée d'une ellipsoïde triangulée.
         * Les sommets sont directement stockés dans un FloatArray.
         */
        private fun createEllipsoid(
            cx: Float,
            cy: Float,
            cz: Float,
            rx: Float,
            ry: Float,
            rz: Float
        ): FloatArray {

            val output = PlayerModelFloatArrayBuilder(
                LATITUDE_SEGMENTS *
                    LONGITUDE_SEGMENTS * 18
            )

            for (lat in 0 until LATITUDE_SEGMENTS) {

                val theta1 = PI * lat / LATITUDE_SEGMENTS
                val theta2 = PI * (lat + 1) / LATITUDE_SEGMENTS

                for (lon in 0 until LONGITUDE_SEGMENTS) {

                    val phi1 = 2.0 * PI * lon / LONGITUDE_SEGMENTS
                    val phi2 = 2.0 * PI * (lon + 1) / LONGITUDE_SEGMENTS

                    val a = point(
                        cx, cy, cz, rx, ry, rz,
                        theta1, phi1
                    )

                    val b = point(
                        cx, cy, cz, rx, ry, rz,
                        theta2, phi1
                    )

                    val c = point(
                        cx, cy, cz, rx, ry, rz,
                        theta2, phi2
                    )

                    val d = point(
                        cx, cy, cz, rx, ry, rz,
                        theta1, phi2
                    )

                    output.addTriangle(a, b, c)
                    output.addTriangle(a, c, d)
                }
            }

            return output.toArray()
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

            val sinTheta = sin(theta)

            return floatArrayOf(
                cx + rx * (sinTheta * cos(phi)).toFloat(),
                cy + ry * cos(theta).toFloat(),
                cz + rz * (sinTheta * sin(phi)).toFloat()
            )
        }
    }
}

/**
 * Constructeur de tableaux propre à Player3DModel.
 * Son nom évite tout conflit avec MainActivity.kt.
 */
private class PlayerModelFloatArrayBuilder(
    initialCapacity: Int = 1024
) {

    private var values = FloatArray(
        initialCapacity.coerceAtLeast(3)
    )

    private var count = 0

    fun addTriangle(
        a: FloatArray,
        b: FloatArray,
        c: FloatArray
    ) {

        ensureCapacity(9)

        values[count++] = a[0]
        values[count++] = a[1]
        values[count++] = a[2]

        values[count++] = b[0]
        values[count++] = b[1]
        values[count++] = b[2]

        values[count++] = c[0]
        values[count++] = c[1]
        values[count++] = c[2]
    }

    private fun ensureCapacity(extra: Int) {

        val required = count + extra

        if (required > values.size) {

            var newSize = values.size

            while (newSize < required) {
                newSize = (newSize * 2).coerceAtLeast(required)
            }

            values = values.copyOf(newSize)
        }
    }

    fun toArray(): FloatArray {
        return values.copyOf(count)
    }
}
