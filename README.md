# Villager Friendly Farms Plugin

A Minecraft Paper 1.21.11 plugin that allows players to craft villager-friendly farming devices using specific recipes. These farms appear as custom blocks and accumulate resources over time using a passive calculation system.

## Features

- **Two Farm Types**: Iron Farm and Villager Breeder with unique recipes and outputs
- **Recipe Book Integration**: Recipes appear in the Minecraft crafting book for easy discovery
- **Passive Resource Generation**: Farms accumulate resources over time without background processing
- **Continuous Generation Timer**: Items generate based on real elapsed time, not access frequency
- **Custom Block Appearance**: Farms appear as themed blocks (lit furnace for both types)
- **Chest-like Interface**: Right-click farms to access accumulated resources with visual storage indicators
- **Storage Capacity Management**: Full storage limits with overflow protection and visual warnings
- **Configurable Generation Rates**: Each farm type has its own configurable generation speed
- **Permission System**: Full integration with Bukkit permissions with granular control
- **Block Protection**: Farms are protected from unauthorized access with owner-based security
- **Data Persistence**: Automatic saving/loading with backup system and corruption recovery
- **Admin Commands**: Complete command system for management and debugging

## Building

This project uses Maven. To build:

```bash
mvn clean package
```

The compiled plugin JAR will be in the `target/` directory.

## Installation

1. Build the plugin or download the JAR file
2. Place the JAR in your server's `plugins/` directory
3. Start or restart your server
4. Configure farm settings in `plugins/VillagerFriendlyFarms/config.yml`
5. Farm configurations are automatically created in `plugins/VillagerFriendlyFarms/generators/`

## Farm Types

### Iron Farm
**Recipe Pattern:**
```
BED BED BED
COM HOP COM  
LAV CHE WAT
```

**Ingredients:**
- BED = Bed (any color)
- COM = Composter
- HOP = Hopper
- LAV = Lava Bucket
- CHE = Chest
- WAT = Water Bucket

**Output:** 1 Iron Ingot every 3 seconds  
**Appearance:** Lit Furnace  
**Storage:** 1,728 items (27 slots)

### Villager Breeder
**Recipe Pattern:**
```
WHE TOR WHE
BED COM BED
DIR WAT DIR
```

**Ingredients:**
- WHE = Wheat Seeds
- TOR = Torch
- BED = Bed (any color)
- COM = Composter
- DIR = Dirt
- WAT = Water Bucket

**Output:** 1 Villager Spawn Egg every 30 seconds  
**Appearance:** Composter  
**Storage:** 432 spawn eggs (27 slots)

## Usage

### Crafting Farms
1. **Recipe Discovery**: Open your recipe book to see available farm recipes
2. **Craft Farm**: Arrange ingredients in the 3x3 pattern shown in the recipe book
3. **Place Farm**: Place the crafted farm block in the world
4. **Access Resources**: Right-click the farm to open its interface and collect items

### Farm Interface
- **Storage Slots**: Shows accumulated items (take items like a normal chest)
- **Info Panel**: Displays farm type, generation rate, and storage status
- **Visual Indicators**: Color-coded storage status (green/yellow/red)
- **Time Information**: Shows elapsed time and time until next item

### Generation System
- **Continuous Timer**: Items generate based on real elapsed time since last generation
- **No Timer Reset**: Opening/closing the farm doesn't reset the generation timer
- **Storage Limits**: Generation stops when storage is full to prevent item loss
- **Persistent**: Generation continues across server restarts and chunk loading/unloading

## Commands

### `/vff` - Main command with subcommands:
- `/vff info` - Show plugin information and loaded farms
- `/vff reload` - Reload configuration and recipes (requires permission)
- `/vff give <player> <type>` - Give a farm to a player (requires permission)
- `/vff list` - List available farm types with details

## Permissions

### Basic Permissions
- `villagerfriendlyfarms.create` - Create farms (default: true)
- `villagerfriendlyfarms.use` - Use farms (default: true)

### Administrative Permissions
- `villagerfriendlyfarms.admin` - Full admin access to all farms (default: op)
- `villagerfriendlyfarms.reload` - Reload configuration (default: op)
- `villagerfriendlyfarms.give` - Give farms to players (default: op)
- `villagerfriendlyfarms.bypass.limits` - Bypass farm limits (default: op)
- `villagerfriendlyfarms.access.all` - Access any player's farms (default: op)

## Configuration

### Main Config (`config.yml`)
```yaml
plugin:
  debug: false
  auto-save-interval: 5

defaults:
  storage-capacity: 27
  protection-enabled: true

permissions:
  require-create-permission: true
  require-use-permission: true
  admin-override: true

performance:
  max-farms-per-chunk: 10
  max-farms-per-player: 50
```

### Farm Configurations
Individual farm types are configured in JSON files in the `generators/` directory:
- `iron_farm.json` - Iron Farm configuration
- `villager_breeder.json` - Villager Breeder configuration

Each configuration includes:
- Recipe pattern (3x3 grid)
- Output item and quantity
- Generation time in seconds
- Storage capacity
- Block appearance

## Data Storage

### Persistent Data
- **Farm Data**: Stored in JSON format with automatic backups
- **Block Metadata**: Uses Persistent Data Container (PDC) for block identification
- **Backup System**: Automatic backups every 5 minutes with corruption recovery
- **Data Integrity**: Validation and cleanup of corrupted data

### File Structure
```
plugins/VillagerFriendlyFarms/
├── config.yml                 # Main configuration
├── generators/                 # Farm type definitions
│   ├── iron_farm.json
│   └── villager_breeder.json
├── data/                      # Farm instance data
│   └── generators.json
└── backups/                   # Automatic backups
    ├── generators_2024-12-24_12-00-00.json
    └── ...
```

## Development

This plugin is built using:
- **Paper API 1.21.11** - Modern Minecraft server API
- **Java 21** - Latest LTS Java version
- **Maven** - Build and dependency management
- **Jackson** - JSON serialization/deserialization
- **JUnit 5 + jqwik** - Unit and property-based testing

### Architecture
- **Modular Design**: Clear separation of concerns with dedicated managers
- **Event-Driven**: Bukkit event system for player interactions
- **Async Operations**: Non-blocking data persistence
- **Thread-Safe**: Concurrent data structures for multi-threaded safety

## Troubleshooting

### Common Issues
1. **Recipes not appearing**: Use `/vff reload` and reopen your recipe book
2. **Permission errors**: Check that players have `villagerfriendlyfarms.create` and `villagerfriendlyfarms.use`
3. **Farms not generating**: Ensure storage isn't full and check generation timer
4. **Data corruption**: Plugin automatically recovers from backups

### Debug Mode
Enable debug logging in `config.yml`:
```yaml
plugin:
  debug: true
```

This provides detailed logging for troubleshooting generation, permissions, and data operations.