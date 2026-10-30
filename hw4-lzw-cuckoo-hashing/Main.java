import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.out.println("Usage: java Main c|d inputFile outputFile");
            return;
        }

        String mode = args[0];
        String inFile = args[1];
        String outFile = args[2];

        if (mode.equals("c")) {
            runCompress(inFile, outFile);
        } else if (mode.equals("d")) {
            runDecompress(inFile, outFile);
        } else if (mode.equals("test")) {
            testMyCuckooTable();
            testLZW();
        } else {
            System.out.println("First argument must be 'c' or 'd'.");
        }
    }

    private static void runCompress(String inputFile, String outputFile) throws IOException {
        byte[] inputBytes = Files.readAllBytes(Paths.get(inputFile));
        String text = new String(inputBytes);

        LZW lzw = new LZW();

        long t0 = System.currentTimeMillis();
        byte[] compressed = lzw.compress(text);
        long t1 = System.currentTimeMillis();

        Files.write(Paths.get(outputFile), compressed);

        // measure decompression time too (required stat)
        long t2 = System.currentTimeMillis();
        String restored = lzw.decompress(compressed);
        long t3 = System.currentTimeMillis();

        // Print required statistics
        printStats(inputBytes.length, compressed.length, lzw.getLastCompressedBits(),
                   (t1 - t0), (t3 - t2), lzw.getLastCodeCount(),
                   lzw.getLastResetOccupancyPercent(), lzw.getAvgEvictionsPerPut());

        // Optional correctness check
        if (!restored.equals(text)) {
            System.out.println("WARNING: Decompressed text does not match original.");
        }
    }

    private static void runDecompress(String inputFile, String outputFile) throws IOException {
        byte[] compressed = Files.readAllBytes(Paths.get(inputFile));

        LZW lzw = new LZW();

        long t0 = System.currentTimeMillis();
        String text = lzw.decompress(compressed);
        long t1 = System.currentTimeMillis();

        Files.write(Paths.get(outputFile), text.getBytes());

        // To display all required stats, we can also time a recompress in-memory
        long t2 = System.currentTimeMillis();
        byte[] recompressed = lzw.compress(text);
        long t3 = System.currentTimeMillis();

        printStats(text.getBytes().length, compressed.length, ((long)compressed.length) * 8L,
                   (t3 - t2), (t1 - t0), lzw.getLastCodeCount(),
                   lzw.getLastResetOccupancyPercent(), lzw.getAvgEvictionsPerPut());

        // Optional: check round-trip size sanity
        if (recompressed.length == 0 && text.length() > 0) {
            System.out.println("WARNING: recompression produced empty output unexpectedly.");
        }
    }

    private static void printStats(long originalBytes, long compressedBytes, long compressedBits,
                                   long compressMs, long decompressMs, long codeCount,
                                   double occupancyAtResetPct, double avgEvictionsPerPut) {

        double originalKiB = originalBytes / 1024.0;
        double compressedKiB = compressedBytes / 1024.0;

        System.out.println("Original length: " + originalBytes + " bytes (" + format2(originalKiB) + " KiB)");
        System.out.println("Compressed size: " + compressedBits + " bits, " + compressedBytes + " bytes (" + format2(compressedKiB) + " KiB)");

        double ratio = (originalBytes == 0) ? 0.0 : (100.0 * compressedBytes / (double) originalBytes);
        System.out.println("Compression ratio: " + format2(ratio) + "%");

        System.out.println("Elapsed time for compression: " + compressMs + " ms");
        System.out.println("Elapsed time for decompression: " + decompressMs + " ms");

        System.out.println("Total number of LZW codes: " + codeCount);

        System.out.println("Table occupancy at reset: " + format2(occupancyAtResetPct) + "% (of 2*TABSIZE)");
        System.out.println("Average evictions per put(): " + format2(avgEvictionsPerPut));
    }

    private static String format2(double x) {
        long scaled = (long) (x * 100.0 + 0.5);
        long whole = scaled / 100;
        long frac = scaled % 100;
        if (frac < 10) return whole + ".0" + frac;
        return whole + "." + frac;
    }

    // ======== Required tests ========

    public static void testMyCuckooTable() {
        System.out.println("=== testMyCuckooTable ===");
        MyCuckooTable<String, Integer> t = new MyCuckooTable<>();

        // Add 2 * 2^16 keys. Use a simple LCG (no java.util.Random allowed by spec).
        long seed = 123456789L;
        int total = 2 * MyCuckooTable.TABSIZE;

        int inserted = 0;
        for (int i = 0; i < total; i++) {
            seed = (1103515245L * seed + 12345L) & 0x7FFFFFFFL;
            String k = "K" + seed + "_" + i;
            boolean ok = t.put(k, i);
            if (!ok) break;
            inserted++;
        }

        double occ = 100.0 * (t.size() / (2.0 * MyCuckooTable.TABSIZE));
        double avgEv = (t.getPutCalls() == 0) ? 0.0 : (t.getTotalEvictions() / (double) t.getPutCalls());

        System.out.println("Inserted keys: " + inserted);
        System.out.println("Occupation (of 2*TABSIZE): " + format2(occ) + "%");
        System.out.println("Avg evictions per put(): " + format2(avgEv));
        System.out.println();
    }

    public static void testLZW() {
        System.out.println("=== testLZW ===");
        LZW lzw = new LZW();

        String[] tests = new String[]{
                "ABABABAB",
                "TOBEORNOTTOBEORTOBEORNOT",
                "",
                "HELLO HELLO HELLO",
                "AAAAAAAAAAAAAAAAAAAAAA"
        };

        for (int i = 0; i < tests.length; i++) {
            String s = tests[i];
            byte[] c = lzw.compress(s);
            String d = lzw.decompress(c);

            System.out.println("Test " + i + ": len=" + s.length()
                    + " compressedBytes=" + c.length
                    + " codes=" + lzw.getLastCodeCount());

            if (!s.equals(d)) {
                System.out.println("FAIL: mismatch on test " + i);
            } else {
                System.out.println("PASS");
            }
        }

        System.out.println();
    }
}