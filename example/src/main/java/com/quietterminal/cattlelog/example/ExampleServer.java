package com.quietterminal.cattlelog.example;

import com.quietterminal.cattlelog.Cattlelog;
import com.quietterminal.cattlelog.CowHerd;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.generator.GenerationUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * A minimal Minestom server that demonstrates Cattlelog integration.
 *
 * <p>This server creates a flat grass world, initializes Cattlelog for
 * player data persistence, and optionally spawns physical cow entities
 * representing saved players.</p>
 *
 * <p>What Cattlelog handles automatically once initialized:</p>
 * <ul>
 *   <li>Saving player data (inventory, position, health, XP, game mode) on disconnect</li>
 *   <li>Restoring player data on join</li>
 * </ul>
 */
public class ExampleServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExampleServer.class);
    private static final Pos SPAWN = new Pos(0, 42, 0);

    public static void main(String[] args) {
        MinecraftServer server = MinecraftServer.init();

        // --- World setup ---
        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        InstanceContainer world = instanceManager.createInstanceContainer();
        world.setGenerator(ExampleServer::generateFlat);

        // --- Cattlelog setup ---
        // Initialize with a custom barn directory (or omit the path for the default "barn" folder)
        Cattlelog.initialize(Path.of("player-data"));
        LOGGER.info("Cattlelog initialized — player data will be saved to player-data/");

        // Enable physical cows (optional). Each saved player gets a cow entity in the world.
        CowHerd herd = Cattlelog.enablePhysicalCows();

        // --- Player join handling ---
        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, event -> {
            event.setSpawningInstance(world);
            event.getPlayer().setRespawnPoint(SPAWN);
        });

        // Summon the herd once a player joins so the cows appear in the world
        MinecraftServer.getGlobalEventHandler().addListener(PlayerSpawnEvent.class, event -> {
            if (event.isFirstSpawn()) {
                event.getPlayer().setGameMode(GameMode.SURVIVAL);
                herd.summonHerd(event.getInstance());
            }
        });

        // --- Start ---
        server.start("0.0.0.0", 25565);
        LOGGER.info("Server started on port 25565 — connect with Minecraft 1.21.1");
    }

    /**
     * Generates a flat world: bedrock at y=0, dirt y=1-3, grass block at y=4.
     */
    private static void generateFlat(GenerationUnit unit) {
        unit.modifier().fillHeight(0, 1, Block.BEDROCK);
        unit.modifier().fillHeight(1, 40, Block.DIRT);
        unit.modifier().fillHeight(40, 41, Block.GRASS_BLOCK);
    }
}
