package com.ontadev.xray.listener;

import com.ontadev.libs.ioc.annotation.AutoListener;
import com.ontadev.libs.ioc.annotation.stereotype.Component;
import com.ontadev.xray.service.HighlightManager;
import com.ontadev.xray.service.PlayerLampSettingsService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

@Component
@AutoListener
public class PlayerSessionCleanupListener implements Listener {
    private final HighlightManager highlightManager;
    private final PlayerLampSettingsService playerLampSettingsService;

    public PlayerSessionCleanupListener(HighlightManager highlightManager, PlayerLampSettingsService playerLampSettingsService) {
        this.highlightManager = highlightManager;
        this.playerLampSettingsService = playerLampSettingsService;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        highlightManager.clearFor(event.getPlayer());
        playerLampSettingsService.clear(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        highlightManager.deactivateLampsOf(event.getPlayer());
    }

    @EventHandler
    public void onHeldSlotChange(PlayerItemHeldEvent event) {
        if (event.getNewSlot() != event.getPreviousSlot()) highlightManager.onMainHandSlotChanged(event.getPlayer());
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        highlightManager.stopIfActive(event.getPlayer());
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack dropped = event.getItemDrop().getItemStack();
        highlightManager.onItemDropped(event.getPlayer(), dropped);
        event.getItemDrop().setItemStack(dropped);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        for (ItemStack drop : event.getDrops()) {
            highlightManager.clearActiveFlag(drop);
        }
        highlightManager.stopIfActive(event.getEntity());
    }
}
