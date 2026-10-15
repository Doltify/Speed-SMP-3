package com.speedsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseLootEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;

import java.util.ArrayList;
import java.util.List;

public class LimitListener implements Listener {
    private final ItemLimits limits;

    public LimitListener(ItemLimits limits) {
        this.limits = limits;
    }

    /** True if the smithing result is a brand new item (not just a trim on an existing piece). */
    private boolean createsNewItem(SmithingInventory inv, Material result) {
        ItemStack base = inv.getInputEquipment();
        return base == null || base.getType() != result;
    }

    @EventHandler
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        ItemStack result = event.getResult();
        if (result == null) {
            return;
        }
        Material m = result.getType();
        if (!limits.isTracked(m) || !createsNewItem(event.getInventory(), m)) {
            return;
        }
        if (limits.remaining(m) <= 0) {
            event.setResult(null);
            for (HumanEntity viewer : event.getViewers()) {
                if (viewer instanceof Player p) {
                    p.sendActionBar(Component.text("Server limit reached for this item.", NamedTextColor.RED));
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSmith(SmithItemEvent event) {
        ItemStack result = event.getCurrentItem();
        if (result == null) {
            return;
        }
        Material m = result.getType();
        if (!limits.isTracked(m) || !createsNewItem(event.getInventory(), m)) {
            return;
        }
        if (limits.issue(m, 1) < 1) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onLoot(LootGenerateEvent event) {
        event.setLoot(filter(event.getLoot()));
    }

    @EventHandler
    public void onVaultOrSpawnerLoot(BlockDispenseLootEvent event) {
        event.setDispensedLoot(filter(event.getDispensedLoot()));
    }

    private List<ItemStack> filter(List<ItemStack> loot) {
        List<ItemStack> kept = new ArrayList<>();
        for (ItemStack item : loot) {
            if (item == null) {
                continue;
            }
            if (!limits.isTracked(item.getType())) {
                kept.add(item);
                continue;
            }
            int allowed = limits.issue(item.getType(), item.getAmount());
            if (allowed > 0) {
                item.setAmount(allowed);
                kept.add(item);
            }
        }
        return kept;
    }
}
