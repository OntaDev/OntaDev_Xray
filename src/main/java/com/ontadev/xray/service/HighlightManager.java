package com.ontadev.xray.service;

import com.ontadev.libs.ioc.annotation.stereotype.Service;
import com.ontadev.xray.config.XrayConfig;
import com.ontadev.xray.domain.MaterialEntry;
import com.ontadev.xray.protocol.HighlightEntityManager;
import com.ontadev.xray.protocol.FakeEntity;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class HighlightManager {
    private static final Set<Material> IGNORED = EnumSet.of(Material.AIR, Material.CAVE_AIR, Material.VOID_AIR);

    private final HighlightEntityManager entityManager;
    private final TeamManager teamManager;
    private final MaterialManager materialManager;
    private final LampDataService lampDataService;
    private final PlayerLampSettingsService playerLampSettingsService;
    private final FuelService fuelService;
    private final XrayConfig config;
    private final JavaPlugin plugin;

    private final Map<UUID, Map<Long, HighlightedStand>> activeByViewer = new ConcurrentHashMap<>();

    private final Map<UUID, LampSession> sessions = new ConcurrentHashMap<>();

    public HighlightManager(
            HighlightEntityManager entityManager,
            TeamManager teamManager,
            MaterialManager materialManager,
            LampDataService lampDataService,
            PlayerLampSettingsService playerLampSettingsService,
            FuelService fuelService,
            XrayConfig config,
            JavaPlugin plugin
    ) {
        this.entityManager = entityManager;
        this.teamManager = teamManager;
        this.materialManager = materialManager;
        this.lampDataService = lampDataService;
        this.playerLampSettingsService = playerLampSettingsService;
        this.fuelService = fuelService;
        this.config = config;
        this.plugin = plugin;
    }

    public boolean isActive(UUID player) {
        return sessions.containsKey(player);
    }

    public void start(Player activator, EquipmentSlot hand) {
        endSession(activator);

        LampSession session = new LampSession(hand);
        sessions.put(activator.getUniqueId(), session);

        int interval = Math.max(1, config.getLamp().getRefreshIntervalTicks());
        session.task = Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(activator, session), 0L, interval);
    }

    public void stop(Player activator) {
        endSession(activator);
        deactivateLampsOf(activator);
    }

    private void endSession(Player activator) {
        LampSession session = sessions.remove(activator.getUniqueId());
        if (session != null) {
            if (session.task != null) session.task.cancel();

            for (UUID viewerId : session.viewers) {
                Player viewer = Bukkit.getPlayer(viewerId);
                if (viewer != null) clearViewer(viewer); else activeByViewer.remove(viewerId);
            }
        }
        clearViewer(activator);
    }

    public void onMainHandSlotChanged(Player player) {
        LampSession session = sessions.get(player.getUniqueId());
        if (session != null && session.hand == EquipmentSlot.HAND) stop(player);
    }

    public void onItemDropped(Player player, ItemStack dropped) {
        if (!lampDataService.isLamp(dropped)) return;

        lampDataService.clearActive(dropped);
        stopIfActive(player);
    }

    public void stopIfActive(Player player) {
        if (sessions.containsKey(player.getUniqueId())) stop(player);
    }

    private void tick(Player activator, LampSession session) {
        if (sessions.get(activator.getUniqueId()) != session) return;

        ItemStack lamp = HandItems.get(activator, session.hand);
        if (!activator.isOnline() || !lampDataService.isLamp(lamp) || !lampDataService.isActive(lamp)) {
            stop(activator);
            return;
        }

        if (!drainFuel(activator, lamp, session)) return;

        fuelService.showCharge(activator, lamp);
        refresh(activator, lamp, session);
    }

    private boolean drainFuel(Player activator, ItemStack lamp, LampSession session) {
        int interval = Math.max(1, config.getLamp().getRefreshIntervalTicks());
        session.drainRemainder += config.getFuel().getFuelDrainPerSecond() * interval / 20.0;

        int whole = (int) session.drainRemainder;
        session.drainRemainder -= whole;

        int remaining = whole > 0 ? fuelService.drain(lamp, whole) : lampDataService.getFuel(lamp);
        if (whole > 0) HandItems.set(activator, session.hand, lamp);

        if (remaining > 0) return true;

        stop(activator);
        activator.sendMessage(ChatColor.RED + "Заряд лампы закончился - она выключена.");
        return false;
    }

    private void refresh(Player activator, ItemStack lamp, LampSession session) {
        int radius = lampDataService.getRadius(lamp);
        int maxBlocks = lampDataService.getMaxHighlightedBlocks(lamp);
        boolean forAll = lampDataService.isHighlightForAllPlayers(lamp);

        List<Integer> lampMaterialIds = materialManager.filterExisting(lampDataService.getMaterialIds(lamp));
        Map<Material, MaterialEntry> allowed = resolveAllowedMaterials(lampMaterialIds);

        if (allowed.isEmpty()) {
            if (!session.warnedNoMaterials) {
                activator.sendMessage(ChatColor.YELLOW + "В лампе не выбрано ни одного материала для подсветки.");
                session.warnedNoMaterials = true;
            }
        }

        List<HighlightTarget> found = allowed.isEmpty()
                ? new ArrayList<>()
                : findBlocks(activator.getLocation(), radius, allowed, maxBlocks);

        List<Player> viewers = forAll ? findNearbyPlayers(activator, radius) : List.of(activator);
        Set<UUID> currentViewers = new HashSet<>();
        for (Player viewer : viewers) {
            currentViewers.add(viewer.getUniqueId());
            updateViewer(viewer, found);
        }

        for (UUID previous : new ArrayList<>(session.viewers)) {
            if (currentViewers.contains(previous)) continue;
            Player gone = Bukkit.getPlayer(previous);
            if (gone != null) clearViewer(gone); else activeByViewer.remove(previous);
        }
        session.viewers = currentViewers;
    }

    private Map<Material, MaterialEntry> resolveAllowedMaterials(List<Integer> lampMaterialIds) {
        Map<Material, MaterialEntry> allowed = new HashMap<>();
        for (Integer id : lampMaterialIds) {
            Optional<MaterialEntry> entry = materialManager.get(id);
            entry.ifPresent(materialEntry -> allowed.put(materialEntry.getMaterial(), materialEntry));
        }

        for (MaterialEntry entry : new ArrayList<>(allowed.values())) {
            for (Material variant : MaterialVariants.of(entry.getMaterial())) {
                allowed.putIfAbsent(variant, entry);
            }
        }
        return allowed;
    }

    private List<HighlightTarget> findBlocks(Location center, int radius, Map<Material, MaterialEntry> allowed, int maxBlocks) {
        if (maxBlocks <= 0) return new ArrayList<>();

        World world = center.getWorld();
        if (world == null) return new ArrayList<>();

        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();
        int radiusSquared = radius * radius;
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight();

        PriorityQueue<Candidate> nearest = new PriorityQueue<>(
                Comparator.comparingInt((Candidate candidate) -> candidate.distanceSquared).reversed());

        for (int dx = -radius; dx <= radius; dx++) {
            int x = cx + dx;
            int chunkX = x >> 4;

            for (int dz = -radius; dz <= radius; dz++) {
                int z = cz + dz;

                if (dx * dx + dz * dz > radiusSquared) continue;
                if (!world.isChunkLoaded(chunkX, z >> 4)) continue;

                for (int dy = -radius; dy <= radius; dy++) {
                    int y = cy + dy;
                    if (y < minY || y >= maxY) continue;

                    int distanceSquared = dx * dx + dy * dy + dz * dz;
                    if (distanceSquared > radiusSquared) continue;
                    if (nearest.size() >= maxBlocks && distanceSquared >= nearest.peek().distanceSquared) continue;

                    Block block = world.getBlockAt(x, y, z);
                    Material material = block.getType();
                    if (IGNORED.contains(material)) continue;

                    MaterialEntry entry = allowed.get(material);
                    if (entry == null) continue;

                    nearest.add(new Candidate(distanceSquared,
                            new HighlightTarget(block.getLocation().add(0.5, 0.5, 0.5), material, entry.getId(), entry.getColor())));
                    if (nearest.size() > maxBlocks) nearest.poll();
                }
            }
        }

        List<Candidate> sorted = new ArrayList<>(nearest);
        sorted.sort(Comparator.comparingInt(candidate -> candidate.distanceSquared));
        return sorted.stream().map(candidate -> candidate.target).collect(Collectors.toList());
    }

    private static final class Candidate {
        private final int distanceSquared;
        private final HighlightTarget target;

        private Candidate(int distanceSquared, HighlightTarget target) {
            this.distanceSquared = distanceSquared;
            this.target = target;
        }
    }

    private List<Player> findNearbyPlayers(Player activator, int radius) {
        double radiusSquared = (double) radius * radius;
        World world = activator.getWorld();

        return world.getPlayers().stream()
                .filter(p -> p.getLocation().distanceSquared(activator.getLocation()) <= radiusSquared)
                .collect(Collectors.toList());
    }

    private void updateViewer(Player viewer, List<HighlightTarget> found) {
        UUID viewerId = viewer.getUniqueId();
        Map<Long, HighlightedStand> current = activeByViewer.computeIfAbsent(viewerId, key -> new HashMap<>());

        Set<Long> wanted = new HashSet<>();
        List<HighlightedStand> removed = new ArrayList<>();

        for (HighlightTarget target : found) {
            if (!playerLampSettingsService.isEnabled(viewerId, target.getMaterialId())) continue;

            long key = positionKey(target.getLocation());
            ChatColor color = playerLampSettingsService.colorFor(viewerId, target.getMaterialId(), target.getColor());
            wanted.add(key);

            HighlightedStand existing = current.get(key);
            if (existing != null && existing.getColor() == color && existing.getMaterial() == target.getMaterial()) continue;
            if (existing != null) {
                removed.add(existing);
                current.remove(key);
            }

            FakeEntity stand = entityManager.spawn(viewer, target.getLocation(), target.getMaterial());
            teamManager.addEntry(color, stand.getUuid());
            current.put(key, new HighlightedStand(stand, color, target.getMaterial()));
        }

        for (Map.Entry<Long, HighlightedStand> entry : new ArrayList<>(current.entrySet())) {
            if (wanted.contains(entry.getKey())) continue;
            removed.add(entry.getValue());
            current.remove(entry.getKey());
        }

        destroy(viewer, removed);
    }

    private void clearViewer(Player viewer) {
        Map<Long, HighlightedStand> stands = activeByViewer.remove(viewer.getUniqueId());
        if (stands == null || stands.isEmpty()) return;

        destroy(viewer, new ArrayList<>(stands.values()));
    }

    private void destroy(Player viewer, List<HighlightedStand> stands) {
        if (stands.isEmpty()) return;

        for (HighlightedStand stand : stands) {
            teamManager.removeEntry(stand.getColor(), stand.getStand().getUuid());
        }
        entityManager.destroy(viewer, stands.stream().map(HighlightedStand::getStand).collect(Collectors.toList()));
    }

    public void clearFor(Player viewer) {
        stop(viewer);
    }

    public void deactivateLampsOf(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        boolean changed = false;
        for (ItemStack item : contents) {
            changed |= lampDataService.clearActive(item);
        }
        if (changed) player.getInventory().setContents(contents);
    }

    public void clearActiveFlag(ItemStack item) {
        lampDataService.clearActive(item);
    }

    private static long positionKey(Location location) {
        long x = location.getBlockX() & 0x3FFFFFFL;
        long z = location.getBlockZ() & 0x3FFFFFFL;
        long y = location.getBlockY() & 0xFFFL;
        return (x << 38) | (z << 12) | y;
    }

    private static final class LampSession {
        private final EquipmentSlot hand;
        private BukkitTask task;
        private Set<UUID> viewers = new HashSet<>();
        private double drainRemainder;
        private boolean warnedNoMaterials;

        private LampSession(EquipmentSlot hand) {
            this.hand = hand;
        }
    }

    @Getter
    private static final class HighlightTarget {
        private final Location location;
        private final Material material;
        private final int materialId;
        private final ChatColor color;

        private HighlightTarget(Location location, Material material, int materialId, ChatColor color) {
            this.location = location;
            this.material = material;
            this.materialId = materialId;
            this.color = color;
        }
    }

    @Getter
    private static final class HighlightedStand {
        private final FakeEntity stand;
        private final ChatColor color;
        private final Material material;

        private HighlightedStand(FakeEntity stand, ChatColor color, Material material) {
            this.stand = stand;
            this.color = color;
            this.material = material;
        }
    }
}
