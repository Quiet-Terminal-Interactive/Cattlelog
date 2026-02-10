package com.quietterminal.cattlelog;

import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public final class CowFileManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(CowFileManager.class);
    private final Path barnDirectory;

    public CowFileManager(Path barnDirectory) {
        this.barnDirectory = barnDirectory;
    }

    public void ensureBarnExists() throws IOException {
        Files.createDirectories(barnDirectory);
    }

    public Path cowFilePath(UUID playerUuid) {
        return barnDirectory.resolve(playerUuid.toString() + ".cow");
    }

    public void save(UUID playerUuid, CompoundBinaryTag data) {
        Path path = cowFilePath(playerUuid);
        try (OutputStream out = Files.newOutputStream(path)) {
            BinaryTagIO.writer().writeNamed(
                    java.util.Map.entry("", data),
                    out,
                    BinaryTagIO.Compression.GZIP
            );
            LOGGER.debug("Saved cow file for {}", playerUuid);
        } catch (IOException e) {
            LOGGER.error("Failed to save cow file for {}", playerUuid, e);
        }
    }

    public Optional<CompoundBinaryTag> load(UUID playerUuid) {
        Path path = cowFilePath(playerUuid);
        if (!Files.exists(path)) {
            LOGGER.debug("No cow file found for {} (first time on the ranch)", playerUuid);
            return Optional.empty();
        }

        try (InputStream in = Files.newInputStream(path)) {
            var named = BinaryTagIO.reader().readNamed(in, BinaryTagIO.Compression.GZIP);
            LOGGER.debug("Loaded cow file for {}", playerUuid);
            return Optional.of(named.getValue());
        } catch (IOException e) {
            LOGGER.error("Failed to read cow file for {} (corrupted barn?), skipping", playerUuid, e);
            return Optional.empty();
        }
    }

    public List<UUID> listSavedPlayers() {
        List<UUID> uuids = new ArrayList<>();
        try (Stream<Path> files = Files.list(barnDirectory)) {
            files.filter(p -> p.toString().endsWith(".cow")).forEach(p -> {
                String name = p.getFileName().toString();
                name = name.substring(0, name.length() - 4);
                try {
                    uuids.add(UUID.fromString(name));
                } catch (IllegalArgumentException ignored) {
                }
            });
        } catch (IOException e) {
            LOGGER.error("Failed to list cow files in barn", e);
        }
        return uuids;
    }
}
