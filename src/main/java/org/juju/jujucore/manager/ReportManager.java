package org.juju.jujucore.manager;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class ReportManager {

    private final JavaPlugin plugin;
    private final File file;
    private FileConfiguration reports;

    public ReportManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "reports.yml");
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
                plugin.getLogger().severe("Could not create reports.yml: " + e.getMessage());
            }
        }
        reports = YamlConfiguration.loadConfiguration(file);
        if (!reports.contains("next-id")) {
            reports.set("next-id", 1);
            save();
        }
    }

    public void save() {
        try {
            reports.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save reports.yml: " + e.getMessage());
        }
    }

    public int create(String reporter, String target, String reason) {
        int id = reports.getInt("next-id", 1);
        String path = "reports." + id;
        reports.set(path + ".id", id);
        reports.set(path + ".reporter", reporter);
        reports.set(path + ".target", target);
        reports.set(path + ".reason", reason);
        reports.set(path + ".time", System.currentTimeMillis());
        reports.set(path + ".closed", false);
        reports.set("next-id", id + 1);
        save();
        return id;
    }

    public boolean exists(int id) {
        return reports.contains("reports." + id);
    }

    public boolean close(int id, String closer) {
        String path = "reports." + id;
        if (!reports.contains(path)) {
            return false;
        }
        reports.set(path + ".closed", true);
        reports.set(path + ".closed-by", closer);
        reports.set(path + ".closed-time", System.currentTimeMillis());
        save();
        return true;
    }

    public void clearClosed() {
        ConfigurationSection sec = reports.getConfigurationSection("reports");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            if (reports.getBoolean("reports." + key + ".closed", false)) {
                reports.set("reports." + key, null);
            }
        }
        save();
    }

    public List<Integer> activeReports() {
        List<Integer> ids = new ArrayList<>();
        ConfigurationSection sec = reports.getConfigurationSection("reports");
        if (sec == null) return ids;
        for (String key : sec.getKeys(false)) {
            if (!reports.getBoolean("reports." + key + ".closed", false)) {
                try {
                    ids.add(Integer.parseInt(key));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        ids.sort(Comparator.naturalOrder());
        return ids;
    }

    public String summary(int id) {
        String path = "reports." + id;
        if (!reports.contains(path)) {
            return null;
        }
        String reporter = reports.getString(path + ".reporter", "Unknown");
        String target = reports.getString(path + ".target", "Unknown");
        String reason = reports.getString(path + ".reason", "Unknown");
        boolean closed = reports.getBoolean(path + ".closed", false);
        long time = reports.getLong(path + ".time", 0L);
        return "&8#&b" + id + " &7" + target + " &8von &7" + reporter + " &8- &f" + reason + " &8(" + format(time) + ", " + (closed ? "closed" : "open") + ")";
    }

    public String details(int id) {
        String path = "reports." + id;
        if (!reports.contains(path)) {
            return null;
        }
        String reporter = reports.getString(path + ".reporter", "Unknown");
        String target = reports.getString(path + ".target", "Unknown");
        String reason = reports.getString(path + ".reason", "Unknown");
        boolean closed = reports.getBoolean(path + ".closed", false);
        String closedBy = reports.getString(path + ".closed-by", "-");
        long time = reports.getLong(path + ".time", 0L);
        return "&bReport #" + id + "\n&7Target: &f" + target + "\n&7Reporter: &f" + reporter + "\n&7Reason: &f" + reason + "\n&7Time: &f" + format(time) + "\n&7Status: &f" + (closed ? "closed by " + closedBy : "open");
    }

    private String format(long time) {
        if (time <= 0L) {
            return "unknown";
        }
        return new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(time));
    }
}
