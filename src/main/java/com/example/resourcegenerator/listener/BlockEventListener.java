package com.example.resourcegenerator.listener;

import com.example.resourcegenerator.ResourceGeneratorPlugin;
import com.example.resourcegenerator.config.GeneratorConfig;
import com.example.resourcegenerator.generator.GeneratorData;
import com.example.resourcegenerator.manager.GeneratorManager;
import com.example.resourcegenerator.permission.PermissionManager;
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

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Handles block placement and breaking events for generators.
 */
public class BlockEventListener implements Listener {
    
    private final ResourceGeneratorPlugin plugin;
    private final CraftingEventListener craftingListener;
    private final GeneratorManager generatorManager;
    private final PermissionManager permissionManager;
    private final Logger logger;
    private final NamespacedKey generatorIdKey;
    private final NamespacedKey generatorTypeKey;
    private final NamespacedKey generatorOwnerKey;

    public BlockEventListener(ResourceGeneratorPlugin plugin, CraftingEventListener craftingListener, GeneratorManager generatorManager, PermissionManager permissionManager) {
        this.plugin = plugin;
        this.craftingListener = craftingListener;
        this.generatorManager = generatorManager;
        this.permissionManager = permissionManager;
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
        if (!permissionManager.validateGeneratorCreation(player, true)) {
            event.setCancelled(true);
            return;
        }

        // Check if a generator already exists at this location
        if (generatorManager.hasGeneratorAt(block.getLocation())) {
            event.setCancelled(true);
            player.sendMessage("§cA generator already exists at this location!");
            return;
        }

        // Check generator limits (unless player can bypass)
        if (!permissionManager.canBypassLimits(player) && 
            !generatorManager.canCreateGenerator(block.getLocation(), player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage("§cCannot place generator: limit reached!");
            return;
        }

        // Create generator data and register with manager
        GeneratorData generatorData = generatorManager.createGenerator(
            block.getLocation(),
            generatorType,
            player.getUniqueId()
        );

        if (generatorData == null) {
            event.setCancelled(true);
            player.sendMessage("§cFailed to create generator!");
            return;
        }

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

        // The generator is already registered with the manager

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
        GeneratorData generator = generatorManager.getGeneratorAt(block.getLocation());
        if (generator == null) {
            return; // Not a generator block
        }

        // Get generator information from the manager
        String generatorId = generator.getId().toString();
        String generatorType = generator.getGeneratorType();
        UUID owner = generator.getOwner();

        // Check permissions using permission manager
        if (!permissionManager.validateGeneratorBreaking(player, generator, true)) {
            event.setCancelled(true);
            return;
        }

        // Handle generator destruction with GeneratorManager
        GeneratorData destroyedGenerator = generatorManager.destroyGenerator(generator.getId());
        if (destroyedGenerator != null) {
            // Drop stored items
            List<org.bukkit.inventory.ItemStack> storedItems = destroyedGenerator.getStoredItems();
            for (org.bukkit.inventory.ItemStack item : storedItems) {
                if (item != null && !item.getType().isAir()) {
                    block.getWorld().dropItemNaturally(block.getLocation(), item);
                }
            }
            
            if (!storedItems.isEmpty()) {
                player.sendMessage("§eDropped " + storedItems.size() + " stored items");
            }
        }

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