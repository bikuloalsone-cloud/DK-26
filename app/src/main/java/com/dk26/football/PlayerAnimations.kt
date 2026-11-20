
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
    private var actionTime = 0f
    private var currentAction = PlayerAction.IDLE

    private val twoPi = (2.0 * PI).toFloat()

    fun setAction(action: PlayerAction) {
        if (action != currentAction) {
            currentAction = action
            actionTime = 0f

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

        actionTime += delta

        val frequency = when (currentAction) {
            PlayerAction.IDLE -> 1.2f
            PlayerAction.WALK -> 5.2f
            PlayerAction.RUN -> 8.2f
            PlayerAction.SPRINT -> 10.8f
            PlayerAction.JUMP -> 4f
            PlayerAction.TURN -> 6f
            PlayerAction.DRIBBLE -> 8.8f
            PlayerAction.SHOOT -> 5f
            PlayerAction.DEFEND -> 3.8f
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

        var leftLeg = 0f
        var rightLeg = 0f
        var leftArm = 0f
        var rightArm = 0f
        var torsoLean = 0f
        var torsoTurn = 0f
        var jumpHeight = 0f
        var defensiveLean = 0f

        when (currentAction) {

            PlayerAction.IDLE -> {
                leftLeg = wave * 0.008f
                rightLeg = oppositeWave * 0.008f
                leftArm = wave * 0.018f
                rightArm = oppositeWave * 0.018f
                torsoLean = doubleWave * 0.006f
            }

            PlayerAction.WALK -> {
                leftLeg = wave * 0.38f
                rightLeg = oppositeWave * 0.38f
                leftArm = oppositeWave * 0.24f
                rightArm = wave * 0.24f
                torsoLean = 0.035f
                torsoTurn = wave * 0.025f
                jumpHeight = abs(wave) * 0.012f
            }

            PlayerAction.RUN -> {
                leftLeg = wave * 0.65f
                rightLeg = oppositeWave * 0.65f
                leftArm = oppositeWave * 0.43f
                rightArm = wave * 0.43f
                torsoLean = 0.13f
                torsoTurn = wave * 0.045f
                jumpHeight = abs(wave) * 0.025f
            }

            PlayerAction.SPRINT -> {
                leftLeg = wave * 0.90f
                rightLeg = oppositeWave * 0.90f
                leftArm = oppositeWave * 0.65f
                rightArm = wave * 0.65f
                torsoLean = 0.22f
                torsoTurn = wave * 0.065f
                jumpHeight = abs(wave) * 0.04f
            }

            PlayerAction.JUMP -> {
                // Saut ponctuel : impulsion, montée et retombée.
                val duration = 0.9f
                val progress = (actionTime / duration).coerceIn(0f, 1f)

                val jumpCurve = if (progress < 1f) {
                    sin(progress * PI).toFloat()
                } else {
                    0f
                }

                val preparation = (1f - progress).coerceIn(0f, 1f)

                leftLeg = -0.15f - jumpCurve * 0.28f
                rightLeg = -0.15f - jumpCurve * 0.28f

                leftArm = -0.45f - jumpCurve * 0.30f
                rightArm = -0.45f - jumpCurve * 0.30f

                torsoLean = -0.06f - preparation * 0.04f
                jumpHeight = jumpCurve * 0.42f
            }

            PlayerAction.TURN -> {
                leftLeg = wave * 0.30f
                rightLeg = oppositeWave * 0.30f
                leftArm = oppositeWave * 0.27f
                rightArm = wave * 0.27f
                torsoLean = 0.04f
                torsoTurn = turnDirection * 0.45f
            }

            PlayerAction.DRIBBLE -> {
                leftLeg = wave * 0.58f
                rightLeg = oppositeWave * 0.58f
                leftArm = oppositeWave * 0.32f
                rightArm = wave * 0.32f
                torsoLean = 0.11f

                torsoTurn = turnDirection * 0.14f +
                    doubleWave * 0.04f

                jumpHeight = abs(wave) * 0.018f
            }

            PlayerAction.SHOOT -> {
                // Une seule séquence : préparation, frappe, finition.
                val duration = 0.85f
                val progress = (actionTime / duration).coerceIn(0f, 1f)

                val preparation = when {
                    progress < 0.35f ->
                        progress / 0.35f

                    else -> 1f
                }

                val strike = when {
                    progress < 0.35f -> 0f
                    progress < 0.58f ->
                        (progress - 0.35f) / 0.23f
                    progress < 1f ->
                        1f - (progress - 0.58f) / 0.42f
                    else -> 0f
                }.coerceIn(0f, 1f)

                leftLeg = 0.12f + preparation * 0.18f
                rightLeg = -strike * 1.15f

                leftArm = -0.25f - strike * 0.30f
                rightArm = 0.25f + strike * 0.35f

                torsoLean = -0.10f - strike * 0.12f
                torsoTurn = turnDirection * 0.16f
                jumpHeight = strike * 0.025f
            }

            PlayerAction.DEFEND -> {
                leftLeg = wave * 0.13f
                rightLeg = oppositeWave * 0.13f

                leftArm = -0.12f + wave * 0.08f
                rightArm = -0.12f - wave * 0.08f

                torsoLean = 0.16f
                torsoTurn = turnDirection * 0.12f
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
                rotationZ = doubleWave * 0.02f,
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
        actionTime = 0f
        currentAction = PlayerAction.IDLE
    }

    fun getCurrentAction(): PlayerAction = currentAction
}
