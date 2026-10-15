package com.speedsmp;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class SpeedSMP extends JavaPlugin {
    public static final double DEFAULT_SPEED = 0.1;
    public static final double MIN_SPEED = 0.05;
    public static final double MAX_SPEED = 0.45;
    public static final double STEP = 0.02;

    private NamespacedKey speedKey;
    private ItemLimits itemLimits;
    private CombatManager combatManager;
    private SpeedCrystal speedCrystal;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        speedKey = new NamespacedKey(this, "speed");
        itemLimits = new ItemLimits(this);
        speedCrystal = new SpeedCrystal(this);
        getServer().getPluginManager().registerEvents(new CrystalListener(this, speedCrystal), this);
        new EggBuff(this);
        combatManager = new CombatManager(this);
        getServer().getPluginManager().registerEvents(new CombatListener(this, combatManager), this);
        getServer().getPluginManager().registerEvents(new LimitListener(itemLimits), this);

        getServer().getPluginManager().registerEvents(new PlayerDeathHandler(this), this);

        SpeedCommand command = new SpeedCommand(this);
        Objects.requireNonNull(getCommand("speedsmp")).setExecutor(command);
        Objects.requireNonNull(getCommand("speedsmp")).setTabCompleter(command);

        getLogger().info("SpeedSMP enabled.");
    }

    public SpeedCrystal getSpeedCrystal() {
        return speedCrystal;
    }

    public CombatManager getCombatManager() {
        return combatManager;
    }

    public ItemLimits getItemLimits() {
        return itemLimits;
    }

    public double getSpeed(Player player) {
        Double stored = player.getPersistentDataContainer().get(speedKey, PersistentDataType.DOUBLE);
        return stored == null ? DEFAULT_SPEED : clamp(stored);
    }

    public void applyStored(Player player) {
        Double stored = player.getPersistentDataContainer().get(speedKey, PersistentDataType.DOUBLE);
        if (stored != null) {
            applyToAttribute(player, clamp(stored));
        }
    }

    public double adjust(Player player, double delta) {
        double current = getSpeed(player);
        double newSpeed = current + delta;
        if (delta < 0) {
            // Never lose speed below the base speed (or below where the player already is).
            newSpeed = Math.max(newSpeed, Math.min(current, DEFAULT_SPEED));
        }
        newSpeed = clamp(newSpeed);
        player.getPersistentDataContainer().set(speedKey, PersistentDataType.DOUBLE, newSpeed);
        applyToAttribute(player, newSpeed);
        return newSpeed;
    }

    public void reset(Player player) {
        player.getPersistentDataContainer().remove(speedKey);
        applyToAttribute(player, DEFAULT_SPEED);
    }

    private void applyToAttribute(Player player, double speed) {
        AttributeInstance attribute = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attribute != null) {
            attribute.setBaseValue(speed);
        }
    }

    public static double clamp(double value) {
        double clamped = Math.max(MIN_SPEED, Math.min(MAX_SPEED, value));
        return Math.round(clamped * 1000.0) / 1000.0;
    }
}
