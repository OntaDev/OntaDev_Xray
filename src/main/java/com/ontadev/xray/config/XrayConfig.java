package com.ontadev.xray.config;

import com.google.gson.annotations.SerializedName;
import com.ontadev.libs.config.YamlConfig;
import com.ontadev.libs.ioc.annotation.stereotype.Config;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Config
public class XrayConfig extends YamlConfig {
    private LampDefaults lamp = new LampDefaults();

    private FuelSettings fuel = new FuelSettings();

    private ItemSettings item = new ItemSettings();

    @Override
    public String getFileName() {
        return "config";
    }

    @Getter
    @Setter
    public static class LampDefaults {
        @SerializedName("default-radius")
        private int defaultRadius = 10;

        @SerializedName("default-max-highlighted-blocks")
        private int defaultMaxHighlightedBlocks = 100;

        @SerializedName("default-cooldown")
        private long defaultCooldownSeconds = 1;

        @SerializedName("default-highlight-for-all")
        private boolean defaultHighlightForAll = true;

        @SerializedName("default-materials")
        private List<Material> defaultMaterials = new ArrayList<>(List.of(
                Material.DIAMOND_ORE, Material.GOLD_ORE, Material.EMERALD_ORE
        ));

        @SerializedName("refresh-interval-ticks")
        private int refreshIntervalTicks = 20;
    }

    @Getter
    @Setter
    public static class FuelSettings {
        @SerializedName("fuel-item-material")
        private Material fuelItemMaterial = Material.GLOWSTONE_DUST;

        @SerializedName("fuel-per-item")
        private int fuelPerItem = 10;

        @SerializedName("max-fuel")
        private int maxFuel = 100;

        @SerializedName("fuel-cost-per-use")
        private int fuelCostPerUse = 1;

        @SerializedName("fuel-drain-per-second")
        private double fuelDrainPerSecond = 2.0;
    }

    @Getter
    @Setter
    public static class ItemSettings {
        @SerializedName("lamp-material")
        private Material lampMaterial = Material.LANTERN;
    }
}
