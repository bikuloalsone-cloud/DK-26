
package com.dk26.football

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * DK 26 - Gestion des textures des joueurs avec OpenGL ES 2.
 *
 * Toutes les opérations OpenGL doivent être exécutées
 * sur le thread de rendu OpenGL.
 */
class PlayerTextureRenderer {

    data class TextureHandles(
        val skin: Int,
        val hair: Int,
        val iris: Int
    )

    data class UvMesh(
        val vertices: FloatArray,
        val indices: ShortArray,
        val vertexBuffer: FloatBuffer,
        val indexBuffer: ShortBuffer
    )

    private val loadedTextures =
        mutableListOf<Int>()

    /**
     * Transfère les trois textures du joueur vers le GPU.
     */
    fun uploadTextures(
        textures: PlayerFaceTextures.TextureSet
    ): TextureHandles {

        val skin = uploadBitmap(textures.skin)
        val hair = uploadBitmap(textures.hair)
        val iris = uploadBitmap(textures.iris)

        return TextureHandles(
            skin = skin,
            hair = hair,
            iris = iris
        )
    }

    /**
     * Crée une texture OpenGL à partir d'un Bitmap.
     */
    private fun uploadBitmap(bitmap: Bitmap): Int {

        val textureIds = IntArray(1)

        GLES20.glGenTextures(
            1,
            textureIds,
            0
        )

        val textureId = textureIds[0]

        check(textureId != 0) {
            "Impossible de créer la texture OpenGL"
        }

        GLES20.glBindTexture(
            GLES20.GL_TEXTURE_2D,
            textureId
        )

        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR_MIPMAP_LINEAR
        )

        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR
        )

        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )

        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )

        GLUtils.texImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            bitmap,
            0
        )

        GLES20.glGenerateMipmap(
            GLES20.GL_TEXTURE_2D
        )

        GLES20.glBindTexture(
            GLES20.GL_TEXTURE_2D,
            0
        )

        loadedTextures.add(textureId)

        return textureId
    }

    /**
     * Active une texture pour le rendu.
     */
    fun bindTexture(
        textureId: Int,
        textureUnit: Int = 0
    ) {

        require(textureUnit in 0..7)

        GLES20.glActiveTexture(
            GLES20.GL_TEXTURE0 + textureUnit
        )

        GLES20.glBindTexture(
            GLES20.GL_TEXTURE_2D,
            textureId
        )
    }

    /**
     * Prépare une sphère avec coordonnées UV.
     *
     * Chaque sommet contient :
     * X, Y, Z, U, V.
     */
    fun createUvSphere(
        radius: Float = 1f,
        rings: Int = 16,
        segments: Int = 32
    ): UvMesh {

        require(radius > 0f)
        require(rings >= 3)
        require(segments >= 3)

        val vertices = ArrayList<Float>()
        val indices = ArrayList<Short>()

        for (ring in 0..rings) {

            val v = ring.toFloat() / rings
            val latitude = Math.PI * v

            val sinLatitude = kotlin.math.sin(latitude)
            val cosLatitude = kotlin.math.cos(latitude)

            for (segment in 0..segments) {

                val u = segment.toFloat() / segments
                val longitude = 2.0 * Math.PI * u

                val x = (
                    radius *
                    sinLatitude *
                    kotlin.math.cos(longitude)
                ).toFloat()

                val y = (
                    radius * cosLatitude
                ).toFloat()

                val z = (
                    radius *
                    sinLatitude *
                    kotlin.math.sin(longitude)
                ).toFloat()

                vertices.add(x)
                vertices.add(y)
                vertices.add(z)
                vertices.add(u)
                vertices.add(v)
            }
        }

        val rowSize = segments + 1

        for (ring in 0 until rings) {
            for (segment in 0 until segments) {

                val first =
                    ring * rowSize + segment

                val second = first + rowSize

                indices.add(first.toShort())
                indices.add(second.toShort())
                indices.add((first + 1).toShort())

                indices.add((first + 1).toShort())
                indices.add(second.toShort())
                indices.add((second + 1).toShort())
            }
        }

        val vertexArray = vertices.toFloatArray()
        val indexArray = indices.toShortArray()

        val vertexBuffer = ByteBuffer
            .allocateDirect(vertexArray.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        vertexBuffer.put(vertexArray)
        vertexBuffer.position(0)

        val indexBuffer = ByteBuffer
            .allocateDirect(indexArray.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()

        indexBuffer.put(indexArray)
        indexBuffer.position(0)

        return UvMesh(
            vertices = vertexArray,
            indices = indexArray,
            vertexBuffer = vertexBuffer,
            indexBuffer = indexBuffer
        )
    }

    /**
     * Libère les textures GPU.
     */
    fun release() {

        if (loadedTextures.isNotEmpty()) {

            val ids = loadedTextures.toIntArray()

            GLES20.glDeleteTextures(
                ids.size,
                ids,
                0
            )

            loadedTextures.clear()
        }
    }
}
