# Requirements Document

## Introduction

A Minecraft Paper plugin that allows players to build crop generation farms. These farms appear as barrels with item frames in front displaying the crop type, and generate specific crops over time that players can collect through an inventory interface.

## Glossary

- **Crop_Farm**: A player-built device that appears as a barrel with an item frame and generates specific crops over time
- **Farm_Type**: A predefined crop farm type with a specific recipe and crop output (e.g., Wheat_Farm, Carrot_Farm)
- **Crafting_Grid**: A 3x3 area where players place items according to the farm recipe
- **Farm_Block**: The barrel block that represents the physical farm structure
- **Item_Frame**: The item frame placed in front of the barrel displaying the crop type
- **Farm_Interface**: The chest-like GUI that opens when players right-click the farm block
- **Plugin_System**: The overall Minecraft plugin managing all crop farms
- **Player**: A Minecraft player interacting with the plugin

## Requirements

### Requirement 1: Crop Farm Crafting and Creation

**User Story:** As a player, I want to craft specific crop farm types using defined recipes, so that I can build different crop generation farms.

#### Acceptance Criteria

1. WHEN a player arranges items in a 3x3 crafting pattern matching a crop farm recipe, THE Plugin_System SHALL recognize the valid farm type
2. WHEN a valid Wheat_Farm recipe is crafted (wheat seeds in top row, hoes in middle row, farmland in bottom row), THE Plugin_System SHALL create a Wheat_Farm
3. WHEN a valid Carrot_Farm recipe is crafted (carrots in top row, hoes in middle row, farmland in bottom row), THE Plugin_System SHALL create a Carrot_Farm
4. WHEN a valid Potato_Farm recipe is crafted (potatoes in top row, hoes in middle row, farmland in bottom row), THE Plugin_System SHALL create a Potato_Farm
5. WHEN a valid Beetroot_Farm recipe is crafted (beetroot seeds in top row, hoes in middle row, farmland in bottom row), THE Plugin_System SHALL create a Beetroot_Farm
6. WHEN a farm is successfully crafted, THE Plugin_System SHALL consume the crafting materials and activate the farm
7. WHEN an invalid recipe is attempted, THE Plugin_System SHALL not consume materials and provide feedback
8. THE Plugin_System SHALL validate that all required items are present in the correct positions before creating a farm

### Requirement 2: Crop Farm Visual Structure

**User Story:** As a player, I want my crop farms to have a clear visual representation showing what type of crop they produce, so that I can easily identify different farms.

#### Acceptance Criteria

1. WHEN a crop farm is created, THE Plugin_System SHALL place a barrel block at the specified location
2. WHEN a crop farm is created, THE Plugin_System SHALL place an item frame on the front face of the barrel
3. WHEN a crop farm is created, THE Plugin_System SHALL place the appropriate crop item in the item frame to indicate the farm type
4. THE Plugin_System SHALL protect both the barrel and item frame from unauthorized modification
5. WHEN a player breaks a crop farm, THE Plugin_System SHALL remove both the barrel and item frame and drop all stored items

### Requirement 3: Crop Farm Functionality

**User Story:** As a player, I want my crop farms to calculate and provide accumulated crops when I open them, so that I can collect crops that have been "generated" over time.

#### Acceptance Criteria

1. WHEN a crop farm is created, THE Plugin_System SHALL record the creation timestamp and begin generation calculations
2. WHEN a player right-clicks a farm barrel, THE Plugin_System SHALL calculate crops accumulated since last opening
3. WHEN calculating crops, THE Plugin_System SHALL use the configured generation rate for that farm type and time elapsed
4. THE Plugin_System SHALL add the calculated crops to the farm's storage before displaying the interface
5. THE Plugin_System SHALL respect storage limits and cap generation at maximum capacity

### Requirement 4: Wheat Farm Specific Functionality

**User Story:** As a player, I want my Wheat Farm to produce wheat and wheat seeds over time, so that I can automate wheat collection for food and breeding.

#### Acceptance Criteria

1. WHEN a Wheat_Farm is created, THE Plugin_System SHALL place wheat in the item frame
2. WHEN a player opens a Wheat_Farm, THE Plugin_System SHALL calculate wheat and seeds based on configured generation rate
3. THE Plugin_System SHALL produce wheat and wheat seeds as output resources for Wheat_Farm
4. THE Plugin_System SHALL use the Wheat_Farm specific generation timing (configurable, default 2 minutes per harvest)
5. THE Plugin_System SHALL store wheat products in the farm's internal storage with appropriate ratios

### Requirement 5: Carrot Farm Specific Functionality

**User Story:** As a player, I want my Carrot Farm to produce carrots over time, so that I can automate carrot collection for food and breeding.

#### Acceptance Criteria

1. WHEN a Carrot_Farm is created, THE Plugin_System SHALL place carrots in the item frame
2. WHEN a player opens a Carrot_Farm, THE Plugin_System SHALL calculate carrots based on configured generation rate
3. THE Plugin_System SHALL produce carrots as the output resource for Carrot_Farm
4. THE Plugin_System SHALL use the Carrot_Farm specific generation timing (configurable, default 2 minutes per harvest)
5. THE Plugin_System SHALL store carrots in the farm's internal storage

### Requirement 6: Potato Farm Specific Functionality

**User Story:** As a player, I want my Potato Farm to produce potatoes over time, so that I can automate potato collection for food and breeding.

#### Acceptance Criteria

1. WHEN a Potato_Farm is created, THE Plugin_System SHALL place potatoes in the item frame
2. WHEN a player opens a Potato_Farm, THE Plugin_System SHALL calculate potatoes based on configured generation rate
3. THE Plugin_System SHALL produce potatoes as the output resource for Potato_Farm
4. THE Plugin_System SHALL use the Potato_Farm specific generation timing (configurable, default 2 minutes per harvest)
5. THE Plugin_System SHALL store potatoes in the farm's internal storage

### Requirement 7: Beetroot Farm Specific Functionality

**User Story:** As a player, I want my Beetroot Farm to produce beetroot and beetroot seeds over time, so that I can automate beetroot collection for food and breeding.

#### Acceptance Criteria

1. WHEN a Beetroot_Farm is created, THE Plugin_System SHALL place beetroot in the item frame
2. WHEN a player opens a Beetroot_Farm, THE Plugin_System SHALL calculate beetroot and seeds based on configured generation rate
3. THE Plugin_System SHALL produce beetroot and beetroot seeds as output resources for Beetroot_Farm
4. THE Plugin_System SHALL use the Beetroot_Farm specific generation timing (configurable, default 2 minutes per harvest)
5. THE Plugin_System SHALL store beetroot products in the farm's internal storage with appropriate ratios

### Requirement 8: Automatic Generation System

**User Story:** As a player, I want crop farms to calculate accumulated crops only when I open them, so that the system is efficient and doesn't run continuous background processes.

#### Acceptance Criteria

1. WHEN a crop farm is created, THE Plugin_System SHALL record the creation time but perform no active generation
2. WHEN a player opens a farm interface, THE Plugin_System SHALL calculate time elapsed since last opening
3. WHEN calculating crops, THE Plugin_System SHALL use the configured generation rate to determine how many crops to add
4. THE Plugin_System SHALL add the calculated crops to the farm's storage upon opening
5. THE Plugin_System SHALL update the "last opened" timestamp after adding crops and reset the calculation timer

### Requirement 9: Configuration System

**User Story:** As a server administrator, I want to customize generation rates and storage limits for crop farms through configuration files, so that I can balance the plugin for my server.

#### Acceptance Criteria

1. THE Plugin_System SHALL load generation rates for each crop farm type from a configuration file
2. THE Plugin_System SHALL allow administrators to set different generation intervals for each crop type
3. THE Plugin_System SHALL support configurable storage limits for each farm type
4. WHEN configuration is changed, THE Plugin_System SHALL apply new settings without requiring server restart
5. THE Plugin_System SHALL provide default configuration values for all crop farm types

### Requirement 10: Farm Type System

**User Story:** As a server administrator, I want to define multiple crop farm types with different recipes and outputs, so that players have variety in their farming automation options.

#### Acceptance Criteria

1. THE Plugin_System SHALL support multiple predefined crop farm types (wheat, carrot, potato, beetroot)
2. WHEN new farm types are added, THE Plugin_System SHALL load their recipes, outputs, and generation rates from configuration
3. THE Plugin_System SHALL validate that each farm type has a unique recipe pattern
4. WHEN a player crafts any farm type, THE Plugin_System SHALL create the appropriate farm with correct output and generation rate
5. THE Plugin_System SHALL prevent recipe conflicts between different farm types

### Requirement 11: Farm Interface and Storage

**User Story:** As a player, I want to interact with crop farms through an intuitive interface, so that I can easily collect accumulated crops.

#### Acceptance Criteria

1. WHEN a player right-clicks any farm barrel, THE Plugin_System SHALL open a custom inventory interface
2. THE Plugin_System SHALL display all accumulated crops in the farm's storage slots
3. THE Plugin_System SHALL show farm information and generation progress in the interface
4. WHEN the farm's storage is full, THE Plugin_System SHALL stop generation until space is available
5. THE Plugin_System SHALL allow players to take crops from the farm's storage like a normal chest

### Requirement 12: Persistence and Data Management

**User Story:** As a server administrator, I want crop farm states and accumulated crops to persist across server restarts, so that players don't lose progress or stored crops.

#### Acceptance Criteria

1. WHEN a crop farm is created, THE Plugin_System SHALL save its location, type, storage contents, and last access time to persistent storage
2. WHEN the server restarts, THE Plugin_System SHALL restore all crop farms and continue generation calculations from the saved timestamp
3. WHEN a crop farm is destroyed, THE Plugin_System SHALL remove its data and drop all stored crops
4. THE Plugin_System SHALL handle data corruption gracefully and log any issues
5. THE Plugin_System SHALL maintain accurate time tracking across server restarts and chunk loading/unloading

### Requirement 13: Permission and Security

**User Story:** As a server administrator, I want to control who can create and use crop farms, so that I can manage server balance and permissions.

#### Acceptance Criteria

1. THE Plugin_System SHALL check permissions before allowing crop farm creation
2. THE Plugin_System SHALL check permissions before allowing crop farm usage
3. THE Plugin_System SHALL check permissions before allowing access to farm interfaces
4. WHEN a player lacks permissions, THE Plugin_System SHALL display appropriate error messages
5. THE Plugin_System SHALL integrate with standard Minecraft permission systems

### Requirement 14: Block Protection

**User Story:** As a player, I want my crop farms to be protected from other players, so that my farms and accumulated crops remain secure.

#### Acceptance Criteria

1. WHEN a crop farm is created, THE Plugin_System SHALL protect both the barrel and item frame from unauthorized modification
2. WHEN a non-owner attempts to break a farm barrel or item frame, THE Plugin_System SHALL prevent the action
3. WHEN a non-owner attempts to access a farm interface, THE Plugin_System SHALL check permissions
4. WHERE a player has appropriate permissions, THE Plugin_System SHALL allow access to any crop farm
5. WHEN a farm owner removes their device, THE Plugin_System SHALL drop all stored crops and remove protections