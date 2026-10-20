package com.ontadev.xray.service;

import com.ontadev.libs.ioc.annotation.stereotype.Service;
import com.ontadev.xray.config.XrayConfig;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

@Service
public class FuelService {
    private static final String CHARGE_LORE_PREFIX = ChatColor.GRAY + "Заряд: ";
    private static final int BAR_LENGTH = 10;

    private final XrayConfig config;
    private final LampDataService lampDataService;

    public FuelService(XrayConfig config, LampDataService lampDataService) {
        this.config = config;
        this.lampDataService = lampDataService;
    }

    public boolean hasEnoughFuel(ItemStack lamp) {
        return lampDataService.getFuel(lamp) >= config.getFuel().getFuelCostPerUse();
    }

    public void consumeForUse(ItemStack lamp) {
        ItemMeta meta = lamp.getItemMeta();
        int current = lampDataService.getFuel(lamp);
        int cost = config.getFuel().getFuelCostPerUse();
        int remaining = Math.max(0, current - cost);
        lampDataService.setFuel(meta, remaining);
        applyChargeLore(meta, remaining);
        lamp.setItemMeta(meta);
    }

    public int drain(ItemStack lamp, int amount) {
        ItemMeta meta = lamp.getItemMeta();
        int remaining = Math.max(0, lampDataService.getFuel(lamp) - Math.max(0, amount));
        lampDataService.setFuel(meta, remaining);
        applyChargeLore(meta, remaining);
        lamp.setItemMeta(meta);
        return remaining;
    }

    public void showCharge(Player player, ItemStack lamp) {
        Player.Spigot spigot = player.spigot();
        if (spigot == null) return;

        spigot.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(chargeBar(lampDataService.getFuel(lamp))));
    }

    public int chargePercent(int fuel) {
        int max = config.getFuel().getMaxFuel();
        if (max <= 0) return 0;
        return Math.max(0, Math.min(100, Math.round(fuel * 100f / max)));
    }

    public String chargeBar(int fuel) {
        int percent = chargePercent(fuel);
        int filled = Math.round(percent / 100f * BAR_LENGTH);
        return ChatColor.GRAY + "Заряд лампы: " + chargeColor(percent) + percent + "% "
                + chargeColor(percent) + "█".repeat(filled) + ChatColor.DARK_GRAY + "█".repeat(BAR_LENGTH - filled);
    }

    public void applyChargeLore(ItemMeta meta, int fuel) {
        List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.removeIf(line -> line.startsWith(CHARGE_LORE_PREFIX));

        int percent = chargePercent(fuel);
        lore.add(CHARGE_LORE_PREFIX + chargeColor(percent) + percent + "%");
        meta.setLore(lore);
    }

    private ChatColor chargeColor(int percent) {
        if (percent > 50) return ChatColor.GREEN;
        if (percent > 20) return ChatColor.YELLOW;
        return ChatColor.RED;
    }

    public int refuel(ItemStack lamp, int fuelItemsAvailable) {
        int perItem = config.getFuel().getFuelPerItem();
        int max = config.getFuel().getMaxFuel();
        int current = lampDataService.getFuel(lamp);

        if (fuelItemsAvailable <= 0 || perItem <= 0 || current >= max) return 0;

        int roomLeft = max - current;
        int itemsNeeded = (roomLeft + perItem - 1) / perItem;
        int itemsToConsume = Math.min(itemsNeeded, fuelItemsAvailable);
        int added = Math.min(roomLeft, itemsToConsume * perItem);

        ItemMeta meta = lamp.getItemMeta();
        lampDataService.setFuel(meta, current + added);
        applyChargeLore(meta, current + added);
        lamp.setItemMeta(meta);

        return itemsToConsume;
    }
}
