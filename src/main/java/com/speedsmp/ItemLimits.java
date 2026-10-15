package com.speedsmp;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

/** Tracks how many of each limited item have been issued and enforces the caps. */
public class ItemLimits {
    private final SpeedSMP plugin;
    private final File dataFile;
    private final Map<Material, Integer> limits = new EnumMap<>(Material.class);
    private final Map<Material, Integer> issued = new EnumMap<>(Material.class);

    public ItemLimits(SpeedSMP plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        load();
    }

    private void load() {
        limits.clear();
        issued.clear();

        ConfigurationSection cfg = plugin.getConfig().getConfigurationSection("limits");
        if (cfg != null) {
            for (String key : cfg.getKeys(false)) {
                Material m = Material.matchMaterial(key);
                if (m == null) {
                    plugin.getLogger().warning("Unknown material in config limits: " + key);
                    continue;
                }
                limits.put(m, cfg.getInt(key));
            }
        }

        if (dataFile.exists()) {
            YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
            ConfigurationSection sec = data.getConfigurationSection("issued");
            if (sec != null) {
                for (String key : sec.getKeys(false)) {
                    Material m = Material.matchMaterial(key);
                    if (m != null) {
                        issued.put(m, sec.getInt(key));
                    }
                }
            }
        }
    }

    private void save() {
        YamlConfiguration data = new YamlConfiguration();
        for (Map.Entry<Material, Integer> e : issued.entrySet()) {
            data.set("issued." + e.getKey().name(), e.getValue());
        }
        try {
            data.save(dataFile);
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save data.yml: " + ex.getMessage());
        }
    }

    public boolean isTracked(Material m) {
        return limits.containsKey(m);
    }

    public int getLimit(Material m) {
        return limits.getOrDefault(m, Integer.MAX_VALUE);
    }

    public int getIssued(Material m) {
        return issued.getOrDefault(m, 0);
    }

    public int remaining(Material m) {
        return Math.max(0, getLimit(m) - getIssued(m));
    }

    public Iterable<Material> trackedMaterials() {
        return limits.keySet();
    }

    /** Tries to issue up to {@code amount} items. Returns how many were actually allowed. */
    public int issue(Material m, int amount) {
        if (!isTracked(m)) {
            return amount;
        }
        int allowed = Math.max(0, Math.min(amount, remaining(m)));
        if (allowed > 0) {
            issued.merge(m, allowed, Integer::sum);
            save();
        }
        return allowed;
    }

    /** Admin override, e.g. to account for items that already exist on the server. */
    public void setIssued(Material m, int count) {
        issued.put(m, Math.max(0, count));
        save();
    }
}
