package com.ontadev.xray.service;

import com.ontadev.libs.ioc.annotation.stereotype.Service;
import com.ontadev.xray.config.XrayConfig;
import com.ontadev.xray.domain.MaterialEntry;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

@Service
public class LampItemFactory {
    private final XrayConfig config;
    private final LampDataService lampDataService;
    private final MaterialManager materialManager;
    private final FuelService fuelService;

    public LampItemFactory(XrayConfig config, LampDataService lampDataService, MaterialManager materialManager,
                           FuelService fuelService) {
        this.config = config;
        this.lampDataService = lampDataService;
        this.materialManager = materialManager;
        this.fuelService = fuelService;
    }

    public ItemStack createLamp() {
        ItemStack item = new ItemStack(config.getItem().getLampMaterial());
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GOLD + "Лампа X-ray");
        meta.setLore(List.of(
                ChatColor.GRAY + "ПКМ - включить/выключить подсветку руды рядом",
                ChatColor.GRAY + "Шифт+ПКМ - зарядить топливом из инвентаря"
        ));
        item.setItemMeta(meta);

        return convertToLamp(item);
    }

    public ItemStack convertToLamp(ItemStack source) {
        XrayConfig.LampDefaults defaults = config.getLamp();

        ItemStack item = source.clone();
        item.setAmount(1);
        ItemMeta meta = item.getItemMeta();

        if (!meta.hasDisplayName()) {
            meta.setDisplayName(ChatColor.GOLD + "Лампа X-ray");
        }

        int maxFuel = config.getFuel().getMaxFuel();
        lampDataService.markAsLamp(meta);
        lampDataService.setRadius(meta, defaults.getDefaultRadius());
        lampDataService.setCooldownSeconds(meta, defaults.getDefaultCooldownSeconds());
        lampDataService.setMaxHighlightedBlocks(meta, defaults.getDefaultMaxHighlightedBlocks());
        lampDataService.setHighlightForAllPlayers(meta, defaults.isDefaultHighlightForAll());
        lampDataService.setFuel(meta, maxFuel);
        fuelService.applyChargeLore(meta, maxFuel);
        lampDataService.setActive(meta, false);
        lampDataService.setLastUse(meta, 0L);

        item.setItemMeta(meta);

        return lampDataService.setMaterialIds(item, resolveDefaultMaterialIds(defaults.getDefaultMaterials()));
    }

    public ItemStack convertToFuel(ItemStack source) {
        ItemStack item = source.clone();
        ItemMeta meta = item.getItemMeta();

        if (!meta.hasDisplayName()) {
            meta.setDisplayName(ChatColor.AQUA + "Топливо для лампы");
        }
        if (!meta.hasLore()) {
            meta.setLore(List.of(ChatColor.GRAY + "Шифт+ПКМ по лампе, чтобы зарядить её"));
        }

        lampDataService.markAsFuel(meta);
        item.setItemMeta(meta);
        return item;
    }

    private List<Integer> resolveDefaultMaterialIds(List<Material> defaultMaterials) {
        List<MaterialEntry> all = materialManager.getAll();
        List<Integer> ids = new ArrayList<>();

        for (Material material : defaultMaterials) {
            for (MaterialEntry entry : all) {
                if (entry.getMaterial() == material) {
                    ids.add(entry.getId());
                    break;
                }
            }
        }
        return ids;
    }

    public ItemStack createFuel(int amount) {
        ItemStack item = new ItemStack(config.getFuel().getFuelItemMaterial(), Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.AQUA + "Топливо для лампы");
        meta.setLore(List.of(ChatColor.GRAY + "Шифт+ПКМ по лампе, чтобы зарядить её"));

        lampDataService.markAsFuel(meta);

        item.setItemMeta(meta);
        return item;
    }
}
