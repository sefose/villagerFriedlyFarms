package com.example.villagerfriendlyfarms.command;

import com.example.villagerfriendlyfarms.VillagerFriendlyFarmsPlugin;
import com.example.villagerfriendlyfarms.config.GeneratorConfig;
import com.example.villagerfriendlyfarms.permission.PermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

/**
 * Handles generator-related commands.
 */
public class GeneratorCommand implements CommandExecutor {
    
    private final VillagerFriendlyFarmsPlugin plugin;
    private final PermissionManager permissionManager;
    private final NamespacedKey generatorTypeKey;

    public GeneratorCommand(VillagerFriendlyFarmsPlugin plugin) {
        this.plugin = plugin;
        this.permissionManager = plugin.getPermissionManager();
        this.generatorTypeKey = new NamespacedKey(plugin, "generator_type");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§eVillager Friendly Farms Commands:");
            sender.sendMessage("§7/vff info - Show plugin information");
            sender.sendMessage("§7/vff reload - Reload configuration");
            sender.sendMessage("§7/vff give <player> <type> - Give generator to player");
            sender.sendMessage("§7/vff list - List available generator types");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "info":
                return handleInfo(sender);
            case "reload":
                return handleReload(sender);
            case "give":
                return handleGive(sender, args);
            case "list":
                return handleList(sender);
            default:
                sender.sendMessage("§cUnknown command. Use /vff for help.");
                return true;
        }
    }

    private boolean handleInfo(CommandSender sender) {
        sender.sendMessage("§eVillager Friendly Farms v" + plugin.getDescription().getVersion());
        sender.sendMessage("§7Loaded generators: " + plugin.getConfigManager().getAllConfigurations().size());
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }
        
        Player player = (Player) sender;
        if (!permissionManager.canReload(player)) {
            sender.sendMessage(permissionManager.getPermissionErrorMessage(PermissionManager.PERMISSION_RELOAD));
            return true;
        }

        sender.sendMessage("§eReloading plugin configuration and recipes...");
        plugin.getConfigManager().reloadConfigurations();
        plugin.getRecipeManager().reloadRecipes();
        sender.sendMessage("§aConfiguration and recipes reloaded successfully!");
        sender.sendMessage("§7Players may need to close and reopen their recipe book to see changes.");
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }
        
        Player player = (Player) sender;
        if (!permissionManager.canGiveGenerators(player)) {
            sender.sendMessage(permissionManager.getPermissionErrorMessage(PermissionManager.PERMISSION_GIVE));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("§cUsage: /vff give <player> <type>");
            sender.sendMessage("§7Available types: iron_farm, villager_breeder");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found: " + args[1]);
            return true;
        }

        String generatorType = args[2].toLowerCase();
        GeneratorConfig config = plugin.getConfigManager().getGeneratorConfig(generatorType);
        if (config == null) {
            sender.sendMessage("§cInvalid generator type: " + generatorType);
            sender.sendMessage("§7Available types: iron_farm, villager_breeder");
            return true;
        }

        // Create generator item
        ItemStack generatorItem = createGeneratorItem(config);
        target.getInventory().addItem(generatorItem);
        
        sender.sendMessage("§aGave " + formatGeneratorName(generatorType) + " to " + target.getName());
        target.sendMessage("§aYou received a " + formatGeneratorName(generatorType) + "!");
        
        return true;
    }

    private boolean handleList(CommandSender sender) {
        sender.sendMessage("§eAvailable Generator Types:");
        for (String type : plugin.getConfigManager().getAllConfigurations().keySet()) {
            GeneratorConfig config = plugin.getConfigManager().getGeneratorConfig(type);
            sender.sendMessage("§7- " + formatGeneratorName(type) + " §8(" + type + ")");
            sender.sendMessage("  §8Output: " + config.getOutput().getAmount() + "x " + 
                             formatMaterialName(config.getOutput().getType()) + 
                             " every " + config.getGenerationTimeSeconds() + "s");
        }
        return true;
    }

    private ItemStack createGeneratorItem(GeneratorConfig config) {
        ItemStack item = new ItemStack(config.getBlockType(), 1);
        
        item.editMeta(meta -> {
            meta.setDisplayName("§6" + formatGeneratorName(config.getName()));
            meta.setLore(java.util.Arrays.asList(
                "§7Generator Type: §f" + config.getName(),
                "§7Output: §f" + config.getOutput().getAmount() + "x " + 
                    formatMaterialName(config.getOutput().getType()),
                "§7Generation Time: §f" + config.getGenerationTimeSeconds() + "s",
                "§7Storage Capacity: §f" + config.getStorageCapacity() + " items",
                "",
                "§ePlace this block to create a generator!"
            ));

            // Add persistent data
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(generatorTypeKey, PersistentDataType.STRING, config.getName());
        });

        return item;
    }

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

    private String formatMaterialName(org.bukkit.Material material) {
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