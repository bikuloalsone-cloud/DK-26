
package com.dk26.football

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

    fun applyPose(pose: PlayerPose): List<AnimatedBone> {
        return bones.map { definition ->

            val transform = when (definition.bone) {
                PlayerBone.ROOT,
                PlayerBone.PELVIS,
                PlayerBone.SPINE,
                PlayerBone.CHEST -> pose.torso

                PlayerBone.NECK,
                PlayerBone.HEAD -> pose.head

                PlayerBone.LEFT_SHOULDER,
                PlayerBone.LEFT_UPPER_ARM,
                PlayerBone.LEFT_FOREARM,
                PlayerBone.LEFT_HAND -> pose.leftArm

                PlayerBone.RIGHT_SHOULDER,
                PlayerBone.RIGHT_UPPER_ARM,
                PlayerBone.RIGHT_FOREARM,
                PlayerBone.RIGHT_HAND -> pose.rightArm

                PlayerBone.LEFT_THIGH,
                PlayerBone.LEFT_CALF,
                PlayerBone.LEFT_FOOT -> pose.leftLeg

                PlayerBone.RIGHT_THIGH,
                PlayerBone.RIGHT_CALF,
                PlayerBone.RIGHT_FOOT -> pose.rightLeg
            }

            val rotation = BoneRotation(
                x = transform.rotationX,
                y = transform.rotationY,
                z = transform.rotationZ
            )

            val position = BonePosition(
                x = definition.bindPosition.x + transform.offsetX,
                y = definition.bindPosition.y + transform.offsetY,
                z = definition.bindPosition.z + transform.offsetZ
            )

            AnimatedBone(
                bone = definition.bone,
                parent = definition.parent,
                position = position,
                rotation = rotation
            )
        }
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
