package com.commandermtg.models

import kotlinx.serialization.Serializable

@Serializable
enum class Zone {
    LIBRARY,
    HAND,
    BATTLEFIELD,
    GRAVEYARD,
    EXILE,
    COMMAND_ZONE,
    STACK
}
