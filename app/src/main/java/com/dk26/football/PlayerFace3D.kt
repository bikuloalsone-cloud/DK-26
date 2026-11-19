
package com.dk26.football

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Générateur procédural de détails faciaux 3D pour DK 26.
 * Géométrie entièrement créée par Kotlin.
 *
 * Convention :
 * X = largeur
 * Y = hauteur
 * Z positif = avant du visage
 */
object PlayerFace3D {

    private const val LATITUDE = 24
    private const val LONGITUDE = 32

    private fun skinColor(tone: Int): FloatArray {
        val colors = arrayOf(
            floatArrayOf(0.96f, 0.78f, 0.64f, 1f),
            floatArrayOf(0.82f, 0.57f, 0.40f, 1f),
            floatArrayOf(0.65f, 0.39f, 0.25f, 1f),
            floatArrayOf(0.43f, 0.25f, 0.16f, 1f),
            floatArrayOf(0.29f, 0.16f, 0.11f, 1f),
            floatArrayOf(0.18f, 0.095f, 0.065f, 1f)
        )
        return colors[Math.floorMod(tone, colors.size)].copyOf()
    }

    private fun hairColor(color: Int): FloatArray {
        val colors = arrayOf(
            floatArrayOf(0.045f, 0.025f, 0.018f, 1f),
            floatArrayOf(0.16f, 0.075f, 0.025f, 1f),
            floatArrayOf(0.34f, 0.16f, 0.055f, 1f),
            floatArrayOf(0.65f, 0.36f, 0.12f, 1f),
            floatArrayOf(0.08f, 0.08f, 0.075f, 1f)
        )
        return colors[Math.floorMod(color, colors.size)].copyOf()
    }

    private fun eyeColor(color: Int): FloatArray {
        val colors = arrayOf(
            floatArrayOf(0.22f, 0.11f, 0.055f, 1f),
            floatArrayOf(0.12f, 0.25f, 0.16f, 1f),
            floatArrayOf(0.18f, 0.36f, 0.55f, 1f),
            floatArrayOf(0.40f, 0.25f, 0.12f, 1f),
            floatArrayOf(0.08f, 0.08f, 0.08f, 1f)
        )
        return colors[Math.floorMod(color, colors.size)].copyOf()
    }

    fun generate(player: FootballPlayer): List<PlayerGeometryPart> {
        val result = mutableListOf<PlayerGeometryPart>()

        val appearance = player.appearance
        val scale = appearance.heightCm / 175f

        val width = appearance.faceWidth.coerceIn(0.75f, 1.30f)
        val jaw = appearance.jawWidth.coerceIn(0.70f, 1.30f)
        val nose = appearance.noseSize.coerceIn(0.70f, 1.40f)

        val skin = skinColor(appearance.skinTone)
        val hair = hairColor(appearance.hairColor)
        val iris = eyeColor(appearance.eyeColor)

        val darkSkin = shade(skin, 0.72f)
        val lips = shade(skin, 0.62f)
        val white = floatArrayOf(0.94f, 0.92f, 0.87f, 1f)
        val black = floatArrayOf(0.018f, 0.012f, 0.01f, 1f)
        val shine = floatArrayOf(1f, 1f, 1f, 1f)

        val headY = 1.72f * scale

        // Pommettes
        add(
            result, "FaceLeftCheek",
            -0.083f * width, 1.685f * scale, 0.085f,
            0.045f, 0.045f, 0.035f, skin
        )

        add(
            result, "FaceRightCheek",
            0.083f * width, 1.685f * scale, 0.085f,
            0.045f, 0.045f, 0.035f, skin
        )

        // Relief de la mâchoire
        add(
            result, "FaceChin",
            0f, 1.585f * scale, 0.065f,
            0.061f * jaw, 0.045f, 0.035f, skin
        )

        // Oreilles détaillées
        for (side in listOf(-1f, 1f)) {
            add(
                result, "DetailedEar",
                side * 0.137f * width,
                1.715f * scale,
                0.005f,
                0.027f, 0.055f, 0.027f, skin
            )

            add(
                result, "EarInner",
                side * 0.145f * width,
                1.715f * scale,
                0.027f,
                0.010f, 0.031f, 0.009f,
                darkSkin
            )
        }

        // Structure du nez
        add(
            result, "NoseBridge",
            0f, 1.700f * scale, 0.119f,
            0.022f * nose, 0.060f, 0.030f * nose,
            skin
        )

        add(
            result, "NoseTip",
            0f, 1.665f * scale, 0.145f,
            0.033f * nose, 0.026f, 0.025f * nose,
            skin
        )

        for (side in listOf(-1f, 1f)) {
            add(
                result, "Nostril",
                side * 0.020f * nose,
                1.650f * scale,
                0.154f,
                0.009f, 0.006f, 0.004f,
                darkSkin
            )
        }

        // Yeux, iris, pupilles et reflets
        for (side in listOf(-1f, 1f)) {
            val eyeX = side * 0.052f * width
            val eyeY = 1.755f * scale

            add(
                result, "EyeWhite",
                eyeX, eyeY, 0.116f,
                0.035f, 0.019f, 0.012f,
                white
            )

            add(
                result, "EyeIris",
                eyeX, eyeY, 0.129f,
                0.014f, 0.015f, 0.007f,
                iris
            )

            add(
                result, "EyePupil",
                eyeX, eyeY, 0.135f,
                0.007f, 0.009f, 0.004f,
                black
            )

            add(
                result, "EyeHighlight",
                eyeX - 0.004f, eyeY + 0.005f, 0.140f,
                0.0035f, 0.0035f, 0.002f,
                shine
            )

            // Paupières
            add(
                result, "UpperEyelid",
                eyeX, eyeY + 0.020f, 0.116f,
                0.037f, 0.008f, 0.010f,
                skin
            )

            add(
                result, "LowerEyelid",
                eyeX, eyeY - 0.019f, 0.116f,
                0.034f, 0.006f, 0.008f,
                skin
            )

            // Sourcils
            add(
                result, "Eyebrow",
                eyeX, eyeY + 0.036f, 0.112f,
                0.040f, 0.009f, 0.009f,
                hair
            )
        }

        // Lèvres supérieure et inférieure
        add(
            result, "UpperLip",
            0f, 1.622f * scale, 0.111f,
            0.040f, 0.009f, 0.010f,
            lips
        )

        add(
            result, "LowerLip",
            0f, 1.610f * scale, 0.110f,
            0.034f, 0.008f, 0.009f,
            shade(lips, 1.12f)
        )

        // Chevelure : volume supérieur
        val hairHeight = when (appearance.hairStyle % 5) {
            0 -> 0.055f
            1 -> 0.075f
            2 -> 0.095f
            3 -> 0.065f
            else -> 0.085f
        }

        add(
            result, "HairCap",
            0f, headY + 0.105f * scale,
            -0.008f,
            0.128f * width,
            hairHeight * scale,
            0.123f,
            hair
        )

        // Détails de coiffure
        for (i in 0 until 5) {
            val x = (i - 2) * 0.035f

            add(
                result, "HairDetail",
                x, headY + 0.145f * scale,
                0.005f,
                0.025f, 0.018f, 0.025f,
                shade(hair, 0.85f + i * 0.04f)
            )
        }

        // Favoris
        for (side in listOf(-1f, 1f)) {
            add(
                result, "Sideburn",
                side * 0.115f * width,
                1.685f * scale,
                0.025f,
                0.014f, 0.045f, 0.025f,
                hair
            )
        }

        // Barbe selon la morphologie choisie
        if (appearance.facialHair > 0) {
            add(
                result, "Beard",
                0f, 1.605f * scale,
                0.065f,
                0.090f * jaw, 0.045f, 0.065f,
                hair
            )

            if (appearance.facialHair >= 2) {
                add(
                    result, "Mustache",
                    0f, 1.638f * scale,
                    0.116f,
                    0.042f, 0.010f, 0.012f,
                    hair
                )
            }
        }

        return result
    }

    private fun shade(color: FloatArray, factor: Float): FloatArray {
        return floatArrayOf(
            (color[0] * factor).coerceIn(0f, 1f),
            (color[1] * factor).coerceIn(0f, 1f),
            (color[2] * factor).coerceIn(0f, 1f),
            color[3]
        )
    }

    private fun add(
        output: MutableList<PlayerGeometryPart>,
        name: String,
        x: Float,
        y: Float,
        z: Float,
        rx: Float,
        ry: Float,
        rz: Float,
        color: FloatArray
    ) {
        output.add(
            PlayerGeometryPart(
                name = name,
                vertices = ellipsoid(x, y, z, rx, ry, rz),
                color = color.copyOf()
            )
        )
    }

    private fun ellipsoid(
        cx: Float,
        cy: Float,
        cz: Float,
        rx: Float,
        ry: Float,
        rz: Float
    ): FloatArray {
        val output = ArrayList<Float>()

        for (lat in 0 until LATITUDE) {
            val t1 = PI * lat / LATITUDE
            val t2 = PI * (lat + 1) / LATITUDE

            for (lon in 0 until LONGITUDE) {
                val p1 = 2.0 * PI * lon / LONGITUDE
                val p2 = 2.0 * PI * (lon + 1) / LONGITUDE

                val a = point(cx, cy, cz, rx, ry, rz, t1, p1)
                val b = point(cx, cy, cz, rx, ry, rz, t2, p1)
                val c = point(cx, cy, cz, rx, ry, rz, t2, p2)
                val d = point(cx, cy, cz, rx, ry, rz, t1, p2)

                triangle(output, a, b, c)
                triangle(output, a, c, d)
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

    private fun triangle(
        output: MutableList<Float>,
        a: FloatArray,
        b: FloatArray,
        c: FloatArray
    ) {
        for (point in arrayOf(a, b, c)) {
            output.add(point[0])
            output.add(point[1])
            output.add(point[2])
        }
    }
}
