package com.example.resourcegenerator;

import com.example.resourcegenerator.config.ConfigManager;
import com.example.resourcegenerator.listener.BlockEventListener;
import com.example.resourcegenerator.listener.CraftingEventListener;
import com.example.resourcegenerator.listener.PlayerInteractionListener;
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
    private CraftingEventListener craftingListener;
    private BlockEventListener blockListener;
    private PlayerInteractionListener playerInteractionListener;

    @Override
    public void onEnable() {
        instance = this;
        
        getLogger().info("ResourceGenerator plugin has been enabled!");
        getLogger().info("Version: " + getDescription().getVersion());
        
        // Save default config
        saveDefaultConfig();
        
        // Initialize configuration manager
        configManager = new ConfigManager(this);
        configManager.initialize();
        
        // Validate configurations
        if (!configManager.validateUniqueRecipes()) {
            getLogger().severe("Recipe conflicts detected! Plugin will not function correctly.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize recipe manager
        recipeManager = new RecipeManager(this);
        recipeManager.initialize();
        
        // Validate recipe uniqueness
        if (!recipeManager.validateUniqueRecipes()) {
            getLogger().severe("Recipe validation failed! Plugin will not function correctly.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize event listeners
        craftingListener = new CraftingEventListener(this, recipeManager);
        blockListener = new BlockEventListener(this, craftingListener);
        playerInteractionListener = new PlayerInteractionListener(this);
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(craftingListener, this);
        getServer().getPluginManager().registerEvents(blockListener, this);
        getServer().getPluginManager().registerEvents(playerInteractionListener, this);
        
        // Register commands
        getCommand("rg").setExecutor(new com.example.resourcegenerator.command.GeneratorCommand(this));
        
        getLogger().info("Recipe system initialized successfully!");
        getLogger().info("Event listeners registered successfully!");
        getLogger().info("Commands registered successfully!");
        
        // TODO: Initialize remaining managers and systems in subsequent tasks
        // - GeneratorManager
        // - DataStorage
        // - PlayerInteractionListener
        // - PermissionManager
    }

    @Override
    public void onDisable() {
        getLogger().info("ResourceGenerator plugin has been disabled!");
        
        // TODO: Cleanup and save data in subsequent tasks
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
}