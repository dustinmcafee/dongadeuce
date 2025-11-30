# Dong-A-Deuce - Development TODO

**Current Version:** v4.2.0-alpha
**Desktop:** 99% complete (fully playable!)
**Android:** 95% complete (fully playable!)
**Network Mode:** 96% complete (fully playable!)
**Last Updated:** 2025-11-29

---

## Current Status

Dong-A-Deuce is a **fully functional multiplayer Commander game** with:
- 2-4 player support (hotseat and network)
- 120+ keyboard shortcuts
- Full Commander rules (40 life, 21 commander damage, poison)
- Card images from Scryfall with offline cache
- Settings persistence
- Game log with chat

---

## Won't Implement

These features have been evaluated and won't be implemented:

| Feature | Reason |
|---------|--------|
| **Stack & Priority System** | Players handle stack manually like paper Magic |
| **Combat System Automation** | Players declare attackers/blockers manually |
| **AI Opponents** | Requires stack system for meaningful play |
| **Game Save/Load** | Games meant to be played in one session |
| **Commander Tax Tracking** | Players track manually (game doesn't enforce mana costs) |

---

## Future Enhancements (Optional Polish)

These are nice-to-have features that would improve the experience but aren't required for gameplay.

### Animations
**Effort:** 1-2 weeks

- [ ] Card movement animations (zone transitions)
- [ ] Tap rotation animation (smooth 90-degree turn)
- [ ] Life counter animations (bounce on change)
- [ ] Phase transition effects
- [ ] Smooth drag animations

### Sound Effects
**Effort:** 3-4 days

- [ ] Card draw sound
- [ ] Card play sound
- [ ] Tap sound
- [ ] Life change sound
- [ ] Turn pass sound
- [ ] Volume controls
- [ ] Mute toggle

### Themes
**Effort:** 1 week

- [ ] Light mode support
- [ ] Custom card backs
- [ ] Custom backgrounds/playmats
- [ ] Theme selection in settings
- [ ] Save theme preference

### Deck Builder
**Effort:** 2-3 weeks

- [ ] In-app deck creation
- [ ] Scryfall card search integration
- [ ] Add/remove cards with autocomplete
- [ ] Commander selection with validation
- [ ] Deck validation (Commander rules: singleton, color identity)
- [ ] Save/load deck files
- [ ] Import/export formats (MTGO, Arena, Moxfield)
- [ ] Deck statistics (mana curve, color distribution)

### Spectator Mode
**Effort:** 2-3 days

- [ ] Join network game as spectator
- [ ] Spectator-only view (no hidden information revealed)
- [ ] Spectator chat

### Game Replay
**Effort:** 3-5 days

- [ ] Record all game actions
- [ ] Playback with forward/back controls
- [ ] Export replay files
- [ ] Share replays

---

## Code Quality (Low Priority)

### Refactor GameScreen.kt
**Current:** ~2,500 lines - could be split

- [ ] Extract HotseatGameLayout.kt
- [ ] Extract NetworkGameLayout.kt
- [ ] Extract PlayerArea.kt
- [ ] Extract DialogManager.kt
- [ ] Reduce file to <500 lines

### Testing Improvements

- [ ] Add Compose UI tests
- [ ] Add integration tests for full game flows
- [ ] Increase test coverage to 80%+
- [ ] Add snapshot tests for UI

---

## Completed Features

### Core Gameplay (100%)
- Turn/Phase System with full MTG cycle
- Commander Damage Tracking with 21-damage rule
- Life Tracking with auto-loss detection
- Card Context Menus for all zones
- Library Operations (draw, mill, shuffle, search, tutor, peek, mulligan)
- Zone Viewers (Graveyard, Exile, Command Zone, Sideboard)
- Sideboard support with deck loading
- Drag-and-Drop Battlefield with grid positioning
- Card Images with async loading and offline cache
- Tap/Untap cards
- Counters (add/remove/set +1/+1, charge, custom A-F)
- Card Attachments (auras/equipment)
- Flip Cards and Face Down Cards
- Token Creation (Scryfall search or custom)
- P/T Modifications (increase/decrease/set/reset)
- Card Annotations
- "Doesn't Untap" toggle
- Card Copy/Clone
- Player Counters (poison, energy, experience, mana, custom)
- Die Rolling (D4-D100, coin flip)
- Multi-card Selection with batch operations

### Multiplayer (100%)
- 2-4 Player Hotseat Mode
- 2-4 Player Network Mode (WebSocket)
- Host/Join lobby system
- Player disconnect handling
- Real-time state synchronization
- Chat messages
- Action validation

### Keyboard Shortcuts (120+)
- Game phases (F5-F10)
- Card actions (tap, untap, move zones)
- Power/toughness modifications
- Counter management (A-F, mana colors)
- Library operations
- View zones
- Selection controls
- Stack Until Found

### Settings & Persistence
- Settings Dialog UI (gear icon)
- Player name persistence
- Server address/port persistence
- Default deck directory
- Cross-platform storage

### Technical
- MVVM Architecture (100% compliant)
- StateFlow reactive updates
- Scryfall Integration with bulk cache
- 44+ Unit Tests
- Cross-platform builds (Windows, macOS, Linux)
- GitHub Actions CI/CD

---

## Version History

| Version | Highlights |
|---------|------------|
| v4.2.0-alpha | Android drag-and-drop fix, hand card bug fix, CI consolidation |
| v4.1.0-alpha | Sideboard support, Android feature parity, scry/peek fix |
| v4.0.0-alpha | Android support with KMP |
| v3.5.0 | UI state testing suite (132 tests) |
| v3.4.0 | Comprehensive test suite (270 tests) |
| v3.3.0 | Reveal cards feature, view opponents' zones |
| v3.2.0 | View Hand dialog with Cockatrice-style column layout |
| v3.1.0 | Settings Dialog UI |
| v3.0.0 | 120+ shortcuts, Stack Until Found, build fixes |
| v2.27.0 | Keyboard shortcuts system (65+) |
| v2.26.0 | Network multiplayer, player counters, game log |

---

**The game is fully playable! Remaining items are optional polish.**
