package com.quietterminal.cattlelog;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.instance.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CowHerd {

    private static final Logger LOGGER = LoggerFactory.getLogger(CowHerd.class);
    private final CowFileManager fileManager;
    private final Map<UUID, Entity> cows = new ConcurrentHashMap<>();

    CowHerd(CowFileManager fileManager) {
        this.fileManager = fileManager;
    }

    public void spawnCow(UUID playerUuid, Instance instance, Pos position, String playerName) {
        removeCow(playerUuid);

        Entity cow = new Entity(EntityType.COW);
        cow.set(DataComponents.CUSTOM_NAME, Component.text(playerName));
        cow.setCustomNameVisible(true);
        cow.setInstance(instance, position);
        cows.put(playerUuid, cow);

        LOGGER.debug("Spawned cow for {} at {}", playerName, position);
    }

    public void removeCow(UUID playerUuid) {
        Entity cow = cows.remove(playerUuid);
        if (cow != null) {
            cow.remove();
        }
    }

    public void summonHerd(Instance instance) {
        int count = 0;
        for (UUID uuid : fileManager.listSavedPlayers()) {
            var optCow = fileManager.load(uuid);
            if (optCow.isEmpty()) continue;

            CompoundBinaryTag data = optCow.get();
            String name = data.getString(CowSchema.CUSTOM_NAME, "Unknown");
            CompoundBinaryTag pasture = data.getCompound(CowSchema.PASTURE);

            if (pasture.keySet().isEmpty()) continue;

            Pos pos = new Pos(
                    pasture.getDouble(CowSchema.POS_X, 0.0),
                    pasture.getDouble(CowSchema.POS_Y, 64.0),
                    pasture.getDouble(CowSchema.POS_Z, 0.0),
                    pasture.getFloat(CowSchema.POS_YAW, 0.0f),
                    pasture.getFloat(CowSchema.POS_PITCH, 0.0f)
            );

            spawnCow(uuid, instance, pos, name);
            count++;
        }
        LOGGER.info("Summoned {} cows to the pasture", count);
    }

    public void disperseHerd() {
        cows.keySet().forEach(this::removeCow);
        LOGGER.info("Dispersed the herd");
    }

    public Entity getCow(UUID playerUuid) {
        return cows.get(playerUuid);
    }

    public int size() {
        return cows.size();
    }
}
