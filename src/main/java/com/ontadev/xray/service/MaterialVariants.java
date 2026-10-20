package com.ontadev.xray.service;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public final class MaterialVariants {
    private static final String DEEPSLATE_PREFIX = "DEEPSLATE_";

    private MaterialVariants() {
    }

    public static List<Material> of(Material material) {
        List<Material> variants = new ArrayList<>();

        for (String name : counterpartNames(material.name())) {
            Material variant = Material.getMaterial(name);
            if (variant != null) variants.add(variant);
        }

        return variants;
    }

    static List<String> counterpartNames(String name) {
        switch (name) {
            case "STONE":
                return List.of("DEEPSLATE");
            case "DEEPSLATE":
                return List.of("STONE");
            case "COBBLESTONE":
                return List.of("COBBLED_DEEPSLATE");
            case "COBBLED_DEEPSLATE":
                return List.of("COBBLESTONE");
            default:
                break;
        }

        if (!name.endsWith("_ORE")) return List.of();

        return name.startsWith(DEEPSLATE_PREFIX)
                ? List.of(name.substring(DEEPSLATE_PREFIX.length()))
                : List.of(DEEPSLATE_PREFIX + name);
    }
}
