package com.ontadev.xray.service;

import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public final class HandItems {
    private HandItems() {
    }

    public static ItemStack get(Player player, EquipmentSlot hand) {
        return hand == EquipmentSlot.OFF_HAND
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItemInMainHand();
    }

    public static void set(Player player, EquipmentSlot hand, ItemStack item) {
        if (hand == EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(item);
        } else {
            player.getInventory().setItemInMainHand(item);
        }
    }

    public static Optional<EquipmentSlot> lampHand(LampDataService lampDataService, Player player) {
        if (lampDataService.isLamp(player.getInventory().getItemInMainHand())) return Optional.of(EquipmentSlot.HAND);
        if (lampDataService.isLamp(player.getInventory().getItemInOffHand())) return Optional.of(EquipmentSlot.OFF_HAND);
        return Optional.empty();
    }

    public static ItemStack heldLamp(LampDataService lampDataService, Player player) {
        return lampHand(lampDataService, player).map(hand -> get(player, hand)).orElse(null);
    }

    public static void storeLamp(LampDataService lampDataService, Player player, ItemStack lamp) {
        set(player, lampHand(lampDataService, player).orElse(EquipmentSlot.HAND), lamp);
    }
}
