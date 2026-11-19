
package com.dk26.football

import kotlin.math.PI
import kotlin.math.abs
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

    private val twoPi = (2.0 * PI).toFloat()

    fun setAction(action: PlayerAction) {
        if (action != currentAction) {
            currentAction = action

            if (
                action == PlayerAction.JUMP ||
                action == PlayerAction.SHOOT
            ) {
                phase = 0f
            }
        }
    }

    fun update(
        deltaSeconds: Float,
        movementSpeed: Float = 1f,
        direction: Float = 0f
    ): PlayerPose {

        val delta = deltaSeconds.coerceIn(0f, 0.1f)
        val speed = movementSpeed.coerceIn(0f, 1.5f)
        val turnDirection = direction.coerceIn(-1f, 1f)

        val frequency = when (currentAction) {
            PlayerAction.IDLE -> 1.4f
            PlayerAction.WALK -> 5.0f
            PlayerAction.RUN -> 8.0f
            PlayerAction.SPRINT -> 11.0f
            PlayerAction.JUMP -> 4.0f
            PlayerAction.TURN -> 6.0f
            PlayerAction.DRIBBLE -> 9.0f
            PlayerAction.SHOOT -> 5.0f
            PlayerAction.DEFEND -> 4.0f
        }

        val activity = when (currentAction) {
            PlayerAction.IDLE -> 0.15f
            PlayerAction.DEFEND -> 0.45f
            else -> 0.5f + speed
        }

        phase += delta * frequency * activity

        if (phase >= twoPi) {
            phase %= twoPi
        }

        val wave = sin(phase.toDouble()).toFloat()
        val oppositeWave = -wave
        val doubleWave = sin((phase * 2f).toDouble()).toFloat()

        val leftLeg: Float
        val rightLeg: Float
        val leftArm: Float
        val rightArm: Float
        val torsoLean: Float
        val torsoTurn: Float
        val jumpHeight: Float
        val defensiveLean: Float

        when (currentAction) {

            PlayerAction.IDLE -> {
                leftLeg = wave * 0.015f
                rightLeg = oppositeWave * 0.015f
                leftArm = wave * 0.025f
                rightArm = oppositeWave * 0.025f
                torsoLean = doubleWave * 0.008f
                torsoTurn = 0f
                jumpHeight = 0f
                defensiveLean = 0f
            }

            PlayerAction.WALK -> {
                leftLeg = wave * 0.35f
                rightLeg = oppositeWave * 0.35f
                leftArm = oppositeWave * 0.22f
                rightArm = wave * 0.22f
                torsoLean = 0.025f
                torsoTurn = wave * 0.025f
                jumpHeight = abs(wave) * 0.012f
                defensiveLean = 0f
            }

            PlayerAction.RUN -> {
                leftLeg = wave * 0.62f
                rightLeg = oppositeWave * 0.62f
                leftArm = oppositeWave * 0.42f
                rightArm = wave * 0.42f
                torsoLean = 0.12f
                torsoTurn = wave * 0.045f
                jumpHeight = abs(wave) * 0.025f
                defensiveLean = 0f
            }

            PlayerAction.SPRINT -> {
                leftLeg = wave * 0.88f
                rightLeg = oppositeWave * 0.88f
                leftArm = oppositeWave * 0.62f
                rightArm = wave * 0.62f
                torsoLean = 0.22f
                torsoTurn = wave * 0.065f
                jumpHeight = abs(wave) * 0.04f
                defensiveLean = 0f
            }

            PlayerAction.JUMP -> {
                val jumpWave = sin(phase.toDouble()).toFloat()
                val height = (jumpWave + 1f) * 0.5f

                leftLeg = -0.18f - height * 0.32f
                rightLeg = -0.18f - height * 0.32f
                leftArm = -0.55f - height * 0.25f
                rightArm = -0.55f - height * 0.25f
                torsoLean = -0.08f
                torsoTurn = 0f
                jumpHeight = height * 0.42f
                defensiveLean = 0f
            }

            PlayerAction.TURN -> {
                leftLeg = wave * 0.30f
                rightLeg = oppositeWave * 0.30f
                leftArm = oppositeWave * 0.28f
                rightArm = wave * 0.28f
                torsoLean = 0.035f
                torsoTurn = turnDirection * 0.45f
                jumpHeight = 0f
                defensiveLean = 0f
            }

            PlayerAction.DRIBBLE -> {
                leftLeg = wave * 0.58f
                rightLeg = oppositeWave * 0.58f
                leftArm = oppositeWave * 0.32f
                rightArm = wave * 0.32f
                torsoLean = 0.10f
                torsoTurn = turnDirection * 0.12f +
                    doubleWave * 0.035f
                jumpHeight = abs(wave) * 0.018f
                defensiveLean = 0f
            }

            PlayerAction.SHOOT -> {
                val strike = sin(phase.toDouble()).toFloat()
                val preparation = (1f - strike) * 0.5f
                val followThrough = maxOf(0f, strike)

                leftLeg = 0.12f + preparation * 0.18f
                rightLeg = -strike * 1.05f
                leftArm = -0.25f - followThrough * 0.30f
                rightArm = 0.25f + followThrough * 0.35f
                torsoLean = -0.12f - followThrough * 0.12f
                torsoTurn = turnDirection * 0.16f
                jumpHeight = followThrough * 0.025f
                defensiveLean = 0f
            }

            PlayerAction.DEFEND -> {
                leftLeg = wave * 0.13f
                rightLeg = oppositeWave * 0.13f
                leftArm = -0.12f + wave * 0.08f
                rightArm = -0.12f - wave * 0.08f
                torsoLean = 0.16f
                torsoTurn = turnDirection * 0.12f
                jumpHeight = 0f
                defensiveLean = 0.30f
            }
        }

        val breathing = if (currentAction == PlayerAction.IDLE) {
            sin((phase * 0.7f).toDouble()).toFloat() * 0.012f
        } else {
            0f
        }

        val headTurn = when (currentAction) {
            PlayerAction.TURN -> turnDirection * 0.22f
            PlayerAction.DRIBBLE -> turnDirection * 0.10f
            PlayerAction.DEFEND -> turnDirection * 0.08f
            else -> 0f
        }

        return PlayerPose(
            torso = JointTransform(
                rotationX = torsoLean,
                rotationY = torsoTurn,
                rotationZ = doubleWave * 0.025f,
                offsetY = jumpHeight + breathing
            ),

            head = JointTransform(
                rotationX = -defensiveLean * 0.25f,
                rotationY = headTurn,
                rotationZ = -turnDirection * 0.04f
            ),

            leftArm = JointTransform(
                rotationX = leftArm,
                rotationY = -torsoTurn * 0.25f,
                rotationZ = 0.12f
            ),

            rightArm = JointTransform(
                rotationX = rightArm,
                rotationY = torsoTurn * 0.25f,
                rotationZ = -0.12f
            ),

            leftLeg = JointTransform(
                rotationX = leftLeg,
                rotationY = -torsoTurn * 0.15f,
                offsetY = jumpHeight * 0.5f
            ),

            rightLeg = JointTransform(
                rotationX = rightLeg,
                rotationY = torsoTurn * 0.15f,
                offsetY = jumpHeight * 0.5f
            )
        )
    }

    fun reset() {
        phase = 0f
        currentAction = PlayerAction.IDLE
    }

    fun getCurrentAction(): PlayerAction = currentAction
}
