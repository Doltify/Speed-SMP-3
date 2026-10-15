package com.speedsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Tracks which players are in combat and who they were fighting. */
public class CombatManager {
    private final long durationMs;
    private final Set<String> blockedCommands = new HashSet<>();
    private final Map<UUID, Long> expiry = new HashMap<>();
    private final Map<UUID, UUID> lastOpponent = new HashMap<>();
    private final Set<UUID> punished = new HashSet<>();

    public CombatManager(SpeedSMP plugin) {
        this.durationMs = Math.max(1, plugin.getConfig().getInt("combat.duration-seconds", 15)) * 1000L;
        for (String cmd : plugin.getConfig().getStringList("combat.blocked-commands")) {
            blockedCommands.add(cmd.toLowerCase(Locale.ROOT));
        }

        // Once per second: show the countdown and release players whose tag ran out.
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (UUID id : new ArrayList<>(expiry.keySet())) {
            Player p = Bukkit.getPlayer(id);
            long left = expiry.get(id) - now;
            if (left <= 0) {
                expiry.remove(id);
                lastOpponent.remove(id);
                if (p != null) {
                    p.sendActionBar(Component.text("You are no longer in combat.", NamedTextColor.GREEN));
                }
            } else if (p != null) {
                p.sendActionBar(Component.text(
                        "In combat: " + (left + 999) / 1000 + "s - do not log out!", NamedTextColor.RED));
            }
        }
    }

    public void tag(Player a, Player b) {
        tagOne(a, b);
        tagOne(b, a);
    }

    private void tagOne(Player player, Player opponent) {
        boolean wasTagged = isTagged(player.getUniqueId());
        expiry.put(player.getUniqueId(), System.currentTimeMillis() + durationMs);
        lastOpponent.put(player.getUniqueId(), opponent.getUniqueId());
        if (!wasTagged) {
            player.sendMessage(Component.text(
                    "You are in combat! Logging out will kill you.", NamedTextColor.RED));
        }
    }

    public boolean isTagged(UUID id) {
        Long e = expiry.get(id);
        return e != null && e > System.currentTimeMillis();
    }

    public UUID getOpponent(UUID id) {
        return lastOpponent.get(id);
    }

    public void clear(UUID id) {
        expiry.remove(id);
        lastOpponent.remove(id);
    }

    public boolean isBlockedCommand(String label) {
        return blockedCommands.contains(label.toLowerCase(Locale.ROOT));
    }

    /** Marks a death as a combat-log punishment so the normal death handler doesn't count it twice. */
    public void markPunished(UUID id) {
        punished.add(id);
    }

    /** Returns true (and clears the mark) if this player's death was a combat-log punishment. */
    public boolean consumePunished(UUID id) {
        return punished.remove(id);
    }
}
