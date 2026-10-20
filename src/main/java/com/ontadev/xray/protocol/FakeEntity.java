package com.ontadev.xray.protocol;

import lombok.Getter;
import org.bukkit.Location;

import java.util.UUID;

@Getter
public final class FakeEntity {
    private final int entityId;
    private final UUID uuid;
    private final Location location;

    public FakeEntity(int entityId, UUID uuid, Location location) {
        this.entityId = entityId;
        this.uuid = uuid;
        this.location = location;
    }
}
