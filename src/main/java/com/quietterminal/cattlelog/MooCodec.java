package com.quietterminal.cattlelog;

import java.util.PriorityQueue;

/**
 * Encodes and decodes arbitrary byte data as Huffman-compressed moo text.
 *
 * <p>Each bit in the Huffman-coded stream is represented as a moo token:
 * {@code moo} for {@code 0} and {@code Moo} for {@code 1}. The resulting
 * {@code .cow} file is a text document consisting entirely of moos.</p>
 *
 * <h2>File format</h2>
 * <pre>
 * ~~ CATTLELOG ~~
 * &lt;original byte count&gt;
 * moo Moo moo Moo ...
 * </pre>
 *
 * <p>The moo stream contains the serialized Huffman tree followed by the
 * encoded data. The tree is written depth-first: an internal node is a
 * single {@code moo} (0-bit) and a leaf node is {@code Moo} (1-bit)
 * followed by eight moo tokens representing the byte value.</p>
 */
public final class MooCodec {

    static final String MAGIC = "~~ CATTLELOG ~~";
    private static final String ZERO = "moo";
    private static final String ONE = "Moo";
    private static final int MOOS_PER_LINE = 32;

    private MooCodec() {}

    static sealed interface Node extends Comparable<Node> permits Leaf, Branch {
        long weight();

        @Override
        default int compareTo(Node other) {
            return Long.compare(weight(), other.weight());
        }
    }

    record Leaf(byte value, long weight) implements Node {}
    record Branch(Node left, Node right, long weight) implements Node {}

    /**
     * Encodes raw bytes into a moo-formatted string.
     *
     * @param data the raw bytes to encode
     * @return the full moo-encoded file content including header
     */
    public static String encode(byte[] data) {
        if (data.length == 0) {
            return MAGIC + "\n0\n";
        }

        long[] freq = buildFrequencyTable(data);
        Node root = buildTree(freq);

        String[] codeTable = new String[256];
        buildCodeTable(root, "", codeTable);

        StringBuilder sb = new StringBuilder();
        sb.append(MAGIC).append('\n');
        sb.append(data.length).append('\n');

        BitCollector bits = new BitCollector();
        serializeTree(root, bits);

        for (byte b : data) {
            String code = codeTable[b & 0xFF];
            for (int i = 0; i < code.length(); i++) {
                bits.add(code.charAt(i) == '1');
            }
        }

        int count = 0;
        for (boolean bit : bits.bits()) {
            if (count > 0) {
                sb.append(count % MOOS_PER_LINE == 0 ? '\n' : ' ');
            }
            sb.append(bit ? ONE : ZERO);
            count++;
        }
        sb.append('\n');

        return sb.toString();
    }

    /**
     * Decodes a moo-formatted string back into raw bytes.
     *
     * @param mooText the moo-encoded file content
     * @return the decoded raw bytes
     * @throws IllegalArgumentException if the format is invalid
     */
    public static byte[] decode(String mooText) {
        String[] lines = mooText.split("\n", 3);
        if (lines.length < 2 || !lines[0].equals(MAGIC)) {
            throw new IllegalArgumentException("Not a valid moo file: missing CATTLELOG header");
        }

        int originalSize;
        try {
            originalSize = Integer.parseInt(lines[1].trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not a valid moo file: bad byte count", e);
        }

        if (originalSize == 0) {
            return new byte[0];
        }

        String mooData = lines.length > 2 ? lines[2] : "";
        String[] tokens = mooData.trim().split("\\s+");
        boolean[] bits = new boolean[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            bits[i] = switch (tokens[i]) {
                case ZERO -> false;
                case ONE -> true;
                default -> throw new IllegalArgumentException(
                        "Unknown moo token: '" + tokens[i] + "' at position " + i);
            };
        }

        BitReader reader = new BitReader(bits);

        Node root = deserializeTree(reader);

        byte[] result = new byte[originalSize];
        for (int i = 0; i < originalSize; i++) {
            result[i] = decodeByte(root, reader);
        }

        return result;
    }

    /**
     * Checks whether the given bytes start with the moo file magic header.
     *
     * @param fileContent the raw file bytes
     * @return {@code true} if this looks like a moo-encoded file
     */
    public static boolean isMooFile(byte[] fileContent) {
        return new String(fileContent).startsWith(MAGIC);
    }

    static long[] buildFrequencyTable(byte[] data) {
        long[] freq = new long[256];
        for (byte b : data) {
            freq[b & 0xFF]++;
        }
        return freq;
    }

    static Node buildTree(long[] freq) {
        PriorityQueue<Node> pq = new PriorityQueue<>();
        for (int i = 0; i < 256; i++) {
            if (freq[i] > 0) {
                pq.add(new Leaf((byte) i, freq[i]));
            }
        }

        if (pq.size() == 1) {
            Node only = pq.poll();
            pq.add(new Branch(only, new Leaf((byte) 0, 0), only.weight()));
        }

        while (pq.size() > 1) {
            Node left = pq.poll();
            Node right = pq.poll();
            pq.add(new Branch(left, right, left.weight() + right.weight()));
        }

        return pq.poll();
    }

    static void buildCodeTable(Node node, String prefix, String[] table) {
        switch (node) {
            case Leaf leaf -> table[leaf.value() & 0xFF] = prefix.isEmpty() ? "0" : prefix;
            case Branch branch -> {
                buildCodeTable(branch.left(), prefix + "0", table);
                buildCodeTable(branch.right(), prefix + "1", table);
            }
        }
    }

    private static void serializeTree(Node node, BitCollector bits) {
        switch (node) {
            case Leaf leaf -> {
                bits.add(true);
                byte v = leaf.value();
                for (int i = 7; i >= 0; i--) {
                    bits.add(((v >> i) & 1) == 1);
                }
            }
            case Branch branch -> {
                bits.add(false); // 0 = internal
                serializeTree(branch.left(), bits);
                serializeTree(branch.right(), bits);
            }
        }
    }

    private static Node deserializeTree(BitReader reader) {
        if (reader.read()) {
            byte value = 0;
            for (int i = 7; i >= 0; i--) {
                if (reader.read()) {
                    value |= (byte) (1 << i);
                }
            }
            return new Leaf(value, 0);
        } else {
            Node left = deserializeTree(reader);
            Node right = deserializeTree(reader);
            return new Branch(left, right, 0);
        }
    }

    private static byte decodeByte(Node root, BitReader reader) {
        Node current = root;
        while (current instanceof Branch branch) {
            current = reader.read() ? branch.right() : branch.left();
        }
        return ((Leaf) current).value();
    }

    static final class BitCollector {
        private boolean[] data = new boolean[1024];
        private int size = 0;

        void add(boolean bit) {
            if (size == data.length) {
                boolean[] grown = new boolean[data.length * 2];
                System.arraycopy(data, 0, grown, 0, size);
                data = grown;
            }
            data[size++] = bit;
        }

        boolean[] bits() {
            boolean[] result = new boolean[size];
            System.arraycopy(data, 0, result, 0, size);
            return result;
        }

        int size() {
            return size;
        }
    }

    static final class BitReader {
        private final boolean[] bits;
        private int pos = 0;

        BitReader(boolean[] bits) {
            this.bits = bits;
        }

        boolean read() {
            if (pos >= bits.length) {
                throw new IllegalArgumentException("Unexpected end of moo stream at position " + pos);
            }
            return bits[pos++];
        }
    }
}
