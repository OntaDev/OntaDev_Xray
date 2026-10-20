package com.ontadev.xray.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.ontadev.libs.ioc.annotation.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HighlightEntityManager {
    private static final int FLAGS_INDEX = 0;
    private static final byte FLAG_INVISIBLE = 0x20;
    private static final byte FLAG_GLOWING = 0x40;

    private static final int NO_GRAVITY_INDEX = 5;

    // высота центра блока на голове стойки над её ногами
    private static final double HEAD_BLOCK_CENTER = 1.8;

    // до 1.17 индекс 14, с 1.17 - 15
    private static final int ARMOR_STAND_FLAGS_INDEX = 15;
    private static final int ARMOR_STAND_FLAGS_INDEX_PRE_1_17 = 14;

    private static final byte ARMOR_STAND_NO_BASEPLATE = 0x08;
    private static final byte ARMOR_STAND_MARKER = 0x10;

    private static final int ARMOR_STAND_NETWORK_ID_PRE_1_17 = 1;

    private static final MinecraftVersion V1_17 = new MinecraftVersion("1.17");
    private static final MinecraftVersion V1_17_1 = new MinecraftVersion("1.17.1");
    private static final MinecraftVersion V1_19 = new MinecraftVersion("1.19");
    private static final MinecraftVersion V1_19_3 = new MinecraftVersion("1.19.3");
    private static final MinecraftVersion V1_19_4 = new MinecraftVersion("1.19.4");
    private static final MinecraftVersion V1_20_2 = new MinecraftVersion("1.20.2");

    // 22 в 1.19.4-1.20.1, 23 с 1.20.2
    private static final int BLOCK_DISPLAY_STATE_INDEX = 22;
    private static final int BLOCK_DISPLAY_STATE_INDEX_1_20_2 = 23;

    private final ProtocolManager protocolManager;
    private final MinecraftVersion serverVersion;

    private final EntityType blockDisplayType;

    // отрицательные id не пересекаются с id сущностей сервера
    private final AtomicInteger idCounter = new AtomicInteger(-2);

    public HighlightEntityManager() {
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        this.serverVersion = MinecraftVersion.getCurrentVersion();
        this.blockDisplayType = serverVersion.isAtLeast(V1_19_4) ? findBlockDisplayType() : null;
    }

    private static EntityType findBlockDisplayType() {
        try {
            return EntityType.valueOf("BLOCK_DISPLAY");
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public FakeEntity spawn(Player viewer, Location blockCenter, Material material) {
        int entityId = idCounter.getAndDecrement();
        UUID uuid = UUID.randomUUID();

        // падающий блок не подходит: клиент не рисует его поверх такого же блока мира
        boolean display = blockDisplayType != null && material.isBlock();
        Location location = display
                ? blockCenter.clone().subtract(0.5, 0.5, 0.5)
                : blockCenter.clone().subtract(0, HEAD_BLOCK_CENTER, 0);

        try {
            if (display) {
                protocolManager.sendServerPacket(viewer, buildBlockDisplaySpawnPacket(entityId, uuid, location));
                protocolManager.sendServerPacket(viewer, buildBlockDisplayMetadataPacket(entityId, material));
            } else {
                protocolManager.sendServerPacket(viewer, buildSpawnPacket(entityId, uuid, location));
                protocolManager.sendServerPacket(viewer, buildMetadataPacket(entityId));
                if (material.isItem()) {
                    protocolManager.sendServerPacket(viewer, buildHeadPacket(entityId, material));
                }
            }
        } catch (Exception exception) {
            log.warn("Failed to send fake highlight entity packets to {}", viewer.getName(), exception);
        }

        return new FakeEntity(entityId, uuid, location);
    }

    public void destroy(Player viewer, Collection<FakeEntity> stands) {
        if (stands.isEmpty()) return;

        int[] ids = stands.stream().mapToInt(FakeEntity::getEntityId).toArray();

        try {
            for (PacketContainer packet : buildDestroyPackets(ids)) {
                protocolManager.sendServerPacket(viewer, packet);
            }
        } catch (Exception exception) {
            log.warn("Failed to send entity-destroy packet to {}", viewer.getName(), exception);
        }
    }

    private List<PacketContainer> buildDestroyPackets(int[] ids) {
        List<PacketContainer> packets = new ArrayList<>();

        if (serverVersion.isAtLeast(V1_17_1)) {
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY, true);
            packet.getIntLists().write(0, Arrays.stream(ids).boxed().collect(Collectors.toList()));
            packets.add(packet);
        } else if (serverVersion.isAtLeast(V1_17)) {
            for (int id : ids) {
                PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY, true);
                packet.getIntegers().write(0, id);
                packets.add(packet);
            }
        } else {
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY, true);
            packet.getIntegerArrays().write(0, ids);
            packets.add(packet);
        }

        return packets;
    }

    private PacketContainer buildSpawnPacket(int entityId, UUID uuid, Location location) {
        boolean merged = serverVersion.isAtLeast(V1_19);
        PacketContainer packet = protocolManager.createPacket(
                merged ? PacketType.Play.Server.SPAWN_ENTITY : PacketType.Play.Server.SPAWN_ENTITY_LIVING, true);

        packet.getIntegers().write(0, entityId);
        packet.getUUIDs().write(0, uuid);
        writeArmorStandType(packet);
        packet.getDoubles()
                .write(0, location.getX())
                .write(1, location.getY())
                .write(2, location.getZ());

        byte yaw = angleToByte(location.getYaw());
        byte pitch = angleToByte(location.getPitch());

        packet.getBytes()
                .write(0, merged ? pitch : yaw)
                .write(1, merged ? yaw : pitch)
                .write(2, yaw);

        return packet;
    }

    private PacketContainer buildBlockDisplaySpawnPacket(int entityId, UUID uuid, Location location) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY, true);

        packet.getIntegers().write(0, entityId);
        packet.getUUIDs().write(0, uuid);
        packet.getEntityTypeModifier().write(0, blockDisplayType);
        packet.getDoubles()
                .write(0, location.getX())
                .write(1, location.getY())
                .write(2, location.getZ());

        return packet;
    }

    private PacketContainer buildBlockDisplayMetadataPacket(int entityId, Material material) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA, true);
        packet.getIntegers().write(0, entityId);

        int stateIndex = serverVersion.isAtLeast(V1_20_2) ? BLOCK_DISPLAY_STATE_INDEX_1_20_2 : BLOCK_DISPLAY_STATE_INDEX;
        List<WrappedDataValue> values = new ArrayList<>();
        values.add(new WrappedDataValue(
                FLAGS_INDEX, WrappedDataWatcher.Registry.get(Byte.class), FLAG_GLOWING));
        values.add(new WrappedDataValue(
                stateIndex, WrappedDataWatcher.Registry.getBlockDataSerializer(false),
                WrappedBlockData.createData(material).getHandle()));
        packet.getDataValueCollectionModifier().write(0, values);

        return packet;
    }

    private void writeArmorStandType(PacketContainer packet) {
        if (packet.getEntityTypeModifier().size() > 0) {
            packet.getEntityTypeModifier().write(0, EntityType.ARMOR_STAND);
        } else {
            packet.getIntegers().write(1, ARMOR_STAND_NETWORK_ID_PRE_1_17);
        }
    }

    private PacketContainer buildHeadPacket(int entityId, Material material) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT, true);
        packet.getIntegers().write(0, entityId);
        packet.getSlotStackPairLists().write(0,
                List.of(new Pair<>(EnumWrappers.ItemSlot.HEAD, new ItemStack(material))));
        return packet;
    }

    private PacketContainer buildMetadataPacket(int entityId) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA, true);
        packet.getIntegers().write(0, entityId);
        if (serverVersion.isAtLeast(V1_19_3)) {
            packet.getDataValueCollectionModifier().write(0, buildDataValues());
        } else {
            packet.getWatchableCollectionModifier().write(0, buildMetadata());
        }
        return packet;
    }

    private List<WrappedDataValue> buildDataValues() {
        List<WrappedDataValue> values = new ArrayList<>();
        values.add(new WrappedDataValue(
                FLAGS_INDEX, WrappedDataWatcher.Registry.get(Byte.class), (byte) (FLAG_INVISIBLE | FLAG_GLOWING)));
        values.add(new WrappedDataValue(
                NO_GRAVITY_INDEX, WrappedDataWatcher.Registry.get(Boolean.class), true));
        values.add(new WrappedDataValue(
                ARMOR_STAND_FLAGS_INDEX, WrappedDataWatcher.Registry.get(Byte.class),
                (byte) (ARMOR_STAND_NO_BASEPLATE | ARMOR_STAND_MARKER)));
        return values;
    }

    private List<WrappedWatchableObject> buildMetadata() {
        WrappedDataWatcher watcher = new WrappedDataWatcher();

        watcher.setObject(
                new WrappedDataWatcher.WrappedDataWatcherObject(FLAGS_INDEX, WrappedDataWatcher.Registry.get(Byte.class)),
                (byte) (FLAG_INVISIBLE | FLAG_GLOWING)
        );
        watcher.setObject(
                new WrappedDataWatcher.WrappedDataWatcherObject(NO_GRAVITY_INDEX, WrappedDataWatcher.Registry.get(Boolean.class)),
                true
        );
        watcher.setObject(
                new WrappedDataWatcher.WrappedDataWatcherObject(armorStandFlagsIndex(), WrappedDataWatcher.Registry.get(Byte.class)),
                (byte) (ARMOR_STAND_NO_BASEPLATE | ARMOR_STAND_MARKER)
        );

        return watcher.getWatchableObjects();
    }

    private int armorStandFlagsIndex() {
        return serverVersion.isAtLeast(V1_17) ? ARMOR_STAND_FLAGS_INDEX : ARMOR_STAND_FLAGS_INDEX_PRE_1_17;
    }

    private byte angleToByte(float degrees) {
        return (byte) (degrees * 256.0F / 360.0F);
    }
}
