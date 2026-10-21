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
import com.ontadev.xray.service.MaterialManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Menu
public class GlobalMaterialsGui extends AbstractMenu {
    private static final int PAGE_SIZE = 45;
    private static final int PREV_SLOT = 45;
    private static final int INFO_SLOT = 47;
    private static final int ADD_HAND_SLOT = 49;
    private static final int NEXT_SLOT = 53;

    private static final int EDITED_SLOT = 4;
    private static final int[] PALETTE_SLOTS = {10, 11, 12, 13, 14, 15, 16, 17, 19, 20, 21, 22, 23, 24, 25, 26};
    private static final int BACK_SLOT = 45;

    private final MaterialManager materialManager;
    private final MenuManager menuManager;
    private final PlayerResolver playerResolver;

    private final Map<UUID, Integer> pageByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> pickingColorByPlayer = new ConcurrentHashMap<>();

    public GlobalMaterialsGui(MaterialManager materialManager, MenuManager menuManager, PlayerResolver playerResolver) {
        this.materialManager = materialManager;
        this.menuManager = menuManager;
        this.playerResolver = playerResolver;
    }

    @Override
    public Message title(PlayerSnapshot snapshot) {
        return new Message("<dark_gray>Материалы для подсветки");
    }

    @Override
    public int size() {
        return 54;
    }

    @Override
    public Map<Integer, ItemModel> dynamicItems(PlayerSnapshot snapshot) {
        Player player = Bukkit.getPlayer(snapshot.uuid());

        Optional<MaterialEntry> picking = pickingColorByPlayer.containsKey(snapshot.uuid())
                ? materialManager.get(pickingColorByPlayer.get(snapshot.uuid()))
                : Optional.empty();
        if (picking.isPresent()) {
            return withFiller(colorPickerItems(picking.get()));
        }
        pickingColorByPlayer.remove(snapshot.uuid());

        Map<Integer, ItemModel> items = new HashMap<>();
        List<MaterialEntry> entries = materialManager.getAll();

        int pages = Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.min(pageByPlayer.getOrDefault(snapshot.uuid(), 0), pages - 1);
        pageByPlayer.put(snapshot.uuid(), page);

        int from = page * PAGE_SIZE;
        for (int i = from; i < Math.min(entries.size(), from + PAGE_SIZE); i++) {
            items.put(i - from, materialItem(entries.get(i)));
        }

        if (page > 0) items.put(PREV_SLOT, pageButton("<yellow>◀ Предыдущая страница", -1));
        if (page < pages - 1) items.put(NEXT_SLOT, pageButton("<yellow>Следующая страница ▶", 1));
        items.put(INFO_SLOT, infoItem(page + 1, pages, entries.size()));
        items.put(ADD_HAND_SLOT, addButton(player));

        return withFiller(items);
    }

    private ItemModel infoItem(int page, int pages, int total) {
        return ItemModel.builder()
                .material(Material.BOOK)
                .name(new Message("<gold>Как добавить блок"))
                .lore(new Message(
                        "<gray>Страница <white>" + page + "/" + pages + "<gray>, материалов: <white>" + total,
                        "",
                        "<green>Shift+клик</green> <gray>по блоку в своём инвентаре",
                        "<gray>либо возьмите блок курсором и",
                        "<gray>кликните по этому меню.",
                        "",
                        "<green>ЛКМ</green> <gray>по материалу - выбрать цвет",
                        "<red>ПКМ</red> <gray>по материалу - удалить"
                ))
                .build();
    }

    private ItemModel pageButton(String name, int delta) {
        ItemModel button = ItemModel.builder()
                .material(Material.ARROW)
                .name(new Message(name))
                .build();

        button.onAnyInteraction((player, event) -> {
            pageByPlayer.merge(player.getUniqueId(), delta, Integer::sum);
            if (pageByPlayer.get(player.getUniqueId()) < 0) pageByPlayer.put(player.getUniqueId(), 0);
            refresh(player);
        });

        return button;
    }

    private ItemModel materialItem(MaterialEntry entry) {
        ItemModel item = ItemModel.builder()
                .material(entry.getMaterial())
                .name(new Message("<white>" + entry.getMaterial().name()))
                .lore(new Message(
                        "<gray>ID: <yellow>" + entry.getId(),
                        "<gray>Цвет: <" + ColorPalette.tag(entry.getColor()) + ">" + entry.getColor().name(),
                        "",
                        "<green>ЛКМ</green> <gray>- выбрать цвет",
                        "<green>Shift+ЛКМ</green> <gray>- следующий цвет",
                        "<red>ПКМ</red> <gray>- удалить"
                ))
                .build();

        item.onInteraction((player, event) -> {
            if (!requireEditPermission(player)) return;
            pickingColorByPlayer.put(player.getUniqueId(), entry.getId());
            refresh(player);
        }, InteractionType.LEFT_CLICK);

        item.onInteraction((player, event) -> {
            if (!requireEditPermission(player)) return;
            materialManager.updateColor(entry.getId(), nextColor(entry.getColor()));
            refresh(player);
        }, InteractionType.SHIFT_LEFT_CLICK);

        item.onInteraction((player, event) -> {
            if (!requireEditPermission(player)) return;
            materialManager.remove(entry.getId());
            refresh(player);
        }, InteractionType.RIGHT_CLICK);

        return item;
    }

    private Map<Integer, ItemModel> colorPickerItems(MaterialEntry entry) {
        Map<Integer, ItemModel> items = new HashMap<>();

        items.put(EDITED_SLOT, ItemModel.builder()
                .material(entry.getMaterial())
                .name(new Message("<white>" + entry.getMaterial().name()))
                .lore(new Message("<gray>Цвет сейчас: <" + ColorPalette.tag(entry.getColor()) + ">" + entry.getColor().name()))
                .build());

        List<ChatColor> palette = ColorPalette.colors();
        for (int i = 0; i < palette.size() && i < PALETTE_SLOTS.length; i++) {
            ChatColor color = palette.get(i);
            boolean selected = color == entry.getColor();

            ItemModel item = ItemModel.builder()
                    .material(ColorPalette.icon(color))
                    .name(new Message("<" + ColorPalette.tag(color) + ">" + (selected ? "▶ " : "") + color.name()))
                    .lore(new Message(selected ? "<gray>Выбран сейчас" : "<gray>Клик - выбрать этот цвет"))
                    .build();
            item.onAnyInteraction((player, event) -> {
                if (!requireEditPermission(player)) return;
                materialManager.updateColor(entry.getId(), color);
                pickingColorByPlayer.remove(player.getUniqueId());
                refresh(player);
            });
            items.put(PALETTE_SLOTS[i], item);
        }

        ItemModel back = ItemModel.builder()
                .material(Material.ARROW)
                .name(new Message("<gray>Назад к списку"))
                .build();
        back.onAnyInteraction((player, event) -> {
            pickingColorByPlayer.remove(player.getUniqueId());
            refresh(player);
        });
        items.put(BACK_SLOT, back);

        return items;
    }

    private ItemModel addButton(Player player) {
        Material held = heldMaterial(player);

        ItemModel button = ItemModel.builder()
                .material(held == null ? Material.BARRIER : held)
                .name(new Message("<green>Добавить материал в руке"))
                .lore(new Message(
                        "<gray>Сейчас в руке: <yellow>" + (held == null ? "пусто" : held.name()),
                        "<gray>Клик - добавить его в список"
                ))
                .build();

        button.onAnyInteraction((clicker, event) -> {
            Material handMaterial = heldMaterial(clicker);
            if (handMaterial == null) {
                clicker.sendMessage(ChatColor.RED + "Возьмите в руку материал, который хотите добавить.");
                return;
            }
            tryAdd(clicker, handMaterial);
        });

        return button;
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

    @Override
    public boolean onClick(PlayerSnapshot snapshot, InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int rawSlot = event.getRawSlot();
        boolean inMenu = rawSlot >= 0 && rawSlot < size();

        if (event.getClick() == ClickType.DOUBLE_CLICK || event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return true;
        }

        if (!inMenu) {
            if (event.isShiftClick()) {
                event.setCancelled(true);
                ItemStack clicked = event.getCurrentItem();
                if (clicked != null && clicked.getType() != Material.AIR) tryAdd(player, clicked.getType());
            }
            return true;
        }

        ItemStack cursor = event.getCursor();
        if (cursor != null && cursor.getType() != Material.AIR) {
            event.setCancelled(true);
            tryAdd(player, cursor.getType());
            return true;
        }

        return false;
    }

    @Override
    public boolean onDrag(PlayerSnapshot snapshot, InventoryDragEvent event) {
        boolean touchesMenu = event.getRawSlots().stream().anyMatch(slot -> slot >= 0 && slot < size());
        if (!touchesMenu) return true;

        event.setCancelled(true);
        ItemStack dragged = event.getOldCursor();
        if (dragged.getType() != Material.AIR && event.getWhoClicked() instanceof Player) {
            tryAdd((Player) event.getWhoClicked(), dragged.getType());
        }
        return true;
    }

    private void tryAdd(Player player, Material material) {
        if (!requireEditPermission(player)) return;

        if (!material.isBlock() || material.isAir()) {
            player.sendMessage(ChatColor.RED + material.name() + " - не блок, подсвечивать его нельзя.");
            return;
        }

        if (materialManager.addIfAbsent(material, ChatColor.WHITE).isPresent()) {
            player.sendMessage(ChatColor.GREEN + "Добавлено: " + material.name() + ChatColor.GRAY + " (цвет можно сменить ЛКМ по материалу)");
            refresh(player);
        } else {
            player.sendMessage(ChatColor.YELLOW + material.name() + " уже есть в списке.");
        }
    }

    @Override
    public void onOpen(PlayerSnapshot snapshot, Player player, Inventory inventory) {
        pageByPlayer.remove(snapshot.uuid());
        pickingColorByPlayer.remove(snapshot.uuid());
    }

    @Override
    public void onClose(PlayerSnapshot snapshot, Player player, Inventory inventory) {
        pageByPlayer.remove(snapshot.uuid());
        pickingColorByPlayer.remove(snapshot.uuid());
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean requireEditPermission(Player player) {
        if (player.hasPermission("lamp.materials.edit")) return true;
        player.sendMessage(ChatColor.RED + "У вас нет прав изменять список материалов (lamp.materials.edit).");
        return false;
    }

    private Material heldMaterial(Player player) {
        if (player == null) return null;
        ItemStack item = player.getInventory().getItemInMainHand();
        return item.getType() == Material.AIR ? null : item.getType();
    }

    private ChatColor nextColor(ChatColor current) {
        List<ChatColor> palette = ColorPalette.colors();
        int index = palette.indexOf(current);
        return palette.get((index + 1) % palette.size());
    }

    private void refresh(Player player) {
        menuManager.refresh(playerResolver.getPlayerSnapshot(player));
    }
}
