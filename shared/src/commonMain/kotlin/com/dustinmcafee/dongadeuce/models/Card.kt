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
    val scryfallId: String? = null,
    val backFaceImageUri: String? = null,  // For transform/double-faced cards
    val backFaceName: String? = null       // Name of the back face (e.g., "Insectile Aberration")
) {
    /** True if this is a double-faced/transform card */
    val isDoubleFaced: Boolean
        get() = backFaceImageUri != null || name.contains(" // ")
    val isLegendary: Boolean
        get() = type?.contains("Legendary", ignoreCase = true) == true

    val isCreature: Boolean
        get() = type?.contains("Creature", ignoreCase = true) == true

    val isPlaneswalker: Boolean
        get() = type?.contains("Planeswalker", ignoreCase = true) == true

    /** Can be used as a commander (creature or planeswalker for house rules) */
    val canBeCommander: Boolean
        get() = isCreature || isPlaneswalker
}
