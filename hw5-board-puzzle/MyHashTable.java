/*
 * TCSS 342 - Assignment 5 - Board Puzzle
 * Hash table implementation using separate chaining.
 */

import java.util.ArrayList;

public class MyHashTable<K, V> {
    private static final int DEFAULT_CAPACITY = 16;
    private static final double LOAD_FACTOR = 0.75;

    private ArrayList<ArrayList<Entry<K, V>>> myBuckets;
    private int mySize;

    public MyHashTable() {
        initializeBuckets(DEFAULT_CAPACITY);
        mySize = 0;
    }

    public boolean containsKey(final K theKey) {
        return findEntry(theKey) != null;
    }

    public V put(final K theKey, final V theValue) {
        if ((mySize + 1.0) / myBuckets.size() > LOAD_FACTOR) {
            resize();
        }

        final int index = bucketIndex(theKey, myBuckets.size());
        final ArrayList<Entry<K, V>> bucket = myBuckets.get(index);
        for (int i = 0; i < bucket.size(); i++) {
            final Entry<K, V> entry = bucket.get(i);
            if (keysEqual(entry.key, theKey)) {
                final V oldValue = entry.value;
                entry.value = theValue;
                return oldValue;
            }
        }
        bucket.add(new Entry<K, V>(theKey, theValue));
        mySize++;
        return null;
    }

    public V get(final K theKey) {
        final Entry<K, V> entry = findEntry(theKey);
        return entry == null ? null : entry.value;
    }

    public int size() {
        return mySize;
    }

    private void initializeBuckets(final int theCapacity) {
        myBuckets = new ArrayList<ArrayList<Entry<K, V>>>(theCapacity);
        for (int i = 0; i < theCapacity; i++) {
            myBuckets.add(new ArrayList<Entry<K, V>>());
        }
    }

    private Entry<K, V> findEntry(final K theKey) {
        final int index = bucketIndex(theKey, myBuckets.size());
        final ArrayList<Entry<K, V>> bucket = myBuckets.get(index);
        for (int i = 0; i < bucket.size(); i++) {
            final Entry<K, V> entry = bucket.get(i);
            if (keysEqual(entry.key, theKey)) {
                return entry;
            }
        }
        return null;
    }

    private int bucketIndex(final K theKey, final int theBucketCount) {
        final int hash = theKey == null ? 0 : theKey.hashCode();
        return (hash & 0x7fffffff) % theBucketCount;
    }

    private boolean keysEqual(final K theLeft, final K theRight) {
        if (theLeft == theRight) {
            return true;
        }
        if (theLeft == null || theRight == null) {
            return false;
        }
        return theLeft.equals(theRight);
    }

    private void resize() {
        final ArrayList<ArrayList<Entry<K, V>>> oldBuckets = myBuckets;
        initializeBuckets(oldBuckets.size() * 2);
        mySize = 0;
        for (int i = 0; i < oldBuckets.size(); i++) {
            final ArrayList<Entry<K, V>> bucket = oldBuckets.get(i);
            for (int j = 0; j < bucket.size(); j++) {
                final Entry<K, V> entry = bucket.get(j);
                put(entry.key, entry.value);
            }
        }
    }

    private static final class Entry<K, V> {
        private final K key;
        private V value;

        private Entry(final K theKey, final V theValue) {
            key = theKey;
            value = theValue;
        }
    }
}
