package com.quietterminal.cattlelog;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MooCodecTest {

    @Test
    void roundTrip_helloWorld() {
        byte[] original = "Hello, World!".getBytes(StandardCharsets.UTF_8);
        String moo = MooCodec.encode(original);
        byte[] decoded = MooCodec.decode(moo);
        assertArrayEquals(original, decoded);
    }

    @Test
    void roundTrip_singleByte() {
        byte[] original = {42};
        String moo = MooCodec.encode(original);
        byte[] decoded = MooCodec.decode(moo);
        assertArrayEquals(original, decoded);
    }

    @Test
    void roundTrip_emptyData() {
        byte[] original = {};
        String moo = MooCodec.encode(original);
        byte[] decoded = MooCodec.decode(moo);
        assertArrayEquals(original, decoded);
    }

    @Test
    void roundTrip_allSameByte() {
        byte[] original = new byte[100];
        java.util.Arrays.fill(original, (byte) 0xAB);
        String moo = MooCodec.encode(original);
        byte[] decoded = MooCodec.decode(moo);
        assertArrayEquals(original, decoded);
    }

    @Test
    void roundTrip_allByteValues() {
        byte[] original = new byte[256];
        for (int i = 0; i < 256; i++) {
            original[i] = (byte) i;
        }
        String moo = MooCodec.encode(original);
        byte[] decoded = MooCodec.decode(moo);
        assertArrayEquals(original, decoded);
    }

    @Test
    void roundTrip_randomData() {
        Random rng = new Random(0xC0FFEE);
        byte[] original = new byte[4096];
        rng.nextBytes(original);
        String moo = MooCodec.encode(original);
        byte[] decoded = MooCodec.decode(moo);
        assertArrayEquals(original, decoded);
    }

    @Test
    void roundTrip_twoBytes() {
        byte[] original = {0, 1};
        String moo = MooCodec.encode(original);
        byte[] decoded = MooCodec.decode(moo);
        assertArrayEquals(original, decoded);
    }

    @Test
    void encode_containsMagicHeader() {
        byte[] data = "cow".getBytes(StandardCharsets.UTF_8);
        String moo = MooCodec.encode(data);
        assertTrue(moo.startsWith(MooCodec.MAGIC));
    }

    @Test
    void encode_containsOnlyMoos() {
        byte[] data = "some test data for the cow".getBytes(StandardCharsets.UTF_8);
        String moo = MooCodec.encode(data);
        String[] lines = moo.split("\n");
        for (int i = 2; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            for (String token : line.split("\\s+")) {
                assertTrue(token.equals("moo") || token.equals("Moo"),
                        "Unexpected token: '" + token + "'");
            }
        }
    }

    @Test
    void encode_emptyData_hasNoMoos() {
        String moo = MooCodec.encode(new byte[0]);
        assertEquals(MooCodec.MAGIC + "\n0\n", moo);
    }

    @Test
    void isMooFile_detectsMooFormat() {
        byte[] data = "test".getBytes(StandardCharsets.UTF_8);
        String moo = MooCodec.encode(data);
        assertTrue(MooCodec.isMooFile(moo.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void isMooFile_rejectsGzipData() {
        byte[] gzip = {0x1f, (byte) 0x8b, 0x08, 0x00};
        assertFalse(MooCodec.isMooFile(gzip));
    }

    @Test
    void decode_rejectsInvalidHeader() {
        assertThrows(IllegalArgumentException.class,
                () -> MooCodec.decode("not a moo file"));
    }

    @Test
    void decode_rejectsUnknownToken() {
        String bad = MooCodec.MAGIC + "\n1\nMOO\n";
        assertThrows(IllegalArgumentException.class,
                () -> MooCodec.decode(bad));
    }

    @Test
    void huffmanCompression_isEffective() {
        byte[] data = new byte[1000];
        java.util.Arrays.fill(data, (byte) 'A');
        String moo = MooCodec.encode(data);
        long mooCount = moo.lines()
                .skip(2)
                .flatMap(line -> java.util.Arrays.stream(line.trim().split("\\s+")))
                .filter(t -> t.equals("moo") || t.equals("Moo"))
                .count();
        assertTrue(mooCount < 4000,
                "Expected compression for repetitive data, got " + mooCount + " moos");
    }

    @Test
    void buildFrequencyTable_countsCorrectly() {
        byte[] data = {1, 2, 2, 3, 3, 3};
        long[] freq = MooCodec.buildFrequencyTable(data);
        assertEquals(1, freq[1]);
        assertEquals(2, freq[2]);
        assertEquals(3, freq[3]);
        assertEquals(0, freq[0]);
    }

    @Test
    void buildTree_singleValue_producesValidTree() {
        long[] freq = new long[256];
        freq[42] = 10;
        MooCodec.Node root = MooCodec.buildTree(freq);
        assertInstanceOf(MooCodec.Branch.class, root);
    }

    @Test
    void buildCodeTable_assignsCodeToEveryPresentByte() {
        byte[] data = {10, 20, 30};
        long[] freq = MooCodec.buildFrequencyTable(data);
        MooCodec.Node root = MooCodec.buildTree(freq);
        String[] table = new String[256];
        MooCodec.buildCodeTable(root, "", table);
        assertNotNull(table[10]);
        assertNotNull(table[20]);
        assertNotNull(table[30]);
        assertNull(table[0]);
    }
}
