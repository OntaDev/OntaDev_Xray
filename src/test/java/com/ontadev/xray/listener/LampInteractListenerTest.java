package com.ontadev.xray.listener;

import com.ontadev.xray.gui.LampEditorGui;
import com.ontadev.xray.gui.PlayerMaterialsGui;
import com.ontadev.xray.service.CooldownService;
import com.ontadev.xray.service.FuelService;
import com.ontadev.xray.service.HighlightManager;
import com.ontadev.xray.service.LampDataService;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LampInteractListenerTest {
    private LampDataService lampDataService;
    private FuelService fuelService;
    private CooldownService cooldownService;
    private HighlightManager highlightManager;
    private LampInteractListener listener;
    private Player player;
    private PlayerInventory inventory;
    private ItemStack lamp;
    private ItemStack other;

    @BeforeEach
    void setUp() {
        lampDataService = mock(LampDataService.class);
        fuelService = mock(FuelService.class);
        cooldownService = mock(CooldownService.class);
        highlightManager = mock(HighlightManager.class);
        listener = new LampInteractListener(
                lampDataService,
                cooldownService,
                fuelService,
                highlightManager,
                mock(PlayerMaterialsGui.class),
                mock(LampEditorGui.class));

        player = mock(Player.class);
        inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);

        lamp = mock(ItemStack.class);
        other = mock(ItemStack.class);
        when(lampDataService.isLamp(lamp)).thenReturn(true);
    }

    private PlayerInteractEvent event(Action action, EquipmentSlot hand) {
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(action);
        when(event.getHand()).thenReturn(hand);
        when(event.getPlayer()).thenReturn(player);
        return event;
    }

    @Test
    void shiftRightClickWithLampAndNothingToRefuelWithIsCancelledSoTheLanternIsNotPlaced() {
        when(inventory.getItemInMainHand()).thenReturn(lamp);
        when(inventory.getItemInOffHand()).thenReturn(other);
        when(inventory.getContents()).thenReturn(new ItemStack[]{lamp, other});
        when(player.isSneaking()).thenReturn(true);
        PlayerInteractEvent event = event(Action.RIGHT_CLICK_BLOCK, EquipmentSlot.HAND);

        listener.onInteract(event);

        verify(event).setCancelled(true);
        verify(player).sendMessage(anyString());
    }

    @Test
    void shiftRightClickWithLampTakesFuelFromAnywhereInTheInventory() {
        ItemStack fuel = mock(ItemStack.class);
        when(fuel.getAmount()).thenReturn(5);
        when(lampDataService.isFuel(fuel)).thenReturn(true);
        when(fuelService.refuel(lamp, 5)).thenReturn(3);

        when(inventory.getItemInMainHand()).thenReturn(lamp);
        when(inventory.getItemInOffHand()).thenReturn(other);
        when(inventory.getContents()).thenReturn(new ItemStack[]{other, lamp, other, fuel});
        when(player.isSneaking()).thenReturn(true);
        PlayerInteractEvent event = event(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);

        listener.onInteract(event);

        verify(event).setCancelled(true);
        verify(fuelService).refuel(lamp, 5);
        verify(fuel).setAmount(2);
        verify(inventory).setItem(3, fuel);
    }

    @Test
    void aFullyUsedFuelStackIsRemovedFromItsSlot() {
        ItemStack fuel = mock(ItemStack.class);
        when(fuel.getAmount()).thenReturn(2);
        when(lampDataService.isFuel(fuel)).thenReturn(true);
        when(fuelService.refuel(lamp, 2)).thenReturn(2);

        when(inventory.getItemInMainHand()).thenReturn(lamp);
        when(inventory.getContents()).thenReturn(new ItemStack[]{lamp, fuel});
        when(player.isSneaking()).thenReturn(true);

        listener.onInteract(event(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND));

        verify(inventory).setItem(1, null);
    }

    @Test
    void lampInTheOffHandCannotBePlacedEither() {
        when(inventory.getItemInOffHand()).thenReturn(lamp);
        PlayerInteractEvent event = event(Action.RIGHT_CLICK_BLOCK, EquipmentSlot.OFF_HAND);

        listener.onInteract(event);

        verify(event).setCancelled(true);
    }

    @Test
    void anOrdinaryItemInTheOffHandIsLeftAlone() {
        when(inventory.getItemInOffHand()).thenReturn(other);
        PlayerInteractEvent event = event(Action.RIGHT_CLICK_BLOCK, EquipmentSlot.OFF_HAND);

        listener.onInteract(event);

        verify(event, never()).setCancelled(true);
    }

    @Test
    void rightClickWithTheLampInTheOffHandTurnsItOn() {
        when(player.hasPermission("lamp.use")).thenReturn(true);
        when(cooldownService.remainingSeconds(lamp)).thenReturn(0L);
        when(fuelService.hasEnoughFuel(lamp)).thenReturn(true);
        when(lamp.getItemMeta()).thenReturn(mock(org.bukkit.inventory.meta.ItemMeta.class));
        when(inventory.getItemInMainHand()).thenReturn(other);
        when(inventory.getItemInOffHand()).thenReturn(lamp);
        PlayerInteractEvent event = event(Action.RIGHT_CLICK_AIR, EquipmentSlot.OFF_HAND);

        listener.onInteract(event);

        verify(event).setCancelled(true);
        verify(highlightManager).start(player, EquipmentSlot.OFF_HAND);
        verify(inventory).setItemInOffHand(lamp);
    }

    @Test
    void theOffHandEventIsIgnoredWhenTheMainHandAlsoHoldsALamp() {
        ItemStack mainLamp = mock(ItemStack.class);
        when(lampDataService.isLamp(mainLamp)).thenReturn(true);
        when(inventory.getItemInMainHand()).thenReturn(mainLamp);
        when(inventory.getItemInOffHand()).thenReturn(lamp);
        PlayerInteractEvent event = event(Action.RIGHT_CLICK_AIR, EquipmentSlot.OFF_HAND);

        listener.onInteract(event);

        verify(event).setCancelled(true);
        verify(highlightManager, never()).start(player, EquipmentSlot.OFF_HAND);
    }

    @Test
    void shiftRightClickWithTheLampInTheOffHandRefuelsFromTheInventory() {
        ItemStack fuel = mock(ItemStack.class);
        when(fuel.getAmount()).thenReturn(4);
        when(lampDataService.isFuel(fuel)).thenReturn(true);
        when(fuelService.refuel(lamp, 4)).thenReturn(4);
        when(inventory.getItemInMainHand()).thenReturn(other);
        when(inventory.getItemInOffHand()).thenReturn(lamp);
        when(inventory.getContents()).thenReturn(new ItemStack[]{fuel, other, lamp});
        when(player.isSneaking()).thenReturn(true);

        listener.onInteract(event(Action.RIGHT_CLICK_AIR, EquipmentSlot.OFF_HAND));

        verify(fuelService).refuel(lamp, 4);
        verify(inventory).setItemInOffHand(lamp);
    }
}
