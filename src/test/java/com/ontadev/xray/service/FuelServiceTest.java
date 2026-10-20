package com.ontadev.xray.service;

import com.ontadev.xray.config.XrayConfig;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FuelServiceTest {
    @Mock
    private LampDataService lampDataService;

    @Mock
    private ItemStack lamp;

    @Mock
    private ItemMeta meta;

    private XrayConfig config;

    @BeforeEach
    void setUp() {
        config = new XrayConfig();
        config.getFuel().setFuelPerItem(10);
        config.getFuel().setMaxFuel(100);
        config.getFuel().setFuelCostPerUse(1);

        lenient().when(lamp.getItemMeta()).thenReturn(meta);
    }

    @Test
    void hasEnoughFuelWhenAboveCost() {
        when(lampDataService.getFuel(lamp)).thenReturn(5);

        FuelService service = new FuelService(config, lampDataService);

        assertEquals(true, service.hasEnoughFuel(lamp));
    }

    @Test
    void notEnoughFuelWhenBelowCost() {
        when(lampDataService.getFuel(lamp)).thenReturn(0);

        FuelService service = new FuelService(config, lampDataService);

        assertEquals(false, service.hasEnoughFuel(lamp));
    }

    @Test
    void refuelConsumesOnlyAsManyItemsAsNeededToReachCap() {
        when(lampDataService.getFuel(lamp)).thenReturn(95);

        FuelService service = new FuelService(config, lampDataService);

        int consumed = service.refuel(lamp, 5);

        assertEquals(1, consumed);
    }

    @Test
    void refuelConsumesAllAvailableWhenNotEnoughToFillCap() {
        when(lampDataService.getFuel(lamp)).thenReturn(0);

        FuelService service = new FuelService(config, lampDataService);

        int consumed = service.refuel(lamp, 3);

        assertEquals(3, consumed);
    }

    @Test
    void refuelDoesNothingWhenAlreadyFull() {
        when(lampDataService.getFuel(lamp)).thenReturn(100);

        FuelService service = new FuelService(config, lampDataService);

        int consumed = service.refuel(lamp, 5);

        assertEquals(0, consumed);
    }

    @Test
    void refuelDoesNothingWhenNoFuelItemsAvailable() {
        FuelService service = new FuelService(config, lampDataService);

        int consumed = service.refuel(lamp, 0);

        assertEquals(0, consumed);
    }
}
