package com.ontadev.xray.service;

import com.ontadev.libs.ioc.annotation.stereotype.Service;
import com.ontadev.xray.domain.MaterialEntry;
import com.ontadev.xray.repository.MaterialRepository;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MaterialManager {
    private final MaterialRepository repository;

    private final Map<Integer, MaterialEntry> cache = new LinkedHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public MaterialManager(MaterialRepository repository) {
        this.repository = repository;
        reload();
        seedDefaultsIfEmpty();
    }

    public final void reload() {
        List<MaterialEntry> all = repository.findAllOrdered();

        lock.writeLock().lock();
        try {
            cache.clear();
            for (MaterialEntry entry : all) {
                cache.put(entry.getId(), entry);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void seedDefaultsIfEmpty() {
        if (!cache.isEmpty()) return;

        log.info("Global materials list is empty, seeding defaults");
        add(Material.STONE, ChatColor.WHITE);
        addWithVariants(Material.DIAMOND_ORE, ChatColor.AQUA);
        addWithVariants(Material.GOLD_ORE, ChatColor.YELLOW);
        addWithVariants(Material.REDSTONE_ORE, ChatColor.RED);
        addWithVariants(Material.EMERALD_ORE, ChatColor.GREEN);
    }

    /** Руда и её глубинная (deepslate) версия того же цвета: в слое сланца обычной руды нет. */
    private void addWithVariants(Material material, ChatColor color) {
        add(material, color);
        for (Material variant : MaterialVariants.of(material)) addIfAbsent(variant, color);
    }

    public List<MaterialEntry> getAll() {
        lock.readLock().lock();
        try {
            return List.copyOf(cache.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    public Optional<MaterialEntry> get(int id) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(cache.get(id));
        } finally {
            lock.readLock().unlock();
        }
    }

    public Set<Integer> allIds() {
        lock.readLock().lock();
        try {
            return Set.copyOf(cache.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Integer> filterExisting(List<Integer> lampMaterialIds) {
        Set<Integer> existing = allIds();
        return lampMaterialIds.stream().filter(existing::contains).collect(Collectors.toList());
    }

    public Optional<MaterialEntry> findByMaterial(Material material) {
        lock.readLock().lock();
        try {
            return cache.values().stream().filter(entry -> entry.getMaterial() == material).findFirst();
        } finally {
            lock.readLock().unlock();
        }
    }

    public Optional<MaterialEntry> addIfAbsent(Material material, ChatColor color) {
        if (findByMaterial(material).isPresent()) return Optional.empty();
        return Optional.of(add(material, color));
    }

    public MaterialEntry add(Material material, ChatColor color) {
        MaterialEntry entry = new MaterialEntry(null, material, color);
        MaterialEntry saved = repository.save(entry);

        lock.writeLock().lock();
        try {
            cache.put(saved.getId(), saved);
        } finally {
            lock.writeLock().unlock();
        }
        return saved;
    }

    public void updateColor(int id, ChatColor color) {
        MaterialEntry entry = get(id).orElseThrow(() ->
                new IllegalArgumentException("Unknown material entry id " + id));
        entry.setColor(color);
        repository.save(entry);

        lock.writeLock().lock();
        try {
            cache.put(id, entry);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void remove(int id) {
        repository.deleteById(id);

        lock.writeLock().lock();
        try {
            cache.remove(id);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
