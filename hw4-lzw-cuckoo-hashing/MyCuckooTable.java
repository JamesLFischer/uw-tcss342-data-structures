public class MyCuckooTable<K, V> {

    private static final int lgTABSIZE = 16;
    public static final int TABSIZE = 1 << lgTABSIZE;
    private static final int MaxLoop = 3 * lgTABSIZE;

    private static class Entry<K, V> {
        K key;
        V value;
        Entry(K k, V v) { key = k; value = v; }
    }

    private Entry<K, V>[] T1;
    private Entry<K, V>[] T2;
    private int size;

    // Required helper for SipHash-based h1/h2
    private final SipHash sh = new SipHash();

    // Stats for Main
    private long putCalls;
    private long totalEvictions;
    private int lastPutEvictions;

    @SuppressWarnings("unchecked")
    public MyCuckooTable() {
        T1 = (Entry<K,V>[]) new Entry[TABSIZE];
        T2 = (Entry<K,V>[]) new Entry[TABSIZE];
        size = 0;
        putCalls = 0;
        totalEvictions = 0;
        lastPutEvictions = 0;
    }

    public int size() { return size; }

    public void reset() {
        // No Arrays.fill allowed by spec
        for (int i = 0; i < TABSIZE; i++) {
            T1[i] = null;
            T2[i] = null;
        }
        size = 0;
    }

    /**
     * Update or add. Returns false only when insertion fails (cuckoo loop exceeds MaxLoop).
     */
    public boolean put(K searchKey, V newValue) {
        putCalls++;
        lastPutEvictions = 0;

        // Update if present
        int h1 = hash(searchKey, 1);
        if (T1[h1] != null && T1[h1].key.equals(searchKey)) {
            T1[h1].value = newValue;
            return true;
        }
        int h2 = hash(searchKey, 2);
        if (T2[h2] != null && T2[h2].key.equals(searchKey)) {
            T2[h2].value = newValue;
            return true;
        }

        K key = searchKey;
        V value = newValue;

        for (int loop = 0; loop < MaxLoop; loop++) {

            h1 = hash(key, 1);
            if (T1[h1] == null) {
                T1[h1] = new Entry<>(key, value);
                size++;
                totalEvictions += lastPutEvictions;
                return true;
            }

            // Evict from T1
            Entry<K,V> displaced = T1[h1];
            T1[h1] = new Entry<>(key, value);
            key = displaced.key;
            value = displaced.value;
            lastPutEvictions++;

            h2 = hash(key, 2);
            if (T2[h2] == null) {
                T2[h2] = new Entry<>(key, value);
                size++;
                totalEvictions += lastPutEvictions;
                return true;
            }

            // Evict from T2
            displaced = T2[h2];
            T2[h2] = new Entry<>(key, value);
            key = displaced.key;
            value = displaced.value;
            lastPutEvictions++;
        }

        // Failed insertion
        totalEvictions += lastPutEvictions;
        return false;
    }

    public V get(K searchKey) {
        int h1 = hash(searchKey, 1);
        if (T1[h1] != null && T1[h1].key.equals(searchKey)) return T1[h1].value;

        int h2 = hash(searchKey, 2);
        if (T2[h2] != null && T2[h2].key.equals(searchKey)) return T2[h2].value;

        return null;
    }

    // For stats
    public long getPutCalls() { return putCalls; }
    public long getTotalEvictions() { return totalEvictions; }
    public int getLastPutEvictions() { return lastPutEvictions; }

    private int hash(K key, int fno) {
        String prefix = (fno == 1) ? "1" : "2";
        long h = sh.hash(prefix + key.toString());
        return (int) (h & (TABSIZE - 1));
    }
}