package org.juju.jujucore;

import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.juju.jujucore.command.ChestCommand;
import org.juju.jujucore.command.CoreCommand;
import org.juju.jujucore.listener.ChestListener;
import org.juju.jujucore.listener.CoreListener;
import org.juju.jujucore.listener.VanishListener;
import org.juju.jujucore.manager.ChestLockManager;
import org.juju.jujucore.manager.ChestLogManager;
import org.juju.jujucore.manager.DataManager;
import org.juju.jujucore.manager.KitManager;
import org.juju.jujucore.manager.ReportManager;
import org.juju.jujucore.manager.StateManager;
import org.juju.jujucore.manager.VanishManager;

import java.util.Arrays;
import java.util.List;

public class JujuCore extends JavaPlugin {

    private DataManager data;
    private KitManager kits;
    private ReportManager reports;
    private VanishManager vanish;
    private StateManager state;
    private CoreCommand coreCmd;
    private NamespacedKey trollTntKey;

    public NamespacedKey trollTntKey() {
        return trollTntKey;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        trollTntKey = new NamespacedKey(this, "troll_tnt");
        data = new DataManager(this);
        kits = new KitManager(this);
        reports = new ReportManager(this);
        vanish = new VanishManager(this);
        state = new StateManager();
        coreCmd = new CoreCommand(this, data, kits, reports, vanish, state);
        registerCommands();

        getServer().getPluginManager().registerEvents(new CoreListener(this, data, vanish, state), this);
        getServer().getPluginManager().registerEvents(new VanishListener(this, vanish), this);

        // chest stuff
        ChestLockManager locks = new ChestLockManager(this);
        ChestLogManager log = new ChestLogManager(this);
        ChestListener chestListener = new ChestListener(this, locks, log);
        getServer().getPluginManager().registerEvents(chestListener, this);
        chestListener.hookLogin();
        PluginCommand kiste = getCommand("kiste");
        if (kiste != null) {
            ChestCommand chestCmd = new ChestCommand(this, locks, log);
            kiste.setExecutor(chestCmd);
            kiste.setTabCompleter(chestCmd);
        }
        getLogger().info("JujuCore enabled");
    }

    @Override
    public void onDisable() {
        if (data != null) data.save();
        if (kits != null) kits.save();
        if (reports != null) reports.save();
        getLogger().info("JujuCore disabled");
    }

    public void reloadAll() {
        reloadConfig();
        data.reload();
        kits.reload();
        reports.reload();
    }

    private void registerCommands() {
        List<String> names = Arrays.asList(
            "jujucore", "gm", "gmc", "gms", "gma", "gmsp", "fly", "vanish", "top", "heal", "feed",
            "repair", "speed", "spawn", "setspawn", "sethome", "home", "delhome", "back", "tp", "tphere",
            "invsee", "ec", "msg", "reply", "socialspy", "report", "reports", "day", "night", "weather",
            "clearinventory", "trash", "near", "god", "freeze", "unfreeze", "staffchat", "broadcast", "clearchat",
            "hat", "workbench", "suicide", "seen", "playtime", "check", "warp", "warps", "setwarp", "delwarp",
            "tpa", "tpaccept", "tpdeny", "tpcancel", "kit", "kits", "createkit", "delkit", "troll", "villager"
        );
        for (String name : names) {
            PluginCommand cmd = getCommand(name);
            if (cmd != null) {
                cmd.setExecutor(coreCmd);
                cmd.setTabCompleter(coreCmd);
            }
        }
    }
}
