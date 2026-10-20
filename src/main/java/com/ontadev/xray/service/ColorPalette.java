package com.ontadev.xray.service;

import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.List;

public final class ColorPalette {
    private static final List<ChatColor> COLORS = List.of(
            ChatColor.WHITE, ChatColor.GRAY, ChatColor.DARK_GRAY, ChatColor.BLACK,
            ChatColor.RED, ChatColor.DARK_RED, ChatColor.GOLD, ChatColor.YELLOW,
            ChatColor.GREEN, ChatColor.DARK_GREEN, ChatColor.AQUA, ChatColor.DARK_AQUA,
            ChatColor.BLUE, ChatColor.DARK_BLUE, ChatColor.LIGHT_PURPLE, ChatColor.DARK_PURPLE);

    private ColorPalette() {
    }

    public static List<ChatColor> colors() {
        return COLORS;
    }

    public static String tag(ChatColor color) {
        return color.name().toLowerCase();
    }

    public static Material icon(ChatColor color) {
        switch (color) {
            case WHITE: return Material.WHITE_WOOL;
            case GRAY: return Material.LIGHT_GRAY_WOOL;
            case DARK_GRAY: return Material.GRAY_WOOL;
            case BLACK: return Material.BLACK_WOOL;
            case RED: return Material.RED_WOOL;
            case DARK_RED: return Material.BROWN_WOOL;
            case GOLD: return Material.ORANGE_WOOL;
            case YELLOW: return Material.YELLOW_WOOL;
            case GREEN: return Material.LIME_WOOL;
            case DARK_GREEN: return Material.GREEN_WOOL;
            case AQUA: return Material.LIGHT_BLUE_WOOL;
            case DARK_AQUA: return Material.CYAN_WOOL;
            case BLUE: return Material.BLUE_WOOL;
            case DARK_BLUE: return Material.BLUE_TERRACOTTA;
            case LIGHT_PURPLE: return Material.MAGENTA_WOOL;
            case DARK_PURPLE: return Material.PURPLE_WOOL;
            default: return Material.WHITE_WOOL;
        }
    }
}
