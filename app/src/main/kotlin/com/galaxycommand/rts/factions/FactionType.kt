package com.galaxycommand.rts.factions

/**
 * Enumeration of available factions in the game.
 */
enum class FactionType(
    val displayName: String,
    val description: String,
    val playStyle: PlayStyle
) {

    VANGUARD(
        displayName = "Vanguard",
        description = "Balanced industrial forces with repair capabilities",
        playStyle = PlayStyle.BALANCED
    ),

    SWARM(
        displayName = "Swarm",
        description = "Fast biological swarm with regenerative units",
        playStyle = PlayStyle.AGGRESSIVE
    ),

    SYNODE(
        displayName = "Synod",
        description = "Advanced shielded technology with powerful units",
        playStyle = PlayStyle.TACTICAL
    );

    enum class PlayStyle {
        /**
         * Balanced play style with flexible options
         */
        BALANCED,

        /**
         * Aggressive play style focused on swarming
         */
        AGGRESSIVE,

        /**
         * Tactical play style requiring precise execution
         */
        TACTICAL
    }

    companion object {
        /**
         * Get all available factions
         */
        fun getAll(): List<FactionType> = entries.toList()

        /**
         * Get faction by name (case-insensitive)
         */
        fun fromName(name: String): FactionType? {
            return entries.find {
                it.name.equals(name, ignoreCase = true) ||
                it.displayName.equals(name, ignoreCase = true)
            }
        }

        /**
         * Get faction color resource ID
         */
        fun getColorResId(factionType: FactionType): Int {
            return when (factionType) {
                VANGUARD -> android.R.color.holo_blue_light
                SWARM -> android.R.color.holo_purple
                SYNODE -> android.R.color.holo_orange_light
            }
        }

        /**
         * Get faction icon resource ID
         */
        fun getIconResId(factionType: FactionType): Int {
            return when (factionType) {
                VANGUARD -> android.R.drawable.ic_menu_compass
                SWARM -> android.R.drawable.ic_menu_gallery
                SYNODE -> android.R.drawable.star_on
            }
        }
    }
}
