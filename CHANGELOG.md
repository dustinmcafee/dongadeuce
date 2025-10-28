# Changelog

All notable changes to Commander MTG will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Scryfall API integration for card data
- Deck loading populates game state with actual cards
- Draw card functionality with UI button
- Hand view to see cards in your hand
- Basic card actions (play to battlefield, tap/untap)
- Starting hand draw (7 cards + mulligan)

## [1.0.0] - 2025-10-27

### Added
- Initial project structure with Gradle + Compose Multiplatform
- MVVM architecture with ViewModels and StateFlow
- Core domain models (Card, Deck, Player, GameState, Zone, CardInstance)
- Cockatrice deck format parser
- Main menu with deck loading via file chooser
- Host lobby screen (UI only, networking not implemented)
- Join lobby screen (UI only, networking not implemented)
- Game screen with all MTG zones (Library, Hand, Battlefield, Graveyard, Exile, Command Zone)
- Life tracking with +/- buttons
- Dynamic card counts per zone
- Cross-platform build support (Windows, macOS, Linux)
- Three example Commander decks included
- README and QUICKSTART documentation

### Technical Details
- Kotlin 1.9.21
- Compose Multiplatform 1.5.11
- Ktor 2.3.7 (dependencies added, not yet used)
- kotlinx.serialization for JSON
- GameViewModel manages game state
- MenuViewModel handles navigation and deck loading

### Known Limitations
- No networking implementation
- Cards are name-only (no data from Scryfall)
- No visual card rendering
- No drag-and-drop functionality
- No actual game actions (can't draw, play, or move cards)
- Deck loading parses files but doesn't populate game state
- No commander damage tracking UI
- No turn/phase system

---

## Version History

- **1.0.0** - Initial scaffold with MVVM architecture
- **1.0.1** - (In Progress) Scryfall integration and basic gameplay
