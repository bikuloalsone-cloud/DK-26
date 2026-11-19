
package com.dk26.football

enum class SkillCategory {
    DRIBBLING,
    BALL_CONTROL,
    FINISHING,
    SHOT_POWER,
    PASSING,
    SPEED,
    AGILITY,
    DEFENDING,
    INTERCEPTIONS,
    HEADING,
    PHYSICAL,
    GOALKEEPING
}

enum class SkillAction {
    FINESSE_SHOT,
    POWER_SHOT,
    LONG_PASS,
    THROUGH_PASS,
    QUICK_CONTROL,
    TECHNICAL_DRIBBLE,
    SPEED_BURST,
    AERIAL_DUEL,
    INTERCEPTION,
    POWER_TACKLE,
    LONG_THROW,
    REFLEX_SAVE,
    GOALKEEPER_RUSH
}

data class PlayerSkillProfile(
    val playerId: String,
    val dribbling: Int,
    val ballControl: Int,
    val finishing: Int,
    val shotPower: Int,
    val passing: Int,
    val longPassing: Int,
    val speed: Int,
    val agility: Int,
    val defending: Int,
    val interceptions: Int,
    val heading: Int,
    val physical: Int,
    val goalkeeper: Int,
    val skillMoves: Int,
    val weakFoot: Int,
    val playStyles: Set<PlayStyle>
)

data class SkillEffect(
    val action: SkillAction,
    val powerMultiplier: Float,
    val accuracyMultiplier: Float,
    val speedMultiplier: Float,
    val controlMultiplier: Float
)

object PlayerSkills {

    fun createProfile(player: FootballPlayer): PlayerSkillProfile {
        val a = player.attributes
        val gk = player.goalkeeper

        val goalkeeperRating = if (gk != null) {
            (
                gk.diving +
                gk.handling +
                gk.kicking +
                gk.reflexes +
                gk.positioning
            ) / 5
        } else {
            1
        }

        return PlayerSkillProfile(
            playerId = player.id,
            dribbling = a.dribbling,
            ballControl = a.ballControl,
            finishing = a.finishing,
            shotPower = a.shotPower,
            passing = a.shortPassing,
            longPassing = a.longPassing,
            speed = (a.acceleration + a.sprintSpeed) / 2,
            agility = a.agility,
            defending = (
                a.defensiveAwareness +
                a.standingTackle +
                a.slidingTackle
            ) / 3,
            interceptions = a.interceptions,
            heading = a.heading,
            physical = (
                a.strength +
                a.stamina +
                a.balance
            ) / 3,
            goalkeeper = goalkeeperRating,
            skillMoves = player.skillMoves,
            weakFoot = player.weakFoot,
            playStyles = player.playStyles.toSet()
        )
    }

    fun getEffect(
        profile: PlayerSkillProfile,
        action: SkillAction
    ): SkillEffect {

        val style = playStyleFor(action)

        val hasPlayStyle = style != null &&
            style in profile.playStyles

        val powerBonus = if (hasPlayStyle) 1.12f else 1f
        val accuracyBonus = if (hasPlayStyle) 1.10f else 1f
        val speedBonus = if (hasPlayStyle) 1.08f else 1f
        val controlBonus = if (hasPlayStyle) 1.12f else 1f

        return when (action) {

            SkillAction.FINESSE_SHOT -> SkillEffect(
                action,
                0.90f * powerBonus,
                multiplier(profile.finishing, accuracyBonus),
                1f,
                1f
            )

            SkillAction.POWER_SHOT -> SkillEffect(
                action,
                multiplier(profile.shotPower, powerBonus),
                multiplier(profile.finishing, accuracyBonus),
                0.85f,
                1f
            )

            SkillAction.LONG_PASS -> SkillEffect(
                action,
                multiplier(profile.longPassing, powerBonus),
                multiplier(profile.longPassing, accuracyBonus),
                1f,
                1f
            )

            SkillAction.THROUGH_PASS -> SkillEffect(
                action,
                1f,
                multiplier(profile.passing, accuracyBonus),
                1f,
                multiplier(profile.passing, controlBonus)
            )

            SkillAction.QUICK_CONTROL -> SkillEffect(
                action,
                1f,
                1f,
                1f,
                multiplier(profile.ballControl, controlBonus)
            )

            SkillAction.TECHNICAL_DRIBBLE -> SkillEffect(
                action,
                1f,
                1f,
                multiplier(profile.agility, speedBonus),
                multiplier(profile.dribbling, controlBonus)
            )

            SkillAction.SPEED_BURST -> SkillEffect(
                action,
                1f,
                1f,
                multiplier(profile.speed, speedBonus),
                multiplier(profile.ballControl, controlBonus)
            )

            SkillAction.AERIAL_DUEL -> SkillEffect(
                action,
                multiplier(profile.heading, powerBonus),
                multiplier(profile.heading, accuracyBonus),
                1f,
                multiplier(profile.physical, controlBonus)
            )

            SkillAction.INTERCEPTION -> SkillEffect(
                action,
                multiplier(profile.interceptions, powerBonus),
                multiplier(profile.defending, accuracyBonus),
                1f,
                multiplier(profile.defending, controlBonus)
            )

            SkillAction.POWER_TACKLE -> SkillEffect(
                action,
                multiplier(profile.physical, powerBonus),
                multiplier(profile.defending, accuracyBonus),
                multiplier(profile.speed, speedBonus),
                1f
            )

            SkillAction.LONG_THROW -> SkillEffect(
                action,
                multiplier(profile.physical, powerBonus),
                multiplier(profile.passing, accuracyBonus),
                1f,
                1f
            )

            SkillAction.REFLEX_SAVE -> SkillEffect(
                action,
                multiplier(profile.goalkeeper, powerBonus),
                multiplier(profile.goalkeeper, accuracyBonus),
                multiplier(profile.goalkeeper, speedBonus),
                multiplier(profile.goalkeeper, controlBonus)
            )

            SkillAction.GOALKEEPER_RUSH -> SkillEffect(
                action,
                1f,
                multiplier(profile.goalkeeper, accuracyBonus),
                multiplier(profile.speed, speedBonus),
                multiplier(profile.goalkeeper, controlBonus)
            )
        }
    }

    fun getWeakFootAccuracy(profile: PlayerSkillProfile): Float {
        return when (profile.weakFoot.coerceIn(1, 5)) {
            1 -> 0.55f
            2 -> 0.65f
            3 -> 0.75f
            4 -> 0.88f
            else -> 1f
        }
    }

    fun getSkillMoveLevel(profile: PlayerSkillProfile): Int {
        return profile.skillMoves.coerceIn(1, 5)
    }

    fun canPerformSkillMove(
        profile: PlayerSkillProfile,
        requiredLevel: Int
    ): Boolean {
        return profile.skillMoves >= requiredLevel &&
            requiredLevel in 1..5
    }

    private fun playStyleFor(action: SkillAction): PlayStyle? {
        return when (action) {
            SkillAction.FINESSE_SHOT -> PlayStyle.FINESSE_SHOT
            SkillAction.POWER_SHOT -> PlayStyle.POWER_SHOT
            SkillAction.LONG_PASS -> PlayStyle.LONG_PASS
            SkillAction.THROUGH_PASS -> PlayStyle.THROUGH_PASS
            SkillAction.QUICK_CONTROL -> PlayStyle.QUICK_CONTROL
            SkillAction.TECHNICAL_DRIBBLE -> PlayStyle.TECHNICAL_DRIBBLING
            SkillAction.SPEED_BURST -> PlayStyle.SPEED_BURST
            SkillAction.AERIAL_DUEL -> PlayStyle.AERIAL_THREAT
            SkillAction.INTERCEPTION -> PlayStyle.INTERCEPTOR
            SkillAction.POWER_TACKLE -> PlayStyle.POWER_TACKLE
            SkillAction.LONG_THROW -> PlayStyle.LONG_THROW
            SkillAction.REFLEX_SAVE -> PlayStyle.REFLEX_SAVES
            SkillAction.GOALKEEPER_RUSH -> PlayStyle.RUSH_OUT
        }
    }

    private fun multiplier(
        rating: Int,
        bonus: Float
    ): Float {
        return (0.70f + rating.coerceIn(1, 99) / 100f) * bonus
    }
}
