package org.juju.jujucore.manager;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class KitManager {

    private final JavaPlugin plugin;
    private final File file;
    private FileConfiguration kits;

    public KitManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "kits.yml");
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
                plugin.getLogger().severe("Could not create kits.yml: " + e.getMessage());
            }
        }
        kits = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            kits.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save kits.yml: " + e.getMessage());
        }
    }

    // saves the current inventory of the player as a kit
    public void createKit(String name, Player p, long cooldownSeconds) {
        String path = "kits." + cleanKitName(name);
        List<ItemStack> items = new ArrayList<>();
        addItems(items, p.getInventory().getStorageContents());
        addItems(items, p.getInventory().getArmorContents());
        ItemStack offhand = p.getInventory().getItemInOffHand();
        if (valid(offhand)) items.add(offhand.clone());
        kits.set(path + ".items", items);
        kits.set(path + ".cooldown-seconds", Math.max(0L, cooldownSeconds));
        kits.set(path + ".created-by", p.getName());
        kits.set(path + ".created-at", System.currentTimeMillis());
        save();
    }

    public boolean deleteKit(String name) {
        String path = "kits." + cleanKitName(name);
        if (!kits.contains(path)) return false;
        kits.set(path, null);
        save();
        return true;
    }

    public boolean exists(String name) {
        return kits.contains("kits." + cleanKitName(name));
    }

    public List<String> kitNames() {
        ConfigurationSection sec = kits.getConfigurationSection("kits");
        if (sec == null) return List.of();
        return sec.getKeys(false).stream().sorted(Comparator.naturalOrder()).toList();
    }

    public long getCooldownSeconds(String name) {
        return Math.max(0L, kits.getLong("kits." + cleanKitName(name) + ".cooldown-seconds", 0L));
    }

    public int getItemCount(String name) {
        return getItems(name).size();
    }

    // returns how many items had to be dropped because the inv was full
    public int giveKit(Player p, String name) {
        int dropped = 0;
        for (ItemStack item : getItems(name)) {
            Map<Integer, ItemStack> left = p.getInventory().addItem(item.clone());
            for (ItemStack it : left.values()) {
                if (!valid(it)) continue;
                p.getWorld().dropItemNaturally(p.getLocation(), it);
                dropped++;
            }
        }
        return dropped;
    }

    public String cleanKitName(String name) {
        if (name == null || name.isBlank()) return "default";
        String s = name.toLowerCase().replaceAll("[^a-z0-9_-]", "");
        return s.isBlank() ? "default" : s;
    }

    private List<ItemStack> getItems(String name) {
        List<?> raw = kits.getList("kits." + cleanKitName(name) + ".items", List.of());
        List<ItemStack> items = new ArrayList<>();
        for (Object o : raw) {
            if (o instanceof ItemStack item && valid(item)) items.add(item.clone());
        }
        return items;
    }

    private void addItems(List<ItemStack> items, ItemStack[] from) {
        for (ItemStack item : from) {
            if (valid(item)) items.add(item.clone());
        }
    }

    private boolean valid(ItemStack item) {
        return item != null && item.getType() != Material.AIR && item.getAmount() > 0;
    }
}
