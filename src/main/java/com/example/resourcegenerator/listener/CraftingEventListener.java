package com.example.resourcegenerator.listener;

import com.example.resourcegenerator.ResourceGeneratorPlugin;
import com.example.resourcegenerator.config.GeneratorConfig;
import com.example.resourcegenerator.generator.GeneratorData;
import com.example.resourcegenerator.recipe.RecipeManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.logging.Logger;

/**
 * Handles crafting events to detect and process generator creation.
 */
public class CraftingEventListener implements Listener {
    
    private final ResourceGeneratorPlugin plugin;
    private final RecipeManager recipeManager;
    private final Logger logger;
    private final NamespacedKey generatorIdKey;
    private final NamespacedKey generatorTypeKey;
    private final NamespacedKey generatorOwnerKey;

    public CraftingEventListener(ResourceGeneratorPlugin plugin, RecipeManager recipeManager) {
        this.plugin = plugin;
        this.recipeManager = recipeManager;
        this.logger = plugin.getLogger();
        this.generatorIdKey = new NamespacedKey(plugin, "generator_id");
        this.generatorTypeKey = new NamespacedKey(plugin, "generator_type");
        this.generatorOwnerKey = new NamespacedKey(plugin, "generator_owner");
    }

    /**
     * Handles the PrepareItemCraftEvent to detect generator recipes.
     * This event is called when items are placed in the crafting grid.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        CraftingInventory inventory = event.getInventory();
        ItemStack[] matrix = inventory.getMatrix();

        // Check if this is a generator recipe
        GeneratorConfig config = recipeManager.validateCraftingGrid(matrix);
        if (config != null) {
            // Create the generator block item with metadata
            ItemStack result = createGeneratorItem(config);
            event.getInventory().setResult(result);
            
            if (plugin.getConfig().getBoolean("plugin.debug", false)) {
                logger.info("Detected generator recipe: " + config.getName());
                logger.info(recipeManager.debugCraftingGrid(matrix));
            }
        }
    }

    /**
     * Handles the CraftItemEvent to process generator creation.
     * This event is called when the player actually takes the crafted item.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        CraftingInventory inventory = event.getInventory();
        ItemStack[] matrix = inventory.getMatrix();
        ItemStack result = event.getCurrentItem();

        // Check if this is a generator item
        if (!isGeneratorItem(result)) {
            return;
        }

        // Validate the recipe again
        GeneratorConfig config = recipeManager.validateCraftingGrid(matrix);
        if (config == null) {
            event.setCancelled(true);
            player.sendMessage("§cInvalid generator recipe!");
            return;
        }

        // Check permissions
        if (!player.hasPermission("resourcegenerator.create")) {
            event.setCancelled(true);
            player.sendMessage("§cYou don't have permission to create generators!");
            return;
        }

        // Validate and consume materials
        if (!recipeManager.validateMaterials(matrix, config)) {
            event.setCancelled(true);
            player.sendMessage("§cInsufficient materials for generator recipe!");
            return;
        }

        // The crafting system will automatically consume the materials
        // We just need to ensure the result item has the correct metadata
        updateGeneratorItemMetadata(result, config, player);

        player.sendMessage("§aGenerator crafted successfully! Place it to activate.");
        
        if (plugin.getConfig().getBoolean("plugin.debug", false)) {
            logger.info("Player " + player.getName() + " crafted generator: " + config.getName());
        }
    }

    /**
     * Creates a generator item with the appropriate metadata.
     * @param config The generator configuration
     * @return The generator item with metadata
     */
    private ItemStack createGeneratorItem(GeneratorConfig config) {
        ItemStack item = new ItemStack(config.getBlockType(), 1);
        
        // Add custom metadata to identify this as a generator
        item.editMeta(meta -> {
            meta.setDisplayName("§6" + formatGeneratorName(config.getName()));
            meta.setLore(java.util.Arrays.asList(
                "§7Generator Type: §f" + config.getName(),
                "§7Output: §f" + config.getOutput().getAmount() + "x " + 
                    formatMaterialName(config.getOutput().getType()),
                "§7Generation Time: §f" + config.getGenerationTimeSeconds() + "s",
                "§7Storage Capacity: §f" + config.getStorageCapacity() + " stacks",
                "",
                "§ePlace this block to create a generator!"
            ));

            // Add persistent data
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(generatorTypeKey, PersistentDataType.STRING, config.getName());
        });

        return item;
    }

    /**
     * Updates the metadata of a generator item.
     * @param item The generator item
     * @param config The generator configuration
     * @param player The player crafting the generator
     */
    private void updateGeneratorItemMetadata(ItemStack item, GeneratorConfig config, Player player) {
        item.editMeta(meta -> {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(generatorTypeKey, PersistentDataType.STRING, config.getName());
            pdc.set(generatorOwnerKey, PersistentDataType.STRING, player.getUniqueId().toString());
        });
    }

    /**
     * Checks if an item is a generator item.
     * @param item The item to check
     * @return True if the item is a generator
     */
    private boolean isGeneratorItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(generatorTypeKey, PersistentDataType.STRING);
    }

    /**
     * Gets the generator type from an item's metadata.
     * @param item The generator item
     * @return The generator type, or null if not a generator item
     */
    public String getGeneratorType(ItemStack item) {
        if (!isGeneratorItem(item)) {
            return null;
        }

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.get(generatorTypeKey, PersistentDataType.STRING);
    }

    /**
     * Gets the generator owner from an item's metadata.
     * @param item The generator item
     * @return The owner UUID string, or null if not set
     */
    public String getGeneratorOwner(ItemStack item) {
        if (!isGeneratorItem(item)) {
            return null;
        }

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.get(generatorOwnerKey, PersistentDataType.STRING);
    }

    /**
     * Formats a generator name for display.
     * @param name The generator name
     * @return A formatted display name
     */
    private String formatGeneratorName(String name) {
        String[] words = name.replace("_", " ").toLowerCase().split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1))
                      .append(" ");
            }
        }
        return result.toString().trim();
    }

    /**
     * Formats a material name for display.
     * @param material The material
     * @return A formatted display name
     */
    private String formatMaterialName(Material material) {
        String[] words = material.name().replace("_", " ").toLowerCase().split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1))
                      .append(" ");
            }
        }
        return result.toString().trim();
    }
}