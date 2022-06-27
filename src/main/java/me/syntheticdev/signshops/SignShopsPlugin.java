package me.syntheticdev.signshops;

import me.syntheticdev.signshops.events.BlockListener;
import me.syntheticdev.signshops.events.PlayerInteractListener;
import me.syntheticdev.signshops.events.SignChangeListener;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class SignShopsPlugin extends JavaPlugin {
    private static SignShopsPlugin plugin;
    private static SignShopsManager manager;

    @Override
    public void onEnable() {
        ConfigurationSerialization.registerClass(Shop.class);

        plugin = this;
        manager = new SignShopsManager();
        manager.load();

        this.registerEvents();
    }

    public static SignShopsPlugin getPlugin() {
        return plugin;
    }

    public static SignShopsManager getManager() {
        return manager;
    }

    private void registerEvents() {
        PluginManager manager = Bukkit.getPluginManager();

        manager.registerEvents(new PlayerInteractListener(), this);
        manager.registerEvents(new SignChangeListener(), this);
        manager.registerEvents(new BlockListener(), this);
    }
}
