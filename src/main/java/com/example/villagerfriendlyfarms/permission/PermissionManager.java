package com.example.villagerfriendlyfarms.permission;

import com.example.villagerfriendlyfarms.VillagerFriendlyFarmsPlugin;
import com.example.villagerfriendlyfarms.generator.GeneratorData;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Manages permissions for all generator operations.
 * Integrates with Bukkit's permission system and provides configurable permission requirements.
 */
public class PermissionManager {
    
    private final VillagerFriendlyFarmsPlugin plugin;
    private final Logger logger;
    
    // Permission nodes
    public static final String PERMISSION_CREATE = "villagerfriendlyfarms.create";
    public static final String PERMISSION_USE = "villagerfriendlyfarms.use";
    public static final String PERMISSION_ADMIN = "villagerfriendlyfarms.admin";
    public static final String PERMISSION_RELOAD = "villagerfriendlyfarms.reload";
    public static final String PERMISSION_GIVE = "villagerfriendlyfarms.give";
    public static final String PERMISSION_BYPASS_LIMITS = "villagerfriendlyfarms.bypass.limits";
    public static final String PERMISSION_ACCESS_ALL = "villagerfriendlyfarms.access.all";

    public PermissionManager(VillagerFriendlyFarmsPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /**
     * Initializes the permission manager and registers permissions.
     */
    public void initialize() {
        registerPermissions();
        logger.info("Permission manager initialized");
    }

    /**
     * Registers all plugin permissions with the server.
     */
    private void registerPermissions() {
        // Create permission
        registerPermission(PERMISSION_CREATE, 
            "Allows creating generators", 
            PermissionDefault.TRUE);
        
        // Use permission
        registerPermission(PERMISSION_USE, 
            "Allows using generators", 
            PermissionDefault.TRUE);
        
        // Admin permission
        registerPermission(PERMISSION_ADMIN, 
            "Allows administrative access to all generators", 
            PermissionDefault.OP);
        
        // Reload permission
        registerPermission(PERMISSION_RELOAD, 
            "Allows reloading plugin configuration", 
            PermissionDefault.OP);
        
        // Give permission
        registerPermission(PERMISSION_GIVE, 
            "Allows giving generators to players", 
            PermissionDefault.OP);
        
        // Bypass limits permission
        registerPermission(PERMISSION_BYPASS_LIMITS, 
            "Allows bypassing generator limits", 
            PermissionDefault.OP);
        
        // Access all permission
        registerPermission(PERMISSION_ACCESS_ALL, 
            "Allows accessing any player's generators", 
            PermissionDefault.OP);
    }

    /**
     * Registers a single permission with the server.
     * @param node The permission node
     * @param description The permission description
     * @param defaultValue The default permission value
     */
    private void registerPermission(String node, String description, PermissionDefault defaultValue) {
        Permission permission = new Permission(node, description, defaultValue);
        try {
            plugin.getServer().getPluginManager().addPermission(permission);
        } catch (IllegalArgumentException e) {
            // Permission already exists, update it
            Permission existing = plugin.getServer().getPluginManager().getPermission(node);
            if (existing != null) {
                existing.setDescription(description);
                existing.setDefault(defaultValue);
            }
        }
    }

    /**
     * Checks if a player can create generators.
     * @param player The player to check
     * @return True if the player can create generators
     */
    public boolean canCreateGenerator(Player player) {
        if (!plugin.getConfig().getBoolean("permissions.require-create-permission", true)) {
            return true; // Permission checking disabled
        }
        
        return player.hasPermission(PERMISSION_CREATE) || player.hasPermission(PERMISSION_ADMIN);
    }

    /**
     * Checks if a player can use generators.
     * @param player The player to check
     * @return True if the player can use generators
     */
    public boolean canUseGenerator(Player player) {
        if (!plugin.getConfig().getBoolean("permissions.require-use-permission", true)) {
            return true; // Permission checking disabled
        }
        
        return player.hasPermission(PERMISSION_USE) || player.hasPermission(PERMISSION_ADMIN);
    }

    /**
     * Checks if a player can access a specific generator.
     * @param player The player to check
     * @param generator The generator to access
     * @return True if the player can access the generator
     */
    public boolean canAccessGenerator(Player player, GeneratorData generator) {
        if (generator == null) {
            return false;
        }

        // Check if player is the owner
        if (player.getUniqueId().equals(generator.getOwner())) {
            return canUseGenerator(player);
        }

        // Check admin permissions
        if (plugin.getConfig().getBoolean("permissions.admin-override", true)) {
            return player.hasPermission(PERMISSION_ADMIN) || 
                   player.hasPermission(PERMISSION_ACCESS_ALL);
        }

        return false;
    }

    /**
     * Checks if a player can break a specific generator.
     * @param player The player to check
     * @param generator The generator to break
     * @return True if the player can break the generator
     */
    public boolean canBreakGenerator(Player player, GeneratorData generator) {
        if (generator == null) {
            return false;
        }

        // Check if player is the owner
        if (player.getUniqueId().equals(generator.getOwner())) {
            return true;
        }

        // Check admin permissions
        if (plugin.getConfig().getBoolean("permissions.admin-override", true)) {
            return player.hasPermission(PERMISSION_ADMIN);
        }

        return false;
    }

    /**
     * Checks if a player has administrative privileges.
     * @param player The player to check
     * @return True if the player is an admin
     */
    public boolean isAdmin(Player player) {
        return player.hasPermission(PERMISSION_ADMIN);
    }

    /**
     * Checks if a player can reload the plugin configuration.
     * @param player The player to check
     * @return True if the player can reload
     */
    public boolean canReload(Player player) {
        return player.hasPermission(PERMISSION_RELOAD) || player.hasPermission(PERMISSION_ADMIN);
    }

    /**
     * Checks if a player can give generators to other players.
     * @param player The player to check
     * @return True if the player can give generators
     */
    public boolean canGiveGenerators(Player player) {
        return player.hasPermission(PERMISSION_GIVE) || player.hasPermission(PERMISSION_ADMIN);
    }

    /**
     * Checks if a player can bypass generator limits.
     * @param player The player to check
     * @return True if the player can bypass limits
     */
    public boolean canBypassLimits(Player player) {
        return player.hasPermission(PERMISSION_BYPASS_LIMITS) || player.hasPermission(PERMISSION_ADMIN);
    }

    /**
     * Checks if a player can access any generator regardless of ownership.
     * @param player The player to check
     * @return True if the player can access all generators
     */
    public boolean canAccessAllGenerators(Player player) {
        return player.hasPermission(PERMISSION_ACCESS_ALL) || player.hasPermission(PERMISSION_ADMIN);
    }

    /**
     * Gets a user-friendly error message for insufficient permissions.
     * @param requiredPermission The permission that was required
     * @return A formatted error message
     */
    public String getPermissionErrorMessage(String requiredPermission) {
        String baseMessage = plugin.getConfig().getString("messages.no-permission", 
            "&cYou don't have permission to do that!");
        
        return baseMessage.replace("&", "§") + " §7(Required: " + requiredPermission + ")";
    }

    /**
     * Gets a user-friendly error message for generator access denial.
     * @return A formatted error message
     */
    public String getAccessDeniedMessage() {
        return plugin.getConfig().getString("messages.no-permission", 
            "&cYou don't have permission to do that!").replace("&", "§");
    }

    /**
     * Validates that a player meets all requirements for creating a generator.
     * @param player The player to validate
     * @param sendMessage Whether to send error messages to the player
     * @return True if the player can create a generator
     */
    public boolean validateGeneratorCreation(Player player, boolean sendMessage) {
        if (!canCreateGenerator(player)) {
            if (sendMessage) {
                player.sendMessage(getPermissionErrorMessage(PERMISSION_CREATE));
            }
            return false;
        }
        
        return true;
    }

    /**
     * Validates that a player meets all requirements for using a generator.
     * @param player The player to validate
     * @param generator The generator to access
     * @param sendMessage Whether to send error messages to the player
     * @return True if the player can use the generator
     */
    public boolean validateGeneratorAccess(Player player, GeneratorData generator, boolean sendMessage) {
        if (!canAccessGenerator(player, generator)) {
            if (sendMessage) {
                player.sendMessage(getAccessDeniedMessage());
            }
            return false;
        }
        
        return true;
    }

    /**
     * Validates that a player meets all requirements for breaking a generator.
     * @param player The player to validate
     * @param generator The generator to break
     * @param sendMessage Whether to send error messages to the player
     * @return True if the player can break the generator
     */
    public boolean validateGeneratorBreaking(Player player, GeneratorData generator, boolean sendMessage) {
        if (!canBreakGenerator(player, generator)) {
            if (sendMessage) {
                player.sendMessage(getAccessDeniedMessage());
            }
            return false;
        }
        
        return true;
    }

    /**
     * Logs a permission check for debugging purposes.
     * @param player The player being checked
     * @param permission The permission being checked
     * @param result The result of the check
     */
    private void logPermissionCheck(Player player, String permission, boolean result) {
        if (plugin.getConfig().getBoolean("plugin.debug", false)) {
            logger.info("Permission check: " + player.getName() + " -> " + permission + " = " + result);
        }
    }
}