package me.syntheticdev.signshops.events;

import me.syntheticdev.signshops.Shop;
import me.syntheticdev.signshops.SignShopsPlugin;
import me.syntheticdev.signshops.SignShopsManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerInteractListener implements Listener {
    private SignShopsManager manager;
    public PlayerInteractListener() {
        this.manager = SignShopsPlugin.getManager();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        Shop shop = manager.getShop(block);
        if (shop == null) return;

        if (shop.getSign().getLocation().equals(block.getLocation())) {
            manager.handleSignClick(event, shop);
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            manager.handleShopOpen(event, shop);
            event.setCancelled(true);
        }
    }
}
