# Quick Start Guide

## Running the Application

```bash
./gradlew desktop:run
```

This will launch Dong-A-Deuce, a multiplayer Commander/EDH game client.

## First Time Setup

1. **Download Card Cache** (Recommended)
   - On the main menu, click "Download Cache" under Card Cache
   - This downloads ~500MB of card data from Scryfall for instant deck loading
   - Without cache, cards load individually from the API (slower)

2. **Configure Settings** (Optional)
   - Click the gear icon (⚙️) in the top-right corner
   - Set your player name, default server settings, and deck directory

## Loading a Deck

Example deck files are included in the project root:
- `edgar_markov_deck.txt` - Edgar Markov Vampire tribal
- `first_sliver_deck.txt` - The First Sliver

Deck format (text file):
```
1 Commander Name
1 Card Name
1 Another Card
...
```

## Game Modes

### Local Hotseat (2-6 Players, Same Device)
1. Select "Local Hotseat" mode
2. Choose player count (2-6)
3. Load a deck for each player
4. Click "Start Game"

### Network Multiplayer (2-6 Players, Over Network)

**To Host:**
1. Select "Network" mode
2. Enter your name and load your deck
3. Click "Host Game"
4. Share the server address with other players
5. Wait for players to join and ready up
6. Click "Start Game"

**To Join:**
1. Select "Network" mode
2. Enter your name and load your deck
3. Click "Join Game"
4. Enter the host's address and port
5. Click "Connect", then "Ready!"

## Game Controls

### Mouse Controls
- **Double-click** battlefield card: Tap/untap
- **Double-click** hand card: Play to battlefield
- **Right-click** any card: Context menu with all actions
- **Drag** cards on battlefield: Reposition
- **Drag** from hand: Play to battlefield
- **Click** zone buttons: View zone contents

### Essential Keyboard Shortcuts

**Phases:**
- `F5-F10`: Jump to phase (Untap, Draw, Main1, Combat, Main2, End)
- `Ctrl+Enter`: Pass turn

**Card Actions:**
- `T`: Tap/untap selected card
- `Ctrl+U`: Untap all your permanents
- `Del`: Move to graveyard
- `Ctrl+X`: Move to exile
- `Ctrl+H`: Return to hand

**Library:**
- `Ctrl+D`: Draw a card
- `Ctrl+M`: Mulligan
- `Ctrl+S`: Shuffle library
- `F3`: View library

**Counters:**
- `Ctrl+=`: Add +1/+0
- `Alt+=`: Add +0/+1
- `Ctrl+Alt+=`: Add +1/+1

**Other:**
- `Ctrl+T`: Create token
- `Ctrl+I`: Roll dice
- `Shift+Enter`: Focus chat
- `Esc`: Close dialog

See full list: 120+ shortcuts available (press `?` or check MISSING_FEATURES.md)

## Game Zones

- **Command Zone**: Your commander (click to cast)
- **Library**: Draw deck (click for operations)
- **Hand**: Your cards (visible only to you)
- **Battlefield**: Permanents in play (drag to arrange)
- **Graveyard**: Destroyed/discarded cards
- **Exile**: Exiled cards

## Features

- Full Commander rules (40 life, 21 commander damage)
- Card images from Scryfall with offline cache
- Drag-and-drop battlefield with grid positioning
- Token creation (Scryfall search or custom)
- Card counters (+1/+1, charge, custom)
- Power/toughness modifications
- Card attachments (auras/equipment)
- Die rolling (D4-D100, coin flip)
- Game log with chat
- 120+ keyboard shortcuts

## Building Distributions

```bash
# macOS
./gradlew desktop:packageDmg

# Windows
./gradlew desktop:packageMsi

# Linux
./gradlew desktop:packageDeb
```

## Project Structure

```
dongadeuce/
├── shared/          # Game logic, models, networking
│   ├── models/      # Card, Deck, Player, GameState
│   ├── network/     # WebSocket server/client
│   └── settings/    # User settings, keyboard shortcuts
├── desktop/         # Compose Desktop UI
│   ├── ui/          # UI components
│   └── viewmodel/   # MVVM ViewModels
└── resources/       # Icons and assets
```

## Playing Over the Internet

To play with friends over the internet (not just local network):

### Host Setup (Port Forwarding)

1. **Find your local IP:**
   - Windows: Run `ipconfig` in Command Prompt, look for "IPv4 Address"
   - macOS/Linux: Run `ifconfig` or `ip addr`, look for your local IP (usually 192.168.x.x)

2. **Set up port forwarding on your router:**
   - Log into your router (usually http://192.168.1.1 or http://192.168.0.1)
   - Find "Port Forwarding" or "Virtual Server" settings
   - Add a new rule:
     - External Port: 8080 (or your chosen port)
     - Internal Port: 8080
     - Internal IP: Your computer's local IP
     - Protocol: TCP
   - Save and apply

3. **Find your public IP:**
   - Visit https://whatismyip.com or search "what is my ip"

4. **Share with players:**
   - Give them your public IP and port (e.g., `123.45.67.89:8080`)

### Client Setup

1. Enter the host's **public IP address** and port
2. Click "Connect"

### Alternative: Use a VPN

Services like Hamachi, ZeroTier, or Tailscale create a virtual LAN:
1. All players install the same VPN software
2. Create/join the same network
3. Use the VPN-assigned IP addresses to connect

## Troubleshooting

**Cards not loading?**
- Download the card cache from the main menu
- Check your internet connection

**Can't connect to host?**
- Verify port forwarding is set up correctly
- Check Windows Firewall / macOS Firewall allows the app
- Try temporarily disabling firewall to test
- Ensure the host's router port forwarding is active
- Use a VPN service as an alternative

**Connection refused?**
- The host may not have started hosting yet
- Port forwarding may not be configured correctly
- Try a different port (change in Settings)

**Game is slow?**
- Download the card cache for faster loading
- Close other applications using network bandwidth
