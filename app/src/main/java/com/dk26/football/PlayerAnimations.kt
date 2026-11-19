
package com.dk26.football

import kotlin.math.PI
import kotlin.math.sin

enum class PlayerAction {
    IDLE,
    WALK,
    RUN,
    SPRINT,
    JUMP,
    TURN,
    DRIBBLE,
    SHOOT,
    DEFEND
}

data class JointTransform(
    val rotationX: Float = 0f,
    val rotationY: Float = 0f,
    val rotationZ: Float = 0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val offsetZ: Float = 0f
)

data class PlayerPose(
    val torso: JointTransform,
    val head: JointTransform,
    val leftArm: JointTransform,
    val rightArm: JointTransform,
    val leftLeg: JointTransform,
    val rightLeg: JointTransform
)

class PlayerAnimationController {

    private var phase = 0f
    private var currentAction = PlayerAction.IDLE

    fun setAction(action: PlayerAction) {
        currentAction = action
    }

    fun update(
        deltaSeconds: Float,
        movementSpeed: Float = 1f,
        direction: Float = 0f
    ): PlayerPose {

        val delta = deltaSeconds.coerceIn(0f, 0.1f)
        val speed = movementSpeed.coerceIn(0f, 1.5f)

        val frequency = when (currentAction) {
            PlayerAction.IDLE -> 1.5f
            PlayerAction.WALK -> 5f
            PlayerAction.RUN -> 8f
            PlayerAction.SPRINT -> 11f
            PlayerAction.JUMP -> 5f
            PlayerAction.TURN -> 6f
            PlayerAction.DRIBBLE -> 9f
            PlayerAction.SHOOT -> 7f
            PlayerAction.DEFEND -> 4f
        }

        phase += delta * frequency * (0.5f + speed)

        if (phase > (2f * PI).toFloat()) {
            phase %= (2f * PI).toFloat()
        }

        val wave = sin(phase.toDouble()).toFloat()
        val oppositeWave = sin(phase.toDouble() + PI).toFloat()

        val legMovement = when (currentAction) {
            PlayerAction.IDLE -> 0.015f
            PlayerAction.WALK -> 0.35f
            PlayerAction.RUN -> 0.65f
            PlayerAction.SPRINT -> 0.9f
            PlayerAction.JUMP -> 0.1f
            PlayerAction.TURN -> 0.35f
            PlayerAction.DRIBBLE -> 0.65f
            PlayerAction.SHOOT -> 0.15f
            PlayerAction.DEFEND -> 0.12f
        }

        val leftLegRotation = wave * legMovement
        val rightLegRotation = oppositeWave * legMovement

        val armMovement = when (currentAction) {
            PlayerAction.IDLE -> 0.03f
            PlayerAction.WALK -> 0.22f
            PlayerAction.RUN -> 0.48f
            PlayerAction.SPRINT -> 0.65f
            PlayerAction.JUMP -> 0.45f
            PlayerAction.TURN -> 0.35f
            PlayerAction.DRIBBLE -> 0.42f
            PlayerAction.SHOOT -> 0.55f
            PlayerAction.DEFEND -> 0.18f
        }

        val torsoRotation = when (currentAction) {
            PlayerAction.TURN -> direction * 0.35f
            PlayerAction.SHOOT -> -0.18f
            PlayerAction.DEFEND -> 0.12f
            else -> 0f
        }

        val jumpOffset = if (currentAction == PlayerAction.JUMP) {
            (sin(phase.toDouble()).toFloat() + 1f) * 0.22f
        } else {
            0f
        }

        val shootingLeg = if (currentAction == PlayerAction.SHOOT) {
            0.9f
        } else {
            0f
        }

        val defensivePosture =
            if (currentAction == PlayerAction.DEFEND) 0.3f else 0f

        return PlayerPose(
            torso = JointTransform(
                rotationX = torsoRotation,
                rotationY = wave * 0.04f,
                offsetY = jumpOffset
            ),

            head = JointTransform(
                rotationY = direction * 0.15f,
                rotationX = -defensivePosture * 0.2f
            ),

            leftArm = JointTransform(
                rotationX = wave * armMovement,
                rotationZ = 0.12f
            ),

            rightArm = JointTransform(
                rotationX = oppositeWave * armMovement,
                rotationZ = -0.12f
            ),

            leftLeg = JointTransform(
                rotationX = leftLegRotation,
                offsetY = jumpOffset * 0.5f
            ),

            rightLeg = JointTransform(
                rotationX = if (shootingLeg > 0f) {
                    -shootingLeg
                } else {
                    rightLegRotation
                },
                offsetY = jumpOffset * 0.5f
            )
        )
    }

    fun reset() {
        phase = 0f
        currentAction = PlayerAction.IDLE
    }

    fun getCurrentAction(): PlayerAction = currentAction
}
