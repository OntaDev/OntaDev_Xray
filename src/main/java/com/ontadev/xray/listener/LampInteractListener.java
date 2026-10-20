package com.ontadev.xray.listener;

import com.ontadev.libs.ioc.annotation.AutoListener;
import com.ontadev.libs.ioc.annotation.stereotype.Component;
import com.ontadev.xray.gui.LampEditorGui;
import com.ontadev.xray.gui.PlayerMaterialsGui;
import com.ontadev.xray.service.CooldownService;
import com.ontadev.xray.service.FuelService;
import com.ontadev.xray.service.HandItems;
import com.ontadev.xray.service.HighlightManager;
import com.ontadev.xray.service.LampDataService;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

@Component
@AutoListener
public class LampInteractListener implements Listener {
    private final LampDataService lampDataService;
    private final CooldownService cooldownService;
    private final FuelService fuelService;
    private final HighlightManager highlightManager;
    private final PlayerMaterialsGui playerMaterialsGui;
    private final LampEditorGui lampEditorGui;

    public LampInteractListener(
            LampDataService lampDataService,
            CooldownService cooldownService,
            FuelService fuelService,
            HighlightManager highlightManager,
            PlayerMaterialsGui playerMaterialsGui,
            LampEditorGui lampEditorGui
    ) {
        this.lampDataService = lampDataService;
        this.cooldownService = cooldownService;
        this.fuelService = fuelService;
        this.highlightManager = highlightManager;
        this.playerMaterialsGui = playerMaterialsGui;
        this.lampEditorGui = lampEditorGui;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action == Action.PHYSICAL) return;

        Player player = event.getPlayer();
        boolean rightClick = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
        boolean leftClickAir = action == Action.LEFT_CLICK_AIR;

        EquipmentSlot hand = event.getHand();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        if (hand == EquipmentSlot.OFF_HAND) {
            if (!rightClick || !lampDataService.isLamp(offHand)) return;

            event.setCancelled(true);

            if (lampDataService.isLamp(mainHand)) return;

            useLamp(player, EquipmentSlot.OFF_HAND, offHand, true, false);
            return;
        }
        if (hand != EquipmentSlot.HAND) return;

        if (player.isSneaking() && rightClick && !lampDataService.isLamp(mainHand) && tryRefuel(player, mainHand, offHand)) {
            event.setCancelled(true);
            return;
        }

        if (lampDataService.isLamp(mainHand)) {
            event.setCancelled(useLamp(player, EquipmentSlot.HAND, mainHand, rightClick, leftClickAir) || event.isCancelled());
            return;
        }

        if (leftClickAir && isEmpty(mainHand) && lampDataService.isLamp(offHand)) {
            event.setCancelled(useLamp(player, EquipmentSlot.OFF_HAND, offHand, false, true) || event.isCancelled());
        }
    }

    private boolean isEmpty(ItemStack stack) {
        return stack == null || stack.getType() == Material.AIR;
    }

    private boolean useLamp(Player player, EquipmentSlot hand, ItemStack lamp, boolean rightClick, boolean leftClickAir) {
        if (player.isSneaking() && rightClick) {
            refuelFromInventory(player, lamp, hand);
            return true;
        }

        if (player.isSneaking() && leftClickAir) {
            openLampEditor(player);
            return true;
        }

        if (!player.isSneaking() && leftClickAir) {
            openPlayerMaterials(player);
            return true;
        }

        if (!player.isSneaking() && rightClick) {
            activate(player, lamp, hand);
            return true;
        }

        return false;
    }

    private void activate(Player player, ItemStack lamp, EquipmentSlot hand) {
        if (!player.hasPermission("lamp.use")) return;

        if (highlightManager.isActive(player.getUniqueId())) {
            highlightManager.stop(player);
            showMessage(player, ChatColor.YELLOW + "Лампа выключена");
            return;
        }

        long remaining = cooldownService.remainingSeconds(lamp);
        if (remaining > 0) {
            player.sendMessage(ChatColor.RED + "Лампа перезаряжается: " + remaining + " сек.");
            return;
        }

        if (!fuelService.hasEnoughFuel(lamp)) {
            player.sendMessage(ChatColor.RED + "В лампе закончилось топливо - зарядите её (Shift+ПКМ с топливом в инвентаре).");
            return;
        }

        ItemMeta meta = lamp.getItemMeta();
        lampDataService.setLastUse(meta, System.currentTimeMillis());
        lampDataService.setActive(meta, true);
        lamp.setItemMeta(meta);
        fuelService.consumeForUse(lamp);
        HandItems.set(player, hand, lamp);

        highlightManager.start(player, hand);
        fuelService.showCharge(player, lamp);
    }

    private void showMessage(Player player, String text) {
        Player.Spigot spigot = player.spigot();
        if (spigot == null) return;

        spigot.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(text));
    }

    private void openPlayerMaterials(Player player) {
        if (!player.hasPermission("lamp.gui")) return;
        playerMaterialsGui.open(player);
    }

    private void openLampEditor(Player player) {
        if (!player.hasPermission("lamp.gui.edit")) {
            player.sendMessage(ChatColor.RED + "Недостаточно прав (lamp.gui.edit).");
            return;
        }
        lampEditorGui.open(player);
    }

    private boolean tryRefuel(Player player, ItemStack mainHand, ItemStack offHand) {
        if (lampDataService.isFuel(mainHand) && lampDataService.isLamp(offHand)) {
            doRefuel(player, offHand, mainHand, false);
            return true;
        }
        return false;
    }

    private void refuelFromInventory(Player player, ItemStack lamp, EquipmentSlot hand) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();

        boolean foundFuel = false;
        int consumedItems = 0;
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack stack = contents[slot];
            if (!lampDataService.isFuel(stack)) continue;
            foundFuel = true;

            int consumed = fuelService.refuel(lamp, stack.getAmount());
            if (consumed <= 0) break;

            consumedItems += consumed;
            int left = stack.getAmount() - consumed;
            if (left <= 0) {
                inventory.setItem(slot, null);
            } else {
                stack.setAmount(left);
                inventory.setItem(slot, stack);
            }
        }

        if (!foundFuel) {
            player.sendMessage(ChatColor.RED + "В инвентаре нет топлива для лампы.");
            return;
        }
        if (consumedItems <= 0) {
            player.sendMessage(ChatColor.YELLOW + "Лампа уже полностью заряжена.");
            return;
        }

        HandItems.set(player, hand, lamp);
        player.sendMessage(ChatColor.GREEN + "Лампа заряжена (заряд: " + fuelService.chargePercent(lampDataService.getFuel(lamp)) + "%), использовано топлива: " + consumedItems + ".");
        fuelService.showCharge(player, lamp);
    }

    private void doRefuel(Player player, ItemStack lamp, ItemStack fuelStack, boolean lampInMainHand) {
        int consumed = fuelService.refuel(lamp, fuelStack.getAmount());
        if (consumed <= 0) {
            player.sendMessage(ChatColor.YELLOW + "Лампа уже полностью заряжена.");
            return;
        }

        int remainingFuelItems = fuelStack.getAmount() - consumed;
        ItemStack newFuelStack = remainingFuelItems <= 0 ? null : fuelStack;
        if (newFuelStack != null) {
            newFuelStack.setAmount(remainingFuelItems);
        }

        if (lampInMainHand) {
            player.getInventory().setItemInMainHand(lamp);
            player.getInventory().setItemInOffHand(newFuelStack);
        } else {
            player.getInventory().setItemInOffHand(lamp);
            player.getInventory().setItemInMainHand(newFuelStack);
        }

        player.sendMessage(ChatColor.GREEN + "Лампа заряжена (заряд: " + fuelService.chargePercent(lampDataService.getFuel(lamp)) + "%).");
        fuelService.showCharge(player, lamp);
    }
}
