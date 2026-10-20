package com.ontadev.xray.service;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColorPaletteTest {
    @Test
    void containsAllSixteenTeamColorsAndNoFormattingCodes() {
        assertEquals(16, ColorPalette.colors().size());
        assertEquals(16, ColorPalette.colors().stream().distinct().count());
        assertTrue(ColorPalette.colors().stream().allMatch(ChatColor::isColor));
    }

    @Test
    void everyColorHasAnIconAndAMiniMessageTag() {
        for (ChatColor color : ColorPalette.colors()) {
            assertTrue(ColorPalette.icon(color).isBlock(), color.name());
            assertEquals(color.name().toLowerCase(), ColorPalette.tag(color));
        }
    }
}
