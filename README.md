# Villager Friendly Farms Plugin

A Minecraft Paper 1.21.11 plugin that allows players to craft villager-friendly farming devices using specific recipes.

## Features

- **Custom Farming Devices**: Craft farms like Iron Farms using specific 3x3 recipes
- **Passive Resource Generation**: Farms accumulate resources over time without background processing
- **Custom Block Appearance**: Farms appear as themed blocks (e.g., iron blocks for Iron Farms)
- **Chest-like Interface**: Right-click farms to access accumulated resources
- **Per-Farm Generation Rates**: Each farm type has its own configurable generation speed
- **Permission System**: Full integration with Bukkit permissions
- **Block Protection**: Farms are protected from unauthorized access

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
4. Configure farm generation rates in `plugins/VillagerFriendlyFarms/generators/`

## Usage

### Iron Farm Recipe
```
BED BED BED
COM HOP COM  
LAV CHE WAT
```

Where:
- BED = Bed (any color)
- COM = Composter
- HOP = Hopper
- LAV = Lava Bucket
- CHE = Chest
- WAT = Water Bucket

### Permissions

- `resourcegenerator.create` - Create generators (default: true)
- `resourcegenerator.use` - Use generators (default: true)
- `resourcegenerator.admin` - Admin access to all generators (default: op)
- `resourcegenerator.reload` - Reload configuration (default: op)

## Development

This plugin is built using:
- Paper API 1.21.4
- Java 21
- Maven
- JUnit 5 + jqwik for testing