
package com.dk26.football

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Générateur procédural de textures faciales DK 26.
 *
 * Toutes les textures sont créées par calcul.
 * Aucun fichier graphique externe n'est nécessaire.
 */
object PlayerFaceTextures {

    data class TextureSet(
        val skin: Bitmap,
        val hair: Bitmap,
        val iris: Bitmap
    )

    private val cache = mutableMapOf<String, TextureSet>()

    private val skinPalette = arrayOf(
        intArrayOf(245, 205, 174),
        intArrayOf(211, 151, 110),
        intArrayOf(174, 105, 70),
        intArrayOf(119, 68, 43),
        intArrayOf(82, 45, 30),
        intArrayOf(49, 27, 18)
    )

    private val hairPalette = arrayOf(
        intArrayOf(15, 9, 7),
        intArrayOf(48, 24, 12),
        intArrayOf(91, 48, 22),
        intArrayOf(150, 91, 39),
        intArrayOf(35, 35, 32)
    )

    private val irisPalette = arrayOf(
        intArrayOf(91, 49, 24),
        intArrayOf(49, 105, 69),
        intArrayOf(63, 112, 165),
        intArrayOf(137, 94, 48),
        intArrayOf(45, 43, 39)
    )

    @Synchronized
    fun generate(
        player: FootballPlayer,
        size: Int = 256
    ): TextureSet {

        require(size in 64..1024)

        val appearance = player.appearance

        val key = buildString {
            append(appearance.skinTone)
            append("_")
            append(appearance.hairColor)
            append("_")
            append(appearance.eyeColor)
            append("_")
            append(size)
        }

        cache[key]?.let { return it }

        val textures = TextureSet(
            skin = createSkin(
                appearance.skinTone,
                size
            ),
            hair = createHair(
                appearance.hairColor,
                size
            ),
            iris = createIris(
                appearance.eyeColor,
                size
            )
        )

        cache[key] = textures

        return textures
    }

    fun createSkin(
        tone: Int,
        size: Int = 256
    ): Bitmap {

        require(size in 64..1024)

        val base = skinPalette[
            Math.floorMod(tone, skinPalette.size)
        ]

        val pixels = IntArray(size * size)

        for (y in 0 until size) {
            for (x in 0 until size) {

                val noise = noise(x, y)
                val pores = porePattern(x, y)
                val broad = sin(
                    x * 0.035 + y * 0.021
                ) * 1.8

                val variation =
                    noise * 5.0 +
                    pores * 7.0 +
                    broad

                val red = (base[0] + variation)
                    .toInt().coerceIn(0, 255)

                val green = (
                    base[1] + variation * 0.82
                ).toInt().coerceIn(0, 255)

                val blue = (
                    base[2] + variation * 0.68
                ).toInt().coerceIn(0, 255)

                pixels[y * size + x] =
                    Color.rgb(red, green, blue)
            }
        }

        return createBitmap(size, pixels)
    }

    fun createHair(
        color: Int,
        size: Int = 256
    ): Bitmap {

        require(size in 64..1024)

        val base = hairPalette[
            Math.floorMod(color, hairPalette.size)
        ]

        val pixels = IntArray(size * size)

        for (y in 0 until size) {
            for (x in 0 until size) {

                val fiber = sin(
                    x * 0.28 +
                    y * 0.09 +
                    sin(y * 0.035) * 2.0
                )

                val variation =
                    fiber * 13.0 + noise(x, y) * 5.0

                val red = (
                    base[0] + variation
                ).toInt().coerceIn(0, 255)

                val green = (
                    base[1] + variation * 0.72
                ).toInt().coerceIn(0, 255)

                val blue = (
                    base[2] + variation * 0.48
                ).toInt().coerceIn(0, 255)

                pixels[y * size + x] =
                    Color.rgb(red, green, blue)
            }
        }

        return createBitmap(size, pixels)
    }

    fun createIris(
        color: Int,
        size: Int = 256
    ): Bitmap {

        require(size in 64..1024)

        val base = irisPalette[
            Math.floorMod(color, irisPalette.size)
        ]

        val pixels = IntArray(size * size)

        val center = (size - 1) / 2f
        val radius = size * 0.48f

        for (y in 0 until size) {
            for (x in 0 until size) {

                val dx = (x - center) / radius
                val dy = (y - center) / radius

                val distance = sqrt(
                    dx * dx + dy * dy
                )

                val angle = kotlin.math.atan2(
                    dy.toDouble(),
                    dx.toDouble()
                )

                val fibers = sin(
                    angle * 38.0 +
                    distance * 75.0
                ) * 13.0

                val rings = cos(
                    distance * 45.0
                ) * 9.0

                val variation = fibers + rings

                val pupilFactor = when {
                    distance < 0.23f -> 0.12
                    distance > 0.91f -> 0.38
                    else -> 1.0
                }

                val brightness =
                    pupilFactor * (1.0 + variation / 100.0)

                val red = (
                    base[0] * brightness
                ).toInt().coerceIn(0, 255)

                val green = (
                    base[1] * brightness
                ).toInt().coerceIn(0, 255)

                val blue = (
                    base[2] * brightness
                ).toInt().coerceIn(0, 255)

                pixels[y * size + x] =
                    Color.rgb(red, green, blue)
            }
        }

        return createBitmap(size, pixels)
    }

    private fun createBitmap(
        size: Int,
        pixels: IntArray
    ): Bitmap {

        return Bitmap.createBitmap(
            pixels,
            size,
            size,
            Bitmap.Config.ARGB_8888
        )
    }

    private fun noise(x: Int, y: Int): Double {

        val value = sin(
            x * 127.1 +
            y * 311.7 +
            sin(x * 12.9898 + y * 78.233) * 17.0
        ) * 43758.5453

        return (value - kotlin.math.floor(value)) * 2.0 - 1.0
    }

    private fun porePattern(x: Int, y: Int): Double {

        val value = sin(
            x * 1.73 + y * 2.17
        ) * cos(
            x * 2.31 - y * 1.91
        )

        return if (value > 0.94) -1.0 else 0.0
    }

    @Synchronized
    fun clearCache() {
        cache.values.forEach {
            it.skin.recycle()
            it.hair.recycle()
            it.iris.recycle()
        }

        cache.clear()
    }
}
