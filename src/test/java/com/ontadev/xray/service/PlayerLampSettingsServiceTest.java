package com.ontadev.xray.service;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerLampSettingsServiceTest {
    @Test
    void allMaterialsEnabledByDefault() {
        PlayerLampSettingsService service = new PlayerLampSettingsService();
        UUID player = UUID.randomUUID();

        assertTrue(service.isEnabled(player, 1));
        assertTrue(service.isEnabled(player, 999));
    }

    @Test
    void toggleDisablesThenReEnables() {
        PlayerLampSettingsService service = new PlayerLampSettingsService();
        UUID player = UUID.randomUUID();

        service.toggle(player, 5);
        assertFalse(service.isEnabled(player, 5));

        service.toggle(player, 5);
        assertTrue(service.isEnabled(player, 5));
    }

    @Test
    void togglingOneMaterialDoesNotAffectOthers() {
        PlayerLampSettingsService service = new PlayerLampSettingsService();
        UUID player = UUID.randomUUID();

        service.toggle(player, 1);

        assertFalse(service.isEnabled(player, 1));
        assertTrue(service.isEnabled(player, 2));
    }

    @Test
    void settingsAreIsolatedPerPlayer() {
        PlayerLampSettingsService service = new PlayerLampSettingsService();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();

        service.toggle(a, 1);

        assertFalse(service.isEnabled(a, 1));
        assertTrue(service.isEnabled(b, 1));
    }

    @Test
    void clearResetsPlayerToDefaults() {
        PlayerLampSettingsService service = new PlayerLampSettingsService();
        UUID player = UUID.randomUUID();

        service.toggle(player, 1);
        service.clear(player);

        assertTrue(service.isEnabled(player, 1));
    }

    @Test
    void colorFallsBackToTheGlobalOneUntilThePlayerPicksTheirOwn() {
        PlayerLampSettingsService service = new PlayerLampSettingsService();
        UUID player = UUID.randomUUID();

        assertEquals(ChatColor.AQUA, service.colorFor(player, 7, ChatColor.AQUA));

        service.setColor(player, 7, ChatColor.RED);
        assertEquals(ChatColor.RED, service.colorFor(player, 7, ChatColor.AQUA));
        assertEquals(ChatColor.AQUA, service.colorFor(player, 8, ChatColor.AQUA), "другие материалы не затронуты");
    }

    @Test
    void personalColorsAreIsolatedPerPlayerAndResettable() {
        PlayerLampSettingsService service = new PlayerLampSettingsService();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();

        service.setColor(a, 1, ChatColor.GOLD);

        assertEquals(ChatColor.GOLD, service.colorFor(a, 1, ChatColor.WHITE));
        assertEquals(ChatColor.WHITE, service.colorFor(b, 1, ChatColor.WHITE));

        service.resetColor(a, 1);
        assertEquals(ChatColor.WHITE, service.colorFor(a, 1, ChatColor.WHITE));
    }

    @Test
    void colorPickingStateIsTrackedAndClearedWithThePlayer() {
        PlayerLampSettingsService service = new PlayerLampSettingsService();
        UUID player = UUID.randomUUID();

        assertTrue(service.pickingColorFor(player).isEmpty());

        service.startPickingColor(player, 3);
        assertEquals(3, service.pickingColorFor(player).orElseThrow());

        service.setColor(player, 3, ChatColor.GREEN);
        service.clear(player);

        assertTrue(service.pickingColorFor(player).isEmpty());
        assertEquals(ChatColor.WHITE, service.colorFor(player, 3, ChatColor.WHITE), "выход игрока сбрасывает и личные цвета");
    }
}
