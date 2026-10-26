package org.juju.jujucore.listener;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.EnderChest;
import org.bukkit.block.Lidded;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.event.world.GenericGameEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.juju.jujucore.JujuCore;
import org.juju.jujucore.manager.VanishManager;
import org.juju.jujucore.util.Text;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// all the stuff that could give away a vanished player
public class VanishListener implements Listener {

    private final JujuCore plugin;
    private final VanishManager vanish;
    private final Set<UUID> chestHintShown = new HashSet<>();

    public VanishListener(JujuCore plugin, VanishManager vanish) {
        this.plugin = plugin;
        this.vanish = vanish;
    }

    private boolean vanished(Object entity) {
        return entity instanceof Player p && vanish.isVanished(p);
    }

    @EventHandler(ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent e) {
        if (vanished(e.getTarget())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        if (vanished(e.getEntity())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onArrowPickup(PlayerPickupArrowEvent e) {
        if (vanished(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (vanished(e.getEntity())) e.setCancelled(true);
    }

    // sculk sensors, shriekers, wardens, allays...
    @EventHandler(ignoreCancelled = true)
    public void onGameEvent(GenericGameEvent e) {
        if (vanished(e.getEntity())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent e) {
        if (vanished(e.getEntity())) e.deathMessage(null);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAdvancement(PlayerAdvancementDoneEvent e) {
        if (vanished(e.getPlayer())) e.message(null);
    }

    @EventHandler
    public void onPing(PaperServerListPingEvent e) {
        if (vanish.onlineVanishedCount() == 0) return;
        e.getListedPlayers().removeIf(info -> vanish.isVanished(info.id()));
        e.setNumPlayers(Math.max(0, e.getNumPlayers() - vanish.onlineVanishedCount()));
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!vanish.isVanished(p)) return;

        // pressure plates, tripwire, farmland, turtle eggs, redstone ore
        if (e.getAction() == Action.PHYSICAL) {
            e.setCancelled(true);
            return;
        }
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND) return;
        Block block = e.getClickedBlock();
        if (block == null || p.getGameMode() == GameMode.SPECTATOR) return;
        // sneak + item = place, keep vanilla
        if (p.isSneaking() && !p.getInventory().getItemInMainHand().isEmpty()) return;

        BlockState bs = block.getState();
        if (!(bs instanceof Lidded)) return;

        // opening the real chest plays the lid animation + sound for everyone, so open a copy instead
        e.setCancelled(true);
        if (bs instanceof EnderChest) {
            p.openInventory(p.getEnderChest());
            return;
        }
        if (!(bs instanceof Container container)) return;

        Inventory real = container.getInventory();
        Component title = Component.translatable(titleKey(bs, real.getSize()))
                .append(Component.text(" (lautlos)", NamedTextColor.DARK_GRAY));
        SilentView holder = new SilentView();
        Inventory copy = Bukkit.createInventory(holder, real.getSize(), title);
        holder.inv = copy;
        ItemStack[] items = real.getContents();
        for (int i = 0; i < items.length; i++) {
            copy.setItem(i, items[i] == null ? null : items[i].clone());
        }
        p.openInventory(copy);
        if (chestHintShown.add(p.getUniqueId())) {
            Text.send(plugin, p, "&7Im Vanish öffnest du Kisten lautlos, aber nur zum Anschauen. Zum Rausnehmen kurz &b/v &7aus.");
        }
    }

    private String titleKey(BlockState bs, int size) {
        if (bs instanceof Barrel) return "container.barrel";
        if (bs instanceof ShulkerBox) return "container.shulkerBox";
        if (bs instanceof Chest && size > 27) return "container.chestDouble";
        return "container.chest";
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof SilentView) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof SilentView) e.setCancelled(true);
    }

    // read only copy of a chest
    private static class SilentView implements InventoryHolder {
        private Inventory inv;

        @Override
        public Inventory getInventory() {
            return inv;
        }
    }
}
