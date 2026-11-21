
package com.dk26.football

enum class TeamSide {
    BLUE, RED
}

data class MatchPlayer(
    val player: FootballPlayer,
    val team: TeamSide,
    var x: Float,
    var z: Float,
    val shirtColor: FloatArray,
    val model: Player3DModel
)

data class MatchTeam(
    val name: String,
    val side: TeamSide,
    val players: List<MatchPlayer>
)

object MatchTeamSetup {

    private data class FormationSlot(
        val number: Int,
        val position: PlayerPosition,
        val x: Float,
        val z: Float
    )

    private val formation433 = listOf(
        FormationSlot(1, PlayerPosition.GOALKEEPER, 0f, -47f),

        FormationSlot(2, PlayerPosition.RIGHT_BACK, 24f, -34f),
        FormationSlot(3, PlayerPosition.CENTRE_BACK, 8f, -35f),
        FormationSlot(4, PlayerPosition.CENTRE_BACK, -8f, -35f),
        FormationSlot(5, PlayerPosition.LEFT_BACK, -24f, -34f),

        FormationSlot(6, PlayerPosition.DEFENSIVE_MIDFIELDER, 0f, -22f),
        FormationSlot(8, PlayerPosition.CENTRAL_MIDFIELDER, -12f, -12f),
        FormationSlot(10, PlayerPosition.ATTACKING_MIDFIELDER, 12f, -10f),

        FormationSlot(7, PlayerPosition.RIGHT_WINGER, 20f, 12f),
        FormationSlot(9, PlayerPosition.STRIKER, 0f, 20f),
        FormationSlot(11, PlayerPosition.LEFT_WINGER, -20f, 12f)
    )

    private val blueColor =
        floatArrayOf(0.035f, 0.16f, 0.85f, 1f)

    private val redColor =
        floatArrayOf(0.85f, 0.045f, 0.035f, 1f)

    fun createMatch(): List<MatchTeam> {
        return listOf(
            createTeam(TeamSide.BLUE),
            createTeam(TeamSide.RED)
        )
    }

    private fun createTeam(side: TeamSide): MatchTeam {

        val teamName = when (side) {
            TeamSide.BLUE -> "DK26 BLUE"
            TeamSide.RED -> "DK26 RED"
        }

        val color = when (side) {
            TeamSide.BLUE -> blueColor
            TeamSide.RED -> redColor
        }

        val players = formation433.mapIndexed { index, slot ->

            val id = "${side.name.lowercase()}-${slot.number}"

            val player = PlayerCatalog.findById(id)
                ?: createFootballPlayer(
                    id = id,
                    number = slot.number,
                    position = slot.position,
                    teamName = teamName,
                    index = index
                ).also {
                    PlayerCatalog.add(it)
                }

            val direction = when (side) {
                TeamSide.BLUE -> 1f
                TeamSide.RED -> -1f
            }

            MatchPlayer(
                player = player,
                team = side,
                x = slot.x,
                z = slot.z * direction,
                shirtColor = color.copyOf(),
                model = Player3DModel.create(
                    player = player,
                    shirtColor = color
                )
            )
        }

        return MatchTeam(
            name = teamName,
            side = side,
            players = players
        )
    }

    private fun createFootballPlayer(
        id: String,
        number: Int,
        position: PlayerPosition,
        teamName: String,
        index: Int
    ): FootballPlayer {

        val profile = when (position) {
            PlayerPosition.GOALKEEPER ->
                intArrayOf(48, 85, 20, 20, 15)

            PlayerPosition.CENTRE_BACK ->
                intArrayOf(55, 35, 45, 48, 88)

            PlayerPosition.LEFT_BACK,
            PlayerPosition.RIGHT_BACK ->
                intArrayOf(78, 52, 62, 70, 76)

            PlayerPosition.DEFENSIVE_MIDFIELDER ->
                intArrayOf(65, 62, 72, 75, 82)

            PlayerPosition.CENTRAL_MIDFIELDER ->
                intArrayOf(74, 68, 78, 84, 70)

            PlayerPosition.ATTACKING_MIDFIELDER ->
                intArrayOf(78, 78, 88, 88, 55)

            PlayerPosition.LEFT_WINGER,
            PlayerPosition.RIGHT_WINGER ->
                intArrayOf(90, 78, 86, 90, 45)

            PlayerPosition.STRIKER ->
                intArrayOf(82, 92, 75, 76, 65)
        }

        val pace = profile[0]
        val attack = profile[1]
        val technique = profile[2]
        val passing = profile[3]
        val defense = profile[4]

        fun stat(value: Int): Int =
            (value + index % 5 - 2).coerceIn(1, 99)

        val attributes = PlayerAttributes(
            acceleration = stat(pace),
            sprintSpeed = stat(pace + 2),
            finishing = stat(attack),
            shotPower = stat(attack + 3),
            longShots = stat(attack - 3),
            positioning = stat(attack + 2),
            shortPassing = stat(passing),
            longPassing = stat(passing - 3),
            vision = stat(passing + 2),
            crossing = stat(passing),
            dribbling = stat(technique),
            ballControl = stat(technique + 2),
            agility = stat(pace),
            balance = stat(technique),
            reactions = stat(75),
            stamina = stat(78),
            strength = stat(defense),
            jumping = stat(defense - 2),
            aggression = stat(defense),
            interceptions = stat(defense + 2),
            defensiveAwareness = stat(defense),
            standingTackle = stat(defense + 1),
            slidingTackle = stat(defense - 2),
            heading = stat(defense)
        )

        val appearance = PlayerAppearance(
            skinTone = index % 6,
            heightCm = when (position) {
                PlayerPosition.GOALKEEPER -> 190
                PlayerPosition.CENTRE_BACK -> 185
                PlayerPosition.STRIKER -> 182
                else -> 178
            },
            weightKg = 75,
            headShape = index % 4,
            faceWidth = 1f,
            jawWidth = 1f,
            noseSize = 1f,
            eyeShape = index % 3,
            eyeColor = 0,
            hairStyle = index % 8,
            hairColor = index % 5,
            facialHair = index % 4,
            bodyType = 1
        )

        val goalkeeper = if (
            position == PlayerPosition.GOALKEEPER
        ) {
            GoalkeeperAttributes(
                diving = 82,
                handling = 80,
                kicking = 75,
                reflexes = 84,
                positioning = 82,
                speed = 65
            )
        } else {
            null
        }

        val styles = when (position) {
            PlayerPosition.GOALKEEPER ->
                setOf(PlayStyle.REFLEX_SAVES, PlayStyle.RUSH_OUT)

            PlayerPosition.CENTRE_BACK ->
                setOf(PlayStyle.INTERCEPTOR, PlayStyle.AERIAL_THREAT)

            PlayerPosition.LEFT_BACK,
            PlayerPosition.RIGHT_BACK ->
                setOf(PlayStyle.SPEED_BURST, PlayStyle.LONG_PASS)

            PlayerPosition.DEFENSIVE_MIDFIELDER ->
                setOf(PlayStyle.INTERCEPTOR, PlayStyle.LONG_PASS)

            PlayerPosition.CENTRAL_MIDFIELDER ->
                setOf(PlayStyle.LONG_PASS, PlayStyle.THROUGH_PASS)

            PlayerPosition.ATTACKING_MIDFIELDER ->
                setOf(PlayStyle.THROUGH_PASS, PlayStyle.QUICK_CONTROL)

            PlayerPosition.LEFT_WINGER,
            PlayerPosition.RIGHT_WINGER ->
                setOf(
                    PlayStyle.TECHNICAL_DRIBBLING,
                    PlayStyle.SPEED_BURST,
                    PlayStyle.FINESSE_SHOT
                )

            PlayerPosition.STRIKER ->
                setOf(PlayStyle.POWER_SHOT, PlayStyle.AERIAL_THREAT)
        }

        return FootballPlayer(
            id = id,
            firstName = "Joueur",
            lastName = "%02d".format(number),
            nationality = "DK26",
            birthYear = 2000,
            club = teamName,
            shirtNumber = number,
            position = position,
            alternativePositions = emptyList(),
            preferredFoot = if (number % 2 == 0)
                PreferredFoot.RIGHT else PreferredFoot.LEFT,
            overallRating = (72 + index % 8),
            potential = 85,
            weakFoot = 3,
            skillMoves = 3,
            workRateAttack = 2,
            workRateDefense = 2,
            appearance = appearance,
            attributes = attributes,
            goalkeeper = goalkeeper,
            playStyles = styles
        )
    }
}
