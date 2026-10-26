package org.juju.jujucore.manager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Chest;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// chest owners are stored in the block entity itself, so it survives restarts and is gone when the block is gone.
// works for chests, barrels and shulkers. hoppers only remember who placed them
public class ChestLockManager {

    public static final String BYPASS_PERMISSION = "jujucore.kiste.bypass";

    private final NamespacedKey ownerKey;
    private final NamespacedKey trustedKey;
    private final NamespacedKey placerKey;

    public ChestLockManager(JavaPlugin plugin) {
        this.ownerKey = new NamespacedKey(plugin, "kiste_owner");
        this.trustedKey = new NamespacedKey(plugin, "kiste_trusted");
        this.placerKey = new NamespacedKey(plugin, "kiste_placer");
    }

    // only this one block, not the other half
    public void markChestPlacer(Block block, UUID placer) {
        write(block, placerKey, placer.toString());
    }

    // null for old chests from before we saved placers
    public UUID chestPlacer(Block block) {
        String v = read(block, placerKey);
        return v == null ? null : parseUuid(v);
    }

    // true if the player placed both halves
    public boolean placedEntirelyBy(Block block, UUID player) {
        for (Block b : parts(block)) {
            if (!player.equals(chestPlacer(b))) return false;
        }
        return true;
    }

    public static boolean isLockable(Material type) {
        if (type == Material.BARREL || Tag.SHULKER_BOXES.isTagged(type)) {
            return true;
        }
        return type.name().endsWith("CHEST") && type != Material.ENDER_CHEST;
    }

    public static boolean isLockable(Block block) {
        return isLockable(block.getType());
    }

    // the block + the other half if its a double chest
    public List<Block> parts(Block block) {
        List<Block> list = new ArrayList<>(2);
        list.add(block);
        BlockData bd = block.getBlockData();
        if (bd instanceof Chest chest && chest.getType() != Chest.Type.SINGLE) {
            BlockFace facing = chest.getFacing();
            BlockFace dir = chest.getType() == Chest.Type.LEFT ? clockwise(facing) : counterClockwise(facing);
            Block other = block.getRelative(dir);
            if (other.getType() == block.getType()) {
                list.add(other);
            }
        }
        return list;
    }

    public UUID owner(Block block) {
        for (Block b : parts(block)) {
            String v = read(b, ownerKey);
            if (v != null) return parseUuid(v);
        }
        return null;
    }

    public Set<UUID> trusted(Block block) {
        Set<UUID> set = new LinkedHashSet<>();
        for (Block b : parts(block)) {
            String v = read(b, trustedKey);
            if (v == null || v.isEmpty()) continue;
            for (String s : v.split(",")) {
                UUID id = parseUuid(s);
                if (id != null) set.add(id);
            }
        }
        return set;
    }

    public boolean isLocked(Block block) {
        return isLockable(block) && owner(block) != null;
    }

    public boolean canAccess(Player player, Block block) {
        if (player.hasPermission(BYPASS_PERMISSION)) return true;
        UUID owner = owner(block);
        return owner == null || owner.equals(player.getUniqueId()) || trusted(block).contains(player.getUniqueId());
    }

    public boolean isOwner(Player player, Block block) {
        UUID owner = owner(block);
        return owner != null && owner.equals(player.getUniqueId());
    }

    // sets owner on both halves and keeps the trusted list the same on both
    public void setOwner(Block block, UUID owner, Set<UUID> trusted) {
        String joined = String.join(",", trusted.stream().map(UUID::toString).toList());
        for (Block b : parts(block)) {
            write(b, ownerKey, owner == null ? null : owner.toString());
            write(b, trustedKey, owner == null || joined.isEmpty() ? null : joined);
        }
    }

    public void setTrusted(Block block, Set<UUID> trusted) {
        setOwner(block, owner(block), trusted);
    }

    // hoppers just get a placer tag, everyone can still use them
    public void markPlacer(Block hopper, UUID placer) {
        write(hopper, ownerKey, placer.toString());
    }

    public UUID placer(Block hopper) {
        String v = read(hopper, ownerKey);
        return v == null ? null : parseUuid(v);
    }

    public static String name(UUID id) {
        if (id == null) return "niemand";
        OfflinePlayer p = Bukkit.getOfflinePlayer(id);
        return p.getName() == null ? id.toString().substring(0, 8) : p.getName();
    }

    private String read(Block block, NamespacedKey key) {
        BlockState state = block.getState(false);
        if (!(state instanceof TileState tile)) return null;
        return tile.getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    private void write(Block block, NamespacedKey key, String value) {
        BlockState state = block.getState();
        if (!(state instanceof TileState tile)) return;
        PersistentDataContainer pdc = tile.getPersistentDataContainer();
        if (value == null) {
            pdc.remove(key);
        } else {
            pdc.set(key, PersistentDataType.STRING, value);
        }
        tile.update(true, false);
    }

    private static UUID parseUuid(String s) {
        try {
            return UUID.fromString(s.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static BlockFace clockwise(BlockFace f) {
        return switch (f) {
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            default -> BlockFace.NORTH;
        };
    }

    private static BlockFace counterClockwise(BlockFace f) {
        return switch (f) {
            case NORTH -> BlockFace.WEST;
            case WEST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.EAST;
            default -> BlockFace.NORTH;
        };
    }
}
