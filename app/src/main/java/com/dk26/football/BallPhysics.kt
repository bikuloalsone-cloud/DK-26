
package com.dk26.football

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Physique originale du ballon de DK 26.
 * Coordonnées : X = largeur, Y = hauteur, Z = longueur du terrain.
 */
data class BallVector3(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
) {
    operator fun plus(other: BallVector3) =
        BallVector3(x + other.x, y + other.y, z + other.z)

    operator fun minus(other: BallVector3) =
        BallVector3(x - other.x, y - other.y, z - other.z)

    operator fun times(value: Float) =
        BallVector3(x * value, y * value, z * value)

    fun dot(other: BallVector3): Float =
        x * other.x + y * other.y + z * other.z

    fun cross(other: BallVector3) = BallVector3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    fun length(): Float =
        sqrt(x * x + y * y + z * z)

    fun normalized(): BallVector3 {
        val magnitude = length()
        if (magnitude < 0.00001f) return BallVector3()
        return this * (1f / magnitude)
    }

    fun withY(value: Float) = copy(y = value)

    companion object {
        val ZERO = BallVector3()
        val UP = BallVector3(0f, 1f, 0f)
    }
}

/**
 * Configuration complète du moteur physique.
 */
data class BallPhysicsConfig(
    val radius: Float = 0.11f,
    val mass: Float = 0.43f,
    val gravity: Float = 9.81f,
    val groundRestitution: Float = 0.48f,
    val groundFriction: Float = 0.985f,
    val airResistance: Float = 0.012f,
    val magnusCoefficient: Float = 0.0018f,
    val spinDamping: Float = 0.985f,
    val pitchHalfWidth: Float = 34f,
    val pitchHalfLength: Float = 52.5f,
    val maximumSpeed: Float = 42f
)

/**
 * Collision entre un joueur et le ballon.
 */
data class BallPlayerCollider(
    var position: BallVector3,
    val radius: Float = 0.38f,
    var velocity: BallVector3 = BallVector3.ZERO
)

/**
 * Structure des buts.
 */
data class BallGoalFrame(
    val centerX: Float = 0f,
    val goalLineZ: Float,
    val halfWidth: Float = 3.66f,
    val crossbarHeight: Float = 2.44f,
    val postRadius: Float = 0.06f
)

enum class BallEventType {
    KICKED,
    GROUND_BOUNCE,
    PLAYER_COLLISION,
    POST_COLLISION,
    CROSSBAR_COLLISION,
    GOAL,
    OUT_OF_BOUNDS
}

data class BallEvent(
    val type: BallEventType,
    val position: BallVector3,
    val intensity: Float = 0f
)

/**
 * Moteur physique indépendant du ballon.
 */
class BallPhysics(
    val config: BallPhysicsConfig = BallPhysicsConfig()
) {

    var position: BallVector3 = BallVector3(0f, config.radius, 0f)
        private set

    var velocity: BallVector3 = BallVector3.ZERO
        private set

    var spin: BallVector3 = BallVector3.ZERO
        private set

    var isMoving: Boolean = false
        private set

    var lastKickPower: Float = 0f
        private set

    private val events = ArrayList<BallEvent>(8)

    private val safeRadius = config.radius.coerceAtLeast(0.01f)

    /**
     * Replace le ballon et annule son mouvement.
     */
    fun reset(
        newPosition: BallVector3 = BallVector3(
            0f,
            safeRadius,
            0f
        )
    ) {
        position = newPosition.copy(
            y = max(safeRadius, newPosition.y)
        )
        velocity = BallVector3.ZERO
        spin = BallVector3.ZERO
        isMoving = false
        lastKickPower = 0f
        events.clear()
    }

    /**
     * Frappe générale.
     */
    fun kick(
        direction: BallVector3,
        power: Float,
        lift: Float = 0.08f,
        spinEffect: BallVector3 = BallVector3.ZERO
    ) {
        val strength = power.coerceIn(0f, 1f)
        if (strength <= 0f) return

        var aim = direction.normalized()

        if (aim.length() < 0.001f) {
            aim = BallVector3(0f, 0f, 1f)
        }

        val speed = (
            4f + strength * 38f
        ).coerceAtMost(config.maximumSpeed)

        val vertical = (
            aim.y + lift.coerceIn(-0.5f, 1.5f)
        ).coerceIn(-0.3f, 1f)

        val horizontal = BallVector3(
            aim.x,
            0f,
            aim.z
        ).normalized()

        val finalDirection = BallVector3(
            horizontal.x,
            vertical,
            horizontal.z
        ).normalized()

        velocity = (finalDirection * speed).let {
            if (it.length() > config.maximumSpeed) {
                it.normalized() * config.maximumSpeed
            } else {
                it
            }
        }

        spin = spinEffect
        lastKickPower = strength
        isMoving = true

        events.add(
            BallEvent(
                BallEventType.KICKED,
                position,
                strength
            )
        )
    }

    /**
     * Passe courte ou moyenne.
     */
    fun pass(
        direction: BallVector3,
        power: Float = 0.35f
    ) {
        kick(
            direction = direction,
            power = power.coerceIn(0.05f, 0.65f),
            lift = 0.015f
        )
    }

    /**
     * Passe en profondeur.
     */
    fun throughPass(
        direction: BallVector3,
        power: Float = 0.55f
    ) {
        kick(
            direction = direction,
            power = power.coerceIn(0.15f, 0.8f),
            lift = 0.12f
        )
    }

    /**
     * Tir vers le but.
     */
    fun shoot(
        direction: BallVector3,
        power: Float = 0.85f,
        lift: Float = 0.18f,
        spinEffect: BallVector3 = BallVector3.ZERO
    ) {
        kick(
            direction = direction,
            power = power.coerceIn(0.2f, 1f),
            lift = lift,
            spinEffect = spinEffect
        )
    }

    /**
     * Tir avec effet latéral.
     */
    fun curveShoot(
        direction: BallVector3,
        power: Float = 0.8f,
        curve: Float = 8f,
        lift: Float = 0.2f
    ) {
        val aim = direction.normalized()

        val lateralSpin = BallVector3(
            0f,
            curve.coerceIn(-15f, 15f),
            0f
        )

        kick(
            direction = aim,
            power = power,
            lift = lift,
            spinEffect = lateralSpin
        )
    }

    /**
     * Mise à jour physique.
     */
    fun update(
        deltaSeconds: Float,
        players: List<BallPlayerCollider> = emptyList(),
        goals: List<BallGoalFrame> = emptyList()
    ): List<BallEvent> {

        events.clear()

        val frameTime = deltaSeconds.coerceIn(0f, 0.1f)

        if (frameTime <= 0f) {
            return emptyList()
        }

        val maxStep = 1f / 120f
        val steps = max(
            1,
            kotlin.math.ceil(frameTime / maxStep).toInt()
        )

        val stepTime = frameTime / steps

        repeat(steps) {
            val previousPosition = position

            integrate(stepTime)

            collideWithGround()
            collideWithPlayers(players)
            collideWithGoals(goals)

            detectGoals(
                previousPosition,
                position,
                goals
            )

            enforcePitchBounds()
        }

        isMoving = velocity.length() > 0.025f

        if (!isMoving) {
            velocity = BallVector3.ZERO
            spin = BallVector3.ZERO
        }

        return events.toList()
    }

    /**
     * Gravité, résistance de l'air et effet Magnus.
     */
    private fun integrate(delta: Float) {
        val speed = velocity.length()

        val gravityAcceleration = BallVector3(
            0f,
            -config.gravity,
            0f
        )

        val magnusAcceleration = spin
            .cross(velocity) * config.magnusCoefficient

        val acceleration = gravityAcceleration + magnusAcceleration

        velocity += acceleration * delta

        val airFactor = (
            1f - config.airResistance * delta * speed
        ).coerceIn(0.9f, 1f)

        velocity *= airFactor

        val currentSpeed = velocity.length()

        if (currentSpeed > config.maximumSpeed) {
            velocity = velocity.normalized() * config.maximumSpeed
        }

        position += velocity * delta

        spin *= config.spinDamping.coerceIn(0f, 1f)
    }

    /**
     * Collision avec le sol.
     */
    private fun collideWithGround() {
        if (position.y >= safeRadius) return

        val impact = abs(velocity.y)

        position = position.withY(safeRadius)

        if (velocity.y < 0f) {
            val rebound = impact * config.groundRestitution

            velocity = velocity.withY(
                if (rebound < 0.45f) 0f else rebound
            )

            velocity = BallVector3(
                velocity.x * config.groundFriction,
                velocity.y,
                velocity.z * config.groundFriction
            )

            if (impact > 0.8f) {
                events.add(
                    BallEvent(
                        BallEventType.GROUND_BOUNCE,
                        position,
                        impact
                    )
                )
            }
        }
    }

    /**
     * Collision avec les joueurs.
     */
    private fun collideWithPlayers(
        players: List<BallPlayerCollider>
    ) {
        for (player in players) {
            val difference = position - player.position

            val minimumDistance = safeRadius +
                player.radius.coerceAtLeast(0.05f)

            val distance = difference.length()

            if (distance >= minimumDistance || distance < 0.0001f) {
                continue
            }

            val normal = difference.normalized()

            position = player.position +
                normal * minimumDistance

            val relativeVelocity = velocity - player.velocity
            val normalSpeed = relativeVelocity.dot(normal)

            if (normalSpeed < 0f) {
                val restitution = 0.55f

                val impulse = normal * (
                    -(1f + restitution) * normalSpeed
                )

                velocity += impulse

                val playerSpeed = player.velocity.length()

                events.add(
                    BallEvent(
                        BallEventType.PLAYER_COLLISION,
                        position,
                        max(abs(normalSpeed), playerSpeed)
                    )
                )
            }
        }
    }

    /**
     * Collision du ballon contre un segment.
     */
    private fun collideWithSegment(
        start: BallVector3,
        end: BallVector3,
        collisionRadius: Float
    ): Boolean {

        val segment = end - start
        val lengthSquared = segment.dot(segment)

        val t = if (lengthSquared > 0.000001f) {
            ((position - start).dot(segment) / lengthSquared)
                .coerceIn(0f, 1f)
        } else {
            0f
        }

        val closest = start + segment * t
        val difference = position - closest
        val distance = difference.length()

        if (distance >= collisionRadius || distance < 0.00001f) {
            return false
        }

        val normal = difference.normalized()

        position = closest + normal * collisionRadius

        val normalSpeed = velocity.dot(normal)

        if (normalSpeed < 0f) {
            velocity -= normal * (1.65f * normalSpeed)
        }

        return true
    }

    /**
     * Collision avec les poteaux et la barre transversale.
     */
    private fun collideWithGoals(
        goals: List<BallGoalFrame>
    ) {
        for (goal in goals) {
            val leftX = goal.centerX - goal.halfWidth
            val rightX = goal.centerX + goal.halfWidth
            val z = goal.goalLineZ

            val bottom = safeRadius
            val top = goal.crossbarHeight

            val frameRadius = safeRadius +
                goal.postRadius.coerceAtLeast(0.01f)

            val leftPost = collideWithSegment(
                BallVector3(leftX, bottom, z),
                BallVector3(leftX, top, z),
                frameRadius
            )

            if (leftPost) {
                events.add(
                    BallEvent(
                        BallEventType.POST_COLLISION,
                        position,
                        velocity.length()
                    )
                )
            }

            val rightPost = collideWithSegment(
                BallVector3(rightX, bottom, z),
                BallVector3(rightX, top, z),
                frameRadius
            )

            if (rightPost) {
                events.add(
                    BallEvent(
                        BallEventType.POST_COLLISION,
                        position,
                        velocity.length()
                    )
                )
            }

            val crossbar = collideWithSegment(
                BallVector3(leftX, top, z),
                BallVector3(rightX, top, z),
                frameRadius
            )

            if (crossbar) {
                events.add(
                    BallEvent(
                        BallEventType.CROSSBAR_COLLISION,
                        position,
                        velocity.length()
                    )
                )
            }
        }
    }

    /**
     * Détecte le franchissement de la ligne de but.
     */
    private fun detectGoals(
        previous: BallVector3,
        current: BallVector3,
        goals: List<BallGoalFrame>
    ) {
        for (goal in goals) {
            val line = goal.goalLineZ

            val crossed = (
                (previous.z < line && current.z >= line) ||
                (previous.z > line && current.z <= line)
            )

            if (!crossed) continue

            val distanceZ = current.z - previous.z

            val interpolation = if (
                abs(distanceZ) > 0.00001f
            ) {
                ((line - previous.z) / distanceZ)
                    .coerceIn(0f, 1f)
            } else {
                0f
            }

            val crossingX = previous.x +
                (current.x - previous.x) * interpolation

            val crossingY = previous.y +
                (current.y - previous.y) * interpolation

            val insideWidth = abs(
                crossingX - goal.centerX
            ) < goal.halfWidth - safeRadius

            val insideHeight = crossingY <
                goal.crossbarHeight - safeRadius &&
                crossingY > safeRadius

            if (insideWidth && insideHeight) {
                events.add(
                    BallEvent(
                        BallEventType.GOAL,
                        current,
                        velocity.length()
                    )
                )

                velocity = BallVector3.ZERO
                spin = BallVector3.ZERO
                isMoving = false
                return
            }
        }
    }

    /**
     * Limites du terrain.
     */
    private fun enforcePitchBounds() {
        val limitX = config.pitchHalfWidth - safeRadius
        val limitZ = config.pitchHalfLength - safeRadius

        if (abs(position.x) > limitX) {
            position = position.copy(
                x = position.x.coerceIn(-limitX, limitX)
            )

            velocity = velocity.copy(
                x = -velocity.x * 0.35f
            )

            events.add(
                BallEvent(
                    BallEventType.OUT_OF_BOUNDS,
                    position
                )
            )
        }

        if (abs(position.z) > limitZ) {
            position = position.copy(
                z = position.z.coerceIn(-limitZ, limitZ)
            )

            velocity = velocity.copy(
                z = -velocity.z * 0.35f
            )

            events.add(
                BallEvent(
                    BallEventType.OUT_OF_BOUNDS,
                    position
                )
            )
        }
    }
}
