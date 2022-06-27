package me.syntheticdev.signshops.events;

import me.syntheticdev.signshops.Shop;
import me.syntheticdev.signshops.SignShopsManager;
import me.syntheticdev.signshops.SignShopsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class SignChangeListener implements Listener {
    private SignShopsManager manager;
    public SignChangeListener() {
        this.manager = SignShopsPlugin.getManager();
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        if (!manager.isShopSignFormat(event.getLines())) {
            if (event.getLine(0).trim().equalsIgnoreCase("[Shop]")) {
                player.sendMessage(ChatColor.RED + "[Shop] Incorrect shop sign format.");
            }
            return;
        };

        manager.createShop(event);
    }
}
