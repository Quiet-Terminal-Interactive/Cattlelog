package com.quietterminal.cattlelog;

import net.minestom.server.MinecraftServer;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

public final class Cattlelog {

    private static final Logger LOGGER = LoggerFactory.getLogger(Cattlelog.class);
    private static final String DEFAULT_BARN = "barn";
    private static CowFileManager fileManager;

    private Cattlelog() {}

    public static void initialize() {
        initialize(Path.of(DEFAULT_BARN));
    }

    public static void initialize(Path barnDirectory) {
        fileManager = new CowFileManager(barnDirectory);

        try {
            fileManager.ensureBarnExists();
        } catch (IOException e) {
            LOGGER.error("Failed to create barn directory at {}", barnDirectory, e);
            throw new RuntimeException("Cattlelog could not create barn directory", e);
        }

        EventNode<Event> cattlelogNode = EventNode.all("cattlelog");

        cattlelogNode.addListener(PlayerSpawnEvent.class, event -> {
            if (!event.isFirstSpawn()) return;

            var player = event.getPlayer();
            fileManager.load(player.getUuid()).ifPresent(cow -> {
                PlayerDataDeserializer.deserialize(player, cow);
                LOGGER.info("Welcomed {} back to the ranch", player.getUsername());
            });
        });

        cattlelogNode.addListener(PlayerDisconnectEvent.class, event -> {
            var player = event.getPlayer();
            var cow = PlayerDataSerializer.serialize(player);
            fileManager.save(player.getUuid(), cow);
            LOGGER.info("Put {}'s cow out to pasture", player.getUsername());
        });

        MinecraftServer.getGlobalEventHandler().addChild(cattlelogNode);
        LOGGER.info("Cattlelog initialized - barn at {}", barnDirectory.toAbsolutePath());
    }

    public static CowHerd enablePhysicalCows() {
        if (fileManager == null) {
            throw new IllegalStateException("Call Cattlelog.initialize() before enablePhysicalCows()");
        }

        CowHerd herd = new CowHerd(fileManager);

        EventNode<Event> herdNode = EventNode.all("cattlelog-herd");

        herdNode.addListener(PlayerSpawnEvent.class, event -> {
            if (!event.isFirstSpawn()) return;
            herd.removeCow(event.getPlayer().getUuid());
        });

        herdNode.addListener(PlayerDisconnectEvent.class, event -> {
            var player = event.getPlayer();
            if (player.getInstance() != null) {
                herd.spawnCow(player.getUuid(), player.getInstance(),
                        player.getPosition(), player.getUsername());
            }
        });

        MinecraftServer.getGlobalEventHandler().addChild(herdNode);
        LOGGER.info("Physical cows enabled - cows will appear where players log off");
        return herd;
    }
}
