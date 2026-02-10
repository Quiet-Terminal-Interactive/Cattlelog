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

/**
 * Manages physical cow entities that represent offline players in the world.
 *
 * <p>Each offline player is represented by a named cow entity at their last
 * known position. The cow is removed when the player reconnects.</p>
 */
public final class CowHerd {

    private static final Logger LOGGER = LoggerFactory.getLogger(CowHerd.class);
    private final CowFileManager fileManager;
    private final Map<UUID, Entity> cows = new ConcurrentHashMap<>();

    /**
     * Creates a new herd backed by the given file manager.
     *
     * @param fileManager the file manager used to load saved player data
     */
    CowHerd(CowFileManager fileManager) {
        this.fileManager = fileManager;
    }

    /**
     * Spawns a named cow entity for the given player at the specified position.
     *
     * <p>If a cow already exists for this player, it is removed first.</p>
     *
     * @param playerUuid the player's UUID
     * @param instance   the instance to spawn the cow in
     * @param position   the position to spawn the cow at
     * @param playerName the player's name, displayed above the cow
     */
    public void spawnCow(UUID playerUuid, Instance instance, Pos position, String playerName) {
        removeCow(playerUuid);

        Entity cow = new Entity(EntityType.COW);
        cow.set(DataComponents.CUSTOM_NAME, Component.text(playerName));
        cow.setCustomNameVisible(true);
        cow.setInstance(instance, position);
        cows.put(playerUuid, cow);

        LOGGER.debug("Spawned cow for {} at {}", playerName, position);
    }

    /**
     * Removes and despawns the cow entity for the given player, if one exists.
     *
     * @param playerUuid the player's UUID
     */
    public void removeCow(UUID playerUuid) {
        Entity cow = cows.remove(playerUuid);
        if (cow != null) {
            cow.remove();
        }
    }

    /**
     * Spawns cow entities for all saved players into the given instance.
     *
     * <p>Reads each {@code .cow} file from the barn and spawns a cow at the
     * player's last known position.</p>
     *
     * @param instance the instance to spawn cows in
     */
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

    /**
     * Removes and despawns all cow entities managed by this herd.
     */
    public void disperseHerd() {
        cows.keySet().forEach(this::removeCow);
        LOGGER.info("Dispersed the herd");
    }

    /**
     * Returns the cow entity for the given player, or {@code null} if none exists.
     *
     * @param playerUuid the player's UUID
     * @return the cow entity, or {@code null}
     */
    public Entity getCow(UUID playerUuid) {
        return cows.get(playerUuid);
    }

    /**
     * Returns the number of cow entities currently managed by this herd.
     *
     * @return the herd size
     */
    public int size() {
        return cows.size();
    }
}
