package com.dustinmcafee.dongadeuce.models

/**
 * Domain actions for card interactions.
 * These are pure data classes representing user intents, decoupled from UI.
 */
sealed class CardAction {
    data class Tap(val cardInstance: CardInstance) : CardAction()
    data class Untap(val cardInstance: CardInstance) : CardAction()
    data class FlipCard(val cardInstance: CardInstance) : CardAction()
    data class ToHand(val cardInstance: CardInstance) : CardAction()
    data class ToBattlefield(val cardInstance: CardInstance) : CardAction()
    data class ToGraveyard(val cardInstance: CardInstance) : CardAction()
    data class ToExile(val cardInstance: CardInstance) : CardAction()
    data class ToLibrary(val cardInstance: CardInstance) : CardAction()
    data class ToTop(val cardInstance: CardInstance) : CardAction()
    data class ToCommandZone(val cardInstance: CardInstance) : CardAction()
    data class AddCounter(val cardInstance: CardInstance, val counterType: String) : CardAction()
    data class RemoveCounter(val cardInstance: CardInstance, val counterType: String) : CardAction()
    data class GiveControlTo(val cardInstance: CardInstance, val newControllerId: String, val newControllerName: String) : CardAction()
    data class ViewDetails(val cardInstance: CardInstance) : CardAction()
    data class ShowLibraryPositionDialog(val cardInstance: CardInstance) : CardAction()
    data class ShowCounterDialog(val cardInstance: CardInstance, val counterType: String) : CardAction()
    data class ShowPowerToughnessDialog(val cardInstance: CardInstance) : CardAction()
    data class ToggleDoesntUntap(val cardInstance: CardInstance) : CardAction()
    data class SetAnnotation(val cardInstance: CardInstance) : CardAction()
    data class PlayFaceDown(val cardInstance: CardInstance) : CardAction()
    data class ToggleFaceDown(val cardInstance: CardInstance) : CardAction()
    data class CreateCopy(val cardInstance: CardInstance, val ownerId: String) : CardAction()
    data class RevealTo(val cardInstance: CardInstance, val targetPlayerIds: List<String>) : CardAction()
    data class ViewHand(val playerId: String) : CardAction()
}
