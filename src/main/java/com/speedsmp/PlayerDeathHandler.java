package com.speedsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerDeathHandler implements Listener {
    private final SpeedSMP plugin;

    public PlayerDeathHandler(SpeedSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();

        // Combat-log deaths were already scored when the player quit.
        if (plugin.getCombatManager().consumePunished(victim.getUniqueId())) {
            return;
        }

        Player killer = victim.getKiller();

        if (killer == null || killer.equals(victim)) {
            return;
        }

        double killerSpeed = plugin.adjust(killer, +SpeedSMP.STEP);
        double victimSpeed = plugin.adjust(victim, -SpeedSMP.STEP);

        killer.sendActionBar(Component.text(
                String.format("Kill! Speed is now %.2f", killerSpeed), NamedTextColor.GREEN));
        victim.sendMessage(Component.text(
                String.format("You died to a player. Speed is now %.2f", victimSpeed), NamedTextColor.RED));
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.applyStored(player));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.applyStored(event.getPlayer());
    }
}
