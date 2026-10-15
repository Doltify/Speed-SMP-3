package com.speedsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;

/** Carrying the Ender Dragon Egg grants an extra row of hearts (+10 hearts = +20 health). */
public class EggBuff {
    private static final double BONUS_HEALTH = 20.0;
    private final NamespacedKey key;

    public EggBuff(SpeedSMP plugin) {
        this.key = new NamespacedKey(plugin, "dragon_egg_hearts");
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 10L);
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            update(player);
        }
    }

    private AttributeModifier findModifier(AttributeInstance attribute) {
        for (AttributeModifier m : attribute.getModifiers()) {
            if (key.equals(m.getKey())) {
                return m;
            }
        }
        return null;
    }

    private void update(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (attribute == null) {
            return;
        }
        boolean hasEgg = player.getInventory().contains(Material.DRAGON_EGG);
        AttributeModifier existing = findModifier(attribute);

        if (hasEgg && existing == null) {
            attribute.addTransientModifier(new AttributeModifier(
                    key, BONUS_HEALTH, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
            player.setHealth(Math.min(attribute.getValue(), player.getHealth() + BONUS_HEALTH));
            player.sendActionBar(Component.text("The Dragon Egg grants you extra hearts!", NamedTextColor.LIGHT_PURPLE));
        } else if (!hasEgg && existing != null) {
            attribute.removeModifier(existing);
            if (player.getHealth() > attribute.getValue()) {
                player.setHealth(attribute.getValue());
            }
        }
    }
}
