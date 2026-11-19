
package com.dk26.football

enum class PlayerPosition {
    GOALKEEPER,
    CENTRE_BACK,
    LEFT_BACK,
    RIGHT_BACK,
    DEFENSIVE_MIDFIELDER,
    CENTRAL_MIDFIELDER,
    ATTACKING_MIDFIELDER,
    LEFT_WINGER,
    RIGHT_WINGER,
    STRIKER
}

enum class PreferredFoot {
    LEFT, RIGHT, BOTH
}

enum class PlayStyle {
    FINESSE_SHOT,
    POWER_SHOT,
    LONG_PASS,
    THROUGH_PASS,
    QUICK_CONTROL,
    TECHNICAL_DRIBBLING,
    SPEED_BURST,
    AERIAL_THREAT,
    INTERCEPTOR,
    POWER_TACKLE,
    LONG_THROW,
    REFLEX_SAVES,
    RUSH_OUT
}

data class PlayerAppearance(
    val skinTone: Int,
    val heightCm: Int,
    val weightKg: Int,
    val headShape: Int,
    val faceWidth: Float,
    val jawWidth: Float,
    val noseSize: Float,
    val eyeShape: Int,
    val eyeColor: Int,
    val hairStyle: Int,
    val hairColor: Int,
    val facialHair: Int,
    val bodyType: Int
)

data class PlayerAttributes(
    val acceleration: Int,
    val sprintSpeed: Int,
    val finishing: Int,
    val shotPower: Int,
    val longShots: Int,
    val positioning: Int,
    val shortPassing: Int,
    val longPassing: Int,
    val vision: Int,
    val crossing: Int,
    val dribbling: Int,
    val ballControl: Int,
    val agility: Int,
    val balance: Int,
    val reactions: Int,
    val stamina: Int,
    val strength: Int,
    val jumping: Int,
    val aggression: Int,
    val interceptions: Int,
    val defensiveAwareness: Int,
    val standingTackle: Int,
    val slidingTackle: Int,
    val heading: Int
)

data class GoalkeeperAttributes(
    val diving: Int,
    val handling: Int,
    val kicking: Int,
    val reflexes: Int,
    val positioning: Int,
    val speed: Int
)

data class FootballPlayer(
    val id: String,
    val firstName: String,
    val lastName: String,
    val nationality: String,
    val birthYear: Int,
    val club: String,
    val shirtNumber: Int,
    val position: PlayerPosition,
    val alternativePositions: List<PlayerPosition>,
    val preferredFoot: PreferredFoot,
    val overallRating: Int,
    val potential: Int,
    val weakFoot: Int,
    val skillMoves: Int,
    val workRateAttack: Int,
    val workRateDefense: Int,
    val appearance: PlayerAppearance,
    val attributes: PlayerAttributes,
    val goalkeeper: GoalkeeperAttributes? = null,
    val playStyles: Set<PlayStyle> = emptySet()
) {
    val fullName: String
        get() = "$firstName $lastName"

    val isGoalkeeper: Boolean
        get() = position == PlayerPosition.GOALKEEPER
}

object PlayerCatalog {
    private val players = mutableListOf<FootballPlayer>()

    fun add(player: FootballPlayer) {
        require(player.id.isNotBlank()) { "Player ID is required" }
        require(player.overallRating in 1..99)
        require(player.potential in 1..99)
        require(player.weakFoot in 1..5)
        require(player.skillMoves in 1..5)
        require(player.appearance.heightCm in 140..220)
        require(player.appearance.weightKg in 40..150)
        require(player.attributes.toList().all { it in 1..99 })

        if (player.isGoalkeeper) {
            require(player.goalkeeper != null) {
                "A goalkeeper must have goalkeeper attributes"
            }
        }

        require(players.none { it.id == player.id }) {
            "Player ID already exists: ${player.id}"
        }

        players.add(player)
    }

    fun getAll(): List<FootballPlayer> = players.toList()

    fun findById(id: String): FootballPlayer? =
        players.firstOrNull { it.id == id }

    fun findByPosition(position: PlayerPosition): List<FootballPlayer> =
        players.filter {
            it.position == position ||
            position in it.alternativePositions
        }

    fun findByNationality(nationality: String): List<FootballPlayer> =
        players.filter {
            it.nationality.equals(nationality, ignoreCase = true)
        }

    fun search(query: String): List<FootballPlayer> =
        players.filter {
            it.fullName.contains(query, ignoreCase = true) ||
            it.club.contains(query, ignoreCase = true)
        }

    fun count(): Int = players.size

    fun clear() {
        players.clear()
    }

    private fun PlayerAttributes.toList(): List<Int> = listOf(
        acceleration, sprintSpeed, finishing, shotPower,
        longShots, positioning, shortPassing, longPassing,
        vision, crossing, dribbling, ballControl, agility,
        balance, reactions, stamina, strength, jumping,
        aggression, interceptions, defensiveAwareness,
        standingTackle, slidingTackle, heading
    )
}
