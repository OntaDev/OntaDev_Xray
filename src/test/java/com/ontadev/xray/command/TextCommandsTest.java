package com.ontadev.xray.command;

import com.ontadev.xray.domain.MaterialEntry;
import com.ontadev.xray.gui.GlobalMaterialsGui;
import com.ontadev.xray.service.MaterialManager;
import com.ontadev.xray.service.PlayerLampSettingsService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TextCommandsTest {
    private final Command command = mock(Command.class);
    private MaterialManager materials;
    private final MaterialEntry diamond = new MaterialEntry(1, Material.DIAMOND_ORE, ChatColor.AQUA);

    @BeforeEach
    void setUp() {
        materials = mock(MaterialManager.class);
        when(materials.getAll()).thenReturn(List.of(diamond));
        when(materials.findByMaterial(Material.DIAMOND_ORE)).thenReturn(Optional.of(diamond));
    }

    private CommandSender admin() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("lamp.materials.edit")).thenReturn(true);
        return sender;
    }

    @Test
    void argumentParsersAcceptBlocksColorsSwitchesAndNumbers() {
        assertEquals(Material.DIAMOND_ORE, CommandArgs.block("diamond_ore").orElseThrow());
        assertTrue(CommandArgs.block("stick").isEmpty(), "не блок");
        assertTrue(CommandArgs.block("nonsense").isEmpty());
        assertEquals(ChatColor.RED, CommandArgs.color("RED").orElseThrow());
        assertEquals(ChatColor.DARK_AQUA, CommandArgs.color("dark_aqua").orElseThrow());
        assertTrue(CommandArgs.color("bold").isEmpty(), "стили - не цвета");
        assertTrue(CommandArgs.toggle("on").orElseThrow());
        assertFalse(CommandArgs.toggle("off").orElseThrow());
        assertTrue(CommandArgs.toggle("maybe").isEmpty());
        assertEquals(25L, CommandArgs.number("25").orElseThrow());
        assertTrue(CommandArgs.number("x").isEmpty());
    }

    @Test
    void materialsAddStoresANewBlockWithTheGivenColor() {
        when(materials.addIfAbsent(Material.GOLD_ORE, ChatColor.YELLOW)).thenReturn(Optional.of(new MaterialEntry(2, Material.GOLD_ORE, ChatColor.YELLOW)));

        new MaterialsSubCommand(mock(GlobalMaterialsGui.class), materials).execute(admin(), command, "xray", "add", "gold_ore", "yellow");

        verify(materials).addIfAbsent(Material.GOLD_ORE, ChatColor.YELLOW);
    }

    @Test
    void materialsAddRejectsNonBlocksAndUnknownColors() {
        MaterialsSubCommand sub = new MaterialsSubCommand(mock(GlobalMaterialsGui.class), materials);

        sub.execute(admin(), command, "xray", "add", "stick");
        sub.execute(admin(), command, "xray", "add", "gold_ore", "plaid");

        verify(materials, never()).addIfAbsent(any(), any());
    }

    @Test
    void materialsEditingNeedsThePermission() {
        CommandSender plain = mock(CommandSender.class);

        new MaterialsSubCommand(mock(GlobalMaterialsGui.class), materials).execute(plain, command, "xray", "remove", "diamond_ore");

        verify(materials, never()).remove(1);
    }

    @Test
    void materialsRemoveAndColorWorkByName() {
        MaterialsSubCommand sub = new MaterialsSubCommand(mock(GlobalMaterialsGui.class), materials);

        sub.execute(admin(), command, "xray", "color", "diamond_ore", "red");
        verify(materials).updateColor(1, ChatColor.RED);

        sub.execute(admin(), command, "xray", "remove", "diamond_ore");
        verify(materials).remove(1);
    }

    @Test
    void materialsWithoutArgumentsOpensTheEditorForAPlayerAndListsForTheConsole() {
        GlobalMaterialsGui gui = mock(GlobalMaterialsGui.class);
        MaterialsSubCommand sub = new MaterialsSubCommand(gui, materials);
        Player player = mock(Player.class);

        sub.execute(player, command, "xray");
        verify(gui).open(player);

        CommandSender console = mock(CommandSender.class);
        sub.execute(console, command, "xray");
        verify(console).sendMessage(org.mockito.ArgumentMatchers.contains("Материалы для подсветки"));
    }

    @Test
    void personalToggleAndColorUseTheSameSettingsAsTheMenu() {
        PlayerLampSettingsService settings = new PlayerLampSettingsService();
        Player player = mock(Player.class);
        UUID id = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(id);
        PrefsSubCommand sub = new PrefsSubCommand(materials, settings);

        sub.execute(player, command, "xray", "toggle", "diamond_ore");
        assertFalse(settings.isEnabled(id, 1));

        sub.execute(player, command, "xray", "color", "diamond_ore", "gold");
        assertEquals(ChatColor.GOLD, settings.colorFor(id, 1, ChatColor.AQUA));

        sub.execute(player, command, "xray", "color", "diamond_ore", "reset");
        assertEquals(ChatColor.AQUA, settings.colorFor(id, 1, ChatColor.AQUA));
    }
}
