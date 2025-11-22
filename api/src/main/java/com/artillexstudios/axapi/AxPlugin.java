package com.artillexstudios.axapi;

import com.artillexstudios.axapi.dependencies.DependencyManagerWrapper;
import com.artillexstudios.axapi.gui.AnvilListener;
import com.artillexstudios.axapi.gui.inventory.InventoryUpdater;
import com.artillexstudios.axapi.gui.inventory.listener.InventoryClickListener;
import com.artillexstudios.axapi.gui.inventory.renderer.InventoryRenderers;
import com.artillexstudios.axapi.items.component.DataComponents;
import com.artillexstudios.axapi.nms.NMSHandlers;
import com.artillexstudios.axapi.nms.wrapper.ServerPlayerWrapper;
import com.artillexstudios.axapi.placeholders.PlaceholderAPIHook;
import com.artillexstudios.axapi.scheduler.Scheduler;
import com.artillexstudios.axapi.utils.ComponentSerializer;
import com.artillexstudios.axapi.utils.featureflags.FeatureFlags;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import revxrsal.zapper.DependencyManager;
import revxrsal.zapper.classloader.URLClassLoaderWrapper;

import java.io.File;
import java.net.URLClassLoader;

public abstract class AxPlugin extends JavaPlugin {

    public AxPlugin() {
        DependencyManager manager = new DependencyManager(this.getDescription(), new File(this.getDataFolder(), "libs"), URLClassLoaderWrapper.wrap((URLClassLoader) this.getClassLoader()));
        DependencyManagerWrapper wrapper = new DependencyManagerWrapper(manager);
        wrapper.dependency("org{}apache{}commons:commons-math3:3.6.1");
        wrapper.dependency("com{}github{}ben-manes{}caffeine:caffeine:3.2.3");

        wrapper.relocate("org{}apache{}commons{}math3", "com.artillexstudios.axapi.libs.math3");
        wrapper.relocate("com{}github{}benmanes", "com.artillexstudios.axapi.libs.caffeine");
        try {
            Class.forName("net.kyori.adventure.Adventure", false, this.getClass().getClassLoader());
        } catch (ClassNotFoundException exception) {
            wrapper.dependency("net{}kyori:adventure-api:4.25.0");
        }

        this.dependencies(wrapper);
        manager.load();

        FeatureFlags.refresh(this);
        this.updateFlags();
    }

    public void updateFlags() {

    }

    @Override
    public void onEnable() {
        if (!NMSHandlers.British.initialise(this)) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        ComponentSerializer.INSTANCE.refresh();
        DataComponents.setDataComponentImpl(NMSHandlers.getNmsHandler().dataComponents());
        Scheduler.scheduler.init(this);

        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPlayerQuitEvent(@NotNull final PlayerQuitEvent event) {
                InventoryRenderers.disconnect(event.getPlayer().getUniqueId());
                ServerPlayerWrapper wrapper = ServerPlayerWrapper.wrap(event.getPlayer());
                if (FeatureFlags.ENABLE_PACKET_LISTENERS.get()) {
                    wrapper.uninject();
                }
            }

            @EventHandler
            public void onPlayerJoinEvent(@NotNull final PlayerJoinEvent event) {
                ServerPlayerWrapper wrapper = ServerPlayerWrapper.wrap(event.getPlayer());
                if (!FeatureFlags.ENABLE_PACKET_LISTENERS.get()) {
                    return;
                }

                wrapper.inject();
            }
        }, this);
        Bukkit.getPluginManager().registerEvents(new AnvilListener(), this);
        Bukkit.getPluginManager().registerEvents(new InventoryClickListener(), this);

        if (FeatureFlags.USE_INVENTORY_UPDATER.get()) {
            InventoryUpdater.INSTANCE.start(this);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            ServerPlayerWrapper wrapper = ServerPlayerWrapper.wrap(player);
            wrapper.inject();
        }

        this.enable();

        if (FeatureFlags.PLACEHOLDER_API_HOOK.get() && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderAPIHook(this).register();
        }
    }

    public void enable() {

    }

    @Override
    public void onLoad() {
        this.load();
    }

    public void dependencies(DependencyManagerWrapper manager) {

    }

    public void load() {

    }

    @Override
    public void onDisable() {
        this.disable();
        Scheduler.get().cancelAll();

        for (Player player : Bukkit.getOnlinePlayers()) {
            ServerPlayerWrapper wrapper = ServerPlayerWrapper.wrap(player);
            wrapper.uninject();
        }

        InventoryUpdater.INSTANCE.shutdown();
    }

    public void disable() {

    }

    public void reload() {

    }

    public long reloadWithTime() {
        long start = System.currentTimeMillis();
        this.reload();

        return System.currentTimeMillis() - start;
    }
}