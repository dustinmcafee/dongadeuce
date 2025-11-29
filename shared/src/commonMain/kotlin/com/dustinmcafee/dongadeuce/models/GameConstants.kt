package com.dustinmcafee.dongadeuce.models

/**
 * MTG Commander format constants and rules
 */
object GameConstants {
    // Life totals
    const val STARTING_LIFE = 40
    const val STARTING_HAND_SIZE = 7

    // Commander damage
    const val COMMANDER_DAMAGE_THRESHOLD = 21

    // Player counters
    const val POISON_THRESHOLD = 10 // 10 poison counters = loss

    // Deck construction
    const val DECK_SIZE = 99 // Cards in deck excluding commander
    const val TOTAL_DECK_SIZE = 100 // Cards in deck including commander

    // Common player counter types
    val PLAYER_COUNTER_TYPES = listOf("poison", "energy", "experience")
}
