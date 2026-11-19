
package com.dk26.football

import kotlin.math.cos
import kotlin.math.sin

enum class PlayerBone {
    ROOT,
    PELVIS,
    SPINE,
    CHEST,
    NECK,
    HEAD,

    LEFT_SHOULDER,
    LEFT_UPPER_ARM,
    LEFT_FOREARM,
    LEFT_HAND,

    RIGHT_SHOULDER,
    RIGHT_UPPER_ARM,
    RIGHT_FOREARM,
    RIGHT_HAND,

    LEFT_THIGH,
    LEFT_CALF,
    LEFT_FOOT,

    RIGHT_THIGH,
    RIGHT_CALF,
    RIGHT_FOOT
}

data class BonePosition(
    val x: Float,
    val y: Float,
    val z: Float
)

data class BoneRotation(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
)

data class BoneDefinition(
    val bone: PlayerBone,
    val parent: PlayerBone?,
    val bindPosition: BonePosition
)

data class AnimatedBone(
    val bone: PlayerBone,
    val parent: PlayerBone?,
    val position: BonePosition,
    val rotation: BoneRotation
)

class PlayerSkeleton private constructor(
    val bones: List<BoneDefinition>
) {

    private data class BoneTransform(
        val position: BonePosition,
        val rotation: BoneRotation
    )

    fun applyPose(pose: PlayerPose): List<AnimatedBone> {

        val calculated = HashMap<PlayerBone, BoneTransform>(bones.size)
        val result = ArrayList<AnimatedBone>(bones.size)

        for (definition in bones) {

            val bone = definition.bone
            val parent = definition.parent

            val localRotation = when (bone) {
                PlayerBone.SPINE -> BoneRotation(
                    pose.torso.rotationX,
                    pose.torso.rotationY,
                    pose.torso.rotationZ
                )

                PlayerBone.HEAD -> BoneRotation(
                    pose.head.rotationX,
                    pose.head.rotationY,
                    pose.head.rotationZ
                )

                PlayerBone.LEFT_SHOULDER -> BoneRotation(
                    pose.leftArm.rotationX,
                    pose.leftArm.rotationY,
                    pose.leftArm.rotationZ
                )

                PlayerBone.RIGHT_SHOULDER -> BoneRotation(
                    pose.rightArm.rotationX,
                    pose.rightArm.rotationY,
                    pose.rightArm.rotationZ
                )

                PlayerBone.LEFT_THIGH -> BoneRotation(
                    pose.leftLeg.rotationX,
                    pose.leftLeg.rotationY,
                    pose.leftLeg.rotationZ
                )

                PlayerBone.RIGHT_THIGH -> BoneRotation(
                    pose.rightLeg.rotationX,
                    pose.rightLeg.rotationY,
                    pose.rightLeg.rotationZ
                )

                else -> BoneRotation()
            }

            val localOffset = when (bone) {
                PlayerBone.ROOT -> BonePosition(
                    pose.torso.offsetX,
                    pose.torso.offsetY,
                    pose.torso.offsetZ
                )

                PlayerBone.LEFT_SHOULDER -> BonePosition(
                    pose.leftArm.offsetX,
                    pose.leftArm.offsetY,
                    pose.leftArm.offsetZ
                )

                PlayerBone.RIGHT_SHOULDER -> BonePosition(
                    pose.rightArm.offsetX,
                    pose.rightArm.offsetY,
                    pose.rightArm.offsetZ
                )

                PlayerBone.LEFT_THIGH -> BonePosition(
                    pose.leftLeg.offsetX,
                    pose.leftLeg.offsetY,
                    pose.leftLeg.offsetZ
                )

                PlayerBone.RIGHT_THIGH -> BonePosition(
                    pose.rightLeg.offsetX,
                    pose.rightLeg.offsetY,
                    pose.rightLeg.offsetZ
                )

                PlayerBone.HEAD -> BonePosition(
                    pose.head.offsetX,
                    pose.head.offsetY,
                    pose.head.offsetZ
                )

                else -> BonePosition(0f, 0f, 0f)
            }

            val localPosition = BonePosition(
                definition.bindPosition.x + localOffset.x,
                definition.bindPosition.y + localOffset.y,
                definition.bindPosition.z + localOffset.z
            )

            val parentTransform = if (parent != null) {
                calculated[parent]
            } else {
                null
            }

            val worldPosition: BonePosition
            val worldRotation: BoneRotation

            if (parentTransform == null) {
                worldPosition = localPosition
                worldRotation = localRotation
            } else {
                val rotated = rotatePosition(
                    localPosition,
                    parentTransform.rotation
                )

                worldPosition = BonePosition(
                    parentTransform.position.x + rotated.x,
                    parentTransform.position.y + rotated.y,
                    parentTransform.position.z + rotated.z
                )

                worldRotation = BoneRotation(
                    parentTransform.rotation.x + localRotation.x,
                    parentTransform.rotation.y + localRotation.y,
                    parentTransform.rotation.z + localRotation.z
                )
            }

            val transform = BoneTransform(
                position = worldPosition,
                rotation = worldRotation
            )

            calculated[bone] = transform

            result.add(
                AnimatedBone(
                    bone = bone,
                    parent = parent,
                    position = worldPosition,
                    rotation = worldRotation
                )
            )
        }

        return result
    }

    private fun rotatePosition(
        position: BonePosition,
        rotation: BoneRotation
    ): BonePosition {

        val cx = cos(rotation.x)
        val sx = sin(rotation.x)

        val cy = cos(rotation.y)
        val sy = sin(rotation.y)

        val cz = cos(rotation.z)
        val sz = sin(rotation.z)

        // Rotation autour de X
        val x1 = position.x
        val y1 = position.y * cx - position.z * sx
        val z1 = position.y * sx + position.z * cx

        // Rotation autour de Y
        val x2 = x1 * cy + z1 * sy
        val y2 = y1
        val z2 = -x1 * sy + z1 * cy

        // Rotation autour de Z
        val x3 = x2 * cz - y2 * sz
        val y3 = x2 * sz + y2 * cz

        return BonePosition(
            x = x3,
            y = y3,
            z = z2
        )
    }

    companion object {

        fun create(): PlayerSkeleton {

            val bones = listOf(

                BoneDefinition(
                    PlayerBone.ROOT,
                    null,
                    BonePosition(0f, 0f, 0f)
                ),

                BoneDefinition(
                    PlayerBone.PELVIS,
                    PlayerBone.ROOT,
                    BonePosition(0f, 0.90f, 0f)
                ),

                BoneDefinition(
                    PlayerBone.SPINE,
                    PlayerBone.PELVIS,
                    BonePosition(0f, 0.22f, 0f)
                ),

                BoneDefinition(
                    PlayerBone.CHEST,
                    PlayerBone.SPINE,
                    BonePosition(0f, 0.20f, 0f)
                ),

                BoneDefinition(
                    PlayerBone.NECK,
                    PlayerBone.CHEST,
                    BonePosition(0f, 0.24f, 0f)
                ),

                BoneDefinition(
                    PlayerBone.HEAD,
                    PlayerBone.NECK,
                    BonePosition(0f, 0.16f, 0f)
                ),

                BoneDefinition(
                    PlayerBone.LEFT_SHOULDER,
                    PlayerBone.CHEST,
                    BonePosition(-0.23f, 0.16f, 0f)
                ),

                BoneDefinition(
                    PlayerBone.LEFT_UPPER_ARM,
                    PlayerBone.LEFT_SHOULDER,
                    BonePosition(-0.10f, -0.13f, 0f)
                ),

                BoneDefinition(
                    PlayerBone.LEFT_FOREARM,
                    PlayerBone.LEFT_UPPER_ARM,
                    BonePosition(0f, -0.25f, 0f)
                ),

                BoneDefinition(
                    PlayerBone.LEFT_HAND,
                    PlayerBone.LEFT_FOREARM,
                    BonePosition(0f, -0.23f, 0f)
                ),

                BoneDefinition(
                    PlayerBone.RIGHT_SHOULDER,
                    PlayerBone.CHEST,
                    BonePosition(0.23f, 0.16f, 0f)
                ),

                BoneDefinition(
                    PlayerBone.RIGHT_UPPER_ARM,
                    PlayerBone.RIGHT_SHOULDER,
                    BonePosition(0.10f, -0.13f, 0f)
                ),

                BoneDefinition(
                    PlayerBone.RIGHT_FOREARM,
                    PlayerBone.RIGHT_UPPER_ARM,
                    BonePosition(0f, -0.25f, 0f)
                ),

                BoneDefinition(
                    PlayerBone.RIGHT_HAND,
                    PlayerBone.RIGHT_FOREARM,
                    BonePosition(0f, -0.23f, 0f)
                ),

                BoneDefinition(
                    PlayerBone.LEFT_THIGH,
                    PlayerBone.PELVIS,
                    BonePosition(-0.11f, -0.08f, 0f)
                ),

                BoneDefinition(
                    PlayerBone.LEFT_CALF,
                    PlayerBone.LEFT_THIGH,
                    BonePosition(0f, -0.42f, 0f)
                ),

                BoneDefinition(
                    PlayerBone.LEFT_FOOT,
                    PlayerBone.LEFT_CALF,
                    BonePosition(0f, -0.40f, 0.06f)
                ),

                BoneDefinition(
                    PlayerBone.RIGHT_THIGH,
                    PlayerBone.PELVIS,
                    BonePosition(0.11f, -0.08f, 0f)
                ),

                BoneDefinition(
                    PlayerBone.RIGHT_CALF,
                    PlayerBone.RIGHT_THIGH,
                    BonePosition(0f, -0.42f, 0f)
                ),

                BoneDefinition(
                    PlayerBone.RIGHT_FOOT,
                    PlayerBone.RIGHT_CALF,
                    BonePosition(0f, -0.40f, 0.06f)
                )
            )

            return PlayerSkeleton(bones)
        }
    }
}
