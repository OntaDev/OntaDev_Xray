package com.ontadev.xray.service;

import com.ontadev.xray.domain.MaterialEntry;
import com.ontadev.xray.repository.MaterialRepository;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MaterialManagerTest {
    private MaterialRepository repository;
    private MaterialManager manager;

    @BeforeEach
    void setUp() {
        repository = mock(MaterialRepository.class);
        when(repository.findAllOrdered()).thenReturn(List.of(new MaterialEntry(1, Material.DIAMOND_ORE, ChatColor.AQUA)));
        when(repository.save(any(MaterialEntry.class))).thenAnswer(invocation -> {
            MaterialEntry entry = invocation.getArgument(0);
            entry.setId(7);
            return entry;
        });
        manager = new MaterialManager(repository);
    }

    @Test
    void findsAnExistingEntryByMaterial() {
        assertEquals(1, manager.findByMaterial(Material.DIAMOND_ORE).orElseThrow().getId());
        assertTrue(manager.findByMaterial(Material.GOLD_ORE).isEmpty());
    }

    @Test
    void addIfAbsentDoesNotCreateADuplicate() {
        assertTrue(manager.addIfAbsent(Material.DIAMOND_ORE, ChatColor.WHITE).isEmpty());

        verify(repository, never()).save(any(MaterialEntry.class));
        assertEquals(1, manager.getAll().size());
    }

    @Test
    void addIfAbsentSavesANewMaterialAndMakesItVisibleImmediately() {
        Optional<MaterialEntry> added = manager.addIfAbsent(Material.GOLD_ORE, ChatColor.WHITE);

        assertEquals(7, added.orElseThrow().getId());
        verify(repository, times(1)).save(any(MaterialEntry.class));
        assertEquals(2, manager.getAll().size());
        assertTrue(manager.findByMaterial(Material.GOLD_ORE).isPresent());
    }

    @Test
    void theDefaultFilterHasEveryStandardOreTogetherWithItsDeepslateVersion() {
        org.junit.jupiter.api.Assumptions.assumeTrue(Material.getMaterial("DEEPSLATE_DIAMOND_ORE") != null,
                "в API 1.16.5 глубинной руды нет, проверка идёт только на новых версиях");
        MaterialRepository empty = mock(MaterialRepository.class);
        when(empty.findAllOrdered()).thenReturn(List.of());
        java.util.concurrent.atomic.AtomicInteger ids = new java.util.concurrent.atomic.AtomicInteger();
        when(empty.save(any(MaterialEntry.class))).thenAnswer(invocation -> {
            MaterialEntry entry = invocation.getArgument(0);
            entry.setId(ids.incrementAndGet());
            return entry;
        });

        MaterialManager seeded = new MaterialManager(empty);

        for (Material ore : List.of(Material.DIAMOND_ORE, Material.GOLD_ORE, Material.REDSTONE_ORE, Material.EMERALD_ORE)) {
            Material deepslate = Material.getMaterial("DEEPSLATE_" + ore.name());
            MaterialEntry plain = seeded.findByMaterial(ore).orElseThrow();
            MaterialEntry deep = seeded.findByMaterial(deepslate).orElseThrow(() -> new AssertionError("нет " + deepslate));
            assertEquals(plain.getColor(), deep.getColor(), "цвет глубинной руды совпадает с обычной: " + ore);
        }
    }
}
