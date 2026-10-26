package org.juju.jujucore.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.juju.jujucore.JujuCore;
import org.juju.jujucore.manager.ChestLockManager;
import org.juju.jujucore.manager.ChestLogManager;
import org.juju.jujucore.util.Text;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

// /kiste: owner, friends and log of the chest you look at
public class ChestCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("info", "add", "remove", "sichern", "frei", "log", "hilfe");
    private static final int PER_PAGE = 10;

    private final JujuCore plugin;
    private final ChestLockManager locks;
    private final ChestLogManager log;

    public ChestCommand(JujuCore plugin, ChestLockManager locks, ChestLogManager log) {
        this.plugin = plugin;
        this.locks = locks;
        this.log = log;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            Text.send(plugin, sender, "&cDieser Command ist nur für Spieler.");
            return true;
        }
        String sub = args.length == 0 ? "info" : args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("hilfe") || sub.equals("help") || !SUBCOMMANDS.contains(sub)) {
            help(p);
            return true;
        }
        Block block = p.getTargetBlockExact(5);
        if (block == null || !ChestLockManager.isLockable(block)) {
            Text.send(plugin, p, "&cSchau auf eine Kiste, ein Fass oder eine Shulkerkiste (höchstens 5 Blöcke weit).");
            return true;
        }
        UUID owner = locks.owner(block);
        boolean admin = p.hasPermission(ChestLockManager.BYPASS_PERMISSION);
        boolean isOwner = owner != null && owner.equals(p.getUniqueId());
        switch (sub) {
            case "info" -> info(p, block, owner);
            case "add", "remove" -> {
                if (!isOwner && !admin) {
                    Text.send(plugin, p, owner == null
                            ? "&cDie Kiste gehört niemandem. Mit &b/kiste sichern &cwird sie deine."
                            : "&cNur &b" + ChestLockManager.name(owner) + " &ckann hier Leute hinzufügen.");
                    return true;
                }
                if (args.length < 2) {
                    Text.send(plugin, p, "&cNutze: /kiste " + sub + " <Name>");
                    return true;
                }
                OfflinePlayer target = resolve(args[1]);
                Set<UUID> trusted = new LinkedHashSet<>(locks.trusted(block));
                if (sub.equals("add")) {
                    if (target.getUniqueId().equals(owner)) {
                        Text.send(plugin, p, "&7Der Besitzer hat sowieso Zugriff.");
                        return true;
                    }
                    trusted.add(target.getUniqueId());
                    locks.setTrusted(block, trusted);
                    Text.send(plugin, p, "&b" + displayName(target, args[1]) + " &7hat jetzt Zugriff auf diese Kiste."
                            + (target.hasPlayedBefore() || target.isOnline() ? "" : " &8(war noch nie online, Name genau so schreiben)"));
                    log.log(locks.parts(block), p.getName(), "gab " + displayName(target, args[1]) + " Zugriff");
                } else {
                    if (!trusted.remove(target.getUniqueId())) {
                        Text.send(plugin, p, "&c" + displayName(target, args[1]) + " hatte keinen Zugriff.");
                        return true;
                    }
                    locks.setTrusted(block, trusted);
                    Text.send(plugin, p, "&b" + displayName(target, args[1]) + " &7hat keinen Zugriff mehr.");
                    log.log(locks.parts(block), p.getName(), "nahm " + displayName(target, args[1]) + " den Zugriff");
                }
            }
            case "sichern" -> {
                if (args.length > 1) {
                    // admins can lock it for someone else (old chests without a placer)
                    if (!admin) {
                        Text.send(plugin, p, "&cNur Admins können Kisten für andere sichern.");
                        return true;
                    }
                    OfflinePlayer target = resolve(args[1]);
                    Set<UUID> keep = target.getUniqueId().equals(owner) ? locks.trusted(block) : new LinkedHashSet<>();
                    locks.setOwner(block, target.getUniqueId(), keep);
                    Text.send(plugin, p, "&7Die Kiste gehört jetzt &b" + displayName(target, args[1]) + "&7.");
                    log.log(locks.parts(block), p.getName(), "hat die Kiste für " + displayName(target, args[1]) + " gesichert");
                    return true;
                }
                if (owner != null && !isOwner && !admin) {
                    Text.send(plugin, p, "&cDie Kiste gehört schon &b" + ChestLockManager.name(owner) + "&c.");
                    return true;
                }
                // only whoever placed it (both halves) can lock it, otherwise people could steal chests
                if (!isOwner && !admin && !locks.placedEntirelyBy(block, p.getUniqueId())) {
                    UUID other = null;
                    for (Block part : locks.parts(block)) {
                        UUID placer = locks.chestPlacer(part);
                        if (placer != null && !placer.equals(p.getUniqueId())) {
                            other = placer;
                        }
                    }
                    Text.send(plugin, p, other != null
                            ? "&cDu kannst nur Kisten sichern, die du selbst gesetzt hast. Diese hat &b" + ChestLockManager.name(other) + " &cgesetzt."
                            : "&cDu kannst nur Kisten sichern, die du selbst gesetzt hast. Bei dieser Kiste ist das nicht bekannt, frag einen Admin.");
                    return true;
                }
                Set<UUID> keep = isOwner ? locks.trusted(block) : new LinkedHashSet<>();
                locks.setOwner(block, p.getUniqueId(), keep);
                Text.send(plugin, p, "&7Die Kiste gehört jetzt dir. Freunde: &b/kiste add <Name>");
                log.log(locks.parts(block), p.getName(), "hat die Kiste gesichert");
            }
            case "frei" -> {
                if (!isOwner && !admin) {
                    Text.send(plugin, p, owner == null ? "&7Die Kiste ist schon für alle offen."
                            : "&cNur &b" + ChestLockManager.name(owner) + " &ckann die Kiste freigeben.");
                    return true;
                }
                locks.setOwner(block, null, Set.of());
                Text.send(plugin, p, "&7Die Kiste ist jetzt für alle offen.");
                log.log(locks.parts(block), p.getName(), "hat die Kiste freigegeben");
            }
            case "log" -> {
                if (!isOwner && !admin) {
                    Text.send(plugin, p, "&cDas Protokoll sieht nur der Besitzer der Kiste.");
                    return true;
                }
                int page = args.length > 1 ? Math.max(1, parseInt(args[1])) : 1;
                log.lookup(locks.parts(block), page, PER_PAGE, result -> {
                    if (!p.isOnline()) {
                        return;
                    }
                    if (result.total() == 0) {
                        Text.send(plugin, p, "&7Für diese Kiste gibt es noch keine Einträge.");
                        return;
                    }
                    int pages = (result.total() + PER_PAGE - 1) / PER_PAGE;
                    Text.raw(p, "&8&m--------&r &bKisten-Log &7Seite " + Math.min(page, pages) + "/" + pages + " &8&m--------");
                    for (String line : result.lines()) {
                        Text.raw(p, "&7" + line);
                    }
                    if (page < pages) {
                        Text.raw(p, "&8Weiter: /kiste log " + (page + 1));
                    }
                });
            }
            default -> help(p);
        }
        return true;
    }

    private void info(Player p, Block block, UUID owner) {
        Set<String> placers = new LinkedHashSet<>();
        for (Block part : locks.parts(block)) {
            UUID placer = locks.chestPlacer(part);
            placers.add(placer == null ? "&8unbekannt" : "&b" + ChestLockManager.name(placer));
        }
        Text.send(plugin, p, "&7Gesetzt von: " + String.join("&7, ", placers));
        if (owner == null) {
            boolean mine = locks.placedEntirelyBy(block, p.getUniqueId());
            Text.send(plugin, p, "&7Nicht gesichert, jeder kann sie öffnen."
                    + (mine ? " &b/kiste sichern &7macht sie zu deiner." : ""));
            return;
        }
        List<String> names = new ArrayList<>();
        for (UUID uuid : locks.trusted(block)) {
            names.add(ChestLockManager.name(uuid));
        }
        Text.send(plugin, p, "&7Besitzer: &b" + ChestLockManager.name(owner));
        Text.send(plugin, p, "&7Zugriff: " + (names.isEmpty() ? "&8nur der Besitzer" : "&b" + String.join("&7, &b", names)));
    }

    private void help(Player p) {
        Text.raw(p, "&8&m----------&r &bKisten &8&m----------");
        Text.raw(p, "&7Kisten, Fässer und Shulkerkisten sind für alle offen, bis der, der sie gesetzt hat, sie sichert.");
        Text.raw(p, "&b/kiste info &8» &fwem gehört die Kiste, wer hat Zugriff");
        Text.raw(p, "&b/kiste add <Name> &8» &fZugriff geben");
        Text.raw(p, "&b/kiste remove <Name> &8» &fZugriff wegnehmen");
        Text.raw(p, "&b/kiste sichern &8» &feine Kiste, die du gesetzt hast, zu deiner machen");
        if (p.hasPermission(ChestLockManager.BYPASS_PERMISSION)) {
            Text.raw(p, "&b/kiste sichern <Name> &8» &f(Admin) die Kiste für jemand anderen sichern");
        }
        Text.raw(p, "&b/kiste frei &8» &fdie Kiste für alle öffnen");
        Text.raw(p, "&b/kiste log [Seite] &8» &fwer hat sie geöffnet und was genommen");
    }

    private OfflinePlayer resolve(String name) {
        Player on = Bukkit.getPlayerExact(name);
        if (on != null) {
            return on;
        }
        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(name);
        return cached != null ? cached : Bukkit.getOfflinePlayer(name);
    }

    private static String displayName(OfflinePlayer p, String fallback) {
        return p.getName() == null ? fallback : p.getName();
    }

    private static int parseInt(String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return Text.filter(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2 && sender instanceof Player p) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("add") || (sub.equals("sichern") && p.hasPermission(ChestLockManager.BYPASS_PERMISSION))) {
                List<String> names = new ArrayList<>();
                for (Player other : Bukkit.getOnlinePlayers()) {
                    if (!other.equals(p) && p.canSee(other)) {
                        names.add(other.getName());
                    }
                }
                return Text.filter(names, args[1]);
            }
            if (sub.equals("remove")) {
                Block block = p.getTargetBlockExact(5);
                if (block != null && ChestLockManager.isLockable(block)) {
                    List<String> names = new ArrayList<>();
                    for (UUID uuid : locks.trusted(block)) {
                        names.add(ChestLockManager.name(uuid));
                    }
                    return Text.filter(names, args[1]);
                }
            }
        }
        return List.of();
    }
}
