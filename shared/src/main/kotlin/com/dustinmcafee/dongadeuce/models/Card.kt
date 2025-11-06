package com.dustinmcafee.dongadeuce.models

import kotlinx.serialization.Serializable

/**
 * Represents card metadata from Scryfall or deck list
 */
@Serializable
data class Card(
    val name: String,
    val manaCost: String? = null,
    val cmc: Double? = null,
    val type: String? = null,
    val oracleText: String? = null,
    val power: String? = null,
    val toughness: String? = null,
    val colors: List<String> = emptyList(),
    val imageUri: String? = null,
    val scryfallId: String? = null
) {
    val isLegendary: Boolean
        get() = type?.contains("Legendary", ignoreCase = true) == true

    val isCreature: Boolean
        get() = type?.contains("Creature", ignoreCase = true) == true
}
