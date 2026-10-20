package com.ontadev.xray.service;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CooldownServiceTest {
    @Mock
    private LampDataService lampDataService;

    @Mock
    private ItemStack lamp;

    @Test
    void readyWhenNeverUsed() {
        when(lampDataService.getLastUse(lamp)).thenReturn(0L);
        when(lampDataService.getCooldownSeconds(lamp)).thenReturn(10L);

        CooldownService service = new CooldownService(lampDataService);

        assertTrue(service.isReady(lamp));
        assertEquals(0L, service.remainingSeconds(lamp));
    }

    @Test
    void notReadyRightAfterUse() {
        when(lampDataService.getLastUse(lamp)).thenReturn(System.currentTimeMillis());
        when(lampDataService.getCooldownSeconds(lamp)).thenReturn(10L);

        CooldownService service = new CooldownService(lampDataService);

        assertFalse(service.isReady(lamp));

        assertTrue(service.remainingSeconds(lamp) >= 9L);
        assertTrue(service.remainingSeconds(lamp) <= 10L);
    }

    @Test
    void readyAfterCooldownElapsed() {
        long twentySecondsAgo = System.currentTimeMillis() - 20_000L;
        when(lampDataService.getLastUse(lamp)).thenReturn(twentySecondsAgo);
        when(lampDataService.getCooldownSeconds(lamp)).thenReturn(10L);

        CooldownService service = new CooldownService(lampDataService);

        assertTrue(service.isReady(lamp));
    }
}
