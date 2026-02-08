# Galaxy Conquest RTS

A real-time strategy game for Android mobile devices. Built with native Kotlin and Android SDK.

## Features

- **3 Playable Factions**:
  - **Iron Legion**: Balanced mechanical forces with repair capabilities
  - **Hive Collective**: Fast biological swarm with regenerative units  
  - **Crystal Dynasty**: Advanced shielded technology with powerful but expensive units

- **Core RTS Mechanics**:
  - Real-time unit control and combat
  - Resource gathering (minerals and energy)
  - Unit production and building construction
  - Fog of war and visibility system
  - AI opponent with multiple difficulty levels

- **Mobile-Optimized Controls**:
  - Touch-based selection and movement
  - Multi-touch camera controls (pan, zoom)
  - Box selection for multiple units
  - Context-sensitive command card

## Project Structure

```
galaxy_conquest/
├── app/
│   ├── src/main/
│   │   ├── kotlin/com/galaxycommand/rts/
│   │   │   ├── core/           # Game engine core
│   │   │   │   ├── GameEngine.kt
│   │   │   │   ├── GameLoop.kt
│   │   │   │   ├── Camera.kt
│   │   │   │   ├── InputHandler.kt
│   │   │   │   ├── Vector2.kt
│   │   │   │   └── GameState.kt
│   │   │   ├── entities/       # Game entities
│   │   │   │   ├── Entity.kt
│   │   │   │   ├── Unit.kt
│   │   │   │   ├── Building.kt
│   │   │   │   └── Resource.kt
│   │   │   ├── factions/       # Faction system
│   │   │   │   ├── Faction.kt
│   │   │   │   └── FactionType.kt
│   │   │   ├── systems/        # Game systems
│   │   │   │   ├── PathFinder.kt
│   │   │   │   ├── CombatSystem.kt
│   │   │   │   ├── AIEngine.kt
│   │   │   │   └── FogOfWarManager.kt
│   │   │   └── ui/             # UI components
│   │   │       ├── MainActivity.kt
│   │   │       ├── GameActivity.kt
│   │   │       ├── MenuActivity.kt
│   │   │       └── GameRenderer.kt
│   │   └── res/
│   │       ├── layout/         # XML layouts
│   │       ├── values/         # Resources
│   │       └── drawable/       # Graphics
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## Building the Project

### Prerequisites
- Android Studio (latest version recommended)
- Android SDK 34 or higher
- Kotlin 1.9.0 or higher
- Gradle 8.0 or higher

### Setup
1. Clone or download the project
2. Open Android Studio
3. Import the project from the `galaxy_conquest` directory
4. Wait for Gradle sync to complete
5. Build the project (Build > Make Project)

### Running on Device
1. Connect an Android device via USB
2. Enable USB debugging on the device
3. Run the app (Run > Run 'app')
4. Select your device from the deployment target dialog

## Game Controls

### Selection
- **Single Tap**: Select unit or issue command
- **Double Tap**: Select all units of same type
- **Drag Box**: Select multiple units
- **Shift + Tap**: Add/remove from selection

### Movement
- **Tap Ground**: Move selected units
- **Edge Drag**: Pan camera

### Camera
- **Two-finger Pan**: Scroll the map
- **Pinch**: Zoom in/out
- **Mini-map Tap**: Jump to location

### Commands
- **Attack**: Tap on enemy unit while combat units selected
- **Gather**: Tap on mineral field while workers selected
- **Stop**: Stop all selected units
- **Hold**: Units hold position and engage enemies

## Architecture

### Game Engine
The core `GameEngine` class manages:
- Entity creation and lifecycle
- Game loop and updates
- Input handling coordination
- Resource management

### Entity Component System
- **Units**: Mobile entities with stats, abilities, and state machines
- **Buildings**: Static entities for production and defense
- **Resources**: Mineral deposits for economy

### AI System
The `AIEngine` uses a Finite State Machine (FSM) with states:
- **Economy**: Focus on gathering resources and building workers
- **Expand**: Establish new bases
- **Army**: Build combat units
- **Attack**: Launch offensives
- **Defend**: Respond to threats

### Pathfinding
A* (A-Star) algorithm implementation for unit navigation:
- Grid-based pathfinding
- Dynamic obstacle avoidance
- Path caching for performance

## Customization

### Adding New Units
1. Add unit type to `Building.UnitType` enum
2. Create unit stats in `Unit` companion object
3. Add to faction's available units list in `Faction`
4. Create rendering code in `GameRenderer`

### Adding New Buildings
1. Add building type to `Building.BuildingType` enum
2. Define production capabilities
3. Create building stats and appearance
4. Add to faction building list

### Modifying AI
The AI behavior can be customized by:
- Adjusting difficulty parameters
- Modifying state transition thresholds
- Changing build priorities
- Tweaking reaction speeds

## Performance Considerations

- Uses object pooling for projectiles and effects
- Grid-based fog of war for efficient visibility calculations
- A* pathfinding with caching
- Level-of-detail rendering for distant objects
- Configurable graphics quality settings

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contributing

Contributions are welcome! Please read the contributing guidelines before submitting PRs.

## Acknowledgments

- Built with Android SDK and Kotlin
- Uses A* pathfinding algorithm
- Inspired by classic real-time strategy games
