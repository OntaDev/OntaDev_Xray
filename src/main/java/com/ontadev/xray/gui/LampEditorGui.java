package com.ontadev.xray.gui;

import com.ontadev.libs.item.ItemModel;
import com.ontadev.libs.ioc.annotation.menu.Menu;
import com.ontadev.libs.menu.AbstractMenu;
import com.ontadev.libs.menu.enums.InteractionType;
import com.ontadev.libs.menu.manager.MenuManager;
import com.ontadev.libs.message.Message;
import com.ontadev.libs.player.PlayerResolver;
import com.ontadev.libs.player.PlayerSnapshot;
import com.ontadev.xray.domain.MaterialEntry;
import com.ontadev.xray.service.HandItems;
import com.ontadev.xray.service.LampDataService;
import com.ontadev.xray.service.MaterialManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Menu
public class LampEditorGui extends AbstractMenu {
    private static final int RADIUS_SLOT = 10;
    private static final int COOLDOWN_SLOT = 12;
    private static final int MAX_BLOCKS_SLOT = 14;
    private static final int HIGHLIGHT_ALL_SLOT = 16;
    private static final int MATERIALS_LIST_SLOT = 22;
    private static final int MATERIALS_START_SLOT = 27;

    private final LampDataService lampDataService;
    private final MaterialManager materialManager;
    private final MenuManager menuManager;
    private final PlayerResolver playerResolver;
    private final GlobalMaterialsGui globalMaterialsGui;

    public LampEditorGui(
            LampDataService lampDataService,
            MaterialManager materialManager,
            MenuManager menuManager,
            PlayerResolver playerResolver,
            GlobalMaterialsGui globalMaterialsGui
    ) {
        this.lampDataService = lampDataService;
        this.materialManager = materialManager;
        this.menuManager = menuManager;
        this.playerResolver = playerResolver;
        this.globalMaterialsGui = globalMaterialsGui;
    }

    @Override
    public Message title(PlayerSnapshot snapshot) {
        return new Message("<dark_gray>Редактор лампы");
    }

    @Override
    public int size() {
        return 54;
    }

    @Override
    public Map<Integer, ItemModel> dynamicItems(PlayerSnapshot snapshot) {
        Player player = Bukkit.getPlayer(snapshot.uuid());
        ItemStack lamp = player == null ? null : HandItems.heldLamp(lampDataService, player);

        Map<Integer, ItemModel> items = new HashMap<>();
        if (player == null || !lampDataService.isLamp(lamp)) {
            items.put(22, errorItem());
            return items;
        }

        items.put(RADIUS_SLOT, numberItem(Material.ENDER_EYE, NumericField.RADIUS, lampDataService.getRadius(lamp)));
        items.put(COOLDOWN_SLOT, numberItem(Material.CLOCK, NumericField.COOLDOWN, lampDataService.getCooldownSeconds(lamp)));
        items.put(MAX_BLOCKS_SLOT, numberItem(Material.CHEST, NumericField.MAX_BLOCKS, lampDataService.getMaxHighlightedBlocks(lamp)));
        items.put(HIGHLIGHT_ALL_SLOT, highlightAllToggle(lamp));
        items.put(MATERIALS_LIST_SLOT, materialsListButton());

        int slot = MATERIALS_START_SLOT;
        for (MaterialEntry entry : materialManager.getAll()) {
            if (slot >= size()) break;
            items.put(slot, materialToggleItem(lamp, entry));
            slot++;
        }

        return items;
    }

    private ItemModel materialsListButton() {
        ItemModel item = ItemModel.builder()
                .material(Material.BOOK)
                .name(new Message("<gold>Список возможных материалов"))
                .lore(new Message(
                        "<gray>Общий список, из которого выбираются",
                        "<gray>материалы для ламп: добавить, удалить, цвет.",
                        "",
                        "<green>Клик</green> <gray>- открыть и редактировать"
                ))
                .build();

        item.onAnyInteraction((player, event) -> {
            if (!player.hasPermission("lamp.materials")) {
                player.sendMessage(ChatColor.RED + "Недостаточно прав (lamp.materials).");
                return;
            }
            globalMaterialsGui.open(player);
        });

        return item;
    }

    private ItemModel errorItem() {
        return ItemModel.builder()
                .material(Material.BARRIER)
                .name(new Message("<red>Держите лампу в руке"))
                .build();
    }

    private enum NumericField {
        RADIUS("Радиус", 1),
        COOLDOWN("Перезарядка (сек)", 0),
        MAX_BLOCKS("Макс. блоков подсветки", 1);

        private final String label;
        private final long min;

        NumericField(String label, long min) {
            this.label = label;
            this.min = min;
        }
    }

    private ItemModel numberItem(Material material, NumericField field, long currentValue) {
        ItemModel item = ItemModel.builder()
                .material(material)
                .name(new Message("<yellow>" + field.label))
                .lore(new Message(
                        "<gray>Значение: <white>" + currentValue,
                        "",
                        "<green>ЛКМ</green> <gray>+1  <red>ПКМ</red> <gray>-1",
                        "<green>Shift+ЛКМ</green> <gray>+5  <red>Shift+ПКМ</red> <gray>-5"
                ))
                .build();

        item.onInteraction((player, event) -> applyDelta(player, field, 1), InteractionType.LEFT_CLICK);
        item.onInteraction((player, event) -> applyDelta(player, field, -1), InteractionType.RIGHT_CLICK);
        item.onInteraction((player, event) -> applyDelta(player, field, 5), InteractionType.SHIFT_LEFT_CLICK);
        item.onInteraction((player, event) -> applyDelta(player, field, -5), InteractionType.SHIFT_RIGHT_CLICK);

        return item;
    }

    private void applyDelta(Player player, NumericField field, long delta) {
        if (!requireEditPermission(player)) return;

        ItemStack lamp = currentLampOrNull(player);
        if (lamp == null) return;

        ItemMeta meta = lamp.getItemMeta();
        long value;
        switch (field) {
            case RADIUS:
                value = Math.max(field.min, lampDataService.getRadius(lamp) + delta);
                lampDataService.setRadius(meta, (int) value);
                break;
            case COOLDOWN:
                value = Math.max(field.min, lampDataService.getCooldownSeconds(lamp) + delta);
                lampDataService.setCooldownSeconds(meta, value);
                break;
            case MAX_BLOCKS:
                value = Math.max(field.min, lampDataService.getMaxHighlightedBlocks(lamp) + delta);
                lampDataService.setMaxHighlightedBlocks(meta, (int) value);
                break;
        }

        lamp.setItemMeta(meta);
        HandItems.storeLamp(lampDataService, player, lamp);
        reopen(player);
    }

    private ItemModel highlightAllToggle(ItemStack lamp) {
        boolean forAll = lampDataService.isHighlightForAllPlayers(lamp);

        ItemModel item = ItemModel.builder()
                .material(forAll ? Material.LIME_DYE : Material.GRAY_DYE)
                .name(new Message("<yellow>Подсветка для всех игроков рядом"))
                .lore(new Message(
                        "<gray>Сейчас: " + (forAll ? "<green>включена" : "<red>только для вас"),
                        "",
                        "<green>Клик</green> <gray>- переключить"
                ))
                .build();

        item.onAnyInteraction((player, event) -> {
            if (!requireEditPermission(player)) return;

            ItemStack currentLamp = currentLampOrNull(player);
            if (currentLamp == null) return;

            ItemMeta meta = currentLamp.getItemMeta();
            lampDataService.setHighlightForAllPlayers(meta, !lampDataService.isHighlightForAllPlayers(currentLamp));
            currentLamp.setItemMeta(meta);
            HandItems.storeLamp(lampDataService, player, currentLamp);
            reopen(player);
        });

        return item;
    }

    private ItemModel materialToggleItem(ItemStack lamp, MaterialEntry entry) {
        boolean enabled = lampDataService.getMaterialIds(lamp).contains(entry.getId());

        ItemModel item = ItemModel.builder()
                .material(entry.getMaterial())
                .name(new Message((enabled ? "<green>[✓] " : "<red>[✗] ") + entry.getMaterial().name()))
                .lore(new Message("<gray>Клик - " + (enabled ? "убрать из лампы" : "добавить в лампу")))
                .build();

        item.onAnyInteraction((player, event) -> {
            if (!requireEditPermission(player)) return;

            ItemStack currentLamp = currentLampOrNull(player);
            if (currentLamp == null) return;

            List<Integer> ids = new ArrayList<>(lampDataService.getMaterialIds(currentLamp));
            if (!ids.remove((Integer) entry.getId())) {
                ids.add(entry.getId());
            }

            ItemStack updatedLamp = lampDataService.setMaterialIds(currentLamp, ids);
            HandItems.storeLamp(lampDataService, player, updatedLamp);
            reopen(player);
        });

        return item;
    }

    private boolean requireEditPermission(Player player) {
        if (player.hasPermission("lamp.lamp.edit")) return true;
        player.sendMessage(ChatColor.RED + "У вас нет прав изменять параметры лампы (lamp.lamp.edit).");
        return false;
    }

    private ItemStack currentLampOrNull(Player player) {
        ItemStack lamp = HandItems.heldLamp(lampDataService, player);
        if (lamp == null) {
            player.sendMessage(ChatColor.RED + "Держите лампу в руке.");
            return null;
        }
        return lamp;
    }

    private void reopen(Player player) {
        menuManager.refresh(playerResolver.getPlayerSnapshot(player));
    }
}
