# Changelog

All notable changes to Commander MTG will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.1] - 2025-10-27

### Added
- Scryfall API integration for fetching card data (mana cost, type, oracle text, images)
- Deck loading now populates game state with actual CardInstance objects
- Automatic starting hand draw (7 cards for each player)
- Draw card button in player area
- Hand view dialog showing all cards in hand with card details
- Play card from hand to battlefield functionality
- Loading progress indicator during deck/card data fetching
- Rate limiting for Scryfall API requests (100ms between calls)

### Changed
- MenuViewModel now uses coroutines to fetch card data asynchronously
- Cards now contain full data from Scryfall (not just names)
- Game initialization automatically loads deck and draws starting hands

### Technical Details
- Added Ktor content negotiation and JSON serialization dependencies
- Created ScryfallApi client with proper error handling
- Enhanced GameViewModel with `drawStartingHand` and `getCards` methods
- HandDialog composable for viewing and playing cards from hand

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
