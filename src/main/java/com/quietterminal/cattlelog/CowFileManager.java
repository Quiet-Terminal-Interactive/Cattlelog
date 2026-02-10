package com.quietterminal.cattlelog;

import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
     * Saves the given NBT data to the player's {@code .cow} file using
     * Huffman-compressed moo encoding.
     *
     * @param playerUuid the player's UUID
     * @param data       the compound tag to write
     */
    public void save(UUID playerUuid, CompoundBinaryTag data) {
        Path path = cowFilePath(playerUuid);
        try {
            byte[] nbtBytes = nbtToBytes(data);
            String mooText = MooCodec.encode(nbtBytes);
            Files.writeString(path, mooText);
            LOGGER.debug("Saved cow file for {} ({} bytes -> {} moos)",
                    playerUuid, nbtBytes.length, mooText.length());
        } catch (IOException e) {
            LOGGER.error("Failed to save cow file for {}", playerUuid, e);
        }
    }

    /**
     * Loads the NBT data from the player's {@code .cow} file, if it exists.
     *
     * <p>Supports both the v2 moo-encoded format and the legacy v1 GZIP NBT
     * format for backward compatibility.</p>
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

        try {
            byte[] fileContent = Files.readAllBytes(path);

            byte[] nbtBytes;
            if (MooCodec.isMooFile(fileContent)) {
                String mooText = new String(fileContent);
                nbtBytes = MooCodec.decode(mooText);
                LOGGER.debug("Loaded moo-encoded cow file for {}", playerUuid);
            } else {
                var named = BinaryTagIO.reader().readNamed(
                        new ByteArrayInputStream(fileContent),
                        BinaryTagIO.Compression.GZIP
                );
                LOGGER.debug("Loaded legacy cow file for {}", playerUuid);
                return Optional.of(named.getValue());
            }

            return Optional.of(bytesToNbt(nbtBytes));
        } catch (IOException e) {
            LOGGER.error("Failed to read cow file for {} (corrupted barn?), skipping", playerUuid, e);
            return Optional.empty();
        }
    }

    static byte[] nbtToBytes(CompoundBinaryTag tag) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BinaryTagIO.writer().writeNamed(
                Map.entry("", tag),
                baos,
                BinaryTagIO.Compression.GZIP
        );
        return baos.toByteArray();
    }

    static CompoundBinaryTag bytesToNbt(byte[] bytes) throws IOException {
        var named = BinaryTagIO.reader().readNamed(
                new ByteArrayInputStream(bytes),
                BinaryTagIO.Compression.GZIP
        );
        return named.getValue();
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
