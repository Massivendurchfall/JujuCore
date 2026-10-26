package org.juju.jujucore.manager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.juju.jujucore.util.Text;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VanishManager {

    private final JavaPlugin plugin;
    private final NamespacedKey vanishedKey;
    private final NamespacedKey waypointKey;
    // concurrent because sculk events can come from another thread
    private final Set<UUID> vanished = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> since = new ConcurrentHashMap<>();

    public VanishManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.vanishedKey = new NamespacedKey(plugin, "vanished");
        this.waypointKey = new NamespacedKey(plugin, "vanish_waypoint");
        Component bar = LegacyComponentSerializer.legacySection().deserialize(Text.color("&bVanish &8» &7Niemand sieht dich"));
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID id : vanished) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) p.sendActionBar(bar);
            }
        }, 40L, 40L);
    }

    public boolean toggle(Player player) {
        if (isVanished(player)) {
            show(player);
            return false;
        }
        hide(player);
        return true;
    }

    public void hide(Player player) {
        player.getPersistentDataContainer().set(vanishedKey, PersistentDataType.BYTE, (byte) 1);
        apply(player);
        // fake leave msg so it looks normal when they disappear from tab
        if (fakeMessages()) {
            sendToNonStaff(Component.translatable("multiplayer.player.left", NamedTextColor.YELLOW, player.displayName()), player);
        }
        notifyStaff("&8[&bVanish&8] &7" + player.getName() + " ist jetzt vanished.", player);
    }

    public void show(Player player) {
        player.getPersistentDataContainer().remove(vanishedKey);
        forget(player);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showPlayer(plugin, player);
        }
        player.setCollidable(true);
        player.setSleepingIgnored(false);
        player.setAffectsSpawning(true);
        AttributeInstance wp = player.getAttribute(Attribute.WAYPOINT_TRANSMIT_RANGE);
        if (wp != null) wp.removeModifier(waypointKey);
        player.sendActionBar(Component.empty());
        if (fakeMessages()) {
            sendToNonStaff(Component.translatable("multiplayer.player.joined", NamedTextColor.YELLOW, player.displayName()), player);
        }
        notifyStaff("&8[&bVanish&8] &7" + player.getName() + " ist nicht mehr vanished.", player);
    }

    // vanish is saved in the player data so it stays after relog/restart.
    // returns true if they were vanished (then the join msg gets removed)
    public boolean restoreOnJoin(Player player) {
        if (!player.getPersistentDataContainer().has(vanishedKey, PersistentDataType.BYTE)) {
            return false;
        }
        apply(player);
        notifyStaff("&8[&bVanish&8] &7" + player.getName() + " ist vanished beigetreten.", player);
        return true;
    }

    public void forget(Player player) {
        vanished.remove(player.getUniqueId());
        since.remove(player.getUniqueId());
    }

    public boolean isVanished(Player player) {
        return vanished.contains(player.getUniqueId());
    }

    public boolean isVanished(UUID id) {
        return vanished.contains(id);
    }

    public long vanishedSince(Player player) {
        return since.getOrDefault(player.getUniqueId(), System.currentTimeMillis());
    }

    public int onlineVanishedCount() {
        int n = 0;
        for (UUID id : vanished) {
            if (Bukkit.getPlayer(id) != null) n++;
        }
        return n;
    }

    public String seePermission() {
        return plugin.getConfig().getString("vanish-see-permission", "jujucore.vanish.see");
    }

    // can the sender know the target is online?
    public boolean canSee(CommandSender viewer, Player target) {
        if (!(viewer instanceof Player p) || p.equals(target) || !isVanished(target)) {
            return true;
        }
        return p.hasPermission(seePermission());
    }

    public void applyForJoin(Player joiner) {
        for (UUID id : vanished) {
            Player v = Bukkit.getPlayer(id);
            if (v != null && !v.equals(joiner) && !joiner.hasPermission(seePermission())) {
                joiner.hidePlayer(plugin, v);
            }
        }
    }

    public void refreshViewer(Player viewer) {
        for (UUID id : vanished) {
            Player v = Bukkit.getPlayer(id);
            if (v == null || v.equals(viewer)) continue;
            if (viewer.hasPermission(seePermission())) {
                viewer.showPlayer(plugin, v);
            } else {
                viewer.hidePlayer(plugin, v);
            }
        }
    }

    private void apply(Player player) {
        vanished.add(player.getUniqueId());
        since.put(player.getUniqueId(), System.currentTimeMillis());
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(player) && !p.hasPermission(seePermission())) {
                p.hidePlayer(plugin, player);
            }
        }
        player.setCollidable(false);
        // dont count for sleeping + no mob/phantom spawns around them
        player.setSleepingIgnored(true);
        player.setAffectsSpawning(false);
        // otherwise the locator bar still shows a dot where they are
        AttributeInstance wp = player.getAttribute(Attribute.WAYPOINT_TRANSMIT_RANGE);
        if (wp != null && wp.getModifier(waypointKey) == null) {
            wp.addTransientModifier(new AttributeModifier(waypointKey, -1.0D, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
        }
    }

    private boolean fakeMessages() {
        return plugin.getConfig().getBoolean("vanish-fake-messages", true);
    }

    private void sendToNonStaff(Component msg, Player source) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(source) && !p.hasPermission(seePermission())) {
                p.sendMessage(msg);
            }
        }
    }

    private void notifyStaff(String msg, Player source) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission(seePermission()) && !p.equals(source)) {
                Text.raw(p, msg);
            }
        }
    }
}
