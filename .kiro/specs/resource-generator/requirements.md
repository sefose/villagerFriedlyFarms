# Requirements Document

## Introduction

A Minecraft Paper plugin that allows players to build resource generation devices. These devices consist of a button that, when pressed, generates specified resources and deposits them into a connected chest.

## Glossary

- **Resource_Generator**: A player-built device that appears as a specific block type and functions as both a generator and storage container
- **Generator_Type**: A predefined device type with a specific recipe and resource output (e.g., Iron_Farm)
- **Crafting_Grid**: A 3x3 area where players place items according to the generator recipe
- **Generator_Block**: The visual block representation of a generator (e.g., iron block for Iron_Farm)
- **Generator_Interface**: The chest-like GUI that opens when players right-click the generator block
- **Villager_Breeder**: A specific generator type that appears as a lit furnace and produces villager spawn eggs
- **Iron_Farm**: A specific generator type that appears as a lit furnace and produces iron ingots
- **Plugin_System**: The overall Minecraft plugin managing all resource generators
- **Player**: A Minecraft player interacting with the plugin

## Requirements

### Requirement 1: Device Crafting and Creation

**User Story:** As a player, I want to craft specific generator types using defined recipes, so that I can build different resource generation devices.

#### Acceptance Criteria

1. WHEN a player arranges items in a 3x3 crafting pattern matching a generator recipe, THE Plugin_System SHALL recognize the valid generator type
2. WHEN a valid Iron_Farm recipe is crafted (beds in top row, composter-hopper-composter in middle row, lava bucket-chest-water bucket in bottom row), THE Plugin_System SHALL create an Iron_Farm generator
3. WHEN a valid Villager_Breeder recipe is crafted (carrots in top row, beds in middle row, bread in bottom row), THE Plugin_System SHALL create a Villager_Breeder generator
3. WHEN a valid Villager_Breeder recipe is crafted (carrots in top row, beds in middle row, bread in bottom row), THE Plugin_System SHALL create a Villager_Breeder generator
4. WHEN a generator is successfully crafted, THE Plugin_System SHALL consume the crafting materials and activate the generator
5. WHEN an invalid recipe is attempted, THE Plugin_System SHALL not consume materials and provide feedback
6. THE Plugin_System SHALL validate that all required items are present in the correct positions before creating a generator

### Requirement 2: Generator Functionality

**User Story:** As a player, I want my generators to calculate and provide accumulated resources when I open them, so that I can collect resources that have been "generated" over time.

#### Acceptance Criteria

1. WHEN a generator is created, THE Plugin_System SHALL place the appropriate block type and record the creation timestamp
2. WHEN a player right-clicks a generator block, THE Plugin_System SHALL calculate resources accumulated since last opening
3. WHEN calculating resources, THE Plugin_System SHALL use the configured generation rate for that generator type and time elapsed
4. THE Plugin_System SHALL add the calculated resources to the generator's storage before displaying the interface
5. THE Plugin_System SHALL respect storage limits and cap generation at maximum capacity

### Requirement 3: Iron Farm Specific Functionality

**User Story:** As a player, I want my Iron Farm to produce iron ingots over time, so that I can automate iron collection.

#### Acceptance Criteria

1. WHEN an Iron_Farm is created, THE Plugin_System SHALL place a lit furnace block
2. WHEN a player opens an Iron_Farm, THE Plugin_System SHALL calculate iron ingots based on configured generation rate
3. THE Plugin_System SHALL produce iron ingots as the output resource for Iron_Farm generators
4. THE Plugin_System SHALL use the Iron_Farm specific generation timing (configurable, default 3 seconds per ingot)
5. THE Plugin_System SHALL store iron ingots in the generator's internal storage

### Requirement 4: Villager Breeder Specific Functionality

**User Story:** As a player, I want my Villager Breeder to produce villager spawn eggs over time, so that I can automate villager collection for trading and village building.

#### Acceptance Criteria

1. WHEN a Villager_Breeder is created, THE Plugin_System SHALL place a lit furnace block
2. WHEN a player opens a Villager_Breeder, THE Plugin_System SHALL calculate villager spawn eggs based on configured generation rate
3. THE Plugin_System SHALL produce villager spawn eggs as the output resource for Villager_Breeder generators
4. THE Plugin_System SHALL use the Villager_Breeder specific generation timing (configurable, default 30 seconds per spawn egg)
5. THE Plugin_System SHALL store villager spawn eggs in the generator's internal storage

### Requirement 5: Automatic Generation System

**User Story:** As a player, I want generators to calculate accumulated resources only when I open them, so that the system is efficient and doesn't run continuous background processes.

#### Acceptance Criteria

1. WHEN a generator is created, THE Plugin_System SHALL record the creation time but perform no active generation
2. WHEN a player opens a generator interface, THE Plugin_System SHALL calculate time elapsed since last opening
3. WHEN calculating resources, THE Plugin_System SHALL use the configured generation rate to determine how many items to add
4. THE Plugin_System SHALL add the calculated resources to the generator's storage upon opening
5. THE Plugin_System SHALL update the "last opened" timestamp after adding resources and reset the calculation timer

### Requirement 6: Configuration System

**User Story:** As a server administrator, I want to customize generation rates and storage limits through configuration files, so that I can balance the plugin for my server.

#### Acceptance Criteria

1. THE Plugin_System SHALL load generation rates for each generator type from a configuration file
2. THE Plugin_System SHALL allow administrators to set different generation intervals (e.g., 1 iron per 5 minutes)
3. THE Plugin_System SHALL support configurable storage limits for each generator type
4. WHEN configuration is changed, THE Plugin_System SHALL apply new settings without requiring server restart
5. THE Plugin_System SHALL provide default configuration values for all generator types

### Requirement 7: Generator Type System

**User Story:** As a server administrator, I want to define multiple generator types with different recipes and outputs, so that players have variety in their automation options.

#### Acceptance Criteria

1. THE Plugin_System SHALL support multiple predefined generator types beyond Iron_Farm
2. WHEN new generator types are added, THE Plugin_System SHALL load their recipes, outputs, and generation rates from configuration
3. THE Plugin_System SHALL validate that each generator type has a unique recipe pattern
4. WHEN a player crafts any generator type, THE Plugin_System SHALL create the appropriate generator with correct output and generation rate
5. THE Plugin_System SHALL prevent recipe conflicts between different generator types

### Requirement 8: Generator Interface and Storage

**User Story:** As a player, I want to interact with generators through an intuitive interface, so that I can easily collect accumulated resources.

#### Acceptance Criteria

1. WHEN a player right-clicks any generator block, THE Plugin_System SHALL open a custom inventory interface
2. THE Plugin_System SHALL display all accumulated resources in the generator's storage slots
3. THE Plugin_System SHALL show generation progress or time information in the interface
4. WHEN the generator's storage is full, THE Plugin_System SHALL stop generation until space is available
5. THE Plugin_System SHALL allow players to take items from the generator's storage like a normal chest

**User Story:** As a player, I want to interact with generators through an intuitive interface, so that I can easily manage resource generation and collection.

#### Acceptance Criteria

1. WHEN a player right-clicks any generator block, THE Plugin_System SHALL open a custom inventory interface
2. THE Plugin_System SHALL display the generator's internal storage slots in the interface
3. THE Plugin_System SHALL provide generation controls (buttons or clickable areas) within the interface
4. WHEN the generator's storage is full, THE Plugin_System SHALL prevent further generation and display a warning
5. THE Plugin_System SHALL allow players to take items from the generator's storage like a normal chest

### Requirement 9: Persistence and Data Management

**User Story:** As a server administrator, I want generator states and accumulated resources to persist across server restarts, so that players don't lose progress or stored items.

#### Acceptance Criteria

1. WHEN a generator is created, THE Plugin_System SHALL save its location, type, storage contents, and last access time to persistent storage
2. WHEN the server restarts, THE Plugin_System SHALL restore all generators and continue generation calculations from the saved timestamp
3. WHEN a generator is destroyed, THE Plugin_System SHALL remove its data and drop all stored items
4. THE Plugin_System SHALL handle data corruption gracefully and log any issues
5. THE Plugin_System SHALL maintain accurate time tracking across server restarts and chunk loading/unloading

**User Story:** As a server administrator, I want generator configurations and storage to persist across server restarts, so that players don't lose their setups or stored items.

#### Acceptance Criteria

1. WHEN a generator is created, THE Plugin_System SHALL save its location, type, and storage contents to persistent storage
2. WHEN the server restarts, THE Plugin_System SHALL restore all generators with their stored items
3. WHEN a generator is destroyed, THE Plugin_System SHALL remove its data and drop stored items
4. THE Plugin_System SHALL handle data corruption gracefully and log any issues
5. THE Plugin_System SHALL maintain data integrity for both generator configurations and stored items

### Requirement 10: Permission and Security

**User Story:** As a server administrator, I want to control who can create and use generators, so that I can manage server balance and permissions.

#### Acceptance Criteria

1. THE Plugin_System SHALL check permissions before allowing generator creation
2. THE Plugin_System SHALL check permissions before allowing generator usage
3. THE Plugin_System SHALL check permissions before allowing access to generator interfaces
4. WHEN a player lacks permissions, THE Plugin_System SHALL display appropriate error messages
5. THE Plugin_System SHALL integrate with standard Minecraft permission systems

### Requirement 11: Block Protection

**User Story:** As a player, I want my generators to be protected from other players, so that my devices and accumulated resources remain secure.

#### Acceptance Criteria

1. WHEN a generator is created, THE Plugin_System SHALL protect the generator block from unauthorized modification
2. WHEN a non-owner attempts to break a generator block, THE Plugin_System SHALL prevent the action
3. WHEN a non-owner attempts to access a generator interface, THE Plugin_System SHALL check permissions
4. WHERE a player has appropriate permissions, THE Plugin_System SHALL allow access to any generator
5. WHEN a generator owner removes their device, THE Plugin_System SHALL drop all stored items and remove protections