package com.example.villagerfriendlyfarms.listener;

import com.example.villagerfriendlyfarms.VillagerFriendlyFarmsPlugin;
import com.example.villagerfriendlyfarms.config.GeneratorConfig;
import com.example.villagerfriendlyfarms.generator.GeneratorData;
import com.example.villagerfriendlyfarms.manager.GeneratorManager;
import com.example.villagerfriendlyfarms.permission.PermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Handles player interactions with generator blocks.
 */
public class PlayerInteractionListener implements Listener {
    
    private final VillagerFriendlyFarmsPlugin plugin;
    private final GeneratorManager generatorManager;
    private final PermissionManager permissionManager;
    private final Logger logger;
    private final NamespacedKey generatorIdKey;
    private final NamespacedKey generatorTypeKey;
    private final NamespacedKey generatorOwnerKey;
    
    // Track which generator each player has open
    private final Map<UUID, org.bukkit.Location> openGenerators = new HashMap<>();

    public PlayerInteractionListener(VillagerFriendlyFarmsPlugin plugin, GeneratorManager generatorManager, PermissionManager permissionManager) {
        this.plugin = plugin;
        this.generatorManager = generatorManager;
        this.permissionManager = permissionManager;
        this.logger = plugin.getLogger();
        this.generatorIdKey = new NamespacedKey(plugin, "generator_id");
        this.generatorTypeKey = new NamespacedKey(plugin, "generator_type");
        this.generatorOwnerKey = new NamespacedKey(plugin, "generator_owner");
    }

    /**
     * Handles right-click interactions with generator blocks.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        Player player = event.getPlayer();

        // Check if this block is a generator using the manager
        GeneratorData generator = generatorManager.getGeneratorAt(block.getLocation());
        if (generator == null) {
            return; // Not a generator block
        }

        // Cancel the event to prevent default block interaction
        event.setCancelled(true);

        // Get generator information from the manager
        String generatorId = generator.getId().toString();
        String generatorType = generator.getGeneratorType();
        UUID owner = generator.getOwner();

        // Check permissions using permission manager
        if (!permissionManager.validateGeneratorAccess(player, generator, true)) {
            return;
        }

        // Get generator configuration
        GeneratorConfig config = plugin.getConfigManager().getGeneratorConfig(generatorType);
        if (config == null) {
            player.sendMessage("§cInvalid generator type: " + generatorType);
            return;
        }

        // Calculate and add accumulated resources using generation time
        long currentTime = System.currentTimeMillis();
        
        // Calculate items to generate based on time since last generation
        int itemsToGenerate = generator.calculateItemsToGenerate(
            config.getGenerationTimeSeconds(), 
            config.getStorageCapacity()
        );
        
        // Calculate time remaining until next item
        long elapsedGenerationSeconds = generator.getElapsedGenerationTimeSeconds();
        long remainingSeconds = config.getGenerationTimeSeconds() - (elapsedGenerationSeconds % config.getGenerationTimeSeconds());
        String nextItemMessage = " §8(next in " + formatElapsedTime(remainingSeconds) + ")";
        
        if (itemsToGenerate > 0) {
            // Create a single ItemStack with the total amount to generate
            ItemStack outputItem = config.getOutput().clone();
            outputItem.setAmount(itemsToGenerate);
            
            // Add the stacked items to the generator
            generator.addStoredItem(outputItem);
            
            // Advance the generation timer by the time used for generated items
            generator.advanceGenerationTime(itemsToGenerate, config.getGenerationTimeSeconds());
            
            // Update the generator in the manager
            generatorManager.updateGenerator(generator);
            
            // Update stored items count and PDC
            int currentStoredItems = generator.getTotalItemCount();
            setStoredItems(block, currentStoredItems);
            
            String timeMessage = formatElapsedTime(elapsedGenerationSeconds);
            player.sendMessage("§aGenerated " + itemsToGenerate + " " + 
                             formatMaterialName(config.getOutput().getType()) + "(s) " +
                             "§7(elapsed: " + timeMessage + ")" + nextItemMessage);
            
            if (plugin.getConfig().getBoolean("plugin.debug", false)) {
                logger.info("Generated " + itemsToGenerate + " items for player " + player.getName());
            }
        } else if (elapsedGenerationSeconds > 0) {
            // Show elapsed time even if no full generation cycles completed
            String timeMessage = formatElapsedTime(elapsedGenerationSeconds);
            int currentItemCount = generator.getTotalItemCount();
            
            if (currentItemCount >= config.getStorageCapacity()) {
                // Storage is full
                player.sendMessage("§c⚠ Storage full! No items generated. " +
                                 "§7(elapsed: " + timeMessage + ", " + 
                                 currentItemCount + "/" + config.getStorageCapacity() + ")" + nextItemMessage);
            } else {
                // Not enough time has passed yet
                String storageStatus = " §7(" + currentItemCount + "/" + config.getStorageCapacity() + ")";
                player.sendMessage("§7No new items generated §8(elapsed: " + timeMessage + ")" + 
                                 storageStatus + nextItemMessage);
            }
        } else {
            // First time opening - show welcome message with storage info
            int currentItemCount = generator.getTotalItemCount();
            player.sendMessage("§eWelcome to your " + formatGeneratorName(generatorType) + "! " +
                             "§7Items will generate every " + config.getGenerationTimeSeconds() + " seconds.");
            player.sendMessage("§7Storage: " + currentItemCount + "/" + config.getStorageCapacity() + " items");
        }

        // Update last access time (but keep generation time separate)
        generator.updateLastAccessedTime();
        generatorManager.updateGenerator(generator);
        setLastAccessTime(block, currentTime);

        // Open generator interface with actual stored items
        openGeneratorInterface(player, config, generatorType, generator, block.getLocation());
        
        if (plugin.getConfig().getBoolean("plugin.debug", false)) {
            logger.info("Player " + player.getName() + " opened generator: " + generatorType);
        }
    }

    /**
     * Opens the generator interface for the player.
     */
    private void openGeneratorInterface(Player player, GeneratorConfig config, String generatorType, GeneratorData generator, org.bukkit.Location generatorLocation) {
        // Track which generator this player has open
        openGenerators.put(player.getUniqueId(), generatorLocation);
        
        // Create a chest-like inventory
        Inventory inventory = Bukkit.createInventory(null, 27, 
            "§6" + formatGeneratorName(generatorType) + " Generator");

        // Add the actual stored items from the generator
        List<ItemStack> storedItems = generator.getStoredItems();
        for (int i = 0; i < Math.min(storedItems.size(), 26); i++) { // Leave slot 26 for info
            inventory.setItem(i, storedItems.get(i));
        }

        // Add info item in the last slot with storage status
        ItemStack info = new ItemStack(Material.PAPER);
        info.editMeta(meta -> {
            int storedCount = generator.getTotalItemCount();
            boolean isNearFull = storedCount >= (config.getStorageCapacity() * 0.8);
            boolean isFull = storedCount >= config.getStorageCapacity();
            
            String storageColor = isFull ? "§c" : (isNearFull ? "§e" : "§a");
            String storageStatus = isFull ? " §c[FULL]" : (isNearFull ? " §e[NEARLY FULL]" : "");
            
            meta.setDisplayName("§eGenerator Info" + storageStatus);
            meta.setLore(java.util.Arrays.asList(
                "§7Type: §f" + formatGeneratorName(generatorType),
                "§7Output: §f" + config.getOutput().getAmount() + "x " + 
                    formatMaterialName(config.getOutput().getType()),
                "§7Generation Time: §f" + config.getGenerationTimeSeconds() + "s per item",
                "§7Storage Capacity: §f" + config.getStorageCapacity() + " items",
                "§7Currently Stored: " + storageColor + storedCount + "§7/" + config.getStorageCapacity() + " items",
                "",
                isFull ? "§c⚠ Storage is full! No new items will generate." :
                        (isNearFull ? "§eStorage is nearly full!" : "§aStorage has plenty of space."),
                "",
                "§eThis generator produces items over time!",
                "§eCheck back later for more resources."
            ));
        });
        inventory.setItem(26, info); // Last slot

        player.openInventory(inventory);
    }

    /**
     * Handles inventory clicks in generator interfaces.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();
        
        // Check if this is a generator inventory
        if (inventory.getSize() != 27 || !inventory.getType().equals(InventoryType.CHEST)) {
            return;
        }
        
        String title = event.getView().getTitle();
        if (!title.contains("Generator")) {
            return;
        }

        // Prevent clicking on the info item (last slot)
        if (event.getSlot() == 26) {
            event.setCancelled(true);
            return;
        }

        // Allow taking items but track the extraction
        // Note: This is a simplified implementation
        // In a full implementation, we would track which generator this inventory belongs to
        // and update the stored count accordingly
    }

    /**
     * Handles inventory close events to update stored item counts.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        Inventory inventory = event.getInventory();
        
        // Check if this is a generator inventory
        if (inventory.getSize() != 27 || !inventory.getType().equals(InventoryType.CHEST)) {
            return;
        }
        
        String title = event.getView().getTitle();
        if (!title.contains("Generator")) {
            return;
        }

        // Get the generator location for this player
        org.bukkit.Location generatorLocation = openGenerators.remove(player.getUniqueId());
        if (generatorLocation == null) {
            return;
        }

        // Get the generator from the manager
        GeneratorData generator = generatorManager.getGeneratorAt(generatorLocation);
        if (generator == null) {
            return;
        }

        // Clear the generator's stored items and add the remaining items from inventory
        generator.clearStoredItems();
        
        // Count remaining items in the inventory (excluding info item in slot 26)
        for (int i = 0; i < 26; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && !item.getType().isAir()) {
                generator.addStoredItem(item);
            }
        }

        // Update the generator in the manager and PDC
        generatorManager.updateGenerator(generator);
        Block generatorBlock = generatorLocation.getBlock();
        setStoredItems(generatorBlock, generator.getTotalItemCount());
        
        if (plugin.getConfig().getBoolean("plugin.debug", false)) {
            logger.info("Updated generator at " + formatLocation(generatorLocation) + 
                       " - remaining items: " + generator.getTotalItemCount());
        }
    }

    /**
     * Checks if a block is a generator block.
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
     * Gets the last access time from a block.
     */
    private long getLastAccessTime(Block block) {
        if (!(block.getState() instanceof org.bukkit.block.TileState)) {
            return 0;
        }

        org.bukkit.block.TileState tileState = (org.bukkit.block.TileState) block.getState();
        PersistentDataContainer pdc = tileState.getPersistentDataContainer();
        
        NamespacedKey lastAccessKey = new NamespacedKey(plugin, "last_access_time");
        return pdc.getOrDefault(lastAccessKey, PersistentDataType.LONG, System.currentTimeMillis());
    }

    /**
     * Sets the last access time for a block.
     */
    private void setLastAccessTime(Block block, long time) {
        if (!(block.getState() instanceof org.bukkit.block.TileState)) {
            return;
        }

        org.bukkit.block.TileState tileState = (org.bukkit.block.TileState) block.getState();
        PersistentDataContainer pdc = tileState.getPersistentDataContainer();
        
        NamespacedKey lastAccessKey = new NamespacedKey(plugin, "last_access_time");
        pdc.set(lastAccessKey, PersistentDataType.LONG, time);
        tileState.update();
    }

    /**
     * Gets the stored items count from a block.
     */
    private int getStoredItems(Block block) {
        if (!(block.getState() instanceof org.bukkit.block.TileState)) {
            return 0;
        }

        org.bukkit.block.TileState tileState = (org.bukkit.block.TileState) block.getState();
        PersistentDataContainer pdc = tileState.getPersistentDataContainer();
        
        NamespacedKey storedItemsKey = new NamespacedKey(plugin, "stored_items");
        return pdc.getOrDefault(storedItemsKey, PersistentDataType.INTEGER, 0);
    }

    /**
     * Sets the stored items count for a block.
     */
    private void setStoredItems(Block block, int count) {
        if (!(block.getState() instanceof org.bukkit.block.TileState)) {
            return;
        }

        org.bukkit.block.TileState tileState = (org.bukkit.block.TileState) block.getState();
        PersistentDataContainer pdc = tileState.getPersistentDataContainer();
        
        NamespacedKey storedItemsKey = new NamespacedKey(plugin, "stored_items");
        pdc.set(storedItemsKey, PersistentDataType.INTEGER, count);
        tileState.update();
    }

    /**
     * Formats elapsed time in a human-readable format.
     */
    private String formatElapsedTime(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            long remainingSeconds = seconds % 60;
            if (remainingSeconds == 0) {
                return minutes + "m";
            } else {
                return minutes + "m " + remainingSeconds + "s";
            }
        } else {
            long hours = seconds / 3600;
            long remainingMinutes = (seconds % 3600) / 60;
            if (remainingMinutes == 0) {
                return hours + "h";
            } else {
                return hours + "h " + remainingMinutes + "m";
            }
        }
    }

    /**
     * Formats a generator name for display.
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

    /**
     * Formats a location for logging.
     */
    private String formatLocation(org.bukkit.Location location) {
        return String.format("%s(%d,%d,%d)", 
            location.getWorld().getName(),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ());
    }
}