package com.ontadev.xray.command;

import com.ontadev.xray.service.ColorPalette;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class CommandArgs {
    private static final int MAX_SUGGESTIONS = 40;

    private CommandArgs() {
    }

    static Optional<Material> block(String name) {
        Material material = Material.matchMaterial(name.toUpperCase(Locale.ROOT));
        if (material == null || !material.isBlock() || material.isAir()) return Optional.empty();
        return Optional.of(material);
    }

    static Optional<ChatColor> color(String name) {
        return ColorPalette.colors().stream()
                .filter(color -> color.name().equalsIgnoreCase(name))
                .findFirst();
    }

    static Optional<Boolean> toggle(String text) {
        switch (text.toLowerCase(Locale.ROOT)) {
            case "on": case "true": case "yes": case "1": case "вкл":
                return Optional.of(true);
            case "off": case "false": case "no": case "0": case "выкл":
                return Optional.of(false);
            default:
                return Optional.empty();
        }
    }

    static Optional<Long> number(String text) {
        try {
            return Optional.of(Long.parseLong(text));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    static List<String> colorNames() {
        return ColorPalette.colors().stream().map(color -> color.name().toLowerCase(Locale.ROOT)).collect(Collectors.toList());
    }

    static List<String> blockNames(String typed) {
        return Arrays.stream(Material.values())
                .filter(material -> material.isBlock() && !material.isAir() && !material.name().startsWith("LEGACY_"))
                .map(material -> material.name().toLowerCase(Locale.ROOT))
                .filter(name -> name.startsWith(typed.toLowerCase(Locale.ROOT)))
                .limit(MAX_SUGGESTIONS)
                .collect(Collectors.toList());
    }

    static List<String> startingWith(Stream<String> options, String typed) {
        String prefix = typed.toLowerCase(Locale.ROOT);
        return options.filter(option -> option.toLowerCase(Locale.ROOT).startsWith(prefix)).collect(Collectors.toList());
    }
}
