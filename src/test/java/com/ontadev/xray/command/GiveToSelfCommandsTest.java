package com.ontadev.xray.command;

import com.ontadev.xray.service.LampItemFactory;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GiveToSelfCommandsTest {
    private final Command command = mock(Command.class);
    private LampItemFactory factory;
    private Player player;
    private PlayerInventory inventory;
    private ItemStack lamp;
    private ItemStack fuel;

    @BeforeEach
    void setUp() {
        factory = mock(LampItemFactory.class);
        lamp = mock(ItemStack.class);
        fuel = mock(ItemStack.class);
        when(factory.createLamp()).thenReturn(lamp);
        when(factory.createFuel(anyInt())).thenReturn(fuel);

        player = mock(Player.class);
        inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(player.getName()).thenReturn("Alex");
    }

    @Test
    void giveWithoutArgumentsGivesTheLampToTheSender() {
        new GiveLampSubCommand(factory).execute(player, command, "xray");

        verify(inventory).addItem(lamp);
    }

    @Test
    void givefuelWithoutArgumentsGivesOneFuelToTheSender() {
        new GiveFuelSubCommand(factory).execute(player, command, "xray");

        verify(factory).createFuel(1);
        verify(inventory).addItem(fuel);
    }

    @Test
    void givefuelWithJustANumberGivesThatManyToTheSender() {
        new GiveFuelSubCommand(factory).execute(player, command, "xray", "16");

        verify(factory).createFuel(16);
        verify(inventory).addItem(fuel);
    }

    @Test
    void givefuelRejectsANonPositiveAmount() {
        new GiveFuelSubCommand(factory).execute(player, command, "xray", "0");

        verify(inventory, never()).addItem(fuel);
    }

    @Test
    void makeTurnsTheHeldItemIntoALampOrFuel() {
        ItemStack held = mock(ItemStack.class);
        when(held.getType()).thenReturn(Material.STICK);
        when(inventory.getItemInMainHand()).thenReturn(held);
        ItemStack madeLamp = mock(ItemStack.class);
        ItemStack madeFuel = mock(ItemStack.class);
        when(factory.convertToLamp(held)).thenReturn(madeLamp);
        when(factory.convertToFuel(held)).thenReturn(madeFuel);

        MakeSubCommand make = new MakeSubCommand(factory);
        make.execute(player, command, "xray", "lamp");
        verify(inventory).setItemInMainHand(madeLamp);

        make.execute(player, command, "xray", "fuel");
        verify(inventory).setItemInMainHand(madeFuel);
    }

    @Test
    void makeDoesNothingWithAnEmptyHand() {
        ItemStack air = mock(ItemStack.class);
        when(air.getType()).thenReturn(Material.AIR);
        when(inventory.getItemInMainHand()).thenReturn(air);

        new MakeSubCommand(factory).execute(player, command, "xray", "lamp");

        verify(inventory, never()).setItemInMainHand(lamp);
    }
}
