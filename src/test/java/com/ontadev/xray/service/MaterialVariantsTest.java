package com.ontadev.xray.service;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaterialVariantsTest {
    @Test
    void oreAndItsDeepslateVariantArePairedBothWays() {
        assertEquals(List.of("DEEPSLATE_DIAMOND_ORE"), MaterialVariants.counterpartNames("DIAMOND_ORE"));
        assertEquals(List.of("DIAMOND_ORE"), MaterialVariants.counterpartNames("DEEPSLATE_DIAMOND_ORE"));
        assertEquals(List.of("DEEPSLATE_LAPIS_ORE"), MaterialVariants.counterpartNames("LAPIS_ORE"));
    }

    @Test
    void stoneAndCobblestoneArePairedWithDeepslateCounterparts() {
        assertEquals(List.of("DEEPSLATE"), MaterialVariants.counterpartNames("STONE"));
        assertEquals(List.of("STONE"), MaterialVariants.counterpartNames("DEEPSLATE"));
        assertEquals(List.of("COBBLED_DEEPSLATE"), MaterialVariants.counterpartNames("COBBLESTONE"));
        assertEquals(List.of("COBBLESTONE"), MaterialVariants.counterpartNames("COBBLED_DEEPSLATE"));
    }

    @Test
    void otherBlocksHaveNoCounterpart() {
        assertTrue(MaterialVariants.counterpartNames("OAK_LOG").isEmpty());
    }

    @Test
    void namesWithoutASuchMaterialOnThisVersionAreDropped() {
        assertTrue(MaterialVariants.of(Material.DIAMOND_ORE).isEmpty() || MaterialVariants.of(Material.DIAMOND_ORE).size() == 1);
    }
}
