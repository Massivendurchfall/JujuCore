package org.juju.jujucore.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// spawn, homes, warps, kit cooldowns -> data.yml
public class DataManager {

    private final JavaPlugin plugin;
    private final File file;
    private FileConfiguration cfg;
    private final Map<UUID, Location> lastLocs = new HashMap<>();

    public DataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        reload();
    }

    public void reload() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create data.yml: " + e.getMessage());
            }
        }
        cfg = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.yml: " + e.getMessage());
        }
    }

    public void setSpawn(Location loc) {
        writeLoc("spawn", loc);
        save();
    }

    public Location getSpawn() {
        return readLoc("spawn");
    }

    public void setHome(UUID uuid, String name, Location loc) {
        writeLoc("homes." + uuid + "." + clean(name, "home"), loc);
        save();
    }

    public Location getHome(UUID uuid, String name) {
        return readLoc("homes." + uuid + "." + clean(name, "home"));
    }

    public boolean deleteHome(UUID uuid, String name) {
        String path = "homes." + uuid + "." + clean(name, "home");
        if (!cfg.contains(path)) return false;
        cfg.set(path, null);
        save();
        return true;
    }

    public List<String> getHomes(UUID uuid) {
        return keys("homes." + uuid);
    }

    public int getHomeCount(UUID uuid) {
        return getHomes(uuid).size();
    }

    public void setWarp(String name, Location loc) {
        writeLoc("warps." + clean(name, "spawn"), loc);
        save();
    }

    public Location getWarp(String name) {
        return readLoc("warps." + clean(name, "spawn"));
    }

    public boolean deleteWarp(String name) {
        String path = "warps." + clean(name, "spawn");
        if (!cfg.contains(path)) return false;
        cfg.set(path, null);
        save();
        return true;
    }

    public List<String> getWarps() {
        return keys("warps");
    }

    public void setKitLastUse(UUID uuid, String name, long time) {
        cfg.set("kit-cooldowns." + uuid + "." + clean(name, "default"), time);
        save();
    }

    public long getKitLastUse(UUID uuid, String name) {
        return cfg.getLong("kit-cooldowns." + uuid + "." + clean(name, "default"), 0L);
    }

    // /back, only in memory
    public void setBackLocation(UUID uuid, Location loc) {
        if (loc != null) lastLocs.put(uuid, loc.clone());
    }

    public Location getBackLocation(UUID uuid) {
        Location loc = lastLocs.get(uuid);
        return loc == null ? null : loc.clone();
    }

    public String cleanHomeName(String name) {
        return clean(name, "home");
    }

    public String cleanWarpName(String name) {
        return clean(name, "spawn");
    }

    private List<String> keys(String path) {
        ConfigurationSection sec = cfg.getConfigurationSection(path);
        List<String> list = new ArrayList<>();
        if (sec != null) list.addAll(sec.getKeys(false));
        return list;
    }

    private String clean(String name, String fallback) {
        if (name == null || name.isBlank()) return fallback;
        String s = name.toLowerCase().replaceAll("[^a-z0-9_-]", "");
        return s.isBlank() ? fallback : s;
    }

    private void writeLoc(String path, Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        cfg.set(path + ".world", loc.getWorld().getName());
        cfg.set(path + ".x", loc.getX());
        cfg.set(path + ".y", loc.getY());
        cfg.set(path + ".z", loc.getZ());
        cfg.set(path + ".yaw", loc.getYaw());
        cfg.set(path + ".pitch", loc.getPitch());
    }

    private Location readLoc(String path) {
        if (!cfg.contains(path + ".world")) return null;
        World world = Bukkit.getWorld(cfg.getString(path + ".world", ""));
        if (world == null) return null;
        double x = cfg.getDouble(path + ".x");
        double y = cfg.getDouble(path + ".y");
        double z = cfg.getDouble(path + ".z");
        float yaw = (float) cfg.getDouble(path + ".yaw");
        float pitch = (float) cfg.getDouble(path + ".pitch");
        return new Location(world, x, y, z, yaw, pitch);
    }
}
