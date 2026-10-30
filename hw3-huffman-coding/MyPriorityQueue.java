import java.util.ArrayList;
import java.util.Comparator;

/**
 * James Fischer
 */
public class MyPriorityQueue<T> {

    private ArrayList<T> heap;
    private Comparator<? super T> comparator;

    public MyPriorityQueue() {
        this(null);
    }

    public MyPriorityQueue(Comparator<? super T> theComparator) {
        heap = new ArrayList<T>();
        comparator = theComparator;
    }

    public boolean offer(T element) {
        if (element == null) {
            throw new NullPointerException();
        }
        heap.add(element);
        siftUp(heap.size() - 1);
        return true;
    }

    public boolean add(T element) {
        return offer(element);
    }

    public T peek() {
        if (heap.isEmpty()) {
            return null;
        }
        return heap.get(0);
    }

    public T poll() {
        if (heap.isEmpty()) {
            return null;
        }
        T result = heap.get(0);
        T last = heap.remove(heap.size() - 1);
        if (!heap.isEmpty()) {
            heap.set(0, last);
            siftDown(0);
        }
        return result;
    }

    public boolean isEmpty() {
        return heap.isEmpty();
    }

    public int size() {
        return heap.size();
    }

    private void siftUp(int index) {
        while (index > 0) {
            int parent = (index - 1) / 2;
            if (compare(heap.get(index), heap.get(parent)) >= 0) {
                break;
            }
            swap(index, parent);
            index = parent;
        }
    }

    private void siftDown(int index) {
        int size = heap.size();
        while (true) {
            int left = index * 2 + 1;
            int right = index * 2 + 2;
            int smallest = index;

            if (left < size && compare(heap.get(left), heap.get(smallest)) < 0) {
                smallest = left;
            }
            if (right < size && compare(heap.get(right), heap.get(smallest)) < 0) {
                smallest = right;
            }
            if (smallest == index) {
                break;
            }
            swap(index, smallest);
            index = smallest;
        }
    }

    private int compare(T a, T b) {
        if (comparator != null) {
            return comparator.compare(a, b);
        }
        @SuppressWarnings("unchecked")
        Comparable<? super T> cmp = (Comparable<? super T>) a;
        return cmp.compareTo(b);
    }

    private void swap(int i, int j) {
        T tmp = heap.get(i);
        heap.set(i, heap.get(j));
        heap.set(j, tmp);
    }
}
