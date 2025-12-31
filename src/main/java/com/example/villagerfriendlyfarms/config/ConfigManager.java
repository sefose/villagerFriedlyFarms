package com.example.villagerfriendlyfarms.config;

import com.example.villagerfriendlyfarms.VillagerFriendlyFarmsPlugin;
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
    
    private final VillagerFriendlyFarmsPlugin plugin;
    private final Logger logger;
    private final ObjectMapper mapper;
    private final File configDir;
    private final Map<String, GeneratorConfig> generatorConfigs;

    public ConfigManager(VillagerFriendlyFarmsPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin != null ? plugin.getLogger() : Logger.getLogger("ConfigManager");
        this.mapper = JsonUtils.getMapper();
        this.configDir = new File(getDataFolder(), "generators");
        this.generatorConfigs = new HashMap<>();
    }

    /**
     * Gets the plugin data folder. Can be overridden for testing.
     * @return The data folder
     */
    protected File getDataFolder() {
        return plugin.getDataFolder();
    }

    /**
     * Gets the logger. Can be overridden for testing.
     * @return The logger
     */
    protected Logger getLogger() {
        return logger;
    }

    /**
     * Initializes the configuration system.
     * Creates directories and loads all generator configurations.
     */
    public void initialize() {
        try {
            // Create plugin data directory if it doesn't exist
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }

            // Create generators config directory
            if (!configDir.exists()) {
                configDir.mkdirs();
                getLogger().info("Created generators configuration directory");
            }

            // Load existing configurations
            loadAllConfigurations();

            // Always ensure we have all default configurations
            createMissingDefaultConfigurations();

            getLogger().info("Loaded " + generatorConfigs.size() + " generator configurations");

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize configuration system", e);
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
                getLogger().info("Loaded generator config: " + config.getName());
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Failed to load config file: " + configFile.getName(), e);
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
            getLogger().info("Saved generator config: " + config.getName());
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Failed to save config: " + config.getName(), e);
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
        getLogger().info("Reloading generator configurations...");
        loadAllConfigurations();
        getLogger().info("Reloaded " + generatorConfigs.size() + " generator configurations");
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
                getLogger().severe("Recipe conflict detected between generators: " + 
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
        
        // Create Wheat Farm configuration
        GeneratorConfig wheatFarm = createWheatFarmConfig();
        saveConfiguration(wheatFarm);
        
        // Create Carrot Farm configuration
        GeneratorConfig carrotFarm = createCarrotFarmConfig();
        saveConfiguration(carrotFarm);
        
        // Create Potato Farm configuration
        GeneratorConfig potatoFarm = createPotatoFarmConfig();
        saveConfiguration(potatoFarm);
        
        // Create Beetroot Farm configuration
        GeneratorConfig beetrootFarm = createBeetrootFarmConfig();
        saveConfiguration(beetrootFarm);
    }

    /**
     * Creates any missing default generator configurations.
     */
    private void createMissingDefaultConfigurations() {
        boolean anyCreated = false;
        
        // Check and create Iron Farm if missing
        if (!hasConfiguration("iron_farm")) {
            GeneratorConfig ironFarm = createIronFarmConfig();
            saveConfiguration(ironFarm);
            getLogger().info("Created missing iron_farm configuration");
            anyCreated = true;
        }
        
        // Check and create Villager Breeder if missing
        if (!hasConfiguration("villager_breeder")) {
            GeneratorConfig villagerBreeder = createVillagerBreederConfig();
            saveConfiguration(villagerBreeder);
            getLogger().info("Created missing villager_breeder configuration");
            anyCreated = true;
        }
        
        // Check and create/update Wheat Farm
        if (!hasConfiguration("wheat_farm")) {
            GeneratorConfig wheatFarm = createWheatFarmConfig();
            saveConfiguration(wheatFarm);
            getLogger().info("Created missing wheat_farm configuration");
            anyCreated = true;
        } else {
            // Update existing wheat farm if it has wrong output amount
            GeneratorConfig existing = getGeneratorConfig("wheat_farm");
            if (existing.getOutput().getAmount() != 80) {
                GeneratorConfig wheatFarm = createWheatFarmConfig();
                saveConfiguration(wheatFarm);
                getLogger().info("Updated wheat_farm configuration to 80 items per cycle");
                anyCreated = true;
            }
        }
        
        // Check and create/update Carrot Farm
        if (!hasConfiguration("carrot_farm")) {
            GeneratorConfig carrotFarm = createCarrotFarmConfig();
            saveConfiguration(carrotFarm);
            getLogger().info("Created missing carrot_farm configuration");
            anyCreated = true;
        } else {
            // Update existing carrot farm if it has wrong output amount
            GeneratorConfig existing = getGeneratorConfig("carrot_farm");
            if (existing.getOutput().getAmount() != 80) {
                GeneratorConfig carrotFarm = createCarrotFarmConfig();
                saveConfiguration(carrotFarm);
                getLogger().info("Updated carrot_farm configuration to 80 items per cycle");
                anyCreated = true;
            }
        }
        
        // Check and create/update Potato Farm
        if (!hasConfiguration("potato_farm")) {
            GeneratorConfig potatoFarm = createPotatoFarmConfig();
            saveConfiguration(potatoFarm);
            getLogger().info("Created missing potato_farm configuration");
            anyCreated = true;
        } else {
            // Update existing potato farm if it has wrong output amount
            GeneratorConfig existing = getGeneratorConfig("potato_farm");
            if (existing.getOutput().getAmount() != 80) {
                GeneratorConfig potatoFarm = createPotatoFarmConfig();
                saveConfiguration(potatoFarm);
                getLogger().info("Updated potato_farm configuration to 80 items per cycle");
                anyCreated = true;
            }
        }
        
        // Check and create/update Beetroot Farm
        if (!hasConfiguration("beetroot_farm")) {
            GeneratorConfig beetrootFarm = createBeetrootFarmConfig();
            saveConfiguration(beetrootFarm);
            getLogger().info("Created missing beetroot_farm configuration");
            anyCreated = true;
        } else {
            // Update existing beetroot farm if it has wrong output amount
            GeneratorConfig existing = getGeneratorConfig("beetroot_farm");
            if (existing.getOutput().getAmount() != 80) {
                GeneratorConfig beetrootFarm = createBeetrootFarmConfig();
                saveConfiguration(beetrootFarm);
                getLogger().info("Updated beetroot_farm configuration to 80 items per cycle");
                anyCreated = true;
            }
        }
        
        if (anyCreated) {
            getLogger().info("Created/updated default generator configurations");
            // Reload configurations to include the new ones
            loadAllConfigurations();
        }
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
            480, // 8 minutes (480 seconds) per villager spawn egg
            432   // 27 slots × 16 spawn eggs per slot = 432 spawn eggs max
        );
    }

    /**
     * Creates the default Wheat Farm configuration.
     * Recipe:
     * WHE WHE WHE
     * WHE COM WHE
     * WHE WHE WHE
     */
    private GeneratorConfig createWheatFarmConfig() {
        ItemStack[] recipe = new ItemStack[9];
        
        // All positions: wheat except center (composter)
        recipe[0] = new ItemStack(Material.WHEAT, 1);
        recipe[1] = new ItemStack(Material.WHEAT, 1);
        recipe[2] = new ItemStack(Material.WHEAT, 1);
        recipe[3] = new ItemStack(Material.WHEAT, 1);
        recipe[4] = new ItemStack(Material.COMPOSTER, 1); // Center: composter
        recipe[5] = new ItemStack(Material.WHEAT, 1);
        recipe[6] = new ItemStack(Material.WHEAT, 1);
        recipe[7] = new ItemStack(Material.WHEAT, 1);
        recipe[8] = new ItemStack(Material.WHEAT, 1);

        ItemStack output = new ItemStack(Material.WHEAT, 80); // 80 items per cycle

        return new GeneratorConfig(
            "wheat_farm",
            Material.BARREL,
            recipe,
            output,
            120, // 2 minutes (120 seconds) per harvest
            1728   // 27 slots × 64 items per slot = 1,728 items max
        );
    }

    /**
     * Creates the default Carrot Farm configuration.
     * Recipe:
     * CAR CAR CAR
     * CAR COM CAR
     * CAR CAR CAR
     */
    private GeneratorConfig createCarrotFarmConfig() {
        ItemStack[] recipe = new ItemStack[9];
        
        // All positions: carrot except center (composter)
        recipe[0] = new ItemStack(Material.CARROT, 1);
        recipe[1] = new ItemStack(Material.CARROT, 1);
        recipe[2] = new ItemStack(Material.CARROT, 1);
        recipe[3] = new ItemStack(Material.CARROT, 1);
        recipe[4] = new ItemStack(Material.COMPOSTER, 1); // Center: composter
        recipe[5] = new ItemStack(Material.CARROT, 1);
        recipe[6] = new ItemStack(Material.CARROT, 1);
        recipe[7] = new ItemStack(Material.CARROT, 1);
        recipe[8] = new ItemStack(Material.CARROT, 1);

        ItemStack output = new ItemStack(Material.CARROT, 80); // 80 items per cycle

        return new GeneratorConfig(
            "carrot_farm",
            Material.BARREL,
            recipe,
            output,
            120, // 2 minutes (120 seconds) per harvest
            1728   // 27 slots × 64 items per slot = 1,728 items max
        );
    }

    /**
     * Creates the default Potato Farm configuration.
     * Recipe:
     * POT POT POT
     * POT COM POT
     * POT POT POT
     */
    private GeneratorConfig createPotatoFarmConfig() {
        ItemStack[] recipe = new ItemStack[9];
        
        // All positions: potato except center (composter)
        recipe[0] = new ItemStack(Material.POTATO, 1);
        recipe[1] = new ItemStack(Material.POTATO, 1);
        recipe[2] = new ItemStack(Material.POTATO, 1);
        recipe[3] = new ItemStack(Material.POTATO, 1);
        recipe[4] = new ItemStack(Material.COMPOSTER, 1); // Center: composter
        recipe[5] = new ItemStack(Material.POTATO, 1);
        recipe[6] = new ItemStack(Material.POTATO, 1);
        recipe[7] = new ItemStack(Material.POTATO, 1);
        recipe[8] = new ItemStack(Material.POTATO, 1);

        ItemStack output = new ItemStack(Material.POTATO, 80); // 80 items per cycle

        return new GeneratorConfig(
            "potato_farm",
            Material.BARREL,
            recipe,
            output,
            120, // 2 minutes (120 seconds) per harvest
            1728   // 27 slots × 64 items per slot = 1,728 items max
        );
    }

    /**
     * Creates the default Beetroot Farm configuration.
     * Recipe:
     * BEE BEE BEE
     * BEE COM BEE
     * BEE BEE BEE
     */
    private GeneratorConfig createBeetrootFarmConfig() {
        ItemStack[] recipe = new ItemStack[9];
        
        // All positions: beetroot except center (composter)
        recipe[0] = new ItemStack(Material.BEETROOT, 1);
        recipe[1] = new ItemStack(Material.BEETROOT, 1);
        recipe[2] = new ItemStack(Material.BEETROOT, 1);
        recipe[3] = new ItemStack(Material.BEETROOT, 1);
        recipe[4] = new ItemStack(Material.COMPOSTER, 1); // Center: composter
        recipe[5] = new ItemStack(Material.BEETROOT, 1);
        recipe[6] = new ItemStack(Material.BEETROOT, 1);
        recipe[7] = new ItemStack(Material.BEETROOT, 1);
        recipe[8] = new ItemStack(Material.BEETROOT, 1);

        ItemStack output = new ItemStack(Material.BEETROOT, 80); // 80 items per cycle

        return new GeneratorConfig(
            "beetroot_farm",
            Material.BARREL,
            recipe,
            output,
            120, // 2 minutes (120 seconds) per harvest
            1728   // 27 slots × 64 items per slot = 1,728 items max
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