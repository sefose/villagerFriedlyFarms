package com.example.villagerfriendlyfarms.storage;

import com.example.villagerfriendlyfarms.VillagerFriendlyFarmsPlugin;
import com.example.villagerfriendlyfarms.config.JsonUtils;
import com.example.villagerfriendlyfarms.generator.GeneratorData;
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
    
    private final VillagerFriendlyFarmsPlugin plugin;
    private final Logger logger;
    private final ObjectMapper mapper;
    private final File dataDir;
    private final File backupDir;
    private final File generatorsFile;
    
    // Cache for loaded data
    private final Map<UUID, GeneratorData> dataCache = new ConcurrentHashMap<>();
    private volatile boolean initialized = false;
    
    // Synchronization object for file operations
    private final Object fileLock = new Object();

    public DataStorage(VillagerFriendlyFarmsPlugin plugin) {
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
        // Synchronize to prevent concurrent file operations
        synchronized (fileLock) {
            List<GeneratorData> generators = new ArrayList<>(dataCache.values());
            
            // Create backup before saving
            createBackup();
            
            // Write to temporary file first
            File tempFile = new File(dataDir, "generators.json.tmp");
            
            try {
                // Ensure the data directory exists
                if (!dataDir.exists()) {
                    dataDir.mkdirs();
                }
                
                // Debug: Log what we're trying to save (only in debug mode)
                if (plugin.getConfig().getBoolean("plugin.debug", false)) {
                    logger.info("Attempting to save " + generators.size() + " generators");
                    for (GeneratorData gen : generators) {
                        logger.info("Generator: " + gen.getId() + " type: " + gen.getGeneratorType() + 
                                   " items: " + gen.getTotalItemCount() + " location: " + 
                                   (gen.getLocation().getWorld() != null ? gen.getLocation().getWorld().getName() : "NULL_WORLD"));
                    }
                }
                
                // Try to serialize to string first to catch serialization errors early
                String jsonContent;
                try {
                    jsonContent = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(generators);
                    if (plugin.getConfig().getBoolean("plugin.debug", false)) {
                        logger.info("Successfully serialized " + generators.size() + " generators to JSON string (length: " + jsonContent.length() + ")");
                    }
                } catch (Exception serializeEx) {
                    logger.log(Level.SEVERE, "Failed to serialize generators to JSON string", serializeEx);
                    throw new IOException("Failed to serialize generator data", serializeEx);
                }
                
                // Now write the string to file
                try {
                    // Delete temp file if it exists
                    if (tempFile.exists()) {
                        tempFile.delete();
                    }
                    
                    // Create new temp file
                    if (!tempFile.createNewFile()) {
                        throw new IOException("Could not create temporary file: " + tempFile.getAbsolutePath());
                    }
                    
                    // Write content to file
                    java.nio.file.Files.write(tempFile.toPath(), jsonContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    
                    if (plugin.getConfig().getBoolean("plugin.debug", false)) {
                        logger.info("Successfully wrote JSON content to temporary file: " + tempFile.getAbsolutePath() + " (size: " + tempFile.length() + " bytes)");
                    }
                    
                } catch (Exception writeEx) {
                    logger.log(Level.SEVERE, "Failed to write JSON content to temporary file: " + tempFile.getAbsolutePath(), writeEx);
                    throw new IOException("Failed to write generator data to temporary file", writeEx);
                }
                
                // Verify the temporary file was created and has content
                if (!tempFile.exists()) {
                    throw new IOException("Temporary file was not created: " + tempFile.getAbsolutePath());
                }
                
                if (tempFile.length() == 0) {
                    throw new IOException("Temporary file is empty: " + tempFile.getAbsolutePath() + 
                                        " (Generator count: " + generators.size() + ")");
                }
                
                // Atomic move to final location
                Files.move(tempFile.toPath(), generatorsFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                
                if (plugin.getConfig().getBoolean("plugin.debug", false)) {
                    logger.info("Successfully saved " + generators.size() + " generators to storage");
                }
                
            } catch (IOException e) {
                // Clean up temporary file if it exists
                if (tempFile.exists()) {
                    try {
                        tempFile.delete();
                        if (plugin.getConfig().getBoolean("plugin.debug", false)) {
                            logger.info("Cleaned up temporary file after error");
                        }
                    } catch (Exception deleteEx) {
                        logger.log(Level.WARNING, "Failed to clean up temporary file", deleteEx);
                    }
                }
                throw e; // Re-throw the original exception
            }
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