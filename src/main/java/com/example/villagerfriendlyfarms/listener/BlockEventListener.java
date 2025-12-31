package com.example.villagerfriendlyfarms.listener;

import com.example.villagerfriendlyfarms.VillagerFriendlyFarmsPlugin;
import com.example.villagerfriendlyfarms.config.GeneratorConfig;
import com.example.villagerfriendlyfarms.generator.GeneratorData;
import com.example.villagerfriendlyfarms.manager.GeneratorManager;
import com.example.villagerfriendlyfarms.permission.PermissionManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
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
    
    private final VillagerFriendlyFarmsPlugin plugin;
    private final CraftingEventListener craftingListener;
    private final GeneratorManager generatorManager;
    private final PermissionManager permissionManager;
    private final Logger logger;
    private final NamespacedKey generatorIdKey;
    private final NamespacedKey generatorTypeKey;
    private final NamespacedKey generatorOwnerKey;

    public BlockEventListener(VillagerFriendlyFarmsPlugin plugin, CraftingEventListener craftingListener, GeneratorManager generatorManager, PermissionManager permissionManager) {
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

        // Add visual indicator for all farms (item frame with output item)
        addFarmVisualIndicator(block, config, player);

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
            // Remove visual indicator (item frame) if it exists
            removeFarmVisualIndicator(block);
            
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

    /**
     * Adds a visual indicator (item frame with output item) to a farm.
     * @param farmBlock The farm block
     * @param config The generator configuration
     * @param player The player who placed the farm
     */
    private void addFarmVisualIndicator(Block farmBlock, GeneratorConfig config, Player player) {
        // Add item frames for all farm types (furnace, composter, barrel)
        if (farmBlock.getType() != Material.BARREL && 
            farmBlock.getType() != Material.COMPOSTER && 
            farmBlock.getType() != Material.FURNACE) {
            return;
        }

        // Calculate the face that should face the player
        BlockFace playerFace = getPlayerFacingDirection(player, farmBlock);
        
        // Get the opposite face (where the item frame should be placed)
        BlockFace frameFace = playerFace.getOppositeFace();
        
        // Try to place item frame on the player-facing side first
        if (tryPlaceItemFrame(farmBlock, frameFace, config)) {
            return;
        }
        
        // If primary face failed, try fallback faces
        BlockFace[] fallbackFaces = {BlockFace.SOUTH, BlockFace.NORTH, BlockFace.EAST, BlockFace.WEST};
        
        for (BlockFace face : fallbackFaces) {
            if (face == frameFace) continue; // Skip the face we already tried
            
            if (tryPlaceItemFrame(farmBlock, face, config)) {
                return;
            }
        }
    }

    /**
     * Attempts to place an item frame on a specific face of a block.
     * @param farmBlock The farm block
     * @param face The face to place the item frame on
     * @param config The generator configuration
     * @return True if successfully placed, false otherwise
     */
    private boolean tryPlaceItemFrame(Block farmBlock, BlockFace face, GeneratorConfig config) {
        // Check if the adjacent block is air (can place item frame)
        Block adjacentBlock = farmBlock.getRelative(face);
        
        if (adjacentBlock.getType() != Material.AIR) {
            return false;
        }
        
        try {
            // Get the exact location on the face of the block
            Location frameLocation = getItemFrameLocation(farmBlock, face);
            
            // Validate the location is safe
            if (frameLocation.getBlock().getType() != Material.AIR) {
                return false;
            }
            
            // Spawn item frame using the block's location and face
            ItemFrame itemFrame = (ItemFrame) farmBlock.getWorld().spawnEntity(frameLocation, org.bukkit.entity.EntityType.ITEM_FRAME);
            
            if (itemFrame == null) {
                return false;
            }
            
            // Set the facing direction BEFORE setting other properties
            itemFrame.setFacingDirection(face);
            
            // Set the item in the frame to the output type
            ItemStack displayItem = new ItemStack(config.getOutput().getType(), 1);
            itemFrame.setItem(displayItem);
            
            // Make the item frame fixed (harder to break accidentally)
            itemFrame.setFixed(true);
            
            // Add persistent data to identify this as a farm item frame
            PersistentDataContainer pdc = itemFrame.getPersistentDataContainer();
            pdc.set(new NamespacedKey(plugin, "farm_item_frame"), PersistentDataType.STRING, "true");
            pdc.set(new NamespacedKey(plugin, "farm_location"), PersistentDataType.STRING, 
                   formatLocation(farmBlock.getLocation()));
            
            // Verify the item frame is still valid
            return itemFrame.isValid();
            
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Calculates the exact location for an item frame on a specific face of a block.
     * @param block The block to attach the item frame to
     * @param face The face of the block
     * @return The location for the item frame
     */
    private Location getItemFrameLocation(Block block, BlockFace face) {
        // Get the location of the adjacent block (where the item frame will be placed)
        Block adjacentBlock = block.getRelative(face);
        Location location = adjacentBlock.getLocation().clone();
        
        // Position the item frame in the center of the adjacent block face
        // The item frame will be attached to the farm block from the adjacent block
        switch (face) {
            case SOUTH:
                location.add(0.5, 0.5, 0.0625); // Near the north face of the adjacent block
                break;
            case NORTH:
                location.add(0.5, 0.5, 0.9375); // Near the south face of the adjacent block
                break;
            case EAST:
                location.add(0.0625, 0.5, 0.5); // Near the west face of the adjacent block
                break;
            case WEST:
                location.add(0.9375, 0.5, 0.5); // Near the east face of the adjacent block
                break;
            default:
                // Fallback to center
                location.add(0.5, 0.5, 0.5);
        }
        
        return location;
    }

    /**
     * Gets the direction the player is facing relative to the block.
     * @param player The player
     * @param block The block
     * @return The BlockFace the player is facing
     */
    private BlockFace getPlayerFacingDirection(Player player, Block block) {
        // Use the player's actual facing direction from Bukkit
        float yaw = player.getLocation().getYaw();
        
        // Normalize yaw to 0-360 range
        while (yaw < 0) yaw += 360;
        while (yaw >= 360) yaw -= 360;
        
        // Convert yaw to BlockFace with more precise boundaries
        // Yaw 0 = South, 90 = West, 180 = North, 270 = East
        BlockFace result;
        if (yaw >= 315 || yaw < 45) {
            result = BlockFace.SOUTH;
        } else if (yaw >= 45 && yaw < 135) {
            result = BlockFace.WEST;
        } else if (yaw >= 135 && yaw < 225) {
            result = BlockFace.NORTH;
        } else { // yaw >= 225 && yaw < 315
            result = BlockFace.EAST;
        }
        
        return result;
    }

    /**
     * Removes the visual indicator (item frame) from a farm.
     * @param farmBlock The farm block
     */
    private void removeFarmVisualIndicator(Block farmBlock) {
        // Find and remove any item frames associated with this farm
        Location farmLocation = farmBlock.getLocation();
        
        // Check nearby entities for item frames
        farmBlock.getWorld().getNearbyEntities(farmLocation, 2, 2, 2).forEach(entity -> {
            if (entity instanceof ItemFrame) {
                ItemFrame itemFrame = (ItemFrame) entity;
                PersistentDataContainer pdc = itemFrame.getPersistentDataContainer();
                
                // Check if this item frame belongs to this farm
                if (pdc.has(new NamespacedKey(plugin, "farm_item_frame"), PersistentDataType.STRING)) {
                    String farmLocationStr = pdc.get(new NamespacedKey(plugin, "farm_location"), PersistentDataType.STRING);
                    if (farmLocationStr != null && farmLocationStr.equals(formatLocation(farmLocation))) {
                        // This item frame belongs to this farm, remove it
                        itemFrame.remove();
                    }
                }
            }
        });
    }

    /**
     * Handles item frame breaking to protect farm item frames.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onHangingBreak(HangingBreakEvent event) {
        if (!(event.getEntity() instanceof ItemFrame)) {
            return;
        }

        ItemFrame itemFrame = (ItemFrame) event.getEntity();
        PersistentDataContainer pdc = itemFrame.getPersistentDataContainer();
        
        // Check if this is a farm item frame
        if (pdc.has(new NamespacedKey(plugin, "farm_item_frame"), PersistentDataType.STRING)) {
            // This is a farm item frame, check if it's being broken by a player
            if (event instanceof HangingBreakByEntityEvent) {
                HangingBreakByEntityEvent entityEvent = (HangingBreakByEntityEvent) event;
                if (entityEvent.getRemover() instanceof Player) {
                    Player player = (Player) entityEvent.getRemover();
                    
                    // Get the farm location from the item frame
                    String farmLocationStr = pdc.get(new NamespacedKey(plugin, "farm_location"), PersistentDataType.STRING);
                    if (farmLocationStr != null) {
                        // Parse the location and check if the player can break the farm
                        try {
                            // For now, prevent breaking the item frame directly
                            // Players should break the farm block instead
                            event.setCancelled(true);
                            player.sendMessage("§cYou cannot break the item frame directly. Break the farm block instead.");
                            return;
                        } catch (Exception e) {
                            // Silently handle errors
                        }
                    }
                }
            }
            
            // For non-player causes (like explosions), also protect the frame
            event.setCancelled(true);
        }
    }
}