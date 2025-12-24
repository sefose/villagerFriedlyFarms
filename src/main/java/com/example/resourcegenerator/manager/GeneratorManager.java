package com.example.resourcegenerator.manager;

import com.example.resourcegenerator.ResourceGeneratorPlugin;
import com.example.resourcegenerator.config.GeneratorConfig;
import com.example.resourcegenerator.generator.GeneratorData;
import com.example.resourcegenerator.storage.DataStorage;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Central manager for all generator operations.
 * Handles generator creation, destruction, registry, and validation.
 */
public class GeneratorManager {
    
    private final ResourceGeneratorPlugin plugin;
    private final Logger logger;
    private final DataStorage dataStorage;
    
    // Generator registry - maps generator ID to generator data
    private final Map<UUID, GeneratorData> generators = new ConcurrentHashMap<>();
    
    // Location index - maps location to generator ID for fast lookup
    private final Map<String, UUID> locationIndex = new ConcurrentHashMap<>();
    
    // Player index - maps player UUID to set of generator IDs they own
    private final Map<UUID, Set<UUID>> playerIndex = new ConcurrentHashMap<>();
    
    // Chunk index - maps chunk key to set of generator IDs in that chunk
    private final Map<String, Set<UUID>> chunkIndex = new ConcurrentHashMap<>();

    public GeneratorManager(ResourceGeneratorPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.dataStorage = new DataStorage(plugin);
    }

    /**
     * Initializes the generator manager.
     */
    public void initialize() {
        // Initialize data storage
        dataStorage.initialize();
        
        // Load existing generators from storage
        Collection<GeneratorData> storedGenerators = dataStorage.loadAllGenerators();
        for (GeneratorData generator : storedGenerators) {
            registerGenerator(generator);
        }
        
        logger.info("Generator manager initialized with " + generators.size() + " generators");
    }

    /**
     * Creates a new generator at the specified location.
     * @param location The location for the generator
     * @param generatorType The type of generator to create
     * @param owner The owner of the generator
     * @return The created generator data, or null if creation failed
     */
    public GeneratorData createGenerator(Location location, String generatorType, UUID owner) {
        if (location == null || generatorType == null || owner == null) {
            return null;
        }

        // Check if a generator already exists at this location
        if (hasGeneratorAt(location)) {
            logger.warning("Attempted to create generator at occupied location: " + formatLocation(location));
            return null;
        }

        // Check generator limits
        if (!canCreateGenerator(location, owner)) {
            return null;
        }

        // Create the generator data
        GeneratorData generator = GeneratorData.create(location, generatorType, owner);
        
        // Register the generator
        registerGenerator(generator);
        
        // Save to persistent storage
        dataStorage.saveGenerator(generator);
        
        logger.info("Created generator: " + generatorType + " at " + formatLocation(location) + 
                   " for player " + owner);
        
        return generator;
    }

    /**
     * Destroys a generator and removes it from all registries.
     * @param generatorId The ID of the generator to destroy
     * @return The destroyed generator data, or null if not found
     */
    public GeneratorData destroyGenerator(UUID generatorId) {
        GeneratorData generator = generators.remove(generatorId);
        if (generator == null) {
            return null;
        }

        // Remove from location index
        String locationKey = getLocationKey(generator.getLocation());
        locationIndex.remove(locationKey);

        // Remove from player index
        Set<UUID> playerGenerators = playerIndex.get(generator.getOwner());
        if (playerGenerators != null) {
            playerGenerators.remove(generatorId);
            if (playerGenerators.isEmpty()) {
                playerIndex.remove(generator.getOwner());
            }
        }

        // Remove from chunk index
        String chunkKey = getChunkKey(generator.getLocation());
        Set<UUID> chunkGenerators = chunkIndex.get(chunkKey);
        if (chunkGenerators != null) {
            chunkGenerators.remove(generatorId);
            if (chunkGenerators.isEmpty()) {
                chunkIndex.remove(chunkKey);
            }
        }

        // Remove from persistent storage
        dataStorage.removeGenerator(generatorId);

        logger.info("Destroyed generator: " + generator.getGeneratorType() + 
                   " at " + formatLocation(generator.getLocation()));
        
        return generator;
    }

    /**
     * Registers a generator in all indexes.
     * @param generator The generator to register
     */
    private void registerGenerator(GeneratorData generator) {
        UUID id = generator.getId();
        
        // Add to main registry
        generators.put(id, generator);
        
        // Add to location index
        String locationKey = getLocationKey(generator.getLocation());
        locationIndex.put(locationKey, id);
        
        // Add to player index
        playerIndex.computeIfAbsent(generator.getOwner(), k -> new HashSet<>()).add(id);
        
        // Add to chunk index
        String chunkKey = getChunkKey(generator.getLocation());
        chunkIndex.computeIfAbsent(chunkKey, k -> new HashSet<>()).add(id);
    }

    /**
     * Gets a generator by its ID.
     * @param generatorId The generator ID
     * @return The generator data, or null if not found
     */
    public GeneratorData getGenerator(UUID generatorId) {
        return generators.get(generatorId);
    }

    /**
     * Gets a generator at the specified location.
     * @param location The location to check
     * @return The generator data, or null if none exists
     */
    public GeneratorData getGeneratorAt(Location location) {
        String locationKey = getLocationKey(location);
        UUID generatorId = locationIndex.get(locationKey);
        return generatorId != null ? generators.get(generatorId) : null;
    }

    /**
     * Checks if a generator exists at the specified location.
     * @param location The location to check
     * @return True if a generator exists at this location
     */
    public boolean hasGeneratorAt(Location location) {
        return getGeneratorAt(location) != null;
    }

    /**
     * Gets all generators owned by a player.
     * @param playerUuid The player's UUID
     * @return A list of generator data owned by the player
     */
    public List<GeneratorData> getPlayerGenerators(UUID playerUuid) {
        Set<UUID> generatorIds = playerIndex.get(playerUuid);
        if (generatorIds == null) {
            return new ArrayList<>();
        }

        List<GeneratorData> result = new ArrayList<>();
        for (UUID id : generatorIds) {
            GeneratorData generator = generators.get(id);
            if (generator != null) {
                result.add(generator);
            }
        }
        return result;
    }

    /**
     * Gets all generators in a specific chunk.
     * @param location Any location in the chunk
     * @return A list of generators in that chunk
     */
    public List<GeneratorData> getChunkGenerators(Location location) {
        String chunkKey = getChunkKey(location);
        Set<UUID> generatorIds = chunkIndex.get(chunkKey);
        if (generatorIds == null) {
            return new ArrayList<>();
        }

        List<GeneratorData> result = new ArrayList<>();
        for (UUID id : generatorIds) {
            GeneratorData generator = generators.get(id);
            if (generator != null) {
                result.add(generator);
            }
        }
        return result;
    }

    /**
     * Gets the total number of generators.
     * @return The total generator count
     */
    public int getTotalGeneratorCount() {
        return generators.size();
    }

    /**
     * Gets the number of generators owned by a player.
     * @param playerUuid The player's UUID
     * @return The number of generators owned by the player
     */
    public int getPlayerGeneratorCount(UUID playerUuid) {
        Set<UUID> generatorIds = playerIndex.get(playerUuid);
        return generatorIds != null ? generatorIds.size() : 0;
    }

    /**
     * Gets the number of generators in a chunk.
     * @param location Any location in the chunk
     * @return The number of generators in that chunk
     */
    public int getChunkGeneratorCount(Location location) {
        String chunkKey = getChunkKey(location);
        Set<UUID> generatorIds = chunkIndex.get(chunkKey);
        return generatorIds != null ? generatorIds.size() : 0;
    }

    /**
     * Checks if a generator can be created at the specified location.
     * @param location The proposed location
     * @param owner The proposed owner
     * @return True if the generator can be created
     */
    public boolean canCreateGenerator(Location location, UUID owner) {
        // Check chunk limit
        int maxPerChunk = plugin.getConfig().getInt("performance.max-generators-per-chunk", 10);
        if (getChunkGeneratorCount(location) >= maxPerChunk) {
            logger.info("Cannot create generator: chunk limit reached (" + maxPerChunk + ")");
            return false;
        }

        // Check player limit
        int maxPerPlayer = plugin.getConfig().getInt("performance.max-generators-per-player", 50);
        if (getPlayerGeneratorCount(owner) >= maxPerPlayer) {
            logger.info("Cannot create generator: player limit reached (" + maxPerPlayer + ")");
            return false;
        }

        return true;
    }

    /**
     * Updates a generator's data.
     * @param generator The updated generator data
     */
    public void updateGenerator(GeneratorData generator) {
        if (generator != null && generators.containsKey(generator.getId())) {
            generators.put(generator.getId(), generator);
            // Save to persistent storage
            dataStorage.saveGenerator(generator);
        }
    }

    /**
     * Gets all generators.
     * @return A copy of all generator data
     */
    public Collection<GeneratorData> getAllGenerators() {
        return new ArrayList<>(generators.values());
    }

    /**
     * Clears all generators (used for shutdown/cleanup).
     */
    public void clearAll() {
        generators.clear();
        locationIndex.clear();
        playerIndex.clear();
        chunkIndex.clear();
        logger.info("Cleared all generator data");
    }

    /**
     * Shuts down the generator manager and saves all data.
     */
    public void shutdown() {
        logger.info("Shutting down generator manager");
        dataStorage.shutdown();
        clearAll();
    }

    /**
     * Gets the data storage instance.
     * @return The data storage
     */
    public DataStorage getDataStorage() {
        return dataStorage;
    }

    /**
     * Creates a location key for indexing.
     * @param location The location
     * @return A string key representing the location
     */
    private String getLocationKey(Location location) {
        return location.getWorld().getName() + ":" + 
               location.getBlockX() + ":" + 
               location.getBlockY() + ":" + 
               location.getBlockZ();
    }

    /**
     * Creates a chunk key for indexing.
     * @param location Any location in the chunk
     * @return A string key representing the chunk
     */
    private String getChunkKey(Location location) {
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        return location.getWorld().getName() + ":" + chunkX + ":" + chunkZ;
    }

    /**
     * Formats a location for logging.
     * @param location The location
     * @return A formatted location string
     */
    private String formatLocation(Location location) {
        return String.format("%s(%d,%d,%d)", 
            location.getWorld().getName(),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ());
    }
}