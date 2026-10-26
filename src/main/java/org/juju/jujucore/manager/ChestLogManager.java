package org.juju.jujucore.manager;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;

// logs who opened which chest and what they took/put in.
// format (plugins/JujuCore/kisten-log.txt): millis, world, x, y, z, player, action (tab separated)
// everything here runs async
public class ChestLogManager {

    private static final long MAX_SIZE = 5L * 1024 * 1024;

    private final JavaPlugin plugin;
    private final File file;
    private final File oldFile;
    private final SimpleDateFormat fmt = new SimpleDateFormat("dd.MM. HH:mm", Locale.GERMANY);

    public ChestLogManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "kisten-log.txt");
        this.oldFile = new File(plugin.getDataFolder(), "kisten-log.old.txt");
    }

    public void log(Collection<Block> parts, String player, String action) {
        Block b = parts.iterator().next();
        String line = System.currentTimeMillis() + "\t" + b.getWorld().getName() + "\t" + b.getX() + "\t"
                + b.getY() + "\t" + b.getZ() + "\t" + player + "\t" + action.replace('\t', ' ').replace('\n', ' ');
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> append(line));
    }

    private synchronized void append(String line) {
        try {
            if (file.length() > MAX_SIZE) {
                Files.move(file.toPath(), oldFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            try (BufferedWriter w = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                w.write(line);
                w.newLine();
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Kisten-Log konnte nicht geschrieben werden", e);
        }
    }

    public record Page(int total, List<String> lines) {
    }

    // newest first, works for both halves of a double chest. callback runs on the main thread
    public void lookup(Collection<Block> parts, int page, int perPage, Consumer<Page> callback) {
        Set<String> keys = new HashSet<>();
        for (Block b : parts) {
            keys.add(b.getWorld().getName() + "\t" + b.getX() + "\t" + b.getY() + "\t" + b.getZ());
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<String> hits = new ArrayList<>();
            synchronized (this) {
                for (File f : new File[]{oldFile, file}) {
                    if (!f.exists()) continue;
                    try {
                        for (String line : Files.readAllLines(f.toPath(), StandardCharsets.UTF_8)) {
                            String[] cols = line.split("\t", 7);
                            if (cols.length == 7 && keys.contains(cols[1] + "\t" + cols[2] + "\t" + cols[3] + "\t" + cols[4])) {
                                hits.add(fmt.format(new Date(Long.parseLong(cols[0]))) + "  &b" + cols[5] + " &7" + cols[6]);
                            }
                        }
                    } catch (IOException | NumberFormatException e) {
                        plugin.getLogger().log(Level.WARNING, "Kisten-Log konnte nicht gelesen werden", e);
                    }
                }
            }
            List<String> newest = new ArrayList<>();
            for (int i = hits.size() - 1; i >= 0; i--) {
                newest.add(hits.get(i));
            }
            int from = Math.min(newest.size(), (page - 1) * perPage);
            int to = Math.min(newest.size(), from + perPage);
            Page p = new Page(newest.size(), new ArrayList<>(newest.subList(from, to)));
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(p));
        });
    }
}
