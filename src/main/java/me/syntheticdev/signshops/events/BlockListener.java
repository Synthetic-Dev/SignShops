package me.syntheticdev.signshops.events;

import me.syntheticdev.signshops.Shop;
import me.syntheticdev.signshops.SignShopsManager;
import me.syntheticdev.signshops.SignShopsPlugin;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Logger;

public class BlockListener implements Listener {
    private SignShopsManager manager;
    public BlockListener() {
        this.manager = SignShopsPlugin.getManager();
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Shop shop = manager.getShop(block);
        if (shop == null) return;

        try {
            boolean shouldBreak = manager.destroyShop(event, shop);
            event.setCancelled(!shouldBreak);
        } catch (IOException err) {
            err.printStackTrace();
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        Iterator<Block> blocks = event.blockList().iterator();

        while (blocks.hasNext()) {
            Block block = blocks.next();

            if (manager.isShop(block)) {
                blocks.remove();
            }
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        Iterator<Block> blocks = event.blockList().iterator();

        while (blocks.hasNext()) {
            Block block = blocks.next();

            if (manager.isShop(block)) {
                blocks.remove();
            }
        }
    }
}
