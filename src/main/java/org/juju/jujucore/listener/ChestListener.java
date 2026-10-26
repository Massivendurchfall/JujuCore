package org.juju.jujucore.listener;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.juju.jujucore.JujuCore;
import org.juju.jujucore.manager.ChestLockManager;
import org.juju.jujucore.manager.ChestLogManager;
import org.juju.jujucore.util.Text;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

// chest locking (owner + friends) and the chest log
public class ChestListener implements Listener {

    private final JujuCore plugin;
    private final ChestLockManager locks;
    private final ChestLogManager log;
    private final NamespacedKey hintKey;
    private final Map<UUID, OpenChest> open = new HashMap<>();

    private record OpenChest(List<Block> parts, Map<Material, Integer> before) {
    }

    public ChestListener(JujuCore plugin, ChestLockManager locks, ChestLogManager log) {
        this.plugin = plugin;
        this.locks = locks;
        this.log = log;
        this.hintKey = new NamespacedKey(plugin, "kisten_hinweis");
    }

    // ---- login hint with the /kiste commands, only the first few times ----

    private static final String HINT = String.join("\n",
            "<dark_gray><st>          </st></dark_gray> <aqua><b>Kisten kannst du jetzt sichern</b></aqua> <dark_gray><st>          </st></dark_gray>",
            "<gray>Kisten, Fässer und Shulkerkisten sind für alle offen, bis du sie sicherst. Sichern kannst du nur, was du selbst gesetzt hast. Schau die Kiste an und nutze:</gray>",
            cmd("/kiste sichern", "/kiste sichern") + " <dark_gray>»</dark_gray> <white>deine gesetzte Kiste schützen</white>",
            cmd("/kiste add ", "/kiste add \\<Name>") + " <dark_gray>»</dark_gray> <white>Freund hinzufügen</white>",
            cmd("/kiste remove ", "/kiste remove \\<Name>") + " <dark_gray>»</dark_gray> <white>Freund wieder entfernen</white>",
            cmd("/kiste info", "/kiste info") + " <dark_gray>»</dark_gray> <white>wem gehört sie, wer darf rein</white>",
            cmd("/kiste log", "/kiste log") + " <dark_gray>»</dark_gray> <white>wer war drin, was wurde genommen</white>",
            "<gray>Befehle anklicken zum Eintippen.</gray>");

    private static String cmd(String suggest, String label) {
        return "<click:suggest_command:'" + suggest + "'><hover:show_text:'<gray>Klicken zum Eintippen'><aqua>" + label + "</aqua></hover></click>";
    }

    // with authme the hint has to come after login, otherwise it gets buried under the login spam.
    // authme is only a softdepend so we hook the event by name
    public void hookLogin() {
        if (!Bukkit.getPluginManager().isPluginEnabled("AuthMe")) return;
        try {
            Class<? extends Event> loginEvent = Class.forName("fr.xephi.authme.events.LoginEvent").asSubclass(Event.class);
            Method getPlayer = loginEvent.getMethod("getPlayer");
            Bukkit.getPluginManager().registerEvent(loginEvent, this, EventPriority.MONITOR, (l, ev) -> {
                if (!loginEvent.isInstance(ev)) return;
                try {
                    Player p = (Player) getPlayer.invoke(ev);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> showHint(p), 20L);
                } catch (ReflectiveOperationException ex) {
                    plugin.getLogger().warning("Kisten-Hinweis: AuthMe-Login nicht lesbar: " + ex.getMessage());
                }
            }, plugin);
        } catch (ReflectiveOperationException | ClassCastException ex) {
            plugin.getLogger().warning("Kisten-Hinweis: AuthMe-Login-Event nicht gefunden, Hinweis kommt beim Join");
        }
    }

    @EventHandler
    public void onJoinHint(PlayerJoinEvent e) {
        if (!Bukkit.getPluginManager().isPluginEnabled("AuthMe")) {
            Player p = e.getPlayer();
            Bukkit.getScheduler().runTaskLater(plugin, () -> showHint(p), 40L);
        }
    }

    private void showHint(Player p) {
        if (!p.isOnline()) return;
        int max = plugin.getConfig().getInt("kisten-hinweis-anzahl", 5);
        int seen = p.getPersistentDataContainer().getOrDefault(hintKey, PersistentDataType.INTEGER, 0);
        if (max == 0 || (max > 0 && seen >= max)) return;
        p.getPersistentDataContainer().set(hintKey, PersistentDataType.INTEGER, seen + 1);
        p.sendMessage(MiniMessage.miniMessage().deserialize(HINT));
    }

    // you cant attach a chest to someone elses LOCKED chest (would open it for you).
    // unlocked chests work like vanilla
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlaceCheck(BlockPlaceEvent e) {
        Block block = e.getBlockPlaced();
        Player p = e.getPlayer();
        if (!ChestLockManager.isLockable(block)) return;
        for (Block part : locks.parts(block)) {
            if (!part.equals(block) && !locks.canAccess(p, part)) {
                e.setCancelled(true);
                Text.send(plugin, p, "&cDaneben steht die Kiste von &b" + ChestLockManager.name(locks.owner(part)) + "&c.");
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Block block = e.getBlockPlaced();
        UUID placer = e.getPlayer().getUniqueId();
        if (block.getType() == Material.HOPPER) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (block.getType() == Material.HOPPER) locks.markPlacer(block, placer);
            });
            return;
        }
        if (!ChestLockManager.isLockable(block)) return;

        // new chests are open until the placer does /kiste sichern, we just remember who placed it.
        // if it connects to a locked chest it gets the same owner + friends.
        // 1 tick later so the double chest is done connecting
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!ChestLockManager.isLockable(block)) return;
            locks.markChestPlacer(block, placer);
            UUID owner = locks.owner(block);
            if (owner != null) {
                locks.setOwner(block, owner, new LinkedHashSet<>(locks.trusted(block)));
            }
        });
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND) return;
        Block block = e.getClickedBlock();
        if (block == null || !ChestLockManager.isLockable(block)) return;
        Player p = e.getPlayer();
        // sneaking with a block = building on the chest, not opening
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (p.isSneaking() && hand.getType().isBlock() && !hand.isEmpty()) return;
        if (locks.canAccess(p, block)) return;

        e.setCancelled(true);
        Text.send(plugin, p, "&cDiese Kiste gehört &b" + ChestLockManager.name(locks.owner(block)) + "&c.");
        log.log(locks.parts(block), p.getName(), "wollte öffnen (gesperrt)");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        Location loc = e.getInventory().getLocation();
        if (loc == null || !ChestLockManager.isLockable(loc.getBlock())) return;
        List<Block> parts = locks.parts(loc.getBlock());
        log.log(parts, p.getName(), "geöffnet");
        open.put(p.getUniqueId(), new OpenChest(parts, count(e.getInventory().getContents())));
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        OpenChest chest = open.remove(e.getPlayer().getUniqueId());
        if (chest == null) return;

        // compare what was in there before vs now
        Map<Material, Integer> after = count(e.getInventory().getContents());
        Set<Material> types = new LinkedHashSet<>(chest.before().keySet());
        types.addAll(after.keySet());
        List<String> took = new ArrayList<>();
        List<String> put = new ArrayList<>();
        for (Material type : types) {
            int diff = after.getOrDefault(type, 0) - chest.before().getOrDefault(type, 0);
            String s = Math.abs(diff) + " " + type.name().toLowerCase().replace('_', ' ');
            if (diff < 0) took.add(s);
            else if (diff > 0) put.add(s);
        }
        if (took.isEmpty() && put.isEmpty()) return;

        StringBuilder sb = new StringBuilder();
        if (!took.isEmpty()) sb.append("&cnahm &f").append(String.join(", ", took));
        if (!put.isEmpty()) sb.append(sb.isEmpty() ? "" : "&7; ").append("&alegte &f").append(String.join(", ", put));
        log.log(chest.parts(), e.getPlayer().getName(), sb.toString());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        if (!ChestLockManager.isLockable(block) || !locks.isLocked(block)) return;
        Player p = e.getPlayer();
        if (locks.isOwner(p, block) || p.hasPermission(ChestLockManager.BYPASS_PERMISSION)) {
            log.log(locks.parts(block), p.getName(), "abgebaut");
            return;
        }
        e.setCancelled(true);
        Text.send(plugin, p, "&cDiese Kiste gehört &b" + ChestLockManager.name(locks.owner(block)) + "&c, nur der Besitzer kann sie abbauen.");
        log.log(locks.parts(block), p.getName(), "wollte abbauen (gesperrt)");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        e.blockList().removeIf(locks::isLocked);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        e.blockList().removeIf(locks::isLocked);
    }

    // stop hoppers / hopper minecarts from someone else draining a locked chest
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHopper(InventoryMoveItemEvent e) {
        Location src = e.getSource().getLocation();
        if (src == null || !ChestLockManager.isLockable(src.getBlock())) return;
        Block chest = src.getBlock();
        UUID owner = locks.owner(chest);
        if (owner == null) return;

        InventoryHolder dest = e.getDestination().getHolder(false);
        if (dest instanceof Hopper hopper) {
            UUID placer = locks.placer(hopper.getBlock());
            // old hoppers (from before this existed) dont have a placer, let them work
            if (placer == null || placer.equals(owner) || locks.trusted(chest).contains(placer)) return;
        }
        e.setCancelled(true);
    }

    private static Map<Material, Integer> count(ItemStack[] items) {
        Map<Material, Integer> map = new TreeMap<>();
        for (ItemStack item : items) {
            if (item != null && !item.isEmpty()) {
                map.merge(item.getType(), item.getAmount(), Integer::sum);
            }
        }
        return map;
    }
}
