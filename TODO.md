# Dong-A-Deuce - Development TODO

**Current Version:** v5.0.1
**Desktop:** 99% complete (fully playable!)
**Android:** 99% complete (fully playable!)
**Network Mode:** 100% complete (fully playable!)
**Last Updated:** 2026-03-13

---

## Current Status

Dong-A-Deuce is a **fully functional multiplayer Commander game** with:
- 2-6 player support (hotseat and network)
- 120+ keyboard shortcuts
- Full Commander rules (40 life, 21 commander damage, poison)
- Card images from Scryfall with offline cache
- Settings persistence
- Game log with chat
- Persistent card viewer (Desktop)

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

## Recently Completed

### Persistent Card Viewer (Desktop) ✅
**Completed:** v4.5.2

- [x] Create persistent card viewer composable showing last hovered card
- [x] Display enlarged card image in sidebar (between turn indicator and game log)
- [x] Update on card hover (battlefield and hand cards)
- [x] Tabs to switch between Image and Text views
- [x] Android version (swipe-from-right drawer)

---

## Future Enhancements (Optional Polish)

These are nice-to-have features that would improve the experience but aren't required for gameplay.

### Android UI Improvements
**Effort:** 1-2 weeks

- [x] Battlefield rows limited to 3 (type-based: creatures top, artifacts/enchantments middle, lands bottom)
- [x] Battlefield columns auto-expand as cards are dragged to the right
- [x] Pinch-to-zoom for Android battlefield
- [x] Full battlefield UI for opponents on Android
- [x] Fix tapped cards cut-off on left/right sides
- [x] Enhance "View N cards" from library dialog (match PC client)
- [x] Enhance "View Hand" dialog (match PC client)

### Desktop UI Improvements
**Effort:** 1 week

- [x] Replace battlefield auto-scaling with horizontal scrolling (match Android behavior)
- [x] Fix player's hand overlapping bottom row of battlefield

### Drag-and-Drop Enhancements
**Effort:** 1-2 weeks

- [x] Drag cards from hand to battlefield (Android)
- [x] Drag cards from battlefield to hand (Android)
- [x] Drag cards from hand to battlefield (Desktop)
- [x] Drag cards from battlefield to hand (Desktop)
- [x] Reorder cards within hand by dragging (Desktop & Android)

### Battlefield Visual Fixes
**Effort:** 2-3 days

- [x] Fix overlap when multiple stacks of 3 cards on battlefield (Android)
- [x] Fix overlap when multiple stacks of 3 cards on battlefield (Desktop)

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
- 2-6 Player Hotseat Mode
- 2-6 Player Network Mode (WebSocket)
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
| v5.0.1 | MCP server, DFC flip images, die roller fix, graveyard/exile context menus, battlefield mouse wheel scrolling |
| v5.0.0 | 2-6 player support (hotseat & network), deck validation (100 cards), network/hotseat feature parity |
| v4.7.0 | Hand reordering via drag, drag between hand/battlefield, commander dialog keyboard shortcuts, Android card viewer swipes from left |
| v4.6.0 | Multi-format deck import (Cockatrice .cod, .dec, .dek, .txt, .mwDeck), clipboard paste, commander selection dialog with legendary filter |
| v4.5.14 | Dark theme text color fixes (title, settings icon, card viewer) |
| v4.5.13 | Dark theme background fix for desktop |
| v4.5.12 | Live UI scale + in-game settings, disabled auto game-end |
| v4.5.11 | Weight-based sidebar layout, battlefield min card size 40dp |
| v4.5.10 | UI Scale setting slider (50%-200%) |
| v4.5.9 | Opponent hand count display, sidebar overflow fixes |
| v4.5.8 | Windows UI overflow fix for zone buttons |
| v4.5.7 | CI fix for Android APK version filename |
| v4.5.6 | Windows DPI scaling fix for card/battlefield sizing |
| v4.5.5 | Native distribution icons, lint fixes |
| v4.5.4 | Android card viewer (swipe drawer), UI fixes marked complete |
| v4.5.3 | Resizable card viewer and sidebar (Desktop) |
| v4.5.2 | Persistent card viewer (Desktop), layout fix |
| v4.5.1 | Desktop dynamic card sizing, horizontal scroll, Android zoom 5x, View Details at top of context menu |
| v4.4.0 | Android pinch-to-zoom/pan, full opponent battlefields, turn indicator colors, type-based row fix |
| v4.4.0-alpha | Android APK in CI, enhanced Android dialogs, library view/search |
| v4.3.0-alpha | Give control back fix in network mode |
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
