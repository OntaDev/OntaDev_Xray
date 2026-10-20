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
import com.ontadev.xray.service.ColorPalette;
import com.ontadev.xray.service.HandItems;
import com.ontadev.xray.service.LampDataService;
import com.ontadev.xray.service.MaterialManager;
import com.ontadev.xray.service.PlayerLampSettingsService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Menu
public class PlayerMaterialsGui extends AbstractMenu {
    private static final int[] PALETTE_SLOTS = {1, 2, 3, 4, 5, 6, 7, 8, 10, 11, 12, 13, 14, 15, 16, 17};
    private static final int EDITED_MATERIAL_SLOT = 0;
    private static final int RESET_SLOT = 22;
    private static final int BACK_SLOT = 18;

    private final LampDataService lampDataService;
    private final MaterialManager materialManager;
    private final PlayerLampSettingsService playerLampSettingsService;
    private final MenuManager menuManager;
    private final PlayerResolver playerResolver;

    public PlayerMaterialsGui(
            LampDataService lampDataService,
            MaterialManager materialManager,
            PlayerLampSettingsService playerLampSettingsService,
            MenuManager menuManager,
            PlayerResolver playerResolver
    ) {
        this.lampDataService = lampDataService;
        this.materialManager = materialManager;
        this.playerLampSettingsService = playerLampSettingsService;
        this.menuManager = menuManager;
        this.playerResolver = playerResolver;
    }

    @Override
    public Message title(PlayerSnapshot snapshot) {
        return new Message("<dark_gray>Моя подсветка");
    }

    @Override
    public int size() {
        return 27;
    }

    @Override
    public Map<Integer, ItemModel> dynamicItems(PlayerSnapshot snapshot) {
        Map<Integer, ItemModel> items = new HashMap<>();

        Player player = Bukkit.getPlayer(snapshot.uuid());
        if (player == null) return items;

        ItemStack lamp = HandItems.heldLamp(lampDataService, player);
        if (lamp == null) {
            items.put(13, ItemModel.builder()
                    .material(Material.BARRIER)
                    .name(new Message("<red>Держите лампу в руке"))
                    .build());
            return items;
        }

        Optional<Integer> picking = playerLampSettingsService.pickingColorFor(player.getUniqueId());
        if (picking.isPresent()) {
            Optional<MaterialEntry> edited = materialManager.get(picking.get());
            if (edited.isPresent()) {
                return withFiller(colorPickerItems(player.getUniqueId(), edited.get()));
            }
            playerLampSettingsService.stopPickingColor(player.getUniqueId());
        }

        List<Integer> lampMaterialIds = materialManager.filterExisting(lampDataService.getMaterialIds(lamp));

        int slot = 0;
        for (Integer id : lampMaterialIds) {
            if (slot >= size()) break;

            Map.Entry<Integer, MaterialEntry> resolved = resolve(id);
            if (resolved == null) continue;

            items.put(slot, toggleItem(player.getUniqueId(), resolved.getValue()));
            slot++;
        }

        return withFiller(items);
    }

    // движок меню не очищает старые слоты: подложка перезаписывает предметы прошлого режима
    private Map<Integer, ItemModel> withFiller(Map<Integer, ItemModel> items) {
        for (int slot = 0; slot < size(); slot++) {
            items.computeIfAbsent(slot, key -> ItemModel.builder()
                    .material(Material.GRAY_STAINED_GLASS_PANE)
                    .name(new Message(" "))
                    .build());
        }
        return items;
    }

    private Map.Entry<Integer, MaterialEntry> resolve(int id) {
        return materialManager.get(id)
                .map(entry -> Map.entry(id, entry))
                .orElse(null);
    }

    private ItemModel toggleItem(UUID viewerId, MaterialEntry entry) {
        boolean enabled = playerLampSettingsService.isEnabled(viewerId, entry.getId());
        ChatColor color = playerLampSettingsService.colorFor(viewerId, entry.getId(), entry.getColor());
        boolean custom = playerLampSettingsService.customColor(viewerId, entry.getId()).isPresent();

        ItemModel item = ItemModel.builder()
                .material(entry.getMaterial())
                .name(new Message((enabled ? "<green>[✓] " : "<red>[✗] ") + entry.getMaterial().name()))
                .lore(new Message(
                        "<gray>Цвет: <" + ColorPalette.tag(color) + ">" + color.name() + (custom ? " <dark_gray>(свой)" : ""),
                        "",
                        "<green>ЛКМ</green> <gray>- " + (enabled ? "скрыть у себя" : "показывать у себя"),
                        "<yellow>ПКМ</yellow> <gray>- выбрать цвет"
                ))
                .build();

        item.onInteraction((player, event) -> {
            playerLampSettingsService.toggle(player.getUniqueId(), entry.getId());
            refresh(player);
        }, InteractionType.LEFT_CLICK);

        item.onInteraction((player, event) -> {
            playerLampSettingsService.startPickingColor(player.getUniqueId(), entry.getId());
            refresh(player);
        }, InteractionType.RIGHT_CLICK);

        return item;
    }

    private Map<Integer, ItemModel> colorPickerItems(UUID viewerId, MaterialEntry entry) {
        Map<Integer, ItemModel> items = new HashMap<>();

        ChatColor current = playerLampSettingsService.colorFor(viewerId, entry.getId(), entry.getColor());
        items.put(EDITED_MATERIAL_SLOT, ItemModel.builder()
                .material(entry.getMaterial())
                .name(new Message("<white>" + entry.getMaterial().name()))
                .lore(new Message("<gray>Сейчас: <" + ColorPalette.tag(current) + ">" + current.name()))
                .build());

        List<ChatColor> palette = ColorPalette.colors();
        for (int i = 0; i < palette.size() && i < PALETTE_SLOTS.length; i++) {
            items.put(PALETTE_SLOTS[i], colorItem(entry, palette.get(i), palette.get(i) == current));
        }

        ItemModel reset = ItemModel.builder()
                .material(Material.BARRIER)
                .name(new Message("<red>Вернуть цвет по умолчанию"))
                .lore(new Message("<gray>Общий цвет: <" + ColorPalette.tag(entry.getColor()) + ">" + entry.getColor().name()))
                .build();
        reset.onAnyInteraction((player, event) -> {
            playerLampSettingsService.resetColor(player.getUniqueId(), entry.getId());
            playerLampSettingsService.stopPickingColor(player.getUniqueId());
            refresh(player);
        });
        items.put(RESET_SLOT, reset);

        ItemModel back = ItemModel.builder()
                .material(Material.ARROW)
                .name(new Message("<gray>Назад"))
                .build();
        back.onAnyInteraction((player, event) -> {
            playerLampSettingsService.stopPickingColor(player.getUniqueId());
            refresh(player);
        });
        items.put(BACK_SLOT, back);

        return items;
    }

    private ItemModel colorItem(MaterialEntry entry, ChatColor color, boolean selected) {
        ItemModel item = ItemModel.builder()
                .material(ColorPalette.icon(color))
                .name(new Message("<" + ColorPalette.tag(color) + ">" + (selected ? "▶ " : "") + color.name()))
                .lore(new Message(selected ? "<gray>Выбран сейчас" : "<gray>Клик - покрасить в этот цвет"))
                .build();

        item.onAnyInteraction((player, event) -> {
            playerLampSettingsService.setColor(player.getUniqueId(), entry.getId(), color);
            playerLampSettingsService.stopPickingColor(player.getUniqueId());
            refresh(player);
        });

        return item;
    }

    @Override
    public void onOpen(PlayerSnapshot snapshot, Player player, Inventory inventory) {
        playerLampSettingsService.stopPickingColor(snapshot.uuid());
    }

    @Override
    public void onClose(PlayerSnapshot snapshot, Player player, Inventory inventory) {
        playerLampSettingsService.stopPickingColor(snapshot.uuid());
    }

    private void refresh(Player player) {
        menuManager.refresh(playerResolver.getPlayerSnapshot(player));
    }
}
