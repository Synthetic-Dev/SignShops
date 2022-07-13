package me.syntheticdev.signshops.events;

import me.syntheticdev.signshops.SignShopsManager;
import me.syntheticdev.signshops.SignShopsPlugin;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.InventoryHolder;

public class InventoryListener implements Listener {
    private SignShopsManager manager;
    public InventoryListener() {
        this.manager = SignShopsPlugin.getManager();
    }

    @EventHandler
    public void onHopperTake(InventoryMoveItemEvent event) {
        Block source = event.getSource().getLocation().getBlock();
        if (!manager.isShop(source)) return;

        InventoryHolder destination = event.getDestination().getHolder();
        if (destination instanceof Hopper
                || destination instanceof HopperMinecart) {
            event.setCancelled(true);
        }
    }
}
