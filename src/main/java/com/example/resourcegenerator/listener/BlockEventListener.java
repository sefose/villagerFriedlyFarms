package com.example.resourcegenerator.listener;

import com.example.resourcegenerator.ResourceGeneratorPlugin;
import com.example.resourcegenerator.config.GeneratorConfig;
import com.example.resourcegenerator.generator.GeneratorData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Handles block placement and breaking events for generators.
 */
public class BlockEventListener implements Listener {
    
    private final ResourceGeneratorPlugin plugin;
    private final CraftingEventListener craftingListener;
    private final Logger logger;
    private final NamespacedKey generatorIdKey;
    private final NamespacedKey generatorTypeKey;
    private final NamespacedKey generatorOwnerKey;

    public BlockEventListener(ResourceGeneratorPlugin plugin, CraftingEventListener craftingListener) {
        this.plugin = plugin;
        this.craftingListener = craftingListener;
        this.logger = plugin.getLogger();
        this.generatorIdKey = new NamespacedKey(plugin, "generator_id");
        this.generatorTypeKey = new NamespacedKey(plugin, "generator_type");
        this.generatorOwnerKey = new NamespacedKey(plugin, "generator_owner");
    }

    /**
     * Handles block placement to detect generator placement.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();
        ItemStack item = event.getItemInHand();

        // Check if this is a generator item
        String generatorType = craftingListener.getGeneratorType(item);
        if (generatorType == null) {
            return; // Not a generator item
        }

        // Get the generator configuration
        GeneratorConfig config = plugin.getConfigManager().getGeneratorConfig(generatorType);
        if (config == null) {
            event.setCancelled(true);
            player.sendMessage("§cInvalid generator type: " + generatorType);
            logger.warning("Player " + player.getName() + " tried to place unknown generator type: " + generatorType);
            return;
        }

        // Verify the block type matches the configuration
        if (block.getType() != config.getBlockType()) {
            event.setCancelled(true);
            player.sendMessage("§cGenerator block type mismatch!");
            return;
        }

        // Check permissions
        if (!player.hasPermission("resourcegenerator.create")) {
            event.setCancelled(true);
            player.sendMessage("§cYou don't have permission to place generators!");
            return;
        }

        // TODO: Check generator limits (max per chunk, max per player)
        // This will be implemented when we add the GeneratorManager

        // Create generator data
        GeneratorData generatorData = GeneratorData.create(
            block.getLocation(),
            generatorType,
            player.getUniqueId()
        );

        // Add persistent data to the block
        if (block.getState() instanceof org.bukkit.block.TileState) {
            org.bukkit.block.TileState tileState = (org.bukkit.block.TileState) block.getState();
            PersistentDataContainer pdc = tileState.getPersistentDataContainer();
            
            pdc.set(generatorIdKey, PersistentDataType.STRING, generatorData.getId().toString());
            pdc.set(generatorTypeKey, PersistentDataType.STRING, generatorType);
            pdc.set(generatorOwnerKey, PersistentDataType.STRING, player.getUniqueId().toString());
            
            // Set initial last access time
            NamespacedKey lastAccessKey = new NamespacedKey(plugin, "last_access_time");
            pdc.set(lastAccessKey, PersistentDataType.LONG, System.currentTimeMillis());
            
            tileState.update();
            
            // Make furnace appear "lit" to distinguish it as a generator
            if (block.getType() == Material.FURNACE) {
                org.bukkit.block.data.type.Furnace furnaceData = 
                    (org.bukkit.block.data.type.Furnace) block.getBlockData();
                furnaceData.setLit(true);
                block.setBlockData(furnaceData);
            }
        }

        // TODO: Register the generator with GeneratorManager
        // This will be implemented in the next task

        player.sendMessage("§aGenerator placed successfully! Type: " + 
                          formatGeneratorName(generatorType));
        
        if (plugin.getConfig().getBoolean("plugin.debug", false)) {
            logger.info("Player " + player.getName() + " placed generator: " + generatorType + 
                       " at " + formatLocation(block.getLocation()));
        }
    }

    /**
     * Handles block breaking to detect generator destruction.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Check if this block is a generator
        if (!isGeneratorBlock(block)) {
            return; // Not a generator block
        }

        // Get generator information
        String generatorId = getGeneratorId(block);
        String generatorType = getGeneratorType(block);
        String ownerUuid = getGeneratorOwner(block);

        if (generatorId == null || generatorType == null || ownerUuid == null) {
            logger.warning("Generator block missing metadata at " + formatLocation(block.getLocation()));
            return;
        }

        // Check permissions
        UUID owner = UUID.fromString(ownerUuid);
        if (!player.getUniqueId().equals(owner) && 
            !player.hasPermission("resourcegenerator.admin")) {
            event.setCancelled(true);
            player.sendMessage("§cYou don't have permission to break this generator!");
            return;
        }

        // TODO: Handle generator destruction with GeneratorManager
        // - Remove generator data
        // - Drop stored items
        // - Clean up persistent data
        // This will be implemented when we add the GeneratorManager

        player.sendMessage("§cGenerator destroyed: " + formatGeneratorName(generatorType));
        
        if (plugin.getConfig().getBoolean("plugin.debug", false)) {
            logger.info("Player " + player.getName() + " destroyed generator: " + generatorType + 
                       " at " + formatLocation(block.getLocation()));
        }
    }

    /**
     * Checks if a block is a generator block.
     * @param block The block to check
     * @return True if the block is a generator
     */
    private boolean isGeneratorBlock(Block block) {
        if (!(block.getState() instanceof org.bukkit.block.TileState)) {
            return false;
        }

        org.bukkit.block.TileState tileState = (org.bukkit.block.TileState) block.getState();
        PersistentDataContainer pdc = tileState.getPersistentDataContainer();
        
        return pdc.has(generatorIdKey, PersistentDataType.STRING);
    }

    /**
     * Gets the generator ID from a block.
     * @param block The generator block
     * @return The generator ID, or null if not a generator
     */
    private String getGeneratorId(Block block) {
        if (!(block.getState() instanceof org.bukkit.block.TileState)) {
            return null;
        }

        org.bukkit.block.TileState tileState = (org.bukkit.block.TileState) block.getState();
        PersistentDataContainer pdc = tileState.getPersistentDataContainer();
        
        return pdc.get(generatorIdKey, PersistentDataType.STRING);
    }

    /**
     * Gets the generator type from a block.
     * @param block The generator block
     * @return The generator type, or null if not a generator
     */
    private String getGeneratorType(Block block) {
        if (!(block.getState() instanceof org.bukkit.block.TileState)) {
            return null;
        }

        org.bukkit.block.TileState tileState = (org.bukkit.block.TileState) block.getState();
        PersistentDataContainer pdc = tileState.getPersistentDataContainer();
        
        return pdc.get(generatorTypeKey, PersistentDataType.STRING);
    }

    /**
     * Gets the generator owner from a block.
     * @param block The generator block
     * @return The owner UUID string, or null if not a generator
     */
    private String getGeneratorOwner(Block block) {
        if (!(block.getState() instanceof org.bukkit.block.TileState)) {
            return null;
        }

        org.bukkit.block.TileState tileState = (org.bukkit.block.TileState) block.getState();
        PersistentDataContainer pdc = tileState.getPersistentDataContainer();
        
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