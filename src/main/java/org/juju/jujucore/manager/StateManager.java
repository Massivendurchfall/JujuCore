package org.juju.jujucore.manager;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class StateManager {

    private final Set<UUID> godMode = new HashSet<>();
    private final Set<UUID> frozen = new HashSet<>();
    private final Set<UUID> staffChat = new HashSet<>();
    private final Set<UUID> silentTp = new HashSet<>();

    // for trolls that just turn the camera, so it doesnt mess up /back
    public void teleportSilently(Player p, Location loc) {
        silentTp.add(p.getUniqueId());
        try {
            p.teleport(loc);
        } finally {
            silentTp.remove(p.getUniqueId());
        }
    }

    public boolean isSilentTeleport(UUID uuid) {
        return silentTp.contains(uuid);
    }

    public boolean toggleGod(UUID uuid) {
        if (godMode.contains(uuid)) {
            godMode.remove(uuid);
            return false;
        }
        godMode.add(uuid);
        return true;
    }

    public boolean isGod(UUID uuid) {
        return godMode.contains(uuid);
    }

    public void setFrozen(UUID uuid, boolean value) {
        if (value) {
            frozen.add(uuid);
        } else {
            frozen.remove(uuid);
        }
    }

    public boolean toggleFrozen(UUID uuid) {
        if (frozen.contains(uuid)) {
            frozen.remove(uuid);
            return false;
        }
        frozen.add(uuid);
        return true;
    }

    public boolean isFrozen(UUID uuid) {
        return frozen.contains(uuid);
    }

    public boolean toggleStaffChat(UUID uuid) {
        if (staffChat.contains(uuid)) {
            staffChat.remove(uuid);
            return false;
        }
        staffChat.add(uuid);
        return true;
    }

    public boolean isStaffChat(UUID uuid) {
        return staffChat.contains(uuid);
    }

    public void clear(UUID uuid) {
        godMode.remove(uuid);
        frozen.remove(uuid);
        staffChat.remove(uuid);
    }
}
