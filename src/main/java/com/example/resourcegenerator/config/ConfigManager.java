package com.example.resourcegenerator.config;

import com.example.resourcegenerator.ResourceGeneratorPlugin;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages loading, saving, and hot-reloading of generator configurations.
 */
public class ConfigManager {
    
    private final ResourceGeneratorPlugin plugin;
    private final Logger logger;
    private final ObjectMapper mapper;
    private final File configDir;
    private final Map<String, GeneratorConfig> generatorConfigs;

    public ConfigManager(ResourceGeneratorPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.mapper = JsonUtils.getMapper();
        this.configDir = new File(plugin.getDataFolder(), "generators");
        this.generatorConfigs = new HashMap<>();
    }

    /**
     * Initializes the configuration system.
     * Creates directories and loads all generator configurations.
     */
    public void initialize() {
        try {
            // Create plugin data directory if it doesn't exist
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            // Create generators config directory
            if (!configDir.exists()) {
                configDir.mkdirs();
                logger.info("Created generators configuration directory");
            }

            // Load existing configurations
            loadAllConfigurations();

            // Create default configurations if none exist
            if (generatorConfigs.isEmpty()) {
                createDefaultConfigurations();
                logger.info("Created default generator configurations");
            }

            logger.info("Loaded " + generatorConfigs.size() + " generator configurations");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize configuration system", e);
        }
    }

    /**
     * Loads all generator configurations from the config directory.
     */
    public void loadAllConfigurations() {
        generatorConfigs.clear();

        if (!configDir.exists()) {
            return;
        }

        File[] configFiles = configDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (configFiles == null) {
            return;
        }

        for (File configFile : configFiles) {
            try {
                GeneratorConfig config = mapper.readValue(configFile, GeneratorConfig.class);
                generatorConfigs.put(config.getName(), config);
                logger.info("Loaded generator config: " + config.getName());
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to load config file: " + configFile.getName(), e);
            }
        }
    }

    /**
     * Saves a generator configuration to file.
     * @param config The configuration to save
     */
    public void saveConfiguration(GeneratorConfig config) {
        try {
            File configFile = new File(configDir, config.getName() + ".json");
            mapper.writerWithDefaultPrettyPrinter().writeValue(configFile, config);
            generatorConfigs.put(config.getName(), config);
            logger.info("Saved generator config: " + config.getName());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save config: " + config.getName(), e);
        }
    }

    /**
     * Gets a generator configuration by name.
     * @param name The generator name
     * @return The configuration, or null if not found
     */
    public GeneratorConfig getGeneratorConfig(String name) {
        return generatorConfigs.get(name);
    }

    /**
     * Gets all loaded generator configurations.
     * @return A copy of the configurations map
     */
    public Map<String, GeneratorConfig> getAllConfigurations() {
        return new HashMap<>(generatorConfigs);
    }

    /**
     * Checks if a generator configuration exists.
     * @param name The generator name
     * @return True if the configuration exists
     */
    public boolean hasConfiguration(String name) {
        return generatorConfigs.containsKey(name);
    }

    /**
     * Reloads all configurations from disk.
     * This allows for hot-reloading without server restart.
     */
    public void reloadConfigurations() {
        logger.info("Reloading generator configurations...");
        loadAllConfigurations();
        logger.info("Reloaded " + generatorConfigs.size() + " generator configurations");
    }

    /**
     * Validates that all generator configurations have unique recipe patterns.
     * @return True if all recipes are unique
     */
    public boolean validateUniqueRecipes() {
        Map<String, String> recipeHashes = new HashMap<>();
        
        for (GeneratorConfig config : generatorConfigs.values()) {
            String recipeHash = calculateRecipeHash(config.getRecipe());
            
            if (recipeHashes.containsKey(recipeHash)) {
                logger.severe("Recipe conflict detected between generators: " + 
                             config.getName() + " and " + recipeHashes.get(recipeHash));
                return false;
            }
            
            recipeHashes.put(recipeHash, config.getName());
        }
        
        return true;
    }

    /**
     * Finds a generator configuration that matches the given crafting grid.
     * @param craftingGrid The 3x3 crafting grid to match
     * @return The matching configuration, or null if none found
     */
    public GeneratorConfig findMatchingRecipe(ItemStack[] craftingGrid) {
        for (GeneratorConfig config : generatorConfigs.values()) {
            if (config.matchesRecipe(craftingGrid)) {
                return config;
            }
        }
        return null;
    }

    /**
     * Creates default generator configurations.
     */
    private void createDefaultConfigurations() {
        // Create Iron Farm configuration
        GeneratorConfig ironFarm = createIronFarmConfig();
        saveConfiguration(ironFarm);
        
        // Create Villager Breeder configuration
        GeneratorConfig villagerBreeder = createVillagerBreederConfig();
        saveConfiguration(villagerBreeder);
    }

    /**
     * Creates the default Iron Farm configuration.
     * Recipe:
     * BED BED BED
     * COM HOP COM
     * LAV CHE WAT
     */
    private GeneratorConfig createIronFarmConfig() {
        ItemStack[] recipe = new ItemStack[9];
        
        // Top row: 3 beds
        recipe[0] = new ItemStack(Material.RED_BED, 1);
        recipe[1] = new ItemStack(Material.RED_BED, 1);
        recipe[2] = new ItemStack(Material.RED_BED, 1);
        
        // Middle row: composter, hopper, composter
        recipe[3] = new ItemStack(Material.COMPOSTER, 1);
        recipe[4] = new ItemStack(Material.HOPPER, 1);
        recipe[5] = new ItemStack(Material.COMPOSTER, 1);
        
        // Bottom row: lava bucket, chest, water bucket
        recipe[6] = new ItemStack(Material.LAVA_BUCKET, 1);
        recipe[7] = new ItemStack(Material.CHEST, 1);
        recipe[8] = new ItemStack(Material.WATER_BUCKET, 1);

        ItemStack output = new ItemStack(Material.IRON_INGOT, 1);

        return new GeneratorConfig(
            "iron_farm",
            Material.FURNACE,
            recipe,
            output,
            3, // 3 seconds per iron ingot
            1728   // 27 slots × 64 items per slot = 1,728 items max
        );
    }

    /**
     * Creates the default Villager Breeder configuration.
     * Recipe:
     * WHE TOR WHE
     * BED COM BED
     * DIR WAT DIR
     */
    private GeneratorConfig createVillagerBreederConfig() {
        ItemStack[] recipe = new ItemStack[9];
        
        // Top row: wheat seeds, torch, wheat seeds
        recipe[0] = new ItemStack(Material.WHEAT_SEEDS, 1);
        recipe[1] = new ItemStack(Material.TORCH, 1);
        recipe[2] = new ItemStack(Material.WHEAT_SEEDS, 1);
        
        // Middle row: bed, composter, bed
        recipe[3] = new ItemStack(Material.RED_BED, 1);
        recipe[4] = new ItemStack(Material.COMPOSTER, 1);
        recipe[5] = new ItemStack(Material.RED_BED, 1);
        
        // Bottom row: dirt, water bucket, dirt
        recipe[6] = new ItemStack(Material.DIRT, 1);
        recipe[7] = new ItemStack(Material.WATER_BUCKET, 1);
        recipe[8] = new ItemStack(Material.DIRT, 1);

        ItemStack output = new ItemStack(Material.VILLAGER_SPAWN_EGG, 1);

        return new GeneratorConfig(
            "villager_breeder",
            Material.COMPOSTER,
            recipe,
            output,
            30, // 30 seconds per villager spawn egg
            432   // 27 slots × 16 spawn eggs per slot = 432 spawn eggs max
        );
    }

    /**
     * Calculates a hash for a recipe pattern to detect conflicts.
     * @param recipe The recipe array
     * @return A hash string representing the recipe pattern
     */
    private String calculateRecipeHash(ItemStack[] recipe) {
        StringBuilder hash = new StringBuilder();
        for (ItemStack item : recipe) {
            if (item == null) {
                hash.append("NULL");
            } else {
                hash.append(item.getType().name()).append(":").append(item.getAmount());
            }
            hash.append("|");
        }
        return hash.toString();
    }
}