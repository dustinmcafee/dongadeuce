# Keyboard Shortcuts Implementation Plan

## Overview

This document outlines the implementation plan for adding comprehensive keyboard shortcuts to Dong-A-Deuce, providing power users with efficient game control similar to other MTG applications.

---

## Architecture

### Components to Create

1. **KeyboardShortcuts.kt** (shared module)
   - `ShortcutAction` sealed class with all shortcut actions
   - `ShortcutBinding` data class (action + key combination)
   - `KeyboardShortcutsSettings` class for persistence
   - Default bindings map

2. **KeyboardShortcutHandler.kt** (desktop module)
   - Compose keyboard event handling
   - Maps key events to ShortcutActions
   - Context-aware shortcuts (different in dialogs vs game)

3. **KeyboardShortcutsDialog.kt** (desktop module)
   - UI for viewing/customizing shortcuts
   - Category-based tree view
   - Key recording widget
   - Reset to defaults button

---

## Default Keyboard Shortcuts

### Game Phases
| Shortcut | Action |
|----------|--------|
| F5 | Untap Phase |
| F6 | Draw Phase |
| F7 | First Main Phase |
| F8 | Combat Phase |
| F9 | Second Main Phase |
| F10 | End Phase |
| Ctrl+Space / Tab | Next Phase |
| Ctrl+Enter | Pass Turn |

### Card Actions (Selected Card)
| Shortcut | Action |
|----------|--------|
| T | Tap/Untap Card |
| Ctrl+U | Untap All |
| Alt+U | Toggle "Doesn't Untap" |
| Alt+F | Flip Card |
| Ctrl+J | Clone Card |
| Ctrl+T | Create Token |
| Alt+N | Set Annotation |
| Delete / Ctrl+Del | Move to Graveyard |
| Ctrl+B | Move to Bottom of Library |

### Power/Toughness (Selected Card)
| Shortcut | Action |
|----------|--------|
| Ctrl+= | Add Power (+1/+0) |
| Ctrl+- | Remove Power (-1/-0) |
| Alt+= | Add Toughness (+0/+1) |
| Alt+- | Remove Toughness (-0/-1) |
| Ctrl+Alt+= | Add Both (+1/+1) |
| Ctrl+Alt+- | Remove Both (-1/-1) |
| Ctrl+P | Set Power/Toughness |
| Ctrl+Alt+0 | Reset Power/Toughness |

### Life & Counters
| Shortcut | Action |
|----------|--------|
| F12 | Add Life (+1) |
| F11 | Remove Life (-1) |
| Ctrl+L | Set Life Total |

### Drawing & Library
| Shortcut | Action |
|----------|--------|
| Ctrl+D | Draw Card |
| Ctrl+E | Draw Multiple Cards |
| Ctrl+Shift+D | Undo Draw (put back on top) |
| Ctrl+M | Mulligan |
| Ctrl+S | Shuffle Library |
| Ctrl+Y | Play Top Card of Library |
| Alt+Y | Mill Top Card (to graveyard) |
| Alt+M | Mill Multiple Cards |

### View Zones
| Shortcut | Action |
|----------|--------|
| F3 | View Library |
| F4 | View Graveyard |
| Ctrl+W | Peek Top Cards of Library |
| Ctrl+Shift+W | Peek Bottom Cards of Library |
| Esc | Close Active Dialog |

### Gameplay
| Shortcut | Action |
|----------|--------|
| Ctrl+I | Roll Dice |
| Ctrl+R | Remove Arrows |
| F2 | Concede |
| Shift+Enter | Focus Chat Input |

---

## Implementation Steps

### Phase 1: Core Infrastructure
1. Create `ShortcutAction` sealed class with all action types
2. Create `ShortcutBinding` data class for key combinations
3. Create `KeyboardShortcutsSettings` with load/save to settings.json
4. Define default shortcuts map

### Phase 2: Event Handling
1. Create `KeyboardShortcutHandler` composable
2. Wrap GameScreen with keyboard event handler
3. Map key events to ShortcutActions
4. Connect actions to existing GameViewModel methods

### Phase 3: Card Selection System
1. Add `selectedCardId: String?` to GameViewModel state
2. Add click-to-select behavior for battlefield cards
3. Visual indicator for selected card (highlight border)
4. Shortcuts operate on selected card when applicable

### Phase 4: Settings UI
1. Create `KeyboardShortcutsDialog` with category tree
2. Key recording input field
3. Conflict detection (warn on duplicate bindings)
4. Save/load custom bindings
5. Reset to defaults button

---

## Technical Notes

### Key Event Handling in Compose

```kotlin
Modifier.onKeyEvent { event ->
    if (event.type == KeyEventType.KeyDown) {
        val binding = KeyBinding(
            key = event.key,
            ctrl = event.isCtrlPressed,
            alt = event.isAltPressed,
            shift = event.isShiftPressed
        )
        val action = shortcutsSettings.getAction(binding)
        if (action != null) {
            handleAction(action)
            true
        } else {
            false
        }
    } else {
        false
    }
}
```

### Context-Aware Shortcuts

Shortcuts should be disabled or modified when:
- A dialog is open (only Esc to close)
- Text input is focused (allow typing)
- Game is paused (network disconnect)

### Persistence Format

Add to existing `settings.json`:

```json
{
  "playerName": "Player 1",
  "serverAddress": "localhost",
  "serverPort": 8080,
  "shortcuts": {
    "nextPhase": "Ctrl+Space",
    "passTurn": "Ctrl+Enter",
    "drawCard": "Ctrl+D",
    ...
  }
}
```

---

## Files to Create/Modify

### New Files
- `shared/src/main/kotlin/.../settings/KeyboardShortcuts.kt`
- `desktop/src/main/kotlin/.../ui/KeyboardShortcutHandler.kt`
- `desktop/src/main/kotlin/.../ui/KeyboardShortcutsDialog.kt`

### Modified Files
- `shared/src/main/kotlin/.../settings/UserSettings.kt` - Add shortcuts map
- `desktop/src/main/kotlin/.../ui/GameScreen.kt` - Add keyboard handler wrapper
- `desktop/src/main/kotlin/.../viewmodel/GameViewModel.kt` - Add selectedCardId state

---

## Estimated Effort

| Phase | Effort |
|-------|--------|
| Phase 1: Core Infrastructure | 0.5 days |
| Phase 2: Event Handling | 0.5 days |
| Phase 3: Card Selection | 0.5 days |
| Phase 4: Settings UI | 1 day |
| **Total** | **2.5 days** |

---

## Future Enhancements

- Multiple key bindings per action
- Import/export shortcut profiles
- Shortcut hints in context menus
- On-screen shortcut cheat sheet (toggle with ?)
