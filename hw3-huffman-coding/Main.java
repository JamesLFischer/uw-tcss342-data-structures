import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * James Fischer 
 */
public class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            
            testCodingTree();
            return;
        }

        String inputFileName = args[0];

        try {
            // Read input text
            String message = readFileToString(inputFileName);

            // Original file size in bytes (from disk)
            File inputFile = new File(inputFileName);
            long originalBytes = inputFile.length();

            // Compress
            long startCompress = System.currentTimeMillis();
            CodingTree tree = new CodingTree(message);
            long endCompress = System.currentTimeMillis();
            long compressMillis = endCompress - startCompress;

            // Write codes
            writeCodes("codes.txt", tree.codes);

            // Write compressed binary file
            writeCompressed("compressed.bin", tree.bits);

            // Compressed file size in bytes (from disk)
            File compressedFile = new File("compressed.bin");
            long compressedBytes = compressedFile.length();

            // Decompress from compressed.bin using the same codes
            long startDecompress = System.currentTimeMillis();
            List<Byte> compressedBits = readCompressed("compressed.bin");
            String decoded = tree.decode(compressedBits, tree.codes,
                                        tree.getOriginalLength());
            long endDecompress = System.currentTimeMillis();
            long decompressMillis = endDecompress - startDecompress;

            // Write decompressed text to a file
            writeStringToFile("decompressed.txt", decoded);

            // Decompressed file size in bytes (from disk)
            File decompressedFile = new File("decompressed.txt");
            long decompressedBytes = decompressedFile.length();

            // Compare original and decoded in memory
            boolean match = message.equals(decoded);

            // Compute sizes in KiB
            double originalKiB = originalBytes / 1024.0;
            double compressedKiB = compressedBytes / 1024.0;
            double decompressedKiB = decompressedBytes / 1024.0;

            // Compression ratio as integer percentage (truncated)
            int ratioPercent = 0;
            if (originalBytes > 0) {
                ratioPercent = (int) (compressedBytes * 100 / originalBytes);
            }

            // Print statistics in the same style as the instructor's sample
            System.out.printf("Original file size: %d bytes (%.3f KiB)%n",
                              originalBytes, originalKiB);
            System.out.printf("Compressed file size: %d bytes (%.3f KiB)%n",
                              compressedBytes, compressedKiB);
            System.out.printf("Compression ratio: %d%%%n", ratioPercent);
            System.out.printf("Running time: %.1f milliseconds%n",
                              (double) compressMillis);
            System.out.printf("Decompressed size: %d bytes (%.3f KiB) -- running time: %.1f milliseconds%n",
                              decompressedBytes, decompressedKiB, (double) decompressMillis);
            System.out.printf("Decompressed == original ? %b%n", match);

        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("Runtime error: " + e.getMessage());
        }
    }

    /**
     * Required by the assignment: test the CodingTree on a short phrase
     * so you can verify correctness.
     */
    public static void testCodingTree() {
        String test = "hello huffman";
        System.out.println("=== testCodingTree() ===");
        System.out.println("Original message: " + test);

        CodingTree tree = new CodingTree(test);

        System.out.println("Codes:");
        for (Map.Entry<Character, String> entry : tree.codes.entrySet()) {
            System.out.println("'" + printableChar(entry.getKey()) + "' = " + entry.getValue());
        }

        String decoded = tree.decode(tree.bits, tree.codes, tree.getOriginalLength());
        System.out.println("Decoded message: " + decoded);
        System.out.println("Decoded == original ? " + test.equals(decoded));
        System.out.println("=========================");
    }

    private static String readFileToString(String fileName) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(fileName));
            int c;
            while ((c = reader.read()) != -1) {
                sb.append((char) c);
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return sb.toString();
    }


    private static void writeCodes(String fileName, Map<Character, String> codes) throws IOException {
        PrintWriter out = null;
        try {
            out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(fileName)));
            for (Map.Entry<Character, String> entry : codes.entrySet()) {
                char ch = entry.getKey();
                String code = entry.getValue();
                out.println(printableChar(ch) + "=" + code);
            }
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    /**
     * Convert a character to a printable representation for codes.txt.
     */
    private static String printableChar(char ch) {
        switch (ch) {
            case '\n':
                return "\\n";
            case '\r':
                return "\\r";
            case '\t':
                return "\\t";
            case ' ':
                return "space";
            default:
                if (Character.isISOControl(ch)) {
                    return String.format("0x%04x", (int) ch);
                } else {
                    return Character.toString(ch);
                }
        }
    }

    /**
     * Write the packed Huffman bits to a binary file.
     */
    private static void writeCompressed(String fileName, List<Byte> bits) throws IOException {
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(fileName));
            byte[] buffer = new byte[bits.size()];
            for (int i = 0; i < bits.size(); i++) {
                buffer[i] = bits.get(i);
            }
            out.write(buffer);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    /**
     * Read all bytes from the binary compressed file into a List<Byte>.
     */
    private static List<Byte> readCompressed(String fileName) throws IOException {
        List<Byte> result = new ArrayList<Byte>();
        InputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(fileName));
            int value;
            while ((value = in.read()) != -1) {
                result.add((byte) value);
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return result;
    }

    /**
     * Write a String to a text file.
     */
    private static void writeStringToFile(String fileName, String text) throws IOException {
        PrintWriter out = null;
        try {
            out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(fileName)));
            out.print(text);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
}
