# Implementation Plan: Resource Generator Plugin

## Overview

This implementation plan breaks down the resource generator plugin into discrete coding tasks. Each task builds incrementally toward a complete Paper 1.21.11 plugin that allows players to craft generator devices using specific recipes. The generators appear as custom blocks and accumulate resources over time using a passive calculation system.

## Tasks

- [x] 1. Set up project structure and dependencies
  - Create Maven project with Paper API 1.21.11 dependency
  - Set up plugin.yml with basic plugin information
  - Create main plugin class extending JavaPlugin
  - _Requirements: All requirements depend on basic plugin structure_

- [x] 2. Implement core data models and configuration system
  - [x] 2.1 Create GeneratorConfig and GeneratorData classes
    - Define Java classes for generator configuration and instance data
    - Implement JSON serialization/deserialization methods
    - _Requirements: 6.1, 7.1, 7.2_

  - [ ]* 2.2 Write property test for configuration loading
    - **Property 6: Configuration System Integrity**
    - **Validates: Requirements 6.1, 6.2, 6.3, 6.4, 6.5, 7.2**

  - [x] 2.3 Implement configuration file management
    - Create ConfigManager class to load/save generator configurations
    - Implement hot-reloading of configuration changes
    - Create default configuration files for Iron Farm and Villager Breeder
    - _Requirements: 6.1, 6.4, 6.5_

  - [ ]* 2.4 Write unit tests for configuration management
    - Test configuration loading, saving, and hot-reloading
    - Test default configuration creation
    - _Requirements: 6.1, 6.4, 6.5_

- [x] 3. Implement recipe system and crafting validation
  - [x] 3.1 Create RecipeManager class
    - Implement recipe registration and pattern matching
    - Create recipe validation logic for 3x3 crafting grids
    - _Requirements: 1.1, 1.6, 7.3_

  - [ ]* 3.2 Write property test for recipe pattern recognition
    - **Property 1: Recipe Pattern Recognition**
    - **Validates: Requirements 1.1, 1.6, 7.3**

  - [x] 3.3 Implement CraftingEventListener
    - Listen for PrepareItemCraftEvent to detect generator crafting
    - Validate crafting patterns and handle material consumption
    - _Requirements: 1.2, 1.4, 1.5_

  - [ ]* 3.4 Write property test for valid crafting behavior
    - **Property 2: Valid Crafting Behavior**
    - **Validates: Requirements 1.4**

  - [ ]* 3.5 Write property test for invalid crafting protection
    - **Property 3: Invalid Crafting Protection**
    - **Validates: Requirements 1.5**

- [x] 4. Implement block placement and interaction system
  - [x] 4.1 Create BlockEventListener for generator placement and destruction
    - Handle BlockPlaceEvent to create generators from crafted items
    - Handle BlockBreakEvent to destroy generators and drop items
    - Add persistent data to generator blocks
    - _Requirements: 2.1, 11.1, 11.2_

  - [x] 4.2 Implement PlayerInteractionListener for generator interfaces
    - Handle right-click events on generator blocks
    - Calculate accumulated resources based on elapsed time
    - Open chest-like inventory interface for generators
    - _Requirements: 2.2, 2.3, 2.4, 8.1, 8.2, 8.5_

  - [ ]* 4.3 Write property test for time-based resource calculation
    - **Property 4: Time-Based Resource Calculation**
    - **Validates: Requirements 2.2, 2.3, 2.4, 3.2, 3.3, 4.2, 4.3, 4.4**

  - [ ]* 4.4 Write property test for generator interface consistency
    - **Property 7: Generator Interface Consistency**
    - **Validates: Requirements 8.1, 8.2, 8.5**

- [x] 5. Implement command system
  - [x] 5.1 Create GeneratorCommand class
    - Implement /rg command with subcommands (info, reload, give, list)
    - Add administrative functions for managing generators
    - _Requirements: 6.4, 10.1, 10.2, 10.3_

- [ ] 6. Implement generator management system
  - [x] 6.1 Create GeneratorManager class
    - Centralize generator creation, destruction, and registry
    - Implement generator limits (per chunk, per player)
    - Add generator lookup and validation methods
    - _Requirements: 2.1, 2.5, 9.1, 9.2_

  - [ ]* 6.2 Write property test for passive generation system
    - **Property 5: Passive Generation System**
    - **Validates: Requirements 5.1, 5.4, 5.5**

  - [x] 6.3 Integrate GeneratorManager with existing listeners
    - Update BlockEventListener to use GeneratorManager
    - Update PlayerInteractionListener to use GeneratorManager
    - Ensure proper generator registration and cleanup
    - _Requirements: 2.1, 2.5, 9.3_

- [ ] 7. Implement data persistence and storage
  - [x] 7.1 Create DataStorage class using file storage
    - Implement generator data saving and loading to JSON files
    - Handle server restart scenarios and data recovery
    - Implement automatic backup and corruption handling
    - _Requirements: 9.1, 9.2, 9.4, 9.5_

  - [ ]* 7.2 Write property test for data persistence round-trip
    - **Property 9: Data Persistence Round-Trip**
    - **Validates: Requirements 9.1, 9.2, 9.5**

  - [x] 7.3 Implement generator cleanup and data removal
    - Handle generator destruction and data cleanup
    - Implement item dropping when generators are destroyed
    - Clean up orphaned data files
    - _Requirements: 9.3, 11.5**

  - [ ]* 7.4 Write property test for generator cleanup consistency
    - **Property 10: Generator Cleanup Consistency**
    - **Validates: Requirements 9.3, 11.5**

- [ ] 8. Implement permission system and security
  - [x] 8.1 Create PermissionManager class
    - Implement permission checking for all generator operations
    - Integrate with Bukkit permission system
    - Add configurable permission requirements
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

  - [ ]* 8.2 Write property test for permission-based access control
    - **Property 11: Permission-Based Access Control**
    - **Validates: Requirements 10.1, 10.2, 10.3, 11.3, 11.4**

  - [x] 8.3 Enhance block protection system
    - Improve BlockEventListener with proper permission checks
    - Prevent unauthorized generator modification
    - Add admin override functionality
    - _Requirements: 11.1, 11.2, 11.3, 11.4**

  - [ ]* 8.4 Write property test for block protection enforcement
    - **Property 12: Block Protection Enforcement**
    - **Validates: Requirements 11.1, 11.2**

- [ ] 9. Implement storage capacity management
  - [x] 9.1 Add storage capacity limits to generator interface
    - Prevent resource generation when storage is full
    - Handle partial resource generation when storage has limited space
    - Add visual indicators for storage status
    - _Requirements: 8.4_

  - [ ]* 9.2 Write property test for storage capacity management
    - **Property 8: Storage Capacity Management**
    - **Validates: Requirements 8.4**

- [ ] 10. Integration and final wiring
  - [x] 10.1 Wire all components together in main plugin class
    - Initialize GeneratorManager, DataStorage, and PermissionManager
    - Ensure proper startup and shutdown sequences
    - Add comprehensive error handling and logging
    - _Requirements: All requirements_

  - [ ]* 10.2 Write integration tests
    - Test end-to-end generator creation and usage flows
    - Test server restart scenarios
    - Test permission and security features
    - _Requirements: All requirements_

  - [x] 10.3 Implement enhanced error handling and logging
    - Add comprehensive error handling throughout the plugin
    - Implement graceful degradation for data corruption
    - Add detailed logging for debugging and monitoring
    - _Requirements: 9.4, 10.4_

- [x] 11. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Property tests validate universal correctness properties using jqwik framework
- Unit tests validate specific examples and edge cases
- The implementation supports two generator types: Iron Farm and Villager Breeder
- Both generators use the same mechanics but different recipes and outputs
- The implementation uses Paper API 1.21.11 features including PDC for block metadata
- Current implementation already includes basic generator functionality with time-based resource calculation