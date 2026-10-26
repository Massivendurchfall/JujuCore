package org.juju.jujucore.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.juju.jujucore.JujuCore;
import org.juju.jujucore.manager.DataManager;
import org.juju.jujucore.manager.KitManager;
import org.juju.jujucore.manager.ReportManager;
import org.juju.jujucore.manager.StateManager;
import org.juju.jujucore.manager.VanishManager;
import org.juju.jujucore.util.HerobrineSkin;
import org.juju.jujucore.util.Text;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class CoreCommand implements CommandExecutor, TabCompleter {

    private final JujuCore plugin;
    private final DataManager data;
    private final KitManager kits;
    private final ReportManager reports;
    private final VanishManager vanish;
    private final StateManager state;
    private final Map<UUID, UUID> lastMessage = new HashMap<>();
    private final List<UUID> socialSpy = new ArrayList<>();
    private final Map<UUID, TpaRequest> pendingTpa = new HashMap<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY);

    public CoreCommand(JujuCore plugin, DataManager data, KitManager kits, ReportManager reports, VanishManager vanish, StateManager state) {
        this.plugin = plugin;
        this.data = data;
        this.kits = kits;
        this.reports = reports;
        this.vanish = vanish;
        this.state = state;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase();
        switch (name) {
            case "jujucore" -> handleMain(sender, args);
            case "gm", "gmc", "gms", "gma", "gmsp" -> handleGamemode(sender, name, args);
            case "fly" -> handleFly(sender, args);
            case "vanish" -> handleVanish(sender, args);
            case "top" -> handleTop(sender);
            case "heal" -> handleHeal(sender, args);
            case "feed" -> handleFeed(sender, args);
            case "repair" -> handleRepair(sender, args);
            case "speed" -> handleSpeed(sender, args);
            case "spawn" -> handleSpawn(sender);
            case "setspawn" -> handleSetSpawn(sender);
            case "sethome" -> handleSetHome(sender, args);
            case "home" -> handleHome(sender, args);
            case "delhome" -> handleDelHome(sender, args);
            case "back" -> handleBack(sender);
            case "tp" -> handleTeleport(sender, args);
            case "tphere" -> handleTeleportHere(sender, args);
            case "invsee" -> handleInvsee(sender, args);
            case "ec" -> handleEnderChest(sender, args);
            case "msg" -> handleMessage(sender, args);
            case "reply" -> handleReply(sender, args);
            case "socialspy" -> handleSocialSpy(sender);
            case "report" -> handleReport(sender, args);
            case "reports" -> handleReports(sender, args);
            case "day" -> handleTime(sender, 1000L, "Tag");
            case "night" -> handleTime(sender, 13000L, "Nacht");
            case "weather" -> handleWeather(sender, args);
            case "clearinventory" -> handleClearInventory(sender, args);
            case "trash" -> handleTrash(sender);
            case "near" -> handleNear(sender, args);
            case "god" -> handleGod(sender, args);
            case "freeze" -> handleFreeze(sender, args, true);
            case "unfreeze" -> handleFreeze(sender, args, false);
            case "staffchat" -> handleStaffChat(sender, args);
            case "broadcast" -> handleBroadcast(sender, args);
            case "clearchat" -> handleClearChat(sender);
            case "hat" -> handleHat(sender);
            case "workbench" -> handleWorkbench(sender);
            case "suicide" -> handleSuicide(sender);
            case "seen" -> handleSeen(sender, args);
            case "playtime" -> handlePlaytime(sender, args);
            case "check" -> handleCheck(sender, args);
            case "warp" -> handleWarp(sender, args);
            case "warps" -> handleWarps(sender);
            case "setwarp" -> handleSetWarp(sender, args);
            case "delwarp" -> handleDelWarp(sender, args);
            case "kit" -> handleKit(sender, args);
            case "kits" -> handleKits(sender);
            case "createkit" -> handleCreateKit(sender, args);
            case "delkit" -> handleDelKit(sender, args);
            case "tpa" -> handleTpa(sender, args);
            case "tpaccept" -> handleTpAccept(sender);
            case "tpdeny" -> handleTpDeny(sender);
            case "tpcancel" -> handleTpCancel(sender);
            case "troll" -> handleTroll(sender, args);
            case "villager" -> handleVillager(sender, args);
            default -> Text.send(plugin, sender, "&cUnbekannter Command.");
        }
        return true;
    }

    private void handleMain(CommandSender sender, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!has(sender, "jujucore.reload")) {
                return;
            }
            plugin.reloadAll();
            Text.send(plugin, sender, "&aReload fertig.");
            return;
        }
        Text.raw(sender, "&8&m----------------&r &bJujuCore &8&m----------------");
        Text.raw(sender, "&7/gm <0|1|2|3> [player] &8- &fGamemode setzen");
        Text.raw(sender, "&7/fly, /god, /v, /top &8- &fAdmin Basics");
        Text.raw(sender, "&7/spawn, /setspawn, /home, /warp, /back &8- &fTeleport Basics");
        Text.raw(sender, "&7/tpa, /tpaccept, /tpdeny &8- &fTeleport Anfragen");
        Text.raw(sender, "&7/msg, /r, /sc, /report, /reports &8- &fChat und Staff Tools");
        Text.raw(sender, "&7/invsee, /ec, /heal, /feed, /repair, /speed &8- &fNützliche Commands");
        Text.raw(sender, "&7/hat, /workbench, /seen, /playtime, /check &8- &fExtra Tools");
        Text.raw(sender, "&7/troll <player> <action> &8- &fLustige Staff Pranks");
        Text.raw(sender, "&8&m------------------------------------------");
    }

    private void handleGamemode(CommandSender sender, String commandName, String[] args) {
        GameMode mode = null;
        int targetIndex = 0;
        if (commandName.equals("gmc")) {
            mode = GameMode.CREATIVE;
        } else if (commandName.equals("gms")) {
            mode = GameMode.SURVIVAL;
        } else if (commandName.equals("gma")) {
            mode = GameMode.ADVENTURE;
        } else if (commandName.equals("gmsp")) {
            mode = GameMode.SPECTATOR;
        } else {
            if (args.length == 0) {
                Text.send(plugin, sender, "&cNutze: /gm <0|1|2|3> [player]");
                return;
            }
            mode = parseGameMode(args[0]);
            targetIndex = 1;
        }
        if (mode == null) {
            Text.send(plugin, sender, "&cDieser Gamemode existiert nicht.");
            return;
        }
        Player target = targetFromArgs(sender, args, targetIndex, "jujucore.gamemode.others");
        if (target == null) {
            return;
        }
        boolean self = sender instanceof Player player && player.equals(target);
        if (self && !has(sender, "jujucore.gamemode")) {
            return;
        }
        target.setGameMode(mode);
        Text.send(plugin, target, "&7Dein Gamemode ist jetzt &b" + mode.name().toLowerCase() + "&7.");
        if (!self) {
            Text.send(plugin, sender, "&7Gamemode von &b" + target.getName() + " &7auf &b" + mode.name().toLowerCase() + " &7gesetzt.");
        }
    }

    private void handleFly(CommandSender sender, String[] args) {
        Player target = targetFromArgs(sender, args, 0, "jujucore.fly.others");
        if (target == null) {
            return;
        }
        boolean self = sender instanceof Player player && player.equals(target);
        if (self && !has(sender, "jujucore.fly")) {
            return;
        }
        boolean enabled = !target.getAllowFlight();
        target.setAllowFlight(enabled);
        target.setFlying(enabled && target.getLocation().getBlock().isPassable());
        Text.send(plugin, target, "&7Fly ist jetzt " + (enabled ? "&aaktiviert" : "&cdeaktiviert") + "&7.");
        if (!self) {
            Text.send(plugin, sender, "&7Fly von &b" + target.getName() + " &7ist jetzt " + (enabled ? "&aaktiviert" : "&cdeaktiviert") + "&7.");
        }
    }

    private void handleVanish(CommandSender sender, String[] args) {
        Player target = targetFromArgs(sender, args, 0, "jujucore.vanish.others");
        if (target == null) {
            return;
        }
        boolean self = sender instanceof Player player && player.equals(target);
        if (self && !has(sender, "jujucore.vanish")) {
            return;
        }
        boolean vanished = vanish.toggle(target);
        Text.send(plugin, target, "&7Vanish ist jetzt " + (vanished ? "&aaktiviert" : "&cdeaktiviert") + "&7.");
        if (!self) {
            Text.send(plugin, sender, "&7Vanish von &b" + target.getName() + " &7ist jetzt " + (vanished ? "&aaktiviert" : "&cdeaktiviert") + "&7.");
        }
    }

    private void handleTop(CommandSender sender) {
        if (!has(sender, "jujucore.top")) {
            return;
        }
        Player player = playerOnly(sender);
        if (player == null) {
            return;
        }
        Location top = findTop(player.getLocation());
        if (top == null) {
            Text.send(plugin, sender, "&cKein sicherer Block über dir gefunden.");
            return;
        }
        player.teleport(top);
        Text.send(plugin, player, "&7Du wurdest nach oben teleportiert.");
    }

    private void handleHeal(CommandSender sender, String[] args) {
        Player target = targetFromArgs(sender, args, 0, "jujucore.heal.others");
        if (target == null) {
            return;
        }
        boolean self = sender instanceof Player player && player.equals(target);
        if (self && !has(sender, "jujucore.heal")) {
            return;
        }
        AttributeInstance maxHealth = target.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            target.setHealth(maxHealth.getValue());
        }
        target.setFireTicks(0);
        target.setFoodLevel(20);
        target.setSaturation(20.0F);
        Text.send(plugin, target, "&aDu wurdest geheilt.");
        if (!self) {
            Text.send(plugin, sender, "&b" + target.getName() + " &7wurde geheilt.");
        }
    }

    private void handleFeed(CommandSender sender, String[] args) {
        Player target = targetFromArgs(sender, args, 0, "jujucore.feed.others");
        if (target == null) {
            return;
        }
        boolean self = sender instanceof Player player && player.equals(target);
        if (self && !has(sender, "jujucore.feed")) {
            return;
        }
        target.setFoodLevel(20);
        target.setSaturation(20.0F);
        Text.send(plugin, target, "&aDu wurdest gefüttert.");
        if (!self) {
            Text.send(plugin, sender, "&b" + target.getName() + " &7wurde gefüttert.");
        }
    }

    private void handleRepair(CommandSender sender, String[] args) {
        if (!has(sender, "jujucore.repair")) {
            return;
        }
        Player player = playerOnly(sender);
        if (player == null) {
            return;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("all")) {
            int repaired = repairInventory(player.getInventory());
            Text.send(plugin, player, "&a" + repaired + " Items repariert.");
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR || !repairItem(item)) {
            Text.send(plugin, player, "&cHalte ein beschädigtes Item in der Hand oder nutze /repair all.");
            return;
        }
        Text.send(plugin, player, "&aItem repariert.");
    }

    private void handleSpeed(CommandSender sender, String[] args) {
        if (args.length == 0) {
            Text.send(plugin, sender, "&cNutze: /speed <1-10> [player]");
            return;
        }
        Player target = targetFromArgs(sender, args, 1, "jujucore.speed.others");
        if (target == null) {
            return;
        }
        boolean self = sender instanceof Player player && player.equals(target);
        if (self && !has(sender, "jujucore.speed")) {
            return;
        }
        int speed = parseInt(args[0], -1);
        if (speed < 1 || speed > 10) {
            Text.send(plugin, sender, "&cSpeed muss zwischen 1 und 10 sein.");
            return;
        }
        float value = speed / 10.0F;
        target.setWalkSpeed(value > 0.8F ? 0.8F : value);
        target.setFlySpeed(value);
        Text.send(plugin, target, "&7Deine Speed ist jetzt &b" + speed + "&7.");
        if (!self) {
            Text.send(plugin, sender, "&7Speed von &b" + target.getName() + " &7ist jetzt &b" + speed + "&7.");
        }
    }

    private void handleSpawn(CommandSender sender) {
        if (!has(sender, "jujucore.spawn")) {
            return;
        }
        Player player = playerOnly(sender);
        if (player == null) {
            return;
        }
        Location spawn = data.getSpawn();
        if (spawn == null) {
            spawn = player.getWorld().getSpawnLocation();
        }
        player.teleport(spawn);
        Text.send(plugin, player, "&7Du wurdest zum Spawn teleportiert.");
    }

    private void handleSetSpawn(CommandSender sender) {
        if (!has(sender, "jujucore.setspawn")) {
            return;
        }
        Player player = playerOnly(sender);
        if (player == null) {
            return;
        }
        data.setSpawn(player.getLocation());
        Text.send(plugin, player, "&aSpawn gesetzt.");
    }

    private void handleSetHome(CommandSender sender, String[] args) {
        if (!has(sender, "jujucore.sethome")) {
            return;
        }
        Player player = playerOnly(sender);
        if (player == null) {
            return;
        }
        String name = args.length > 0 ? data.cleanHomeName(args[0]) : "home";
        int maxHomes = plugin.getConfig().getInt("max-homes", 3);
        if (!player.hasPermission("jujucore.sethome.unlimited") && data.getHome(player.getUniqueId(), name) == null && data.getHomeCount(player.getUniqueId()) >= maxHomes) {
            Text.send(plugin, player, "&cDu hast bereits die maximale Anzahl an Homes erreicht.");
            return;
        }
        data.setHome(player.getUniqueId(), name, player.getLocation());
        Text.send(plugin, player, "&aHome &b" + name + " &agesetzt.");
    }

    private void handleHome(CommandSender sender, String[] args) {
        if (!has(sender, "jujucore.home")) {
            return;
        }
        Player player = playerOnly(sender);
        if (player == null) {
            return;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("list")) {
            List<String> homes = data.getHomes(player.getUniqueId());
            Text.send(plugin, player, homes.isEmpty() ? "&cDu hast keine Homes." : "&7Homes: &b" + String.join("&7, &b", homes));
            return;
        }
        String name = args.length > 0 ? data.cleanHomeName(args[0]) : "home";
        Location home = data.getHome(player.getUniqueId(), name);
        if (home == null) {
            Text.send(plugin, player, "&cDieses Home existiert nicht. Nutze /home list.");
            return;
        }
        player.teleport(home);
        Text.send(plugin, player, "&7Teleportiert zu Home &b" + name + "&7.");
    }

    private void handleDelHome(CommandSender sender, String[] args) {
        if (!has(sender, "jujucore.delhome")) {
            return;
        }
        Player player = playerOnly(sender);
        if (player == null) {
            return;
        }
        String name = args.length > 0 ? data.cleanHomeName(args[0]) : "home";
        if (!data.deleteHome(player.getUniqueId(), name)) {
            Text.send(plugin, player, "&cDieses Home existiert nicht.");
            return;
        }
        Text.send(plugin, player, "&aHome &b" + name + " &agelöscht.");
    }

    private void handleBack(CommandSender sender) {
        if (!has(sender, "jujucore.back")) {
            return;
        }
        Player player = playerOnly(sender);
        if (player == null) {
            return;
        }
        Location back = data.getBackLocation(player.getUniqueId());
        if (back == null) {
            Text.send(plugin, player, "&cEs gibt keinen Back-Punkt.");
            return;
        }
        player.teleport(back);
        Text.send(plugin, player, "&7Zurück teleportiert.");
    }

    private void handleTeleport(CommandSender sender, String[] args) {
        if (!has(sender, "jujucore.tp")) {
            return;
        }
        if (args.length == 0) {
            Text.send(plugin, sender, "&cNutze: /tp <player> [target] oder /tp [player] <x> <y> <z>");
            return;
        }
        Player first = args.length <= 2 ? quietPlayer(sender, args[0]) : null;
        Player second = args.length == 2 ? quietPlayer(sender, args[1]) : null;
        if (args.length == 1 && first != null) {
            Player player = playerOnly(sender);
            if (player == null) {
                return;
            }
            player.teleport(first.getLocation());
            Text.send(plugin, player, "&7Teleportiert zu &b" + first.getName() + "&7.");
            return;
        }
        if (args.length == 2 && first != null && second != null) {
            first.teleport(second.getLocation());
            Text.send(plugin, sender, "&b" + first.getName() + " &7wurde zu &b" + second.getName() + " &7teleportiert.");
            return;
        }
        // coords, ~ ~ ~, selectors etc -> just let vanilla /tp do it
        Bukkit.dispatchCommand(sender, "minecraft:tp " + String.join(" ", args));
    }

    // like findPlayer but no error msg, and coords/selectors never count as a name
    private Player quietPlayer(CommandSender sender, String name) {
        if (name.isEmpty() || "~^@-+.0123456789".indexOf(name.charAt(0)) >= 0) {
            return null;
        }
        Player exact = Bukkit.getPlayerExact(name);
        if (exact != null && vanish.canSee(sender, exact)) {
            return exact;
        }
        List<Player> matches = Bukkit.matchPlayer(name).stream().filter(player -> vanish.canSee(sender, player)).toList();
        return matches.size() == 1 ? matches.getFirst() : null;
    }

    private void handleTeleportHere(CommandSender sender, String[] args) {
        if (!has(sender, "jujucore.tphere")) {
            return;
        }
        Player player = playerOnly(sender);
        if (player == null) {
            return;
        }
        if (args.length == 0) {
            Text.send(plugin, sender, "&cNutze: /tphere <player>");
            return;
        }
        Player target = findPlayer(sender, args[0]);
        if (target == null) {
            return;
        }
        target.teleport(player.getLocation());
        Text.send(plugin, sender, "&b" + target.getName() + " &7wurde zu dir teleportiert.");
    }

    private void handleInvsee(CommandSender sender, String[] args) {
        if (!has(sender, "jujucore.invsee")) {
            return;
        }
        Player player = playerOnly(sender);
        if (player == null) {
            return;
        }
        if (args.length == 0) {
            Text.send(plugin, sender, "&cNutze: /invsee <player>");
            return;
        }
        Player target = findPlayer(sender, args[0]);
        if (target == null) {
            return;
        }
        player.openInventory(target.getInventory());
        Text.send(plugin, player, "&7Inventar von &b" + target.getName() + " &7geöffnet.");
    }

    private void handleEnderChest(CommandSender sender, String[] args) {
        Player player = playerOnly(sender);
        if (player == null) {
            return;
        }
        if (args.length == 0) {
            if (!has(sender, "jujucore.ec")) {
                return;
            }
            player.openInventory(player.getEnderChest());
            return;
        }
        if (!has(sender, "jujucore.ec.others")) {
            return;
        }
        Player target = findPlayer(sender, args[0]);
        if (target == null) {
            return;
        }
        player.openInventory(target.getEnderChest());
        Text.send(plugin, player, "&7Enderchest von &b" + target.getName() + " &7geöffnet.");
    }

    private void handleMessage(CommandSender sender, String[] args) {
        if (!has(sender, "jujucore.msg")) {
            return;
        }
        if (args.length < 2) {
            Text.send(plugin, sender, "&cNutze: /msg <player> <message>");
            return;
        }
        Player target = findPlayer(sender, args[0]);
        if (target == null) {
            return;
        }
        String message = Text.join(args, 1);
        sendPrivateMessage(sender, target, message);
    }

    private void handleReply(CommandSender sender, String[] args) {
        if (!has(sender, "jujucore.reply")) {
            return;
        }
        if (args.length == 0) {
            Text.send(plugin, sender, "&cNutze: /reply <message>");
            return;
        }
        if (!(sender instanceof Player player)) {
            Text.send(plugin, sender, "&cNur Spieler können /reply nutzen.");
            return;
        }
        UUID targetId = lastMessage.get(player.getUniqueId());
        if (targetId == null) {
            Text.send(plugin, sender, "&cDu hast niemandem zum Antworten.");
            return;
        }
        Player target = Bukkit.getPlayer(targetId);
        if (target == null) {
            Text.send(plugin, sender, "&cDieser Spieler ist nicht mehr online.");
            return;
        }
        sendPrivateMessage(sender, target, Text.join(args, 0));
    }

    private void handleSocialSpy(CommandSender sender) {
        if (!has(sender, "jujucore.socialspy")) {
            return;
        }
        Player player = playerOnly(sender);
        if (player == null) {
            return;
        }
        if (socialSpy.contains(player.getUniqueId())) {
            socialSpy.remove(player.getUniqueId());
            Text.send(plugin, player, "&7SocialSpy ist jetzt &cdeaktiviert&7.");
        } else {
            socialSpy.add(player.getUniqueId());
            Text.send(plugin, player, "&7SocialSpy ist jetzt &aaktiviert&7.");
        }
    }

    private void handleReport(CommandSender sender, String[] args) {
        if (!has(sender, "jujucore.report")) {
            return;
        }
        if (args.length < 2) {
            Text.send(plugin, sender, "&cNutze: /report <player> <reason>");
            return;
        }
        Player target = findPlayer(sender, args[0]);
        if (target == null) {
            return;
        }
        if (sender instanceof Player player && player.equals(target)) {
            Text.send(plugin, sender, "&cDu kannst dich nicht selbst reporten.");
            return;
        }
        String reporter = sender instanceof Player player ? player.getName() : "Console";
        String reason = Text.join(args, 1);
        int id = reports.create(reporter, target.getName(), reason);
        Text.send(plugin, sender, "&aReport &b#" + id + " &awurde gesendet.");
        String staffPermission = plugin.getConfig().getString("report-staff-permission", "jujucore.reports");
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission(staffPermission)) {
                Text.raw(online, "&8[&cReport&8] &b#" + id + " &7" + reporter + " &8-> &7" + target.getName() + "&8: &f" + reason);
            }
        }
    }

    private void handleReports(CommandSender sender, String[] args) {
        if (!has(sender, "jujucore.reports")) {
            return;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            List<Integer> open = reports.activeReports();
            if (open.isEmpty()) {
                Text.send(plugin, sender, "&aKeine offenen Reports.");
                return;
            }
            Text.raw(sender, "&8&m------------&r &cOffene Reports &8&m------------");
            for (int id : open) {
                Text.raw(sender, reports.summary(id));
            }
            return;
        }
        if (args[0].equalsIgnoreCase("info")) {
            if (args.length < 2) {
                Text.send(plugin, sender, "&cNutze: /reports info <id>");
                return;
            }
            int id = parseInt(args[1], -1);
            String details = reports.details(id);
            if (details == null) {
                Text.send(plugin, sender, "&cReport nicht gefunden.");
                return;
            }
            for (String line : details.split("\\n")) {
                Text.raw(sender, line);
            }
            return;
        }
        if (args[0].equalsIgnoreCase("close")) {
            if (args.length < 2) {
                Text.send(plugin, sender, "&cNutze: /reports close <id>");
                return;
            }
            int id = parseInt(args[1], -1);
            if (!reports.close(id, sender.getName())) {
                Text.send(plugin, sender, "&cReport nicht gefunden.");
                return;
            }
            Text.send(plugin, sender, "&aReport &b#" + id + " &ageschlossen.");
            return;
        }
        if (args[0].equalsIgnoreCase("clear")) {
            reports.clearClosed();
            Text.send(plugin, sender, "&aGeschlossene Reports gelöscht.");
            return;
        }
        Text.send(plugin, sender, "&cNutze: /reports [list|info|close|clear]");
    }

    private void handleTime(CommandSender sender, long time, String name) {
        if (!has(sender, "jujucore.time")) {
            return;
        }
        if (sender instanceof Player player) {
            player.getWorld().setTime(time);
            Text.send(plugin, sender, "&7Zeit in deiner Welt auf &b" + name + " &7gesetzt.");
            return;
        }
        for (World world : Bukkit.getWorlds()) {
            world.setTime(time);
        }
        Text.send(plugin, sender, "&7Zeit in allen Welten auf &b" + name + " &7gesetzt.");
    }

    private void handleWeather(CommandSender sender, String[] args) {
        if (!has(sender, "jujucore.weather")) {
            return;
        }
        if (args.length == 0) {
            Text.send(plugin, sender, "&cNutze: /weather <clear|rain|thunder>");
            return;
        }
        World world = sender instanceof Player player ? player.getWorld() : Bukkit.getWorlds().getFirst();
        String type = args[0].toLowerCase();
        switch (type) {
            case "clear", "sun" -> {
                world.setStorm(false);
                world.setThundering(false);
                Text.send(plugin, sender, "&7Wetter auf &bclear &7gesetzt.");
            }
            case "rain" -> {
                world.setStorm(true);
                world.setThundering(false);
                Text.send(plugin, sender, "&7Wetter auf &brain &7gesetzt.");
            }
            case "thunder", "storm" -> {
                world.setStorm(true);
                world.setThundering(true);
                Text.send(plugin, sender, "&7Wetter auf &bthunder &7gesetzt.");
            }
            default -> Text.send(plugin, sender, "&cNutze: /weather <clear|rain|thunder>");
        }
    }

    private void handleClearInventory(CommandSender sender, String[] args) {
        Player target = targetFromArgs(sender, args, 0, "jujucore.clearinventory.others");
        if (target == null) {
            return;
        }
        boolean self = sender instanceof Player player && player.equals(target);
        if (self && !has(sender, "jujucore.clearinventory")) {
            return;
        }
        target.getInventory().clear();
        target.getInventory().setArmorContents(null);
        target.getInventory().setItemInOffHand(null);
        Text.send(plugin, target, "&cDein Inventar wurde geleert.");
        if (!self) {
            Text.send(plugin, sender, "&7Inventar von &b" + target.getName() + " &7geleert.");
        }
    }

    private void handleTrash(CommandSender sender) {
        if (!has(sender, "jujucore.trash")) {
            return;
        }
        Player player = playerOnly(sender);
        if (player == null) {
            return;
        }
        Inventory inventory = Bukkit.createInventory(null, 54, Text.color("&cTrash"));
        player.openInventory(inventory);
        Text.send(plugin, player, "&7Alles was du hier reinlegst, verschwindet beim Schließen.");
    }

    private void handleNear(CommandSender sender, String[] args) {
        if (!has(sender, "jujucore.near")) {
            return;
        }
        Player player = playerOnly(sender);
        if (player == null) {
            return;
        }
        int radius = args.length > 0 ? parseInt(args[0], 100) : 100;
        if (radius < 1) {
            radius = 100;
        }
        List<String> names = new ArrayList<>();
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.equals(player) || target.getWorld() != player.getWorld()) {
                continue;
            }
            if (vanish.isVanished(target) && !player.hasPermission(plugin.getConfig().getString("vanish-see-permission", "jujucore.vanish.see"))) {
                continue;
            }
            if (target.getLocation().distance(player.getLocation()) <= radius) {
                names.add(target.getName());
            }
        }
        if (names.isEmpty()) {
            Text.send(plugin, player, "&7Keine Spieler im Radius von &b" + radius + " &7Blöcken.");
            return;
        }
        Text.send(plugin, player, "&7Spieler in der Nähe: &b" + String.join("&7, &b", names));
    }

    private void handleGod(CommandSender sender, String[] args) {
        Player target = targetFromArgs(sender, args, 0, "jujucore.god.others");
        if (target == null) {
            return;
        }
        boolean self = sender instanceof Player player && player.equals(target);
        if (self && !has(sender, "jujucore.god")) {
            return;
        }
        boolean enabled = state.toggleGod(target.getUniqueId());
        Text.send(plugin, target, "&7Godmode ist jetzt " + (enabled ? "&aaktiviert" : "&cdeaktiviert") + "&7.");
        if (!self) {
            Text.send(plugin, sender, "&7Godmode von &b" + target.getName() + " &7ist jetzt " + (enabled ? "&aaktiviert" : "&cdeaktiviert") + "&7.");
        }
    }

    private void handleFreeze(CommandSender sender, String[] args, boolean value) {
        if (!has(sender, "jujucore.freeze")) {
            return;
        }
        if (args.length == 0) {
            Text.send(plugin, sender, value ? "&cNutze: /freeze <player>" : "&cNutze: /unfreeze <player>");
            return;
        }
        Player target = findPlayer(sender, args[0]);
        if (target == null) {
            return;
        }
        state.setFrozen(target.getUniqueId(), value);
        boolean enabled = value;
        Text.send(plugin, target, enabled ? "&cDu wurdest eingefroren." : "&aDu bist nicht mehr eingefroren.");
        Text.send(plugin, sender, "&b" + target.getName() + " &7ist jetzt " + (enabled ? "&ceingefroren" : "&afrei") + "&7.");
    }

    private void handleStaffChat(CommandSender sender, String[] args) {
        if (!has(sender, "jujucore.staffchat")) {
            return;
        }
        if (args.length == 0) {
            Player player = playerOnly(sender);
            if (player == null) {
                Text.send(plugin, sender, "&cNutze: /staffchat <message>");
                return;
            }
            boolean enabled = state.toggleStaffChat(player.getUniqueId());
            Text.send(plugin, player, "&7StaffChat-Modus ist jetzt " + (enabled ? "&aaktiviert" : "&cdeaktiviert") + "&7.");
            return;
        }
        String senderName = sender instanceof Player player ? player.getName() : "Console";
        String message = Text.join(args, 0);
        sendStaffChat(senderName, message);
    }

    private void handleBroadcast(CommandSender sender, String[] args) {
        if (!has(sender, "jujucore.broadcast")) {
            return;
        }
        if (args.length == 0) {
            Text.send(plugin, sender, "&cNutze: /broadcast <message>");
            return;
        }
        String message = Text.join(args, 0);
        for (Player player : Bukkit.getOnlinePlayers()) {
            Text.raw(player, "&8[&bBroadcast&8] &f" + message);
        }
        Bukkit.getConsoleSender().sendMessage(Text.color("[Broadcast] " + message));
    }

    private void handleClearChat(CommandSender sender) {
        if (!has(sender, "jujucore.clearchat")) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (int i = 0; i < 120; i++) {
                player.sendMessage(" ");
            }
            Text.raw(player, "&8[&bChat&8] &7Der Chat wurde von &b" + sender.getName() + " &7geleert.");
        }
        Text.send(plugin, sender, "&aChat geleert.");
    }

    private void handleHat(CommandSender sender) {
        if (!has(sender, "jujucore.hat")) {
            return;
        }
        Player player = playerOnly(sender);
        if (player == null) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        ItemStack hand = inventory.getItemInMainHand();
        if (hand.getType() == Material.AIR) {
            Text.send(plugin, player, "&cHalte ein Item in der Hand.");
            return;
        }
        ItemStack helmet = inventory.getHelmet();
        inventory.setHelmet(hand.clone());
        inventory.setItemInMainHand(helmet == null ? new ItemStack(Material.AIR) : helmet);
        Text.send(plugin, player, "&aDu trägst jetzt dein Hand-Item als Hut.");
    }

    private void handleWorkbench(CommandSender sender) {
        if (!has(sender, "jujucore.workbench")) {
            return;
        }
        Player player = playerOnly(sender);
        if (player == null) {
            return;
        }
        player.openWorkbench(player.getLocation(), true);
    }

    private void handleSuicide(CommandSender sender) {
        if (!has(sender, "jujucore.suicide")) {
            return;
        }
        Player player = playerOnly(sender);
        if (player == null) {
            return;
        }
        player.setHealth(0.0D);
    }

    private void handleSeen(CommandSender sender, String[] args) {
        if (!has(sender, "jujucore.seen")) {
            return;
        }
        if (args.length == 0) {
            Text.send(plugin, sender, "&cNutze: /seen <player>");
            return;
        }
        Player online = Bukkit.getPlayerExact(args[0]);
        if (online != null && vanish.canSee(sender, online)) {
            Text.send(plugin, sender, "&b" + online.getName() + " &7ist gerade &aonline&7.");
            return;
        }
        if (online != null) {
            // vanished -> pretend they left when they went into vanish
            long since = vanish.vanishedSince(online);
            Text.send(plugin, sender, "&b" + online.getName() + " &7war zuletzt online: &b" + formatDate(since) + " &8(" + formatAgo(since) + ")");
            return;
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(args[0]);
        if (!offline.hasPlayedBefore()) {
            Text.send(plugin, sender, "&cDieser Spieler wurde noch nie gesehen.");
            return;
        }
        long lastPlayed = offline.getLastPlayed();
        Text.send(plugin, sender, "&b" + offline.getName() + " &7war zuletzt online: &b" + formatDate(lastPlayed) + " &8(" + formatAgo(lastPlayed) + ")");
    }

    private void handlePlaytime(CommandSender sender, String[] args) {
        Player target;
        if (args.length > 0) {
            if (!has(sender, "jujucore.playtime.others")) {
                return;
            }
            target = findPlayer(sender, args[0]);
        } else {
            if (!has(sender, "jujucore.playtime")) {
                return;
            }
            target = playerOnly(sender);
        }
        if (target == null) {
            return;
        }
        long seconds = getPlayTimeSeconds(target);
        Text.send(plugin, sender, "&7Spielzeit von &b" + target.getName() + "&7: &b" + formatDuration(seconds));
    }

    private void handleCheck(CommandSender sender, String[] args) {
        if (!has(sender, "jujucore.check")) {
            return;
        }
        if (args.length == 0) {
            Text.send(plugin, sender, "&cNutze: /check <player>");
            return;
        }
        Player target = findPlayer(sender, args[0]);
        if (target == null) {
            return;
        }
        Location location = target.getLocation();
        Text.raw(sender, "&8&m------------&r &bCheck: " + target.getName() + " &8&m------------");
        Text.raw(sender, "&7UUID: &f" + target.getUniqueId());
        Text.raw(sender, "&7Welt: &b" + target.getWorld().getName());
        Text.raw(sender, "&7Position: &b" + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ());
        Text.raw(sender, "&7Gamemode: &b" + target.getGameMode().name().toLowerCase());
        Text.raw(sender, "&7Health/Food: &b" + Math.round(target.getHealth()) + "&7/&b" + target.getFoodLevel());
        Text.raw(sender, "&7Ping: &b" + target.getPing() + "ms");
        Text.raw(sender, "&7Fly: " + (target.getAllowFlight() ? "&aJa" : "&cNein") + " &8| &7Vanish: " + (vanish.isVanished(target) ? "&aJa" : "&cNein") + " &8| &7God: " + (state.isGod(target.getUniqueId()) ? "&aJa" : "&cNein"));
        Text.raw(sender, "&7Frozen: " + (state.isFrozen(target.getUniqueId()) ? "&cJa" : "&aNein") + " &8| &7Spielzeit: &b" + formatDuration(getPlayTimeSeconds(target)));
    }

    private void handleWarp(CommandSender sender, String[] args) {
        if (!has(sender, "jujucore.warp")) {
            return;
        }
        Player player = playerOnly(sender);
        if (player == null) {
            return;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            handleWarps(sender);
            return;
        }
        String name = data.cleanWarpName(args[0]);
        Location warp = data.getWarp(name);
        if (warp == null) {
            Text.send(plugin, player, "&cDieser Warp existiert nicht. Nutze /warps.");
            return;
        }
        player.teleport(warp);
        Text.send(plugin, player, "&7Teleportiert zu Warp &b" + name + "&7.");
    }

    private void handleWarps(CommandSender sender) {
        if (!has(sender, "jujucore.warp")) {
            return;
        }
        List<String> warps = data.getWarps();
        Text.send(plugin, sender, warps.isEmpty() ? "&cEs gibt keine Warps." : "&7Warps: &b" + String.join("&7, &b", warps));
    }

    private void handleSetWarp(CommandSender sender, String[] args) {
        if (!has(sender, "jujucore.setwarp")) {
            return;
        }
        Player player = playerOnly(sender);
        if (player == null) {
            return;
        }
        if (args.length == 0) {
            Text.send(plugin, sender, "&cNutze: /setwarp <name>");
            return;
        }
        String name = data.cleanWarpName(args[0]);
        data.setWarp(name, player.getLocation());
        Text.send(plugin, player, "&aWarp &b" + name + " &agesetzt.");
    }

    private void handleDelWarp(CommandSender sender, String[] args) {
        if (!has(sender, "jujucore.delwarp")) {
            return;
        }
        if (args.length == 0) {
            Text.send(plugin, sender, "&cNutze: /delwarp <name>");
            return;
        }
        String name = data.cleanWarpName(args[0]);
        if (!data.deleteWarp(name)) {
            Text.send(plugin, sender, "&cDieser Warp existiert nicht.");
            return;
        }
        Text.send(plugin, sender, "&aWarp &b" + name + " &agelöscht.");
    }

    private void handleKit(CommandSender sender, String[] args) {
        Player player = playerOnly(sender);
        if (player == null) {
            return;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            handleKits(sender);
            return;
        }
        String name = kits.cleanKitName(args[0]);
        if (!kits.exists(name)) {
            Text.send(plugin, player, "&cDieses Kit existiert nicht. Nutze /kits.");
            return;
        }
        if (!hasKitAccess(player, name)) {
            Text.send(plugin, player, "&cDafuer hast du keine Rechte. &8(jujucore.kit." + name + ")");
            return;
        }
        long cooldownSeconds = kits.getCooldownSeconds(name);
        if (cooldownSeconds > 0L && !player.hasPermission("jujucore.kit.cooldown.bypass") && !player.hasPermission("jujucore.admin")) {
            long lastUse = data.getKitLastUse(player.getUniqueId(), name);
            long remainingMillis = cooldownSeconds * 1000L - (System.currentTimeMillis() - lastUse);
            if (remainingMillis > 0L) {
                Text.send(plugin, player, "&cDu kannst dieses Kit wieder in &b" + formatDuration((remainingMillis + 999L) / 1000L) + " &cnutzen.");
                return;
            }
        }
        int dropped = kits.giveKit(player, name);
        data.setKitLastUse(player.getUniqueId(), name, System.currentTimeMillis());
        if (dropped > 0) {
            Text.send(plugin, player, "&aKit &b" + name + " &aerhalten. &7" + dropped + " Stack(s) wurden gedroppt, weil dein Inventar voll war.");
            return;
        }
        Text.send(plugin, player, "&aKit &b" + name + " &aerhalten.");
    }

    private void handleKits(CommandSender sender) {
        List<String> names = availableKits(sender);
        if (names.isEmpty()) {
            Text.send(plugin, sender, "&cEs gibt keine Kits, die du nutzen kannst.");
            return;
        }
        Text.send(plugin, sender, "&7Kits: &b" + String.join("&7, &b", names));
    }

    private void handleCreateKit(CommandSender sender, String[] args) {
        if (!has(sender, "jujucore.kit.create")) {
            return;
        }
        Player player = playerOnly(sender);
        if (player == null) {
            return;
        }
        if (args.length == 0) {
            Text.send(plugin, player, "&cNutze: /createkit <name> [cooldownSekunden]");
            return;
        }
        String name = kits.cleanKitName(args[0]);
        long cooldownSeconds = args.length > 1 ? parseLong(args[1], 0L) : 0L;
        if (cooldownSeconds < 0L) {
            cooldownSeconds = 0L;
        }
        kits.createKit(name, player, cooldownSeconds);
        Text.send(plugin, player, "&aKit &b" + name + " &amit &b" + kits.getItemCount(name) + " &aItem-Stack(s) gespeichert.");
    }

    private void handleDelKit(CommandSender sender, String[] args) {
        if (!has(sender, "jujucore.kit.delete")) {
            return;
        }
        if (args.length == 0) {
            Text.send(plugin, sender, "&cNutze: /delkit <name>");
            return;
        }
        String name = kits.cleanKitName(args[0]);
        if (!kits.deleteKit(name)) {
            Text.send(plugin, sender, "&cDieses Kit existiert nicht.");
            return;
        }
        Text.send(plugin, sender, "&aKit &b" + name + " &ageloescht.");
    }

    private void handleTpa(CommandSender sender, String[] args) {
        if (!has(sender, "jujucore.tpa")) {
            return;
        }
        Player requester = playerOnly(sender);
        if (requester == null) {
            return;
        }
        if (args.length == 0) {
            Text.send(plugin, requester, "&cNutze: /tpa <player>");
            return;
        }
        Player target = findPlayer(sender, args[0]);
        if (target == null) {
            return;
        }
        if (target.equals(requester)) {
            Text.send(plugin, requester, "&cDu kannst dich nicht zu dir selbst teleportieren.");
            return;
        }
        long timeout = plugin.getConfig().getLong("tpa-timeout-seconds", 60L) * 1000L;
        pendingTpa.put(target.getUniqueId(), new TpaRequest(requester.getUniqueId(), System.currentTimeMillis() + timeout));
        Text.send(plugin, requester, "&7TPA-Anfrage an &b" + target.getName() + " &7gesendet.");
        Text.raw(target, Text.prefix(plugin) + Text.color("&b" + requester.getName() + " &7möchte sich zu dir teleportieren."));
        Text.raw(target, Text.prefix(plugin) + Text.color("&7Nutze &a/tpaccept &7oder &c/tpdeny&7."));
    }

    private void handleTpAccept(CommandSender sender) {
        if (!has(sender, "jujucore.tpa")) {
            return;
        }
        Player target = playerOnly(sender);
        if (target == null) {
            return;
        }
        TpaRequest request = pendingTpa.remove(target.getUniqueId());
        if (request == null || request.isExpired()) {
            Text.send(plugin, target, "&cDu hast keine offene TPA-Anfrage.");
            return;
        }
        Player requester = Bukkit.getPlayer(request.requester());
        if (requester == null) {
            Text.send(plugin, target, "&cDer Spieler ist nicht mehr online.");
            return;
        }
        requester.teleport(target.getLocation());
        Text.send(plugin, requester, "&aTPA angenommen. Du wurdest teleportiert.");
        Text.send(plugin, target, "&aDu hast die TPA-Anfrage angenommen.");
    }

    private void handleTpDeny(CommandSender sender) {
        if (!has(sender, "jujucore.tpa")) {
            return;
        }
        Player target = playerOnly(sender);
        if (target == null) {
            return;
        }
        TpaRequest request = pendingTpa.remove(target.getUniqueId());
        if (request == null || request.isExpired()) {
            Text.send(plugin, target, "&cDu hast keine offene TPA-Anfrage.");
            return;
        }
        Player requester = Bukkit.getPlayer(request.requester());
        if (requester != null) {
            Text.send(plugin, requester, "&cDeine TPA-Anfrage wurde abgelehnt.");
        }
        Text.send(plugin, target, "&cTPA-Anfrage abgelehnt.");
    }

    private void handleTpCancel(CommandSender sender) {
        if (!has(sender, "jujucore.tpa")) {
            return;
        }
        Player requester = playerOnly(sender);
        if (requester == null) {
            return;
        }
        UUID removedTarget = null;
        for (Map.Entry<UUID, TpaRequest> entry : pendingTpa.entrySet()) {
            if (entry.getValue().requester().equals(requester.getUniqueId())) {
                removedTarget = entry.getKey();
                break;
            }
        }
        if (removedTarget == null) {
            Text.send(plugin, requester, "&cDu hast keine offene TPA-Anfrage.");
            return;
        }
        pendingTpa.remove(removedTarget);
        Player target = Bukkit.getPlayer(removedTarget);
        if (target != null) {
            Text.send(plugin, target, "&c" + requester.getName() + " hat die TPA-Anfrage abgebrochen.");
        }
        Text.send(plugin, requester, "&cTPA-Anfrage abgebrochen.");
    }

    private void handleTroll(CommandSender sender, String[] args) {
        if (!has(sender, "jujucore.troll")) {
            return;
        }
        if (args.length < 2) {
            Text.send(plugin, sender, "&cNutze: /troll <player> <" + String.join("|", TROLLS) + "|random>");
            return;
        }
        Player target = findPlayer(sender, args[0]);
        if (target == null) {
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        if (action.equals("random")) {
            // random never picks the kick ones, too annoying
            List<String> actions = TROLLS.stream().filter(name -> !name.equals("fakeban") && !name.equals("fakecrash")).toList();
            action = actions.get(ThreadLocalRandom.current().nextInt(actions.size()));
        }
        // none of these should tell the target its a troll, it has to look like a bug
        UUID uuid = target.getUniqueId();
        switch (action) {
            case "jumpscare", "scare" -> {
                target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 20 * 3, 0, false, false, false));
                target.playSound(target.getLocation(), "entity.ender_dragon.growl", 1.0F, 0.6F);
                target.getWorld().strikeLightningEffect(target.getLocation());
            }
            case "fakeop", "op" -> target.sendMessage(adminMessage(Component.translatable("commands.op.success", target.name())));
            case "fakedeop", "deop" -> target.sendMessage(adminMessage(Component.translatable("commands.deop.success", target.name())));
            case "creeper" -> {
                // creeper hiss right behind them
                Location behind = behind(target, 1.5D);
                target.playSound(behind, "entity.creeper.primed", 1.0F, 0.5F);
                later(uuid, 30L, current -> current.playSound(behind, "entity.generic.explode", 1.0F, 1.0F));
            }
            case "launch", "rocket" -> {
                target.setFallDistance(0.0F);
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 7, 0, false, false, false));
                target.setVelocity(new Vector(0.0D, 1.35D, 0.0D));
                later(uuid, 35L, current -> current.setFallDistance(0.0F));
            }
            case "slap" -> {
                // like getting hit by something invisible
                Vector direction = target.getLocation().getDirection().multiply(-0.9D);
                direction.setY(0.45D);
                target.setVelocity(direction);
                target.playHurtAnimation(0.0F);
                target.getWorld().playSound(target.getLocation(), "entity.player.hurt", 1.0F, 1.0F);
            }
            case "spin" -> {
                for (int i = 1; i <= 12; i++) {
                    later(uuid, i * 2L, current -> {
                        Location location = current.getLocation();
                        location.setYaw(location.getYaw() + 90.0F);
                        state.teleportSilently(current, location);
                    });
                }
            }
            case "blind" -> target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 6, 0, false, false, false));
            case "fakeban", "ban" -> target.kick(Component.translatable("multiplayer.disconnect.banned"));
            case "herobrine" -> {
                if (!herobrine(target)) {
                    Text.send(plugin, sender, "&cHinter &b" + target.getName() + " &cist kein Platz zum Stehen, Herobrine kam nicht.");
                    return;
                }
            }
            case "ghost" -> {
                // footsteps coming from behind, then nothing
                for (int i = 0; i < 4; i++) {
                    double distance = 4.0D - i;
                    later(uuid, i * 8L, current -> {
                        Location step = behind(current, distance);
                        current.playSound(step, stepSound(step), 0.6F, 1.0F);
                    });
                }
                later(uuid, 40L, current -> current.playSound(current.getLocation(), "ambient.cave", 1.0F, 0.8F));
            }
            case "fakelag", "lag" -> {
                // rubberband back to the same spot like lag
                Location anchor = target.getLocation();
                for (int i = 1; i <= 6; i++) {
                    later(uuid, i * 8L, current -> {
                        if (current.getWorld() != anchor.getWorld()) {
                            return;
                        }
                        Location back = anchor.clone();
                        back.setYaw(current.getLocation().getYaw());
                        back.setPitch(current.getLocation().getPitch());
                        current.setFallDistance(0.0F);
                        state.teleportSilently(current, back);
                    });
                }
            }
            case "hiccup" -> {
                for (long delay : new long[]{0L, 25L, 55L}) {
                    later(uuid, delay, current -> {
                        current.setVelocity(current.getVelocity().setY(0.32D));
                        current.getWorld().playSound(current.getLocation(), "entity.player.burp", 0.8F, 1.8F);
                    });
                }
            }
            case "nod" -> {
                float[] pitches = {-50.0F, 45.0F, -50.0F, 45.0F, -50.0F, 45.0F, 0.0F};
                for (int i = 0; i < pitches.length; i++) {
                    float pitch = pitches[i];
                    later(uuid, (i + 1) * 3L, current -> {
                        Location location = current.getLocation();
                        location.setPitch(pitch);
                        state.teleportSilently(current, location);
                    });
                }
            }
            case "shuffle" -> {
                PlayerInventory inventory = target.getInventory();
                List<ItemStack> hotbar = new ArrayList<>();
                for (int slot = 0; slot < 9; slot++) {
                    hotbar.add(inventory.getItem(slot));
                }
                Collections.shuffle(hotbar);
                for (int slot = 0; slot < 9; slot++) {
                    inventory.setItem(slot, hotbar.get(slot));
                }
            }
            case "diamonds" -> {
                // fake diamond ore only the target sees, real blocks dont change
                List<Location> changed = new ArrayList<>();
                Block feet = target.getLocation().getBlock();
                for (int x = -2; x <= 2; x++) {
                    for (int z = -2; z <= 2; z++) {
                        Block ground = feet.getRelative(x, -1, z);
                        if (ground.getType().isSolid()) {
                            target.sendBlockChange(ground.getLocation(), Material.DIAMOND_ORE.createBlockData());
                            changed.add(ground.getLocation());
                        }
                    }
                }
                later(uuid, 20L * 6, current -> {
                    for (Location location : changed) {
                        current.sendBlockChange(location, location.getBlock().getBlockData());
                    }
                });
            }
            case "tnt" -> {
                // real primed tnt but its a dud (CoreListener cancels it)
                Location spot = behind(target, 1.5D);
                target.getWorld().spawn(spot, TNTPrimed.class, tnt -> {
                    tnt.setFuseTicks(80);
                    tnt.setYield(0.0F);
                    tnt.setIsIncendiary(false);
                    tnt.getPersistentDataContainer().set(plugin.trollTntKey(), PersistentDataType.BYTE, (byte) 1);
                });
                target.getWorld().playSound(spot, "entity.tnt.primed", 1.0F, 1.0F);
            }
            case "fakecrash", "crash" -> target.kick(Component.translatable("disconnect.genericReason", Component.text("java.net.SocketException: Connection reset")));
            default -> {
                Text.send(plugin, sender, "&cUnbekannter Troll. Nutze: " + String.join(", ", TROLLS) + ", random");
                return;
            }
        }
        Text.send(plugin, sender, "&aTroll &b" + action + " &aauf &b" + target.getName() + " &aausgeführt.");
    }

    private static final List<String> TROLLS = List.of(
            "jumpscare", "fakeop", "fakedeop", "creeper", "launch", "slap", "spin", "blind", "fakeban",
            "herobrine", "ghost", "fakelag", "hiccup", "nod", "shuffle", "diamonds", "tnt", "fakecrash");

    // herobrine stands ~10 blocks behind the target and stares. only the target sees him,
    // he vanishes once they look at him. no chat msgs, way creepier like that.
    // returns false if theres no space behind them (wall, water)
    private boolean herobrine(Player target) {
        UUID uuid = target.getUniqueId();
        Location spot = null;
        for (double distance : new double[]{10.0D, 12.0D, 8.0D, 14.0D, 6.0D}) {
            spot = standingSpot(behind(target, distance), target.getLocation().getBlockY());
            if (spot != null) {
                break;
            }
        }
        if (spot == null) {
            return false;
        }
        spot.setDirection(target.getEyeLocation().toVector().subtract(spot.toVector().add(new Vector(0.0D, 1.62D, 0.0D))));
        Mannequin herobrine = spot.getWorld().spawn(spot, Mannequin.class, mannequin -> {
            mannequin.setVisibleByDefault(false);
            mannequin.setPersistent(false);
            mannequin.setInvulnerable(true);
            mannequin.setSilent(true);
            mannequin.setImmovable(true);
            mannequin.setCollidable(false);
            mannequin.setProfile(HerobrineSkin.profile());
            mannequin.setDescription(Component.empty());
        });
        target.showEntity(plugin, herobrine);
        later(uuid, 60L, current -> {
            if (herobrine.isValid()) {
                current.playSound(herobrine.getLocation(), "ambient.cave", 1.0F, 0.9F);
            }
        });
        int[] age = {0};
        int[] seenFor = {-1};
        BukkitTask[] task = new BukkitTask[1];
        task[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Player current = Bukkit.getPlayer(uuid);
            age[0] += 2;
            boolean gone = current == null || current.isDead() || current.getWorld() != herobrine.getWorld() || !herobrine.isValid();
            // seen for half a sec, or 20s passed
            if (gone || age[0] > 20 * 20 || seenFor[0] >= 10) {
                herobrine.remove();
                task[0].cancel();
                return;
            }
            Vector toTarget = current.getEyeLocation().toVector().subtract(herobrine.getEyeLocation().toVector());
            Location facing = herobrine.getLocation().setDirection(toTarget);
            herobrine.setRotation(facing.getYaw(), facing.getPitch());
            herobrine.setBodyYaw(facing.getYaw());
            if (seenFor[0] >= 0) {
                seenFor[0] += 2;
                return;
            }
            Vector toHim = toTarget.multiply(-1.0D);
            if (current.getEyeLocation().getDirection().angle(toHim) < Math.toRadians(20.0D) && current.hasLineOfSight(herobrine)) {
                seenFor[0] = 0;
            }
        }, 2L, 2L);
        return true;
    }

    // ground with 2 air blocks above, checks a bit up and down
    private Location standingSpot(Location around, int targetY) {
        World world = around.getWorld();
        int x = around.getBlockX();
        int z = around.getBlockZ();
        for (int y = targetY + 6; y >= targetY - 10; y--) {
            Block below = world.getBlockAt(x, y - 1, z);
            Block feet = world.getBlockAt(x, y, z);
            Block head = world.getBlockAt(x, y + 1, z);
            if (below.getType().isSolid() && feet.isPassable() && head.isPassable() && !feet.isLiquid() && !head.isLiquid()) {
                return new Location(world, x + 0.5D, y, z + 0.5D);
            }
        }
        return null;
    }

    // looks like the vanilla "[Server: ...]" op message
    private Component adminMessage(Component content) {
        return Component.translatable("chat.type.admin", NamedTextColor.GRAY, Component.text("Server"), content).decorate(TextDecoration.ITALIC);
    }

    // spot behind the player at foot height
    private Location behind(Player player, double distance) {
        Vector direction = player.getLocation().getDirection().setY(0.0D);
        if (direction.lengthSquared() < 0.0001D) {
            direction = new Vector(0.0D, 0.0D, 1.0D);
        }
        return player.getLocation().subtract(direction.normalize().multiply(distance));
    }

    private Sound stepSound(Location location) {
        return location.getBlock().getRelative(0, -1, 0).getBlockData().getSoundGroup().getStepSound();
    }

    // run later, but only if theyre still online
    private void later(UUID uuid, long delay, Consumer<Player> task) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player current = Bukkit.getPlayer(uuid);
            if (current != null) {
                task.accept(current);
            }
        }, delay);
    }


    private void sendPrivateMessage(CommandSender sender, Player target, String message) {
        String senderName = sender instanceof Player player ? player.getName() : "Console";
        Text.raw(sender, "&8[&7Du &8-> &b" + target.getName() + "&8] &f" + message);
        Text.raw(target, "&8[&b" + senderName + " &8-> &7Dir&8] &f" + message);
        if (sender instanceof Player player) {
            lastMessage.put(player.getUniqueId(), target.getUniqueId());
            lastMessage.put(target.getUniqueId(), player.getUniqueId());
        }
        for (UUID uuid : socialSpy) {
            Player spy = Bukkit.getPlayer(uuid);
            if (spy == null || spy.equals(sender) || spy.equals(target)) {
                continue;
            }
            Text.raw(spy, "&8[&cSpy&8] &7" + senderName + " &8-> &7" + target.getName() + "&8: &f" + message);
        }
    }

    private void sendStaffChat(String senderName, String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("jujucore.staffchat") || player.hasPermission("jujucore.admin")) {
                Text.raw(player, "&8[&bSC&8] &7" + senderName + "&8: &f" + message);
            }
        }
        Bukkit.getConsoleSender().sendMessage(Text.color("[SC] " + senderName + ": " + message));
    }

    private List<String> availableKits(CommandSender sender) {
        List<String> available = new ArrayList<>();
        for (String name : kits.kitNames()) {
            if (hasKitAccess(sender, name)) {
                available.add(name);
            }
        }
        return available;
    }

    private boolean hasKitAccess(CommandSender sender, String name) {
        return sender.hasPermission("jujucore.kit")
                || sender.hasPermission("jujucore.kit." + name)
                || sender.hasPermission("jujucore.admin");
    }

    private Player targetFromArgs(CommandSender sender, String[] args, int index, String othersPermission) {
        if (args.length > index) {
            if (!has(sender, othersPermission)) {
                return null;
            }
            return findPlayer(sender, args[index]);
        }
        return playerOnly(sender);
    }

    private Player findPlayer(CommandSender sender, String name) {
        Player exact = Bukkit.getPlayerExact(name);
        if (exact != null && vanish.canSee(sender, exact)) {
            return exact;
        }
        List<Player> matches = Bukkit.matchPlayer(name).stream().filter(player -> vanish.canSee(sender, player)).toList();
        if (matches.isEmpty()) {
            Text.send(plugin, sender, "&cSpieler nicht gefunden.");
            return null;
        }
        if (matches.size() > 1) {
            Text.send(plugin, sender, "&cMehrere Spieler gefunden: &7" + String.join(", ", matches.stream().map(Player::getName).toList()));
            return null;
        }
        return matches.getFirst();
    }

    private Player playerOnly(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            Text.send(plugin, sender, "&cDieser Command ist nur für Spieler.");
            return null;
        }
        return player;
    }

    private void handleVillager(CommandSender sender, String[] args) {
        if (!has(sender, "jujucore.villager")) {
            return;
        }
        Player player = playerOnly(sender);
        if (player == null) {
            return;
        }
        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "";
        switch (sub) {
            case "book" -> handleVillagerBook(player, args);
            case "clearhate" -> handleVillagerClearHate(player, args);
            default -> {
                Text.send(plugin, player, "&cNutze: /villager book <verzauberung> [stufe]");
                Text.send(plugin, player, "&cOder: /villager clearhate [radius]");
            }
        }
    }

    // the librarian you look at sells the book you picked from now on, same price
    private void handleVillagerBook(Player player, String[] args) {
        if (args.length < 2) {
            Text.send(plugin, player, "&cNutze: /villager book <verzauberung> [stufe]");
            return;
        }
        Enchantment enchantment = findEnchantment(args[1]);
        if (enchantment == null) {
            Text.send(plugin, player, "&cUnbekannte Verzauberung: &f" + args[1]);
            return;
        }
        int max = enchantment.getMaxLevel();
        int level = args.length > 2 ? parseInt(args[2], -1) : max;
        if (level < 1 || level > max) {
            Text.send(plugin, player, "&cStufe für &f" + enchantmentName(enchantment) + " &cgeht von 1 bis " + max + ".");
            return;
        }
        Villager villager = targetVillager(player);
        if (villager == null) {
            Text.send(plugin, player, "&cSchau einen Dorfbewohner an, höchstens 6 Blöcke entfernt.");
            return;
        }
        if (!Villager.Profession.LIBRARIAN.equals(villager.getProfession())) {
            Text.send(plugin, player, "&cDas ist kein Bibliothekar.");
            return;
        }

        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        meta.addStoredEnchant(enchantment, level, true);
        book.setItemMeta(meta);

        List<MerchantRecipe> recipes = new ArrayList<>(villager.getRecipes());
        int index = -1;
        for (int i = 0; i < recipes.size(); i++) {
            if (recipes.get(i).getResult().getType() == Material.ENCHANTED_BOOK) {
                index = i;
                break;
            }
        }
        MerchantRecipe recipe;
        if (index >= 0) {
            MerchantRecipe old = recipes.get(index);
            recipe = new MerchantRecipe(book, old.getUses(), old.getMaxUses(), old.hasExperienceReward(),
                    old.getVillagerExperience(), old.getPriceMultiplier(), old.getDemand(), old.getSpecialPrice());
            recipe.setIngredients(old.getIngredients());
            villager.setRecipe(index, recipe);
        } else {
            // no book trade yet -> add one, price is around what vanilla would ask
            int price = 2 + (5 + level * 10) / 2 + 3 * level;
            if (enchantment.isTreasure()) {
                price *= 2;
            }
            recipe = new MerchantRecipe(book, 0, 12, true, 1, 0.2F);
            recipe.addIngredient(new ItemStack(Material.EMERALD, Math.min(64, price)));
            recipe.addIngredient(new ItemStack(Material.BOOK));
            recipes.add(recipe);
            villager.setRecipes(recipes);
        }
        // once traded the villager keeps its job + trades
        if (villager.getVillagerExperience() < 1) {
            villager.setVillagerExperience(1);
        }
        int emeralds = recipe.getIngredients().isEmpty() ? 0 : recipe.getIngredients().get(0).getAmount();
        Text.send(plugin, player, "&aDer Bibliothekar verkauft jetzt &b" + enchantmentName(enchantment) + " " + level
                + " &afür &b" + emeralds + " Smaragde &aund ein Buch.");
    }

    // villagers forget all gossip about players so prices go back to normal
    private void handleVillagerClearHate(Player player, String[] args) {
        int radius = args.length > 1 ? parseInt(args[1], 64) : 64;
        radius = Math.max(1, Math.min(256, radius));
        int count = 0;
        for (Villager villager : player.getWorld().getNearbyEntitiesByType(Villager.class, player.getLocation(), radius)) {
            villager.clearReputations();
            count++;
        }
        Text.send(plugin, player, "&aBei &b" + count + " &aDorfbewohner(n) im Umkreis von " + radius + " Blöcken ist der Hass weg.");
    }

    private Villager targetVillager(Player player) {
        Entity target = player.getTargetEntity(6);
        return target instanceof Villager villager ? villager : null;
    }

    private Enchantment findEnchantment(String input) {
        String key = input.toLowerCase(Locale.ROOT);
        NamespacedKey namespacedKey = key.contains(":") ? NamespacedKey.fromString(key) : NamespacedKey.minecraft(key);
        if (namespacedKey == null) {
            return null;
        }
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(namespacedKey);
    }

    private String enchantmentName(Enchantment enchantment) {
        return enchantment.getKey().getKey();
    }

    private List<String> enchantmentNames() {
        List<String> names = new ArrayList<>();
        for (Enchantment enchantment : RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)) {
            names.add(enchantmentName(enchantment));
        }
        names.sort(null);
        return names;
    }

    private boolean has(CommandSender sender, String permission) {
        if (sender.hasPermission(permission) || sender.hasPermission("jujucore.admin")) {
            return true;
        }
        Text.send(plugin, sender, "&cDafür hast du keine Rechte. &8(" + permission + ")");
        return false;
    }

    private GameMode parseGameMode(String input) {
        return switch (input.toLowerCase()) {
            case "0", "s", "survival" -> GameMode.SURVIVAL;
            case "1", "c", "creative" -> GameMode.CREATIVE;
            case "2", "a", "adventure" -> GameMode.ADVENTURE;
            case "3", "sp", "spec", "spectator" -> GameMode.SPECTATOR;
            default -> null;
        };
    }

    private int parseInt(String input, int fallback) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private long parseLong(String input, long fallback) {
        try {
            return Long.parseLong(input);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private int repairInventory(PlayerInventory inventory) {
        int repaired = 0;
        for (ItemStack item : inventory.getContents()) {
            if (repairItem(item)) {
                repaired++;
            }
        }
        for (ItemStack item : inventory.getArmorContents()) {
            if (repairItem(item)) {
                repaired++;
            }
        }
        if (repairItem(inventory.getItemInOffHand())) {
            repaired++;
        }
        return repaired;
    }

    private boolean repairItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable damageable) || !damageable.hasDamage()) {
            return false;
        }
        damageable.setDamage(0);
        item.setItemMeta(meta);
        return true;
    }

    private Location findTop(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return null;
        }
        int x = location.getBlockX();
        int z = location.getBlockZ();
        int start = Math.min(world.getHighestBlockYAt(x, z) + 2, world.getMaxHeight() - 2);
        int min = world.getMinHeight() + 1;
        for (int y = start; y >= min; y--) {
            Block below = world.getBlockAt(x, y - 1, z);
            Block feet = world.getBlockAt(x, y, z);
            Block head = world.getBlockAt(x, y + 1, z);
            if (below.getType().isSolid() && feet.isPassable() && head.isPassable() && !feet.isLiquid() && !head.isLiquid()) {
                return new Location(world, x + 0.5D, y, z + 0.5D, location.getYaw(), location.getPitch());
            }
        }
        return null;
    }

    private long getPlayTimeSeconds(Player player) {
        try {
            return player.getStatistic(Statistic.valueOf("PLAY_TIME")) / 20L;
        } catch (IllegalArgumentException ignored) {
            return player.getStatistic(Statistic.valueOf("PLAY_ONE_MINUTE")) / 20L;
        }
    }

    private String formatDate(long millis) {
        if (millis <= 0L) {
            return "unbekannt";
        }
        return dateFormat.format(new Date(millis));
    }

    private String formatAgo(long millis) {
        if (millis <= 0L) {
            return "unbekannt";
        }
        long seconds = Math.max(0L, (System.currentTimeMillis() - millis) / 1000L);
        return "vor " + formatDuration(seconds);
    }

    private String formatDuration(long seconds) {
        long days = seconds / 86400L;
        long hours = seconds % 86400L / 3600L;
        long minutes = seconds % 3600L / 60L;
        long secs = seconds % 60L;
        if (days > 0L) {
            return days + "d " + hours + "h " + minutes + "m";
        }
        if (hours > 0L) {
            return hours + "h " + minutes + "m";
        }
        if (minutes > 0L) {
            return minutes + "m " + secs + "s";
        }
        return secs + "s";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase();
        if (args.length == 1) {
            return switch (name) {
                case "jujucore" -> Text.filter(List.of("reload"), args[0]);
                case "gm" -> Text.filter(List.of("0", "1", "2", "3", "survival", "creative", "adventure", "spectator"), args[0]);
                case "tp" -> Text.filter(withTilde(playerNames(sender)), args[0]);
                case "gmc", "gms", "gma", "gmsp", "fly", "vanish", "heal", "feed", "tphere", "invsee", "ec", "clearinventory", "god", "freeze", "unfreeze", "tpa", "seen", "playtime", "check", "troll" -> Text.filter(playerNames(sender), args[0]);
                case "repair" -> Text.filter(List.of("all"), args[0]);
                case "speed" -> Text.filter(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"), args[0]);
                case "home", "delhome" -> sender instanceof Player player ? Text.filter(data.getHomes(player.getUniqueId()), args[0]) : List.of();
                case "warp", "delwarp" -> Text.filter(data.getWarps(), args[0]);
                case "kit" -> Text.filter(availableKits(sender), args[0]);
                case "delkit" -> Text.filter(kits.kitNames(), args[0]);
                case "report" -> Text.filter(playerNames(sender), args[0]);
                case "reports" -> Text.filter(List.of("list", "info", "close", "clear"), args[0]);
                case "weather" -> Text.filter(List.of("clear", "rain", "thunder"), args[0]);
                case "villager" -> Text.filter(List.of("book", "clearhate"), args[0]);
                default -> List.of();
            };
        }
        if (args.length == 2) {
            return switch (name) {
                case "gm", "speed" -> Text.filter(playerNames(sender), args[1]);
                case "tp" -> Text.filter(withTilde(playerNames(sender)), args[1]);
                case "createkit" -> Text.filter(List.of("0", "60", "300", "3600", "86400"), args[1]);
                case "reports" -> args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("close") ? Text.filter(reportIds(), args[1]) : List.of();
                case "troll" -> {
                    List<String> trolls = new ArrayList<>(TROLLS);
                    trolls.add("random");
                    yield Text.filter(trolls, args[1]);
                }
                case "villager" -> args[0].equalsIgnoreCase("book") ? Text.filter(enchantmentNames(), args[1])
                        : args[0].equalsIgnoreCase("clearhate") ? Text.filter(List.of("16", "32", "64", "128"), args[1]) : List.of();
                default -> List.of();
            };
        }
        if ((args.length == 3 || args.length == 4) && name.equals("tp")) {
            return Text.filter(List.of("~"), args[args.length - 1]);
        }
        if (args.length == 3 && name.equals("villager") && args[0].equalsIgnoreCase("book")) {
            Enchantment enchantment = findEnchantment(args[1]);
            List<String> levels = new ArrayList<>();
            for (int level = 1; enchantment != null && level <= enchantment.getMaxLevel(); level++) {
                levels.add(String.valueOf(level));
            }
            return Text.filter(levels, args[2]);
        }
        return List.of();
    }

    private List<String> playerNames(CommandSender sender) {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (vanish.canSee(sender, player)) {
                names.add(player.getName());
            }
        }
        return names;
    }

    private List<String> withTilde(List<String> values) {
        List<String> result = new ArrayList<>(values);
        result.add("~");
        return result;
    }

    private List<String> reportIds() {
        List<String> ids = new ArrayList<>();
        for (int id : reports.activeReports()) {
            ids.add(String.valueOf(id));
        }
        return ids;
    }

    private record TpaRequest(UUID requester, long expiresAt) {
        private boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
