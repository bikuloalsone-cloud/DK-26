
package com.dk26.football

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class PlayerGeometryPart(
    val name: String,
    val vertices: FloatArray,
    val color: FloatArray
)

data class GeneratedPlayerModel(
    val playerId: String,
    val parts: List<PlayerGeometryPart>
)

object PlayerAppearanceGenerator {

    fun generate(player: FootballPlayer): GeneratedPlayerModel {
        val appearance = player.appearance
        val parts = mutableListOf<PlayerGeometryPart>()

        val heightScale = appearance.heightCm / 175f
        val bodyScale = when (appearance.bodyType) {
            0 -> 0.88f
            1 -> 1.0f
            2 -> 1.15f
            else -> 1.0f
        }

        val skin = skinColor(appearance.skinTone)
        val hair = colorFromCode(appearance.hairColor)
        val eyes = colorFromCode(appearance.eyeColor)

        // Corps
        addPart(
            parts, "Torso",
            0f, 1.13f * heightScale, 0f,
            0.25f * bodyScale, 0.34f * heightScale, 0.14f * bodyScale,
            floatArrayOf(0.12f, 0.22f, 0.75f, 1f)
        )

        // Cou
        addPart(
            parts, "Neck",
            0f, 1.48f * heightScale, 0f,
            0.075f, 0.12f * heightScale, 0.075f,
            skin
        )

        // Tête et forme du visage
        val headWidth = when (appearance.headShape) {
            0 -> 0.135f
            1 -> 0.155f
            2 -> 0.17f
            else -> 0.15f
        } * appearance.faceWidth

        val headHeight = 0.19f * heightScale
        val headDepth = 0.125f

        addPart(
            parts, "Head",
            0f, 1.72f * heightScale, 0f,
            headWidth, headHeight, headDepth,
            skin
        )

        // Mâchoire
        addPart(
            parts, "Jaw",
            0f, 1.635f * heightScale, 0.025f,
            0.105f * appearance.jawWidth,
            0.075f * heightScale,
            0.105f,
            skin
        )

        // Oreilles
        addPart(
            parts, "LeftEar",
            -headWidth, 1.72f * heightScale, 0f,
            0.035f, 0.065f, 0.035f, skin
        )

        addPart(
            parts, "RightEar",
            headWidth, 1.72f * heightScale, 0f,
            0.035f, 0.065f, 0.035f, skin
        )

        // Nez
        addPart(
            parts, "Nose",
            0f, 1.705f * heightScale, 0.12f,
            0.035f * appearance.noseSize,
            0.065f * appearance.noseSize,
            0.045f,
            skin
        )

        // Yeux
        val eyeWidth = 0.038f
        val eyeHeight = when (appearance.eyeShape) {
            0 -> 0.018f
            1 -> 0.025f
            else -> 0.021f
        }

        addPart(
            parts, "LeftEye",
            -0.052f, 1.755f * heightScale, 0.105f,
            eyeWidth, eyeHeight, 0.018f,
            floatArrayOf(0.96f, 0.96f, 0.93f, 1f)
        )

        addPart(
            parts, "RightEye",
            0.052f, 1.755f * heightScale, 0.105f,
            eyeWidth, eyeHeight, 0.018f,
            floatArrayOf(0.96f, 0.96f, 0.93f, 1f)
        )

        // Iris
        addPart(
            parts, "LeftIris",
            -0.052f, 1.755f * heightScale, 0.123f,
            0.014f, 0.014f, 0.009f, eyes
        )

        addPart(
            parts, "RightIris",
            0.052f, 1.755f * heightScale, 0.123f,
            0.014f, 0.014f, 0.009f, eyes
        )

        // Bouche
        addPart(
            parts, "Mouth",
            0f, 1.625f * heightScale, 0.105f,
            0.045f, 0.012f, 0.012f,
            floatArrayOf(0.38f, 0.12f, 0.12f, 1f)
        )

        // Cheveux
        addPart(
            parts, "Hair",
            0f, 1.84f * heightScale, -0.005f,
            headWidth * 1.04f, 0.075f, headDepth * 1.05f,
            hair
        )

        // Barbe
        if (appearance.facialHair > 0) {
            addPart(
                parts, "Beard",
                0f, 1.615f * heightScale, 0.055f,
                0.105f, 0.065f, 0.075f,
                hair
            )
        }

        // Bras
        addPart(
            parts, "LeftArm",
            -0.29f * bodyScale, 1.16f * heightScale, 0f,
            0.075f, 0.28f * heightScale, 0.075f,
            skin
        )

        addPart(
            parts, "RightArm",
            0.29f * bodyScale, 1.16f * heightScale, 0f,
            0.075f, 0.28f * heightScale, 0.075f,
            skin
        )

        // Mains
        addPart(
            parts, "LeftHand",
            -0.29f * bodyScale, 0.86f * heightScale, 0f,
            0.065f, 0.075f, 0.055f, skin
        )

        addPart(
            parts, "RightHand",
            0.29f * bodyScale, 0.86f * heightScale, 0f,
            0.065f, 0.075f, 0.055f, skin
        )

        // Jambes
        addPart(
            parts, "LeftLeg",
            -0.115f, 0.48f * heightScale, 0f,
            0.09f, 0.43f * heightScale, 0.10f,
            floatArrayOf(0.12f, 0.22f, 0.75f, 1f)
        )

        addPart(
            parts, "RightLeg",
            0.115f, 0.48f * heightScale, 0f,
            0.09f, 0.43f * heightScale, 0.10f,
            floatArrayOf(0.12f, 0.22f, 0.75f, 1f)
        )

        return GeneratedPlayerModel(player.id, parts)
    }

    private fun addPart(
        parts: MutableList<PlayerGeometryPart>,
        name: String,
        x: Float,
        y: Float,
        z: Float,
        radiusX: Float,
        radiusY: Float,
        radiusZ: Float,
        color: FloatArray
    ) {
        parts.add(
            PlayerGeometryPart(
                name = name,
                vertices = createEllipsoid(
                    x, y, z,
                    radiusX, radiusY, radiusZ
                ),
                color = color
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
        val vertices = ArrayList<Float>()
        val latitudeSegments = 12
        val longitudeSegments = 16

        for (latitude in 0 until latitudeSegments) {
            val theta1 = PI * latitude / latitudeSegments
            val theta2 = PI * (latitude + 1) / latitudeSegments

            for (longitude in 0 until longitudeSegments) {
                val phi1 = 2.0 * PI * longitude / longitudeSegments
                val phi2 = 2.0 * PI * (longitude + 1) / longitudeSegments

                val p1 = point(cx, cy, cz, rx, ry, rz, theta1, phi1)
                val p2 = point(cx, cy, cz, rx, ry, rz, theta2, phi1)
                val p3 = point(cx, cy, cz, rx, ry, rz, theta2, phi2)
                val p4 = point(cx, cy, cz, rx, ry, rz, theta1, phi2)

                addTriangle(vertices, p1, p2, p3)
                addTriangle(vertices, p1, p3, p4)
            }
        }

        return vertices.toFloatArray()
    }

    private fun point(
        cx: Float, cy: Float, cz: Float,
        rx: Float, ry: Float, rz: Float,
        theta: Double, phi: Double
    ): FloatArray {
        val x = rx * sin(theta) * cos(phi)
        val y = ry * cos(theta)
        val z = rz * sin(theta) * sin(phi)

        return floatArrayOf(
            cx + x.toFloat(),
            cy + y.toFloat(),
            cz + z.toFloat()
        )
    }

    private fun addTriangle(
        output: MutableList<Float>,
        a: FloatArray,
        b: FloatArray,
        c: FloatArray
    ) {
        for (point in listOf(a, b, c)) {
            output.add(point[0])
            output.add(point[1])
            output.add(point[2])
        }
    }

    private fun skinColor(tone: Int): FloatArray {
        val colors = listOf(
            floatArrayOf(0.96f, 0.77f, 0.62f, 1f),
            floatArrayOf(0.82f, 0.57f, 0.40f, 1f),
            floatArrayOf(0.63f, 0.39f, 0.25f, 1f),
            floatArrayOf(0.38f, 0.23f, 0.16f, 1f),
            floatArrayOf(0.22f, 0.13f, 0.09f, 1f)
        )

        return colors[tone.coerceIn(0, colors.lastIndex)]
    }

    private fun colorFromCode(code: Int): FloatArray {
        val colors = listOf(
            floatArrayOf(0.08f, 0.05f, 0.03f, 1f),
            floatArrayOf(0.30f, 0.15f, 0.07f, 1f),
            floatArrayOf(0.55f, 0.30f, 0.12f, 1f),
            floatArrayOf(0.12f, 0.12f, 0.12f, 1f),
            floatArrayOf(0.65f, 0.40f, 0.15f, 1f),
            floatArrayOf(0.18f, 0.25f, 0.45f, 1f)
        )

        return colors[code.coerceIn(0, colors.lastIndex)]
    }
}
