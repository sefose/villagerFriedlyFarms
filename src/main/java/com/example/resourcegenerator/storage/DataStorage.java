package com.example.resourcegenerator.storage;

import com.example.resourcegenerator.ResourceGeneratorPlugin;
import com.example.resourcegenerator.config.JsonUtils;
import com.example.resourcegenerator.generator.GeneratorData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles persistent storage of generator data using JSON files.
 * Provides automatic backup, corruption recovery, and async operations.
 */
public class DataStorage {
    
    private final ResourceGeneratorPlugin plugin;
    private final Logger logger;
    private final ObjectMapper mapper;
    private final File dataDir;
    private final File backupDir;
    private final File generatorsFile;
    
    // Cache for loaded data
    private final Map<UUID, GeneratorData> dataCache = new ConcurrentHashMap<>();
    private volatile boolean initialized = false;

    public DataStorage(ResourceGeneratorPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.mapper = JsonUtils.getMapper();
        this.dataDir = new File(plugin.getDataFolder(), "data");
        this.backupDir = new File(plugin.getDataFolder(), "backups");
        this.generatorsFile = new File(dataDir, "generators.json");
    }

    /**
     * Initializes the data storage system.
     */
    public void initialize() {
        try {
            // Create directories
            if (!dataDir.exists()) {
                dataDir.mkdirs();
                logger.info("Created data directory");
            }
            
            if (!backupDir.exists()) {
                backupDir.mkdirs();
                logger.info("Created backup directory");
            }

            // Load existing data
            loadAllGenerators();
            
            // Schedule periodic backups
            schedulePeriodicBackups();
            
            initialized = true;
            logger.info("Data storage initialized with " + dataCache.size() + " generators");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize data storage", e);
        }
    }

    /**
     * Saves a generator to persistent storage.
     * @param generator The generator to save
     * @return A future that completes when the save is done
     */
    public CompletableFuture<Boolean> saveGenerator(GeneratorData generator) {
        if (!initialized || generator == null) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Update cache
                dataCache.put(generator.getId(), generator);
                
                // Save to file
                saveAllGenerators();
                
                if (plugin.getConfig().getBoolean("plugin.debug", false)) {
                    logger.info("Saved generator: " + generator.getId());
                }
                
                return true;
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to save generator: " + generator.getId(), e);
                return false;
            }
        });
    }

    /**
     * Loads a generator from persistent storage.
     * @param generatorId The generator ID
     * @return The generator data, or null if not found
     */
    public GeneratorData loadGenerator(UUID generatorId) {
        if (!initialized || generatorId == null) {
            return null;
        }

        return dataCache.get(generatorId);
    }

    /**
     * Removes a generator from persistent storage.
     * @param generatorId The generator ID to remove
     * @return A future that completes when the removal is done
     */
    public CompletableFuture<Boolean> removeGenerator(UUID generatorId) {
        if (!initialized || generatorId == null) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Remove from cache
                GeneratorData removed = dataCache.remove(generatorId);
                
                if (removed != null) {
                    // Save updated data
                    saveAllGenerators();
                    
                    if (plugin.getConfig().getBoolean("plugin.debug", false)) {
                        logger.info("Removed generator: " + generatorId);
                    }
                    
                    return true;
                }
                
                return false;
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to remove generator: " + generatorId, e);
                return false;
            }
        });
    }

    /**
     * Loads all generators from persistent storage.
     * @return A collection of all generator data
     */
    public Collection<GeneratorData> loadAllGenerators() {
        if (!initialized) {
            return new ArrayList<>();
        }

        try {
            if (!generatorsFile.exists()) {
                logger.info("No existing generator data file found");
                return new ArrayList<>();
            }

            // Try to load from main file
            List<GeneratorData> generators = loadGeneratorsFromFile(generatorsFile);
            
            if (generators == null) {
                // Main file corrupted, try backup
                logger.warning("Main generator file corrupted, attempting backup recovery");
                generators = loadFromBackup();
                
                if (generators == null) {
                    logger.severe("All generator data files corrupted, starting fresh");
                    generators = new ArrayList<>();
                }
            }

            // Update cache
            dataCache.clear();
            for (GeneratorData generator : generators) {
                dataCache.put(generator.getId(), generator);
            }

            logger.info("Loaded " + generators.size() + " generators from storage");
            return new ArrayList<>(generators);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to load generator data", e);
            return new ArrayList<>();
        }
    }

    /**
     * Saves all generators to persistent storage.
     */
    private void saveAllGenerators() throws IOException {
        List<GeneratorData> generators = new ArrayList<>(dataCache.values());
        
        // Create backup before saving
        createBackup();
        
        // Write to temporary file first
        File tempFile = new File(dataDir, "generators.json.tmp");
        mapper.writerWithDefaultPrettyPrinter().writeValue(tempFile, generators);
        
        // Atomic move to final location
        Files.move(tempFile.toPath(), generatorsFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        
        if (plugin.getConfig().getBoolean("plugin.debug", false)) {
            logger.info("Saved " + generators.size() + " generators to storage");
        }
    }

    /**
     * Loads generators from a specific file.
     * @param file The file to load from
     * @return The list of generators, or null if corrupted
     */
    private List<GeneratorData> loadGeneratorsFromFile(File file) {
        try {
            CollectionType listType = mapper.getTypeFactory()
                .constructCollectionType(List.class, GeneratorData.class);
            return mapper.readValue(file, listType);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to load generators from file: " + file.getName(), e);
            return null;
        }
    }

    /**
     * Creates a backup of the current generator data.
     */
    private void createBackup() {
        try {
            if (!generatorsFile.exists()) {
                return;
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            File backupFile = new File(backupDir, "generators_" + timestamp + ".json");
            
            Files.copy(generatorsFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            // Clean up old backups (keep last 10)
            cleanupOldBackups();
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to create backup", e);
        }
    }

    /**
     * Attempts to load from the most recent backup.
     * @return The loaded generators, or null if no valid backup found
     */
    private List<GeneratorData> loadFromBackup() {
        File[] backupFiles = backupDir.listFiles((dir, name) -> name.startsWith("generators_") && name.endsWith(".json"));
        
        if (backupFiles == null || backupFiles.length == 0) {
            return null;
        }

        // Sort by modification time (newest first)
        Arrays.sort(backupFiles, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));

        // Try each backup file until we find a valid one
        for (File backupFile : backupFiles) {
            List<GeneratorData> generators = loadGeneratorsFromFile(backupFile);
            if (generators != null) {
                logger.info("Successfully recovered from backup: " + backupFile.getName());
                return generators;
            }
        }

        return null;
    }

    /**
     * Cleans up old backup files, keeping only the most recent ones.
     */
    private void cleanupOldBackups() {
        try {
            File[] backupFiles = backupDir.listFiles((dir, name) -> name.startsWith("generators_") && name.endsWith(".json"));
            
            if (backupFiles == null || backupFiles.length <= 10) {
                return; // Keep all if 10 or fewer
            }

            // Sort by modification time (newest first)
            Arrays.sort(backupFiles, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));

            // Delete files beyond the first 10
            for (int i = 10; i < backupFiles.length; i++) {
                if (backupFiles[i].delete()) {
                    logger.fine("Deleted old backup: " + backupFiles[i].getName());
                }
            }

        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to cleanup old backups", e);
        }
    }

    /**
     * Schedules periodic backups.
     */
    private void schedulePeriodicBackups() {
        int intervalMinutes = plugin.getConfig().getInt("plugin.auto-save-interval", 5);
        long intervalTicks = intervalMinutes * 60 * 20; // Convert minutes to ticks

        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try {
                if (!dataCache.isEmpty()) {
                    saveAllGenerators();
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed during periodic backup", e);
            }
        }, intervalTicks, intervalTicks);

        logger.info("Scheduled periodic backups every " + intervalMinutes + " minutes");
    }

    /**
     * Performs a manual backup of all data.
     * @return A future that completes when the backup is done
     */
    public CompletableFuture<Boolean> performBackup() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                createBackup();
                logger.info("Manual backup completed successfully");
                return true;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Manual backup failed", e);
                return false;
            }
        });
    }

    /**
     * Gets the total number of stored generators.
     * @return The number of generators in storage
     */
    public int getStoredGeneratorCount() {
        return dataCache.size();
    }

    /**
     * Checks if a generator exists in storage.
     * @param generatorId The generator ID
     * @return True if the generator exists
     */
    public boolean hasGenerator(UUID generatorId) {
        return initialized && dataCache.containsKey(generatorId);
    }

    /**
     * Gets all generator IDs in storage.
     * @return A set of all generator IDs
     */
    public Set<UUID> getAllGeneratorIds() {
        return new HashSet<>(dataCache.keySet());
    }

    /**
     * Shuts down the data storage system and saves all pending data.
     */
    public void shutdown() {
        if (!initialized) {
            return;
        }

        try {
            logger.info("Shutting down data storage, saving " + dataCache.size() + " generators");
            saveAllGenerators();
            dataCache.clear();
            initialized = false;
            logger.info("Data storage shutdown complete");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to shutdown data storage cleanly", e);
        }
    }

    /**
     * Validates the integrity of stored data.
     * @return True if all data is valid
     */
    public boolean validateDataIntegrity() {
        try {
            int validCount = 0;
            int invalidCount = 0;

            for (GeneratorData generator : dataCache.values()) {
                if (isValidGeneratorData(generator)) {
                    validCount++;
                } else {
                    invalidCount++;
                    logger.warning("Invalid generator data found: " + generator.getId());
                }
            }

            logger.info("Data integrity check: " + validCount + " valid, " + invalidCount + " invalid generators");
            return invalidCount == 0;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to validate data integrity", e);
            return false;
        }
    }

    /**
     * Validates a single generator data object.
     * @param generator The generator to validate
     * @return True if the generator data is valid
     */
    private boolean isValidGeneratorData(GeneratorData generator) {
        return generator != null &&
               generator.getId() != null &&
               generator.getLocation() != null &&
               generator.getGeneratorType() != null &&
               !generator.getGeneratorType().trim().isEmpty() &&
               generator.getOwner() != null &&
               generator.getLastAccessedTime() > 0;
    }
}