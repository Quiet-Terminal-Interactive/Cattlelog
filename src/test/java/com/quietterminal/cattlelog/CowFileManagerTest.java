package com.quietterminal.cattlelog;

import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CowFileManagerTest {

    @TempDir
    Path tempBarn;

    @Test
    void saveAndLoad_roundTrip() throws IOException {
        CowFileManager manager = new CowFileManager(tempBarn);
        manager.ensureBarnExists();

        UUID uuid = UUID.randomUUID();
        CompoundBinaryTag data = CompoundBinaryTag.builder()
                .putString("id", "minecraft:cow")
                .putString("CustomName", "Bessie")
                .putFloat("Health", 18.5f)
                .putInt("CattlelogVersion", CowSchema.CURRENT_VERSION)
                .build();

        manager.save(uuid, data);

        Optional<CompoundBinaryTag> loaded = manager.load(uuid);
        assertTrue(loaded.isPresent());
        assertEquals("minecraft:cow", loaded.get().getString("id"));
        assertEquals("Bessie", loaded.get().getString("CustomName"));
        assertEquals(18.5f, loaded.get().getFloat("Health"));
        assertEquals(CowSchema.CURRENT_VERSION, loaded.get().getInt("CattlelogVersion"));
    }

    @Test
    void savedFile_containsMoos() throws IOException {
        CowFileManager manager = new CowFileManager(tempBarn);
        manager.ensureBarnExists();

        UUID uuid = UUID.randomUUID();
        CompoundBinaryTag data = CompoundBinaryTag.builder()
                .putString("id", "minecraft:cow")
                .putString("CustomName", "Daisy")
                .build();

        manager.save(uuid, data);

        String content = Files.readString(manager.cowFilePath(uuid));
        assertTrue(content.startsWith(MooCodec.MAGIC),
                "Cow file should start with CATTLELOG magic header");
        assertTrue(content.contains("moo") || content.contains("Moo"),
                "Cow file should contain moo tokens");
    }

    @Test
    void load_legacyGzipFormat() throws IOException {
        CowFileManager manager = new CowFileManager(tempBarn);
        manager.ensureBarnExists();

        UUID uuid = UUID.randomUUID();
        CompoundBinaryTag data = CompoundBinaryTag.builder()
                .putString("id", "minecraft:cow")
                .putString("CustomName", "OldBessie")
                .putInt("CattlelogVersion", 1)
                .build();

        Path path = manager.cowFilePath(uuid);
        try (OutputStream out = Files.newOutputStream(path)) {
            BinaryTagIO.writer().writeNamed(
                    Map.entry("", data),
                    out,
                    BinaryTagIO.Compression.GZIP
            );
        }

        Optional<CompoundBinaryTag> loaded = manager.load(uuid);
        assertTrue(loaded.isPresent());
        assertEquals("OldBessie", loaded.get().getString("CustomName"));
        assertEquals(1, loaded.get().getInt("CattlelogVersion"));
    }

    @Test
    void load_missingFile_returnsEmpty() {
        CowFileManager manager = new CowFileManager(tempBarn);
        Optional<CompoundBinaryTag> loaded = manager.load(UUID.randomUUID());
        assertTrue(loaded.isEmpty());
    }

    @Test
    void listSavedPlayers_findsFiles() throws IOException {
        CowFileManager manager = new CowFileManager(tempBarn);
        manager.ensureBarnExists();

        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        CompoundBinaryTag data = CompoundBinaryTag.builder()
                .putString("id", "minecraft:cow")
                .build();

        manager.save(uuid1, data);
        manager.save(uuid2, data);

        var players = manager.listSavedPlayers();
        assertEquals(2, players.size());
        assertTrue(players.contains(uuid1));
        assertTrue(players.contains(uuid2));
    }

    @Test
    void nbtToBytes_andBack_roundTrip() throws IOException {
        CompoundBinaryTag original = CompoundBinaryTag.builder()
                .putString("greeting", "moo")
                .putInt("legs", 4)
                .putFloat("milkLevel", 99.9f)
                .build();

        byte[] bytes = CowFileManager.nbtToBytes(original);
        CompoundBinaryTag restored = CowFileManager.bytesToNbt(bytes);

        assertEquals("moo", restored.getString("greeting"));
        assertEquals(4, restored.getInt("legs"));
        assertEquals(99.9f, restored.getFloat("milkLevel"), 0.01f);
    }
}
