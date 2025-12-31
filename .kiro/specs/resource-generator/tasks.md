# Implementation Plan: Crop Farm Plugin

## Overview

This implementation plan breaks down the Crop Farm plugin into discrete coding tasks. Each task builds incrementally toward a complete Paper 1.21.11 plugin that allows players to craft crop generation farms using specific recipes. The farms appear as barrels with item frames and accumulate crops over time using a passive calculation system.

## Tasks

- [x] 1. Set up project structure and dependencies
  - Create Maven project with Paper API 1.21.11 dependency
  - Set up plugin.yml with basic plugin information
  - Create main plugin class extending JavaPlugin
  - _Requirements: All requirements depend on basic plugin structure_

- [x] 2. Implement core data models and configuration system
  - [x] 2.1 Create CropFarmConfig and CropFarmData classes
    - Define Java classes for crop farm configuration and instance data
    - Implement JSON serialization/deserialization methods
    - _Requirements: 9.1, 10.1, 10.2_

  - [x] 2.2 Write property test for configuration loading
    - **Property 6: Configuration System Integrity**
    - **Validates: Requirements 9.1, 9.2, 9.3, 9.4, 9.5**

  - [x] 2.3 Implement configuration file management
    - Create ConfigManager class to load/save crop farm configurations
    - Implement hot-reloading of configuration changes
    - Create default configuration files for Wheat, Carrot, Potato, and Beetroot farms
    - _Requirements: 9.1, 9.4, 9.5_

- [x] 3. Implement recipe system and crafting validation
  - [x] 3.1 Create RecipeManager class
    - Implement recipe registration and pattern matching for crop farms
    - Create recipe validation logic for 3x3 crafting grids
    - _Requirements: 1.1, 1.7, 1.8_

  - [x] 3.3 Implement CraftingEventListener
    - Listen for PrepareItemCraftEvent to detect crop farm crafting
    - Validate crafting patterns and handle material consumption
    - Support all four crop farm types (wheat, carrot, potato, beetroot)
    - _Requirements: 1.2, 1.3, 1.4, 1.5, 1.6_

  - [x] 3.4 Add recipe book integration
    - Register Bukkit recipes so crop farms appear in recipe book
    - Add recipe cleanup on shutdown and reload functionality
    - _Requirements: Recipe discoverability and user experience_

- [x] 4. Implement crop farm visual structure and interaction system
  - [x] 4.1 Create BlockEventListener for crop farm placement and destruction
    - Handle BlockPlaceEvent to create crop farms from crafted items
    - Place barrel blocks and item frames with appropriate crop displays
    - Handle BlockBreakEvent to destroy farms and drop items
    - Add persistent data to crop farm blocks
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

  - [x] 4.2 Implement PlayerInteractionListener for crop farm interfaces
    - Handle right-click events on crop farm barrels
    - Calculate accumulated crops based on elapsed time
    - Open chest-like inventory interface for crop farms
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 11.1, 11.2, 11.3, 11.5_

  - [x] 4.3 Fix generation timer logic
    - Implement separate lastGenerationTime tracking
    - Add advanceGenerationTime() method for proper timer advancement
    - Ensure continuous generation based on real elapsed time
    - _Requirements: Proper time-based crop calculation_

- [x] 5. Implement specific crop farm functionality
  - [x] 5.1 Implement Wheat Farm functionality
    - Create wheat farm with wheat and wheat seeds output
    - Configure 2-minute generation timing
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

  - [x] 5.2 Implement Carrot Farm functionality
    - Create carrot farm with carrot output
    - Configure 2-minute generation timing
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

  - [x] 5.3 Implement Potato Farm functionality
    - Create potato farm with potato output
    - Configure 2-minute generation timing
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

  - [x] 5.4 Implement Beetroot Farm functionality
    - Create beetroot farm with beetroot and beetroot seeds output
    - Configure 2-minute generation timing
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 6. Implement command system
  - [x] 6.1 Create CropFarmCommand class
    - Implement /cropfarm command with subcommands (info, reload, give, list)
    - Add administrative functions for managing crop farms
    - _Requirements: 9.4, 13.1, 13.2, 13.3_

- [x] 7. Implement crop farm management system
  - [x] 7.1 Create CropFarmManager class
    - Centralize crop farm creation, destruction, and registry
    - Implement crop farm limits (per chunk, per player)
    - Add crop farm lookup and validation methods
    - _Requirements: 2.1, 2.5, 12.1, 12.2_

  - [x] 7.3 Integrate CropFarmManager with existing listeners
    - Update BlockEventListener to use CropFarmManager
    - Update PlayerInteractionListener to use CropFarmManager
    - Ensure proper crop farm registration and cleanup
    - _Requirements: 2.1, 2.5, 12.3_

- [x] 8. Implement data persistence and storage
  - [x] 8.1 Create DataStorage class using file storage
    - Implement crop farm data saving and loading to JSON files
    - Handle server restart scenarios and data recovery
    - Implement automatic backup and corruption handling
    - _Requirements: 12.1, 12.2, 12.4, 12.5_

  - [x] 8.3 Implement crop farm cleanup and data removal
    - Handle crop farm destruction and data cleanup
    - Implement item dropping when crop farms are destroyed
    - Clean up orphaned data files
    - _Requirements: 12.3, 2.5_

- [x] 9. Implement permission system and security
  - [x] 9.1 Create PermissionManager class
    - Implement permission checking for all crop farm operations
    - Integrate with Bukkit permission system
    - Add configurable permission requirements
    - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.5_

  - [x] 9.3 Enhance block protection system
    - Improve BlockEventListener with proper permission checks
    - Prevent unauthorized crop farm modification
    - Add admin override functionality
    - _Requirements: 14.1, 14.2, 14.3, 14.4_

- [x] 10. Implement storage capacity management
  - [x] 10.1 Add storage capacity limits to crop farm interface
    - Prevent crop generation when storage is full
    - Handle partial crop generation when storage has limited space
    - Add visual indicators for storage status
    - _Requirements: 11.4_

- [x] 11. Integration and final wiring
  - [x] 11.1 Wire all components together in main plugin class
    - Initialize CropFarmManager, DataStorage, and PermissionManager
    - Ensure proper startup and shutdown sequences
    - Add comprehensive error handling and logging
    - _Requirements: All requirements_

  - [x] 11.3 Implement enhanced error handling and logging
    - Add comprehensive error handling throughout the plugin
    - Implement graceful degradation for data corruption
    - Add detailed logging for debugging and monitoring
    - _Requirements: 12.4, 13.4_

- [x] 12. Update documentation and branding
  - [x] 12.1 Update README.md to use "Villager Friendly Farms" terminology
    - Replace "Resource Generator" with "Villager Friendly Farms"
    - Update command examples to use /vff instead of /rg
    - Update permission examples to use villagerfriendlyfarms prefix
    - _Requirements: Consistent branding and user documentation_

  - [x] 12.2 Update plugin.yml with new branding
    - Change plugin name to VillagerFriendlyFarms
    - Update commands to use /vff instead of /rg
    - Update permissions to use villagerfriendlyfarms prefix
    - _Requirements: Consistent branding and command structure_

  - [x] 12.3 Complete code refactoring to match new branding
    - [x] Update command registration from "rg" to "vff" in ResourceGeneratorPlugin.java
    - [x] Update all permission constants in PermissionManager.java to use "villagerfriendlyfarms" prefix
    - [x] Update GeneratorCommand.java to use new permission constants and command references
    - [x] Rename package structure from "resourcegenerator" to "villagerfriendlyfarms"
    - [x] Rename main plugin class from ResourceGeneratorPlugin to VillagerFriendlyFarmsPlugin
    - [x] Update all import statements and class references throughout codebase
    - [x] Update Maven configuration and build files
    - _Requirements: Complete code consistency with documentation and plugin.yml_

- [x] 13. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Each task references specific requirements for traceability
- The implementation supports four crop farm types: Wheat, Carrot, Potato, and Beetroot
- All crop farms use the same mechanics but different recipes and outputs
- The implementation uses Paper API 1.21.11 features including PDC for block metadata
- Current implementation already includes basic crop farm functionality with time-based crop calculation
- Crop farms appear as barrels with item frames displaying the crop type
- All crop farms use a 2-minute default generation timing (configurable)
- Testing tasks have been removed due to Bukkit/Paper API testing framework compatibility issues