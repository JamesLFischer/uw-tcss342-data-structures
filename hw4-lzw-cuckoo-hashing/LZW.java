public class LZW {

    public LZW() { }

    static final int RESTART = 256;

    private static final int MIN_BITS = 9;
    private static final int MAX_BITS = 16;
    private static final int MAX_CODES = 1 << MAX_BITS; // 65536

    // Stats captured from last compress
    private long lastCodeCount;
    private long lastCompressedBits;
    private double lastResetOccupancyPercent;
    private double avgResetOccupancyPercent;
    private long resetCount;
    private double avgEvictionsPerPut;

    // ========= Public getters for Main statistics =========
    public long getLastCodeCount() { return lastCodeCount; }
    public long getLastCompressedBits() { return lastCompressedBits; }
    public double getLastResetOccupancyPercent() { return lastResetOccupancyPercent; }
    public double getAvgResetOccupancyPercent() { return (resetCount == 0) ? 0.0 : avgResetOccupancyPercent; }
    public double getAvgEvictionsPerPut() { return avgEvictionsPerPut; }

    public byte[] compress(String fullText) {
        if (fullText == null) fullText = "";

        // Initialize dictionary: 0..255 single-byte strings + RESTART (empty string)
        MyCuckooTable<String, Integer> dict = new MyCuckooTable<>();
        for (int i = 0; i < 256; i++) {
            dict.put(String.valueOf((char) i), Integer.valueOf(i));
        }
        dict.put("", Integer.valueOf(RESTART));

        int codeLen = MIN_BITS;
        int nextCode = RESTART + 1; // 257

        // Output bytes buffer (grow manually; no extra classes required)
        byte[] out = new byte[Math.max(32, fullText.length() / 2)];
        int outLen = 0;

        int bitBuffer = 0;
        int bitCount = 0;

        // Reset stats
        lastCodeCount = 0;
        lastCompressedBits = 0;
        lastResetOccupancyPercent = 0.0;
        avgResetOccupancyPercent = 0.0;
        resetCount = 0;

        String w = "";
        for (int i = 0; i < fullText.length(); i++) {
            char ch = fullText.charAt(i);
            String wc = w + ch;

            Integer code = dict.get(wc);
            if (code != null) {
                w = wc;
            } else {
                // emit code for w
                Integer wcode = dict.get(w);
                if (wcode != null) {
                    int val = wcode.intValue();
                    int[] res = writeBits(out, outLen, bitBuffer, bitCount, val, codeLen);
                    out = ensureCapacity(out, res[0]);
                    outLen = res[0];
                    bitBuffer = res[1];
                    bitCount = res[2];
                    lastCodeCount++;
                    lastCompressedBits += codeLen;
                }

                // attempt to add wc
                if (nextCode < MAX_CODES) {
                    boolean ok = dict.put(wc, Integer.valueOf(nextCode));
                    if (!ok) {
                        // record occupancy when forced reset
                        double occ = 100.0 * (dict.size() / (2.0 * MyCuckooTable.TABSIZE));
                        lastResetOccupancyPercent = occ;
                        resetCount++;
                        avgResetOccupancyPercent = avgResetOccupancyPercent + (occ - avgResetOccupancyPercent) / resetCount;

                        // emit RESTART using current codeLen
                        int[] res2 = writeBits(out, outLen, bitBuffer, bitCount, RESTART, codeLen);
                        out = ensureCapacity(out, res2[0]);
                        outLen = res2[0];
                        bitBuffer = res2[1];
                        bitCount = res2[2];
                        lastCodeCount++;
                        lastCompressedBits += codeLen;

                        // reset dictionary + code length
                        dict.reset();
                        for (int j = 0; j < 256; j++) {
                            dict.put(String.valueOf((char) j), Integer.valueOf(j));
                        }
                        dict.put("", Integer.valueOf(RESTART));
                        codeLen = MIN_BITS;
                        nextCode = RESTART + 1;
                    } else {
                        nextCode++;
                        if (nextCode == (1 << codeLen) && codeLen < MAX_BITS) {
                            codeLen++;
                        }
                    }
                }

                w = String.valueOf(ch);
            }
        }

        if (w.length() > 0) {
            Integer wcode = dict.get(w);
            int val = wcode.intValue();
            int[] res = writeBits(out, outLen, bitBuffer, bitCount, val, codeLen);
            out = ensureCapacity(out, res[0]);
            outLen = res[0];
            bitBuffer = res[1];
            bitCount = res[2];
            lastCodeCount++;
            lastCompressedBits += codeLen;
        }

        // flush remaining bits
        if (bitCount > 0) {
            if (outLen >= out.length) out = ensureCapacity(out, outLen + 1);
            out[outLen++] = (byte) ((bitBuffer << (8 - bitCount)) & 0xFF);
        }

        // eviction stats from dict
        long puts = dict.getPutCalls();
        long ev = dict.getTotalEvictions();
        avgEvictionsPerPut = (puts == 0) ? 0.0 : ((double) ev / (double) puts);

        // trim
        byte[] trimmed = new byte[outLen];
        for (int i = 0; i < outLen; i++) trimmed[i] = out[i];
        return trimmed;
    }

    public String decompress(byte[] compressed) {
        if (compressed == null) compressed = new byte[0];

        // Code->String dictionary (only String[] allowed — no HashMap)
        String[] dict = new String[MAX_CODES];
        for (int i = 0; i < 256; i++) {
            dict[i] = String.valueOf((char) i);
        }
        dict[RESTART] = "";

        int codeLen = MIN_BITS;
        int nextCode = RESTART + 1;

        // Bit reader holders
        Holder pos = Holder.of(0);
        Holder buf = Holder.of(0);
        Holder cnt = Holder.of(0);

        // Output: use char[] grown manually — NO StringBuilder
        char[] out = new char[Math.max(32, compressed.length * 2)];
        int outLen = 0;

        // Read first code
        int prevCode = readBits(compressed, codeLen, pos, buf, cnt);
        if (prevCode == -1) return "";

        // Handle RESTART as very first code
        if (prevCode == RESTART) {
            for (int i = 0; i < 256; i++) dict[i] = String.valueOf((char) i);
            dict[RESTART] = "";
            codeLen = MIN_BITS;
            nextCode = RESTART + 1;
            prevCode = readBits(compressed, codeLen, pos, buf, cnt);
            if (prevCode == -1) return "";
        }

        String prevStr = dict[prevCode];
        out = appendStr(out, outLen, prevStr);
        outLen += prevStr.length();

        while (true) {
            int code = readBits(compressed, codeLen, pos, buf, cnt);
            if (code == -1) break;

            if (code == RESTART) {
                // Reset dictionary and code length
                for (int i = 0; i < 256; i++) dict[i] = String.valueOf((char) i);
                dict[RESTART] = "";
                codeLen = MIN_BITS;
                nextCode = RESTART + 1;

                int next = readBits(compressed, codeLen, pos, buf, cnt);
                if (next == -1) break;
                prevStr = dict[next];
                out = appendStr(out, outLen, prevStr);
                outLen += prevStr.length();
                continue;
            }

            String entry;
            if (dict[code] != null) {
                entry = dict[code];
            } else {
                // LZW exception case: code not yet in dict
                entry = prevStr + prevStr.charAt(0);
            }

            out = appendStr(out, outLen, entry);
            outLen += entry.length();

            if (nextCode < MAX_CODES) {
                dict[nextCode++] = prevStr + entry.charAt(0);
                if (nextCode == (1 << codeLen) && codeLen < MAX_BITS) codeLen++;
            }

            prevStr = entry;
        }

        return new String(out, 0, outLen);
    }

    // ======= char[] helper — replaces StringBuilder =======

    private static char[] appendStr(char[] arr, int usedLen, String s) {
        int needed = usedLen + s.length();
        if (needed > arr.length) {
            int newLen = arr.length * 2;
            if (newLen < needed) newLen = needed;
            char[] newArr = new char[newLen];
            for (int i = 0; i < usedLen; i++) newArr[i] = arr[i];
            arr = newArr;
        }
        for (int i = 0; i < s.length(); i++) arr[usedLen + i] = s.charAt(i);
        return arr;
    }

    // ======= Bit packing helpers =======

    private static byte[] ensureCapacity(byte[] arr, int neededLen) {
        if (neededLen <= arr.length) return arr;
        int newLen = arr.length * 2;
        if (newLen < neededLen) newLen = neededLen;
        byte[] n = new byte[newLen];
        for (int i = 0; i < arr.length; i++) n[i] = arr[i];
        return n;
    }

    private static int[] writeBits(byte[] out, int outLen, int bitBuffer, int bitCount, int value, int numBits) {
        bitBuffer = (bitBuffer << numBits) | (value & ((1 << numBits) - 1));
        bitCount += numBits;

        while (bitCount >= 8) {
            int b = (bitBuffer >> (bitCount - 8)) & 0xFF;
            if (outLen >= out.length) out = ensureCapacity(out, outLen + 1);
            out[outLen++] = (byte) b;
            bitCount -= 8;
            bitBuffer = bitBuffer & ((1 << bitCount) - 1);
        }
        return new int[]{outLen, bitBuffer, bitCount};
    }

    private static int readBits(byte[] data, int numBits, Holder pos, Holder bitBuf, Holder bitCnt) {
        int bitBuffer = bitBuf.v;
        int bitCount = bitCnt.v;
        int p = pos.v;

        while (bitCount < numBits) {
            if (p >= data.length) {
                bitBuf.v = bitBuffer;
                bitCnt.v = bitCount;
                pos.v = p;
                return -1;
            }
            bitBuffer = (bitBuffer << 8) | (data[p] & 0xFF);
            p++;
            bitCount += 8;
        }

        int shift = bitCount - numBits;
        int mask = (1 << numBits) - 1;
        int value = (bitBuffer >> shift) & mask;

        bitCount -= numBits;
        bitBuffer = bitBuffer & ((1 << bitCount) - 1);

        bitBuf.v = bitBuffer;
        bitCnt.v = bitCount;
        pos.v = p;
        return value;
    }

    // Tiny mutable int holder to avoid using extra libraries
    private static class Holder {
        int v;
        private Holder(int v) { this.v = v; }
        static Holder of(int v) { return new Holder(v); }
    }
}