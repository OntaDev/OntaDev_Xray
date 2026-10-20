package com.ontadev.xray.command;

import com.ontadev.xray.service.LampItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class GiveSubCommandsCompletionTest {
    private final CommandSender sender = mock(CommandSender.class);
    private final GiveLampSubCommand giveLamp = new GiveLampSubCommand(mock(LampItemFactory.class));
    private final GiveFuelSubCommand giveFuel = new GiveFuelSubCommand(mock(LampItemFactory.class));

    private static Player player(String name) {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(name);
        return player;
    }

    @Test
    void giveSuggestsOnlinePlayerNamesForTheFirstArgument() {
        List<Player> online = List.of(player("Alex"), player("Steve"));
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.<java.util.Collection<? extends Player>>when(Bukkit::getOnlinePlayers).thenReturn(online);

            assertEquals(List.of("Alex", "Steve"), giveLamp.complete(sender, ""));
            assertTrue(giveLamp.complete(sender, "Alex", "").isEmpty());
        }
    }

    @Test
    void givefuelSuggestsPlayersThenAmounts() {
        List<Player> online = List.of(player("Alex"));
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.<java.util.Collection<? extends Player>>when(Bukkit::getOnlinePlayers).thenReturn(online);

            assertEquals(List.of("Alex"), giveFuel.complete(sender, "A"));
            assertEquals(List.of("1", "8", "16", "32", "64"), giveFuel.complete(sender, "Alex", ""));
            assertTrue(giveFuel.complete(sender, "Alex", "8", "").isEmpty());
        }
    }
}
