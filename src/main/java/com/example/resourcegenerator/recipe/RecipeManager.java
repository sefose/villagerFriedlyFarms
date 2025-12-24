package com.example.resourcegenerator.recipe;

import com.example.resourcegenerator.ResourceGeneratorPlugin;
import com.example.resourcegenerator.config.ConfigManager;
import com.example.resourcegenerator.config.GeneratorConfig;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Manages generator recipes and crafting pattern validation.
 * Handles recipe registration, pattern matching, and conflict detection.
 */
public class RecipeManager {
    
    private final ResourceGeneratorPlugin plugin;
    private final ConfigManager configManager;
    private final Logger logger;
    private final Map<String, GeneratorConfig> registeredRecipes;

    public RecipeManager(ResourceGeneratorPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.logger = plugin.getLogger();
        this.registeredRecipes = new HashMap<>();
    }

    /**
     * Initializes the recipe manager by loading all generator recipes.
     */
    public void initialize() {
        loadAllRecipes();
        logger.info("Recipe manager initialized with " + registeredRecipes.size() + " recipes");
    }

    /**
     * Loads all generator recipes from the configuration manager.
     */
    public void loadAllRecipes() {
        registeredRecipes.clear();
        
        Map<String, GeneratorConfig> configs = configManager.getAllConfigurations();
        for (GeneratorConfig config : configs.values()) {
            registerRecipe(config);
        }
    }

    /**
     * Registers a generator recipe.
     * @param config The generator configuration containing the recipe
     */
    public void registerRecipe(GeneratorConfig config) {
        if (config == null) {
            logger.warning("Attempted to register null generator config");
            return;
        }

        // Check for recipe conflicts
        GeneratorConfig existing = findMatchingRecipe(config.getRecipe());
        if (existing != null && !existing.getName().equals(config.getName())) {
            logger.severe("Recipe conflict detected! Generator '" + config.getName() + 
                         "' has the same recipe as '" + existing.getName() + "'");
            return;
        }

        registeredRecipes.put(config.getName(), config);
        logger.info("Registered recipe for generator: " + config.getName());
    }

    /**
     * Validates a 3x3 crafting grid against all registered recipes.
     * @param craftingGrid The crafting grid to validate (9 elements)
     * @return The matching generator config, or null if no match found
     */
    public GeneratorConfig validateCraftingGrid(ItemStack[] craftingGrid) {
        if (craftingGrid == null || craftingGrid.length != 9) {
            return null;
        }

        // Normalize the crafting grid (convert null to air)
        ItemStack[] normalizedGrid = normalizeCraftingGrid(craftingGrid);

        // Check against all registered recipes
        for (GeneratorConfig config : registeredRecipes.values()) {
            if (config.matchesRecipe(normalizedGrid)) {
                return config;
            }
        }

        return null;
    }

    /**
     * Finds a generator recipe that matches the given crafting pattern.
     * @param craftingGrid The crafting grid to match
     * @return The matching generator config, or null if none found
     */
    public GeneratorConfig findMatchingRecipe(ItemStack[] craftingGrid) {
        return validateCraftingGrid(craftingGrid);
    }

    /**
     * Checks if a crafting grid represents a valid generator recipe.
     * @param craftingGrid The crafting grid to check
     * @return True if the grid matches any registered recipe
     */
    public boolean isValidGeneratorRecipe(ItemStack[] craftingGrid) {
        return validateCraftingGrid(craftingGrid) != null;
    }

    /**
     * Gets all registered generator recipes.
     * @return A copy of the registered recipes map
     */
    public Map<String, GeneratorConfig> getRegisteredRecipes() {
        return new HashMap<>(registeredRecipes);
    }

    /**
     * Gets a specific generator recipe by name.
     * @param name The generator name
     * @return The generator config, or null if not found
     */
    public GeneratorConfig getRecipe(String name) {
        return registeredRecipes.get(name);
    }

    /**
     * Checks if a recipe is registered for the given generator name.
     * @param name The generator name
     * @return True if a recipe exists
     */
    public boolean hasRecipe(String name) {
        return registeredRecipes.containsKey(name);
    }

    /**
     * Validates that the player has sufficient materials for the recipe.
     * @param craftingGrid The crafting grid
     * @param config The generator configuration
     * @return True if the player has enough materials
     */
    public boolean validateMaterials(ItemStack[] craftingGrid, GeneratorConfig config) {
        if (craftingGrid == null || config == null) {
            return false;
        }

        ItemStack[] recipe = config.getRecipe();
        
        for (int i = 0; i < 9; i++) {
            ItemStack requiredItem = recipe[i];
            ItemStack providedItem = craftingGrid[i];

            // Skip empty slots
            if (requiredItem == null || requiredItem.getType() == Material.AIR) {
                continue;
            }

            // Check if player provided the required item
            if (providedItem == null || providedItem.getType() == Material.AIR) {
                return false;
            }

            // Check material type and amount
            if (requiredItem.getType() != providedItem.getType() ||
                requiredItem.getAmount() > providedItem.getAmount()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Consumes the required materials from the crafting grid.
     * @param craftingGrid The crafting grid to consume from
     * @param config The generator configuration
     * @return True if materials were successfully consumed
     */
    public boolean consumeMaterials(ItemStack[] craftingGrid, GeneratorConfig config) {
        if (!validateMaterials(craftingGrid, config)) {
            return false;
        }

        ItemStack[] recipe = config.getRecipe();
        
        for (int i = 0; i < 9; i++) {
            ItemStack requiredItem = recipe[i];
            ItemStack providedItem = craftingGrid[i];

            if (requiredItem == null || requiredItem.getType() == Material.AIR) {
                continue;
            }

            // Consume the required amount
            int newAmount = providedItem.getAmount() - requiredItem.getAmount();
            if (newAmount <= 0) {
                craftingGrid[i] = null;
            } else {
                providedItem.setAmount(newAmount);
            }
        }

        return true;
    }

    /**
     * Reloads all recipes from the configuration manager.
     */
    public void reloadRecipes() {
        logger.info("Reloading generator recipes...");
        loadAllRecipes();
        logger.info("Reloaded " + registeredRecipes.size() + " generator recipes");
    }

    /**
     * Validates that all registered recipes have unique patterns.
     * @return True if all recipes are unique
     */
    public boolean validateUniqueRecipes() {
        Map<String, String> recipeHashes = new HashMap<>();
        
        for (GeneratorConfig config : registeredRecipes.values()) {
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
     * Normalizes a crafting grid by converting null items to air.
     * @param craftingGrid The original crafting grid
     * @return A normalized copy of the crafting grid
     */
    private ItemStack[] normalizeCraftingGrid(ItemStack[] craftingGrid) {
        ItemStack[] normalized = new ItemStack[9];
        
        for (int i = 0; i < 9; i++) {
            ItemStack item = craftingGrid[i];
            if (item == null || item.getType() == Material.AIR) {
                normalized[i] = null;
            } else {
                normalized[i] = item.clone();
            }
        }
        
        return normalized;
    }

    /**
     * Calculates a hash for a recipe pattern to detect conflicts.
     * @param recipe The recipe array
     * @return A hash string representing the recipe pattern
     */
    private String calculateRecipeHash(ItemStack[] recipe) {
        StringBuilder hash = new StringBuilder();
        for (ItemStack item : recipe) {
            if (item == null || item.getType() == Material.AIR) {
                hash.append("AIR");
            } else {
                hash.append(item.getType().name()).append(":").append(item.getAmount());
            }
            hash.append("|");
        }
        return hash.toString();
    }

    /**
     * Creates a debug string representation of a crafting grid.
     * @param craftingGrid The crafting grid
     * @return A formatted string showing the 3x3 pattern
     */
    public String debugCraftingGrid(ItemStack[] craftingGrid) {
        if (craftingGrid == null || craftingGrid.length != 9) {
            return "Invalid crafting grid";
        }

        StringBuilder debug = new StringBuilder("\nCrafting Grid:\n");
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = row * 3 + col;
                ItemStack item = craftingGrid[index];
                
                if (item == null || item.getType() == Material.AIR) {
                    debug.append("[   ] ");
                } else {
                    String name = item.getType().name();
                    if (name.length() > 3) {
                        name = name.substring(0, 3);
                    }
                    debug.append("[").append(name).append("] ");
                }
            }
            debug.append("\n");
        }
        
        return debug.toString();
    }
}