package com.speedsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SpeedCommand implements CommandExecutor, TabCompleter {
    private final SpeedSMP plugin;

    public SpeedCommand(SpeedSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return false;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (sub.equals("withdraw")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Only players can withdraw speed.", NamedTextColor.RED));
                return true;
            }
            double current = plugin.getSpeed(player);
            if (current - SpeedSMP.STEP < SpeedSMP.DEFAULT_SPEED - 1e-9) {
                player.sendMessage(Component.text("You can't withdraw speed while at base speed.", NamedTextColor.RED));
                return true;
            }
            double newSpeed = plugin.adjust(player, -SpeedSMP.STEP);
            var leftover = player.getInventory().addItem(plugin.getSpeedCrystal().create());
            leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            player.sendMessage(Component.text(
                    String.format("You withdrew a Speed Crystal. Speed is now %.2f", newSpeed), NamedTextColor.AQUA));
            return true;
        }

        if (!sender.hasPermission("speedsmp.admin")) {
            sender.sendMessage(Component.text("You don't have permission to use that.", NamedTextColor.RED));
            return true;
        }

        switch (sub) {
            case "limits" -> {
                ItemLimits limits = plugin.getItemLimits();
                sender.sendMessage(Component.text("Item limits (issued/limit):", NamedTextColor.GOLD));
                for (Material m : limits.trackedMaterials()) {
                    sender.sendMessage(Component.text(
                            " " + m.name() + ": " + limits.getIssued(m) + "/" + limits.getLimit(m),
                            NamedTextColor.YELLOW));
                }
                return true;
            }
            case "setcount" -> {
                if (args.length != 3) {
                    return false;
                }
                Material m = Material.matchMaterial(args[1]);
                if (m == null || !plugin.getItemLimits().isTracked(m)) {
                    sender.sendMessage(Component.text("Not a limited item: " + args[1], NamedTextColor.RED));
                    return true;
                }
                int count;
                try {
                    count = Integer.parseInt(args[2]);
                } catch (NumberFormatException ex) {
                    sender.sendMessage(Component.text("Count must be a number.", NamedTextColor.RED));
                    return true;
                }
                plugin.getItemLimits().setIssued(m, count);
                sender.sendMessage(Component.text(
                        "Set issued count of " + m.name() + " to " + Math.max(0, count), NamedTextColor.GREEN));
                return true;
            }
            case "reset", "get" -> {
                if (args.length != 2) {
                    return false;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(Component.text("Player not found or not online: " + args[1], NamedTextColor.RED));
                    return true;
                }
                if (args[0].equalsIgnoreCase("reset")) {
                    plugin.reset(target);
                    sender.sendMessage(Component.text(
                            "Reset " + target.getName() + "'s speed to " + SpeedSMP.DEFAULT_SPEED, NamedTextColor.GREEN));
                    target.sendMessage(Component.text("Your speed was reset to default.", NamedTextColor.YELLOW));
                } else {
                    sender.sendMessage(Component.text(
                            target.getName() + "'s speed: " + plugin.getSpeed(target), NamedTextColor.AQUA));
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : List.of("withdraw", "reset", "get", "limits", "setcount")) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(s);
            }
        } else if (args.length == 2) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            if (args[0].equalsIgnoreCase("setcount")) {
                for (Material m : plugin.getItemLimits().trackedMaterials()) {
                    if (m.name().toLowerCase(Locale.ROOT).startsWith(prefix)) out.add(m.name());
                }
            } else if (args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("get")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) out.add(p.getName());
                }
            }
        }
        return out;
    }
}
