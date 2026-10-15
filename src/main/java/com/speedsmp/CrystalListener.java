package com.speedsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class CrystalListener implements Listener {
    private final SpeedSMP plugin;
    private final SpeedCrystal crystal;

    public CrystalListener(SpeedSMP plugin, SpeedCrystal crystal) {
        this.plugin = plugin;
        this.crystal = crystal;
    }

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (!crystal.isCrystal(item)) {
            return;
        }

        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);

        Player player = event.getPlayer();
        if (plugin.getSpeed(player) + SpeedSMP.STEP > SpeedSMP.MAX_SPEED + 1e-9) {
            player.sendMessage(Component.text("Your speed is already at the maximum.", NamedTextColor.RED));
            return;
        }

        double speed = plugin.adjust(player, +SpeedSMP.STEP);
        item.setAmount(item.getAmount() - 1);
        player.sendActionBar(Component.text(
                String.format("Speed Crystal used. Speed is now %.2f", speed), NamedTextColor.GREEN));
    }
}
