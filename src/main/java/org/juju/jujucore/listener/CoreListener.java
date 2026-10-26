package org.juju.jujucore.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.persistence.PersistentDataType;
import org.juju.jujucore.JujuCore;
import org.juju.jujucore.manager.DataManager;
import org.juju.jujucore.manager.StateManager;
import org.juju.jujucore.manager.VanishManager;
import org.juju.jujucore.util.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CoreListener implements Listener {

    private final JujuCore plugin;
    private final DataManager data;
    private final VanishManager vanish;
    private final StateManager state;

    public CoreListener(JujuCore plugin, DataManager data, VanishManager vanish, StateManager state) {
        this.plugin = plugin;
        this.data = data;
        this.vanish = vanish;
        this.state = state;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent e) {
        if (vanish.restoreOnJoin(e.getPlayer())) {
            e.joinMessage(null);
        }
        vanish.applyForJoin(e.getPlayer());
        vanish.refreshViewer(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent e) {
        if (vanish.isVanished(e.getPlayer())) {
            e.quitMessage(null);
            vanish.forget(e.getPlayer());
        }
        state.clear(e.getPlayer().getUniqueId());
    }

    // troll tnt doesnt actually explode
    @EventHandler
    public void onExplosionPrime(ExplosionPrimeEvent e) {
        if (e.getEntity() instanceof TNTPrimed tnt && tnt.getPersistentDataContainer().has(plugin.trollTntKey(), PersistentDataType.BYTE)) {
            e.setCancelled(true);
            tnt.getWorld().playSound(tnt.getLocation(), "block.fire.extinguish", 1.0F, 1.4F);
            tnt.remove();
        }
    }

    // vanilla /list would still show vanished players
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onListCommand(PlayerCommandPreprocessEvent e) {
        String cmd = e.getMessage().length() > 1 ? e.getMessage().substring(1).split(" ")[0].toLowerCase(Locale.ROOT) : "";
        if (!cmd.equals("list") && !cmd.equals("minecraft:list")) return;
        Player p = e.getPlayer();
        if (vanish.onlineVanishedCount() == 0 || p.hasPermission(vanish.seePermission())) return;

        List<Component> names = new ArrayList<>();
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (vanish.canSee(p, other)) names.add(other.name());
        }
        e.setCancelled(true);
        p.sendMessage(Component.translatable("commands.list.players",
                Component.text(names.size()), Component.text(Bukkit.getMaxPlayers()), Component.join(JoinConfiguration.commas(true), names)));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        data.setBackLocation(e.getEntity().getUniqueId(), e.getEntity().getLocation());
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        if (e.isCancelled()) return;
        if (e.getFrom().getWorld() == null || state.isSilentTeleport(e.getPlayer().getUniqueId())) return;
        data.setBackLocation(e.getPlayer().getUniqueId(), e.getFrom());
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p && state.isGod(p.getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (!state.isFrozen(e.getPlayer().getUniqueId()) || e.getTo() == null) return;
        if (e.getFrom().getBlockX() != e.getTo().getBlockX() || e.getFrom().getBlockY() != e.getTo().getBlockY() || e.getFrom().getBlockZ() != e.getTo().getBlockZ()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onCommandSend(PlayerCommandSendEvent e) {
        if (!plugin.getConfig().getBoolean("hide-plugin-list", true)) return;
        Player p = e.getPlayer();
        if (p.isOp() || p.hasPermission("jujucore.plugins.bypass") || p.hasPermission("jujucore.admin")) return;
        List<String> blocked = blockedCommands();
        // stuff like "worldedit:pos1" shows which plugins we have
        e.getCommands().removeIf(c -> c.contains(":") || blocked.contains(c.toLowerCase(Locale.ROOT)));
    }

    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent e) {
        if (!plugin.getConfig().getBoolean("hide-plugin-list", true)) return;
        Player p = e.getPlayer();
        if (p.isOp() || p.hasPermission("jujucore.plugins.bypass") || p.hasPermission("jujucore.admin")) return;
        String msg = e.getMessage();
        if (msg.length() <= 1) return;
        String cmd = msg.substring(1).split(" ")[0].toLowerCase(Locale.ROOT);
        if (!blockedCommands().contains(cmd)) return;

        e.setCancelled(true);
        // same text as the real vanilla error
        Text.raw(p, "&cUnknown or incomplete command, see below for error");
        Text.raw(p, "&c/" + cmd + "&c<--[HERE]");
    }

    private List<String> blockedCommands() {
        List<String> list = plugin.getConfig().getStringList("blocked-plugin-commands");
        if (list.isEmpty()) {
            return List.of("plugins", "pl", "bukkit:plugins", "bukkit:pl", "version", "ver", "about", "bukkit:version", "bukkit:ver", "bukkit:about", "paper", "paper:paper", "icanhasbukkit", "help", "?", "bukkit:help", "bukkit:?", "minecraft:help");
        }
        return list.stream().map(s -> s.toLowerCase(Locale.ROOT)).toList();
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        if (!state.isStaffChat(p.getUniqueId())) {
            // so you dont leak that youre online by typing in chat while vanished
            if (vanish.isVanished(p)) {
                if (e.getMessage().startsWith("!") && e.getMessage().length() > 1) {
                    e.setMessage(e.getMessage().substring(1).trim());
                    return;
                }
                e.setCancelled(true);
                Text.send(plugin, p, "&cDu bist im Vanish, die Nachricht wurde nicht gesendet. Mit &f! &cdavor geht sie trotzdem raus.");
            }
            return;
        }
        e.setCancelled(true);
        String msg = e.getMessage();
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (other.hasPermission("jujucore.staffchat") || other.hasPermission("jujucore.admin")) {
                    Text.raw(other, "&8[&bSC&8] &7" + p.getName() + "&8: &f" + msg);
                }
            }
            Bukkit.getConsoleSender().sendMessage(Text.color("[SC] " + p.getName() + ": " + msg));
        });
    }
}
