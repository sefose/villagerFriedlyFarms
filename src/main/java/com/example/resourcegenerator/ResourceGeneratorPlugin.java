package com.example.resourcegenerator;

import com.example.resourcegenerator.config.ConfigManager;
import com.example.resourcegenerator.listener.BlockEventListener;
import com.example.resourcegenerator.listener.CraftingEventListener;
import com.example.resourcegenerator.listener.PlayerInteractionListener;
import com.example.resourcegenerator.manager.GeneratorManager;
import com.example.resourcegenerator.permission.PermissionManager;
import com.example.resourcegenerator.recipe.RecipeManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for the Resource Generator plugin.
 * 
 * This plugin allows players to craft resource generation devices using specific recipes.
 * Generators appear as custom blocks and accumulate resources over time using a passive
 * calculation system.
 */
public class ResourceGeneratorPlugin extends JavaPlugin {

    private static ResourceGeneratorPlugin instance;
    private ConfigManager configManager;
    private RecipeManager recipeManager;
    private GeneratorManager generatorManager;
    private PermissionManager permissionManager;
    private CraftingEventListener craftingListener;
    private BlockEventListener blockListener;
    private PlayerInteractionListener playerInteractionListener;

    @Override
    public void onEnable() {
        instance = this;
        
        try {
            getLogger().info("ResourceGenerator plugin starting up...");
            getLogger().info("Version: " + getDescription().getVersion());
            getLogger().info("Server: " + getServer().getName() + " " + getServer().getVersion());
            
            // Save default config
            saveDefaultConfig();
            
            // Initialize configuration manager
            getLogger().info("Initializing configuration system...");
            configManager = new ConfigManager(this);
            configManager.initialize();
            
            // Validate configurations
            if (!configManager.validateUniqueRecipes()) {
                getLogger().severe("Recipe conflicts detected! Plugin will not function correctly.");
                getLogger().severe("Please check your generator configurations and fix any duplicate recipes.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            // Initialize recipe manager
            getLogger().info("Initializing recipe system...");
            recipeManager = new RecipeManager(this);
            recipeManager.initialize();
            
            // Validate recipe uniqueness
            if (!recipeManager.validateUniqueRecipes()) {
                getLogger().severe("Recipe validation failed! Plugin will not function correctly.");
                getLogger().severe("Please check your generator configurations for conflicts.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            // Initialize generator manager
            getLogger().info("Initializing generator management system...");
            generatorManager = new GeneratorManager(this);
            generatorManager.initialize();
            
            // Initialize permission manager
            getLogger().info("Initializing permission system...");
            permissionManager = new PermissionManager(this);
            permissionManager.initialize();
            
            // Initialize event listeners
            getLogger().info("Registering event listeners...");
            craftingListener = new CraftingEventListener(this, recipeManager, permissionManager);
            blockListener = new BlockEventListener(this, craftingListener, generatorManager, permissionManager);
            playerInteractionListener = new PlayerInteractionListener(this, generatorManager, permissionManager);
            
            // Register event listeners
            getServer().getPluginManager().registerEvents(craftingListener, this);
            getServer().getPluginManager().registerEvents(blockListener, this);
            getServer().getPluginManager().registerEvents(playerInteractionListener, this);
            
            // Register commands
            getLogger().info("Registering commands...");
            getCommand("rg").setExecutor(new com.example.resourcegenerator.command.GeneratorCommand(this));
            
            getLogger().info("Recipe system initialized successfully!");
            getLogger().info("Generator manager initialized successfully!");
            getLogger().info("Permission manager initialized successfully!");
            getLogger().info("Event listeners registered successfully!");
            getLogger().info("Commands registered successfully!");
            
            // All systems initialized successfully
            getLogger().info("ResourceGenerator plugin fully initialized!");
            getLogger().info("- Loaded " + configManager.getAllConfigurations().size() + " generator types");
            getLogger().info("- Loaded " + generatorManager.getTotalGeneratorCount() + " existing generators");
            getLogger().info("- All permissions registered and validated");
            getLogger().info("Plugin is ready for use!");
            
        } catch (Exception e) {
            getLogger().severe("Critical error during plugin initialization!");
            getLogger().severe("Error: " + e.getMessage());
            e.printStackTrace();
            getLogger().severe("Plugin will be disabled to prevent further issues.");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        try {
            getLogger().info("ResourceGenerator plugin shutting down...");
            
            // Shutdown recipe manager and clean up recipes
            if (recipeManager != null) {
                recipeManager.shutdown();
            }
            
            // Shutdown generator manager and save data
            if (generatorManager != null) {
                getLogger().info("Saving generator data...");
                generatorManager.shutdown();
                getLogger().info("Generator data saved successfully");
            }
            
            getLogger().info("ResourceGenerator plugin has been disabled!");
            
        } catch (Exception e) {
            getLogger().severe("Error during plugin shutdown!");
            getLogger().severe("Error: " + e.getMessage());
            e.printStackTrace();
            getLogger().severe("Some data may not have been saved properly.");
        }
    }

    /**
     * Gets the plugin instance.
     * @return The plugin instance
     */
    public static ResourceGeneratorPlugin getInstance() {
        return instance;
    }

    /**
     * Gets the configuration manager.
     * @return The config manager
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Gets the permission manager.
     * @return The permission manager
     */
    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    /**
     * Gets the generator manager.
     * @return The generator manager
     */
    public GeneratorManager getGeneratorManager() {
        return generatorManager;
    }

    /**
     * Gets the recipe manager.
     * @return The recipe manager
     */
    public RecipeManager getRecipeManager() {
        return recipeManager;
    }

    /**
     * Gets the crafting event listener.
     * @return The crafting listener
     */
    public CraftingEventListener getCraftingListener() {
        return craftingListener;
    }

    /**
     * Gets the block event listener.
     * @return The block listener
     */
    public BlockEventListener getBlockListener() {
        return blockListener;
    }

    /**
     * Handles a critical error by logging it and optionally disabling the plugin.
     * @param message The error message
     * @param throwable The exception that occurred
     * @param disablePlugin Whether to disable the plugin
     */
    public void handleCriticalError(String message, Throwable throwable, boolean disablePlugin) {
        getLogger().severe("CRITICAL ERROR: " + message);
        if (throwable != null) {
            getLogger().severe("Exception: " + throwable.getMessage());
            throwable.printStackTrace();
        }
        
        if (disablePlugin) {
            getLogger().severe("Plugin will be disabled to prevent further issues.");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Handles a recoverable error by logging it appropriately.
     * @param message The error message
     * @param throwable The exception that occurred
     */
    public void handleRecoverableError(String message, Throwable throwable) {
        getLogger().warning("Recoverable error: " + message);
        if (throwable != null) {
            getLogger().warning("Exception: " + throwable.getMessage());
            if (getConfig().getBoolean("plugin.debug", false)) {
                throwable.printStackTrace();
            }
        }
    }

    /**
     * Validates the plugin's current state and reports any issues.
     * @return True if the plugin is in a valid state
     */
    public boolean validatePluginState() {
        try {
            boolean isValid = true;
            
            // Check configuration manager
            if (configManager == null) {
                getLogger().warning("Configuration manager is not initialized");
                isValid = false;
            } else if (configManager.getAllConfigurations().isEmpty()) {
                getLogger().warning("No generator configurations loaded");
                isValid = false;
            }
            
            // Check recipe manager
            if (recipeManager == null) {
                getLogger().warning("Recipe manager is not initialized");
                isValid = false;
            } else if (recipeManager.getRegisteredRecipes().isEmpty()) {
                getLogger().warning("No recipes registered");
                isValid = false;
            }
            
            // Check generator manager
            if (generatorManager == null) {
                getLogger().warning("Generator manager is not initialized");
                isValid = false;
            }
            
            // Check permission manager
            if (permissionManager == null) {
                getLogger().warning("Permission manager is not initialized");
                isValid = false;
            }
            
            // Validate data integrity
            if (generatorManager != null && generatorManager.getDataStorage() != null) {
                if (!generatorManager.getDataStorage().validateDataIntegrity()) {
                    getLogger().warning("Data integrity issues detected");
                    isValid = false;
                }
            }
            
            if (isValid) {
                getLogger().info("Plugin state validation passed");
            } else {
                getLogger().warning("Plugin state validation failed - some features may not work correctly");
            }
            
            return isValid;
            
        } catch (Exception e) {
            handleRecoverableError("Error during plugin state validation", e);
            return false;
        }
    }
}