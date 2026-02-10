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

/**
 * Manages reading and writing of {@code .cow} NBT files within a barn directory.
 *
 * <p>Each player's data is stored in a GZIP-compressed named binary tag file
 * identified by their UUID (e.g. {@code <uuid>.cow}).</p>
 */
public final class CowFileManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(CowFileManager.class);
    private final Path barnDirectory;

    /**
     * Creates a new file manager targeting the given barn directory.
     *
     * @param barnDirectory the directory where {@code .cow} files are stored
     */
    public CowFileManager(Path barnDirectory) {
        this.barnDirectory = barnDirectory;
    }

    /**
     * Creates the barn directory (and any parent directories) if it does not exist.
     *
     * @throws IOException if the directory cannot be created
     */
    public void ensureBarnExists() throws IOException {
        Files.createDirectories(barnDirectory);
    }

    /**
     * Returns the path to the {@code .cow} file for the given player.
     *
     * @param playerUuid the player's UUID
     * @return the resolved file path
     */
    public Path cowFilePath(UUID playerUuid) {
        return barnDirectory.resolve(playerUuid.toString() + ".cow");
    }

    /**
     * Saves the given NBT data to the player's {@code .cow} file.
     *
     * @param playerUuid the player's UUID
     * @param data       the compound tag to write
     */
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

    /**
     * Loads the NBT data from the player's {@code .cow} file, if it exists.
     *
     * @param playerUuid the player's UUID
     * @return the compound tag, or empty if the file does not exist or is unreadable
     */
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

    /**
     * Lists the UUIDs of all players that have saved {@code .cow} files in the barn.
     *
     * @return a list of player UUIDs; files with invalid names are silently skipped
     */
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
