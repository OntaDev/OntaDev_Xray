package com.ontadev.xray.service;

import com.ontadev.libs.ioc.annotation.stereotype.Service;

import org.bukkit.ChatColor;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PlayerLampSettingsService {
    private final Map<UUID, Set<Integer>> disabledByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Integer, ChatColor>> colorsByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> pickingColorByPlayer = new ConcurrentHashMap<>();

    public boolean isEnabled(UUID player, int materialId) {
        Set<Integer> disabled = disabledByPlayer.get(player);
        return disabled == null || !disabled.contains(materialId);
    }

    public void toggle(UUID player, int materialId) {
        Set<Integer> disabled = disabledByPlayer.computeIfAbsent(player, key -> ConcurrentHashMap.newKeySet());
        if (!disabled.add(materialId)) {
            disabled.remove(materialId);
        }
    }

    public ChatColor colorFor(UUID player, int materialId, ChatColor defaultColor) {
        return customColor(player, materialId).orElse(defaultColor);
    }

    public Optional<ChatColor> customColor(UUID player, int materialId) {
        Map<Integer, ChatColor> colors = colorsByPlayer.get(player);
        return colors == null ? Optional.empty() : Optional.ofNullable(colors.get(materialId));
    }

    public void setColor(UUID player, int materialId, ChatColor color) {
        colorsByPlayer.computeIfAbsent(player, key -> new ConcurrentHashMap<>()).put(materialId, color);
    }

    public void resetColor(UUID player, int materialId) {
        Map<Integer, ChatColor> colors = colorsByPlayer.get(player);
        if (colors != null) colors.remove(materialId);
    }

    public void startPickingColor(UUID player, int materialId) {
        pickingColorByPlayer.put(player, materialId);
    }

    public Optional<Integer> pickingColorFor(UUID player) {
        return Optional.ofNullable(pickingColorByPlayer.get(player));
    }

    public void stopPickingColor(UUID player) {
        pickingColorByPlayer.remove(player);
    }

    public void clear(UUID player) {
        disabledByPlayer.remove(player);
        colorsByPlayer.remove(player);
        pickingColorByPlayer.remove(player);
    }
}
