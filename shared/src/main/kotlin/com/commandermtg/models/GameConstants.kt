package com.commandermtg.models

/**
 * MTG Commander format constants and rules
 */
object GameConstants {
    // Life totals
    const val STARTING_LIFE = 40
    const val STARTING_HAND_SIZE = 7

    // Commander damage
    const val COMMANDER_DAMAGE_THRESHOLD = 21

    // Deck construction
    const val DECK_SIZE = 99 // Cards in deck excluding commander
    const val TOTAL_DECK_SIZE = 100 // Cards in deck including commander
}
