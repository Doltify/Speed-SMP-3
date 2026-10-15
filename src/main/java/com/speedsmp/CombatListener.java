package com.speedsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class CombatListener implements Listener {
    private final SpeedSMP plugin;
    private final CombatManager combat;

    public CombatListener(SpeedSMP plugin, CombatManager combat) {
        this.plugin = plugin;
        this.combat = combat;
    }

    /** Tag both players whenever one damages the other (melee or projectile). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Entity damager = event.getDamager();
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            damager = shooter;
        }
        if (damager instanceof Player attacker && !attacker.equals(victim)) {
            combat.tag(attacker, victim);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        combat.clear(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!combat.isTagged(player.getUniqueId())) {
            return;
        }
        String label = event.getMessage().substring(1).split(" ")[0];
        int colon = label.indexOf(':');
        if (colon >= 0) {
            label = label.substring(colon + 1); // strip "plugin:" prefix
        }
        if (combat.isBlockedCommand(label)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("You can't use that command while in combat.", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();

        // Don't punish server shutdowns, kicks, or players who aren't actually alive and fighting.
        PlayerQuitEvent.QuitReason reason = event.getReason();
        if (plugin.getServer().isStopping()
                || reason == PlayerQuitEvent.QuitReason.KICKED
                || reason == PlayerQuitEvent.QuitReason.ERRONEOUS_STATE
                || !combat.isTagged(id)
                || player.isDead()
                || player.getGameMode() == GameMode.CREATIVE
                || player.getGameMode() == GameMode.SPECTATOR) {
            combat.clear(id);
            return;
        }

        UUID opponentId = combat.getOpponent(id);
        Player opponent = opponentId == null ? null : Bukkit.getPlayer(opponentId);

        // Count it as a PvP death: the combat logger loses speed, the opponent (if online) gains it.
        combat.markPunished(id);
        plugin.adjust(player, -SpeedSMP.STEP);
        if (opponent != null && opponent.isOnline()) {
            double speed = plugin.adjust(opponent, +SpeedSMP.STEP);
            opponent.sendActionBar(Component.text(
                    String.format("%s combat logged. Speed is now %.2f", player.getName(), speed),
                    NamedTextColor.GREEN));
        }

        combat.clear(id);
        event.quitMessage(Component.text(player.getName() + " combat logged and was killed.", NamedTextColor.RED));
        player.setHealth(0.0); // drops items; bypasses totems
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Clear any leftover state from the previous session.
        combat.clear(event.getPlayer().getUniqueId());
        combat.consumePunished(event.getPlayer().getUniqueId());
    }
}
