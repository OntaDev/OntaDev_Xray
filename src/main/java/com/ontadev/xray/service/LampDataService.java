package com.ontadev.xray.service;

import com.ontadev.libs.ioc.annotation.stereotype.Service;
import com.ontadev.libs.shaded.nbtapi.NBTItem;
import com.ontadev.libs.shaded.nbtapi.NBTList;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

@Service
public class LampDataService {
    private static final String MATERIALS_NBT_KEY = "materials";

    private final NamespacedKey lampMarkerKey;
    private final NamespacedKey fuelMarkerKey;
    private final NamespacedKey radiusKey;
    private final NamespacedKey cooldownKey;
    private final NamespacedKey maxHighlightedKey;
    private final NamespacedKey highlightForAllKey;
    private final NamespacedKey fuelKey;
    private final NamespacedKey lastUseKey;
    private final NamespacedKey activeKey;

    public LampDataService(JavaPlugin plugin) {
        lampMarkerKey = new NamespacedKey(plugin, "lamp");
        fuelMarkerKey = new NamespacedKey(plugin, "fuel_item");
        radiusKey = new NamespacedKey(plugin, "radius");
        cooldownKey = new NamespacedKey(plugin, "cooldown_seconds");
        maxHighlightedKey = new NamespacedKey(plugin, "max_highlighted");
        highlightForAllKey = new NamespacedKey(plugin, "highlight_for_all");
        fuelKey = new NamespacedKey(plugin, "fuel");
        lastUseKey = new NamespacedKey(plugin, "last_use");
        activeKey = new NamespacedKey(plugin, "active");
    }

    public boolean isLamp(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        Byte marker = meta.getPersistentDataContainer().get(lampMarkerKey, PersistentDataType.BYTE);
        return marker != null && marker == 1;
    }

    public boolean isFuel(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        Byte marker = meta.getPersistentDataContainer().get(fuelMarkerKey, PersistentDataType.BYTE);
        return marker != null && marker == 1;
    }

    public void markAsLamp(ItemMeta meta) {
        meta.getPersistentDataContainer().set(lampMarkerKey, PersistentDataType.BYTE, (byte) 1);
    }

    public void markAsFuel(ItemMeta meta) {
        meta.getPersistentDataContainer().set(fuelMarkerKey, PersistentDataType.BYTE, (byte) 1);
    }

    public int getRadius(ItemStack item) {
        return getOrDefault(item, radiusKey, PersistentDataType.INTEGER, 10);
    }

    public void setRadius(ItemMeta meta, int radius) {
        meta.getPersistentDataContainer().set(radiusKey, PersistentDataType.INTEGER, Math.max(1, radius));
    }

    public long getCooldownSeconds(ItemStack item) {
        return getOrDefault(item, cooldownKey, PersistentDataType.LONG, 10L);
    }

    public void setCooldownSeconds(ItemMeta meta, long seconds) {
        meta.getPersistentDataContainer().set(cooldownKey, PersistentDataType.LONG, Math.max(0, seconds));
    }

    public int getMaxHighlightedBlocks(ItemStack item) {
        return getOrDefault(item, maxHighlightedKey, PersistentDataType.INTEGER, 100);
    }

    public void setMaxHighlightedBlocks(ItemMeta meta, int max) {
        meta.getPersistentDataContainer().set(maxHighlightedKey, PersistentDataType.INTEGER, Math.max(1, max));
    }

    public boolean isHighlightForAllPlayers(ItemStack item) {
        return getOrDefault(item, highlightForAllKey, PersistentDataType.BYTE, (byte) 0) == 1;
    }

    public void setHighlightForAllPlayers(ItemMeta meta, boolean forAll) {
        meta.getPersistentDataContainer().set(highlightForAllKey, PersistentDataType.BYTE, (byte) (forAll ? 1 : 0));
    }

    public List<Integer> getMaterialIds(ItemStack item) {
        if (item == null) return new ArrayList<>();

        NBTItem nbtItem = new NBTItem(item);
        NBTList<Integer> list = nbtItem.getIntegerList(MATERIALS_NBT_KEY);
        return new ArrayList<>(list);
    }

    public ItemStack setMaterialIds(ItemStack item, List<Integer> ids) {
        NBTItem nbtItem = new NBTItem(item);
        NBTList<Integer> list = nbtItem.getIntegerList(MATERIALS_NBT_KEY);
        list.clear();
        list.addAll(ids);
        return nbtItem.getItem();
    }

    public int getFuel(ItemStack item) {
        return getOrDefault(item, fuelKey, PersistentDataType.INTEGER, 0);
    }

    public void setFuel(ItemMeta meta, int fuel) {
        meta.getPersistentDataContainer().set(fuelKey, PersistentDataType.INTEGER, Math.max(0, fuel));
    }

    public long getLastUse(ItemStack item) {
        return getOrDefault(item, lastUseKey, PersistentDataType.LONG, 0L);
    }

    public void setLastUse(ItemMeta meta, long timestampMillis) {
        meta.getPersistentDataContainer().set(lastUseKey, PersistentDataType.LONG, timestampMillis);
    }

    public boolean isActive(ItemStack item) {
        return getOrDefault(item, activeKey, PersistentDataType.BYTE, (byte) 0) == 1;
    }

    public void setActive(ItemMeta meta, boolean active) {
        meta.getPersistentDataContainer().set(activeKey, PersistentDataType.BYTE, (byte) (active ? 1 : 0));

        // константа переименована в новых версиях, поэтому по ключу
        Enchantment glint = Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"));
        if (glint == null) return;

        if (active) {
            meta.addEnchant(glint, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else {
            meta.removeEnchant(glint);
            meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
    }

    public boolean clearActive(ItemStack item) {
        if (!isLamp(item) || !isActive(item)) return false;

        ItemMeta meta = item.getItemMeta();
        setActive(meta, false);
        item.setItemMeta(meta);
        return true;
    }

    private <T, Z> Z getOrDefault(ItemStack item, NamespacedKey key, PersistentDataType<T, Z> type, Z def) {
        if (item == null) return def;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return def;
        return meta.getPersistentDataContainer().getOrDefault(key, type, def);
    }
}
