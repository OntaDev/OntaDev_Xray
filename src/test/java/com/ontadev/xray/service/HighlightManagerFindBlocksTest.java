package com.ontadev.xray.service;

import com.ontadev.xray.domain.MaterialEntry;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HighlightManagerFindBlocksTest {
    private final HighlightManager manager = new HighlightManager(null, null, null, null, null, null, null, null);

    @Test
    void limitIsFilledWithTheNearestBlocksNotTheFirstOnesFoundAtTheFarEdge() throws Exception {
        World world = mock(World.class);
        when(world.getMaxHeight()).thenReturn(256);
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);

        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenAnswer(invocation -> {
            Block block = mock(Block.class);
            when(block.getType()).thenReturn(Material.STONE);
            when(block.getLocation()).thenReturn(new Location(world, invocation.<Integer>getArgument(0),
                    invocation.<Integer>getArgument(1), invocation.<Integer>getArgument(2)));
            return block;
        });

        Map<Material, MaterialEntry> allowed = Map.of(
                Material.STONE, new MaterialEntry(1, Material.STONE, ChatColor.WHITE));

        List<?> found = findBlocks(new Location(world, 100.5, 100.5, 100.5), 10, allowed, 5);

        assertEquals(5, found.size());
        List<Double> distances = new ArrayList<>();
        for (Object target : found) {
            Location location = (Location) target.getClass().getMethod("getLocation").invoke(target);
            distances.add(location.distance(new Location(world, 100.5, 100.5, 100.5)));
        }

        assertTrue(distances.stream().allMatch(distance -> distance <= 1.0), "лимит должны занять ближайшие блоки: " + distances);
        assertEquals(distances.stream().sorted().collect(java.util.stream.Collectors.toList()), distances, "результат отсортирован от ближних к дальним");
    }

    @Test
    void blocksBelowZeroInTheDeepslateLayerAreFound() throws Exception {
        World world = mock(World.class);
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenAnswer(invocation -> {
            Block block = mock(Block.class);
            boolean ore = invocation.<Integer>getArgument(1) == -36;
            when(block.getType()).thenReturn(ore ? Material.DIAMOND_ORE : Material.AIR);
            when(block.getLocation()).thenReturn(new Location(world, invocation.<Integer>getArgument(0),
                    invocation.<Integer>getArgument(1), invocation.<Integer>getArgument(2)));
            return block;
        });
        Map<Material, MaterialEntry> allowed = Map.of(
                Material.DIAMOND_ORE, new MaterialEntry(1, Material.DIAMOND_ORE, ChatColor.AQUA));

        List<?> found = findBlocks(new Location(world, 0.5, -36.5, 0.5), 3, allowed, 5);

        assertEquals(5, found.size(), "раньше всё ниже y=0 отбрасывалось, и в слое сланца подсветки не было совсем");
    }

    private List<?> findBlocks(Location center, int radius, Map<Material, MaterialEntry> allowed, int max) throws Exception {
        Method method = HighlightManager.class.getDeclaredMethod("findBlocks", Location.class, int.class, Map.class, int.class);
        method.setAccessible(true);
        return (List<?>) method.invoke(manager, center, radius, allowed, max);
    }
}
