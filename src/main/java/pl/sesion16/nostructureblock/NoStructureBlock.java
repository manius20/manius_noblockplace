package pl.sesion16.nostructureblock;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class NoStructureBlock extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private final Set<Material> blockedMaterials = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadBlockedBlocks();

        getServer().getPluginManager().registerEvents(this, this);

        Objects.requireNonNull(getCommand("nostructureblock")).setExecutor(this);
        Objects.requireNonNull(getCommand("nostructureblock")).setTabCompleter(this);

        getLogger().info("NoStructureBlock został pomyślnie włączony!");
    }

    @Override
    public void onDisable() {
        blockedMaterials.clear();
        getLogger().info("NoStructureBlock został wyłączony.");
    }

    public void loadBlockedBlocks() {
        blockedMaterials.clear();
        if (!getConfig().getBoolean("blocked-blocks.enabled", true)) return;

        List<String> list = getConfig().getStringList("blocked-blocks.list");
        for (String name : list) {
            Material mat = Material.matchMaterial(name);
            if (mat != null) {
                blockedMaterials.add(mat);
            } else {
                getLogger().warning("Nie znaleziono bloku o nazwie: " + name);
            }
        }
    }

    private Component color(String text) {
        if (text == null) return Component.empty();
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!getConfig().getBoolean("blocked-blocks.enabled", true)) return;

        Player player = event.getPlayer();
        String bypassPermission = getConfig().getString("settings.bypass-permission", "nostructureblock.admin");

        if (player.hasPermission(bypassPermission)) return;

        Material placedMaterial = event.getBlockPlaced().getType();
        if (blockedMaterials.contains(placedMaterial)) {
            event.setCancelled(true);
            String rawMsg = getConfig().getString("blocked-blocks.deny-message", "&cNie masz uprawnień do stawiania bloku &e%block%&c!");
            String formattedMsg = rawMsg.replace("%block%", placedMaterial.name());
            player.sendMessage(color(formattedMsg));
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nostructureblock.admin")) {
            sender.sendMessage(color("&cNie masz uprawnień do tej komendy!"));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            loadBlockedBlocks();
            sender.sendMessage(color("&aKonfiguracja NoStructureBlock została pomyślnie przeładowana!"));
            return true;
        }

        sender.sendMessage(color("&cUżycie: /nostructureblock reload"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("nostructureblock.admin")) {
            return List.of("reload");
        }
        return List.of();
    }
}
