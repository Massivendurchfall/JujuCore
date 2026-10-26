package org.juju.jujucore.util;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class Text {

    private Text() {
    }

    public static String color(String s) {
        return s == null ? "" : s.replace('&', '§');
    }

    public static String prefix(JavaPlugin plugin) {
        return color(plugin.getConfig().getString("prefix", "&bJujuCore &8» &7"));
    }

    public static void send(JavaPlugin plugin, CommandSender sender, String msg) {
        sender.sendMessage(prefix(plugin) + color(msg));
    }

    public static void raw(CommandSender sender, String msg) {
        sender.sendMessage(color(msg));
    }

    public static String join(String[] args, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i > start) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }

    // tab complete helper
    public static List<String> filter(List<String> options, String input) {
        String lower = input.toLowerCase();
        List<String> out = new ArrayList<>();
        for (String s : options) {
            if (s.toLowerCase().startsWith(lower)) out.add(s);
        }
        return out;
    }
}
