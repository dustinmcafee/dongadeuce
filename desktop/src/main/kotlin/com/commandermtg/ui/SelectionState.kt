package com.commandermtg.ui

import androidx.compose.runtime.*
import com.commandermtg.models.CardInstance

/**
 * State holder for multi-card selection
 */
@Stable
class SelectionState {
    private val _selectedCards = mutableStateListOf<String>()
    val selectedCards: List<String> get() = _selectedCards

    val hasSelection: Boolean
        get() = _selectedCards.isNotEmpty()

    val selectionCount: Int
        get() = _selectedCards.size

    fun isSelected(cardId: String): Boolean {
        return _selectedCards.contains(cardId)
    }

    fun toggleSelection(cardId: String) {
        if (_selectedCards.contains(cardId)) {
            _selectedCards.remove(cardId)
        } else {
            _selectedCards.add(cardId)
        }
    }

    fun select(cardId: String) {
        if (!_selectedCards.contains(cardId)) {
            _selectedCards.add(cardId)
        }
    }

    fun deselect(cardId: String) {
        _selectedCards.remove(cardId)
    }

    fun clearSelection() {
        _selectedCards.clear()
    }

    fun selectAll(cardIds: List<String>) {
        _selectedCards.clear()
        _selectedCards.addAll(cardIds)
    }
}

/**
 * Composable to provide selection state
 */
@Composable
fun rememberSelectionState(): SelectionState {
    return remember { SelectionState() }
}
