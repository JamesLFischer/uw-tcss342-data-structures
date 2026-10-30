// This work was completed by James Fischer and Kalsoom Babakarkhail
public class MyDeque<Type> {

    private DLNode head;
    private DLNode tail;
    private int size;

    /**
     * Constructs an empty deque.
     */
    public MyDeque() {
        head = null;
        tail = null;
        size = 0;
    }

    /**
     * Adds the item to the front of the deque.
     */
    public void addFirst(Type item) {
        DLNode n = new DLNode(item);

        if (size == 0) {
            head = n;
            tail = n;
        } else {
            n.link(head, null);     // new node points next to old head
            head.link(head.next, n); // old head prev becomes new node
            head = n;
        }

        size++;
    }

    /**
     * Adds the item to the back of the deque.
     */
    public void addLast(Type item) {
        DLNode n = new DLNode(item);

        if (size == 0) {
            head = n;
            tail = n;
        } else {
            n.link(null, tail);        // new node prev points to old tail
            tail.link(n, tail.prev);   // old tail next becomes new node
            tail = n;
        }

        size++;
    }

    /**
     * Removes and returns the item at the front of the deque (or null if empty).
     */
    public Type removeFirst() {
        if (size == 0) {
            return null;
        }

        Type removed = head.item;

        if (size == 1) {
            head = null;
            tail = null;
        } else {
            DLNode newHead = head.next;
            newHead.link(newHead.next, null);
            head = newHead;
        }

        size--;
        return removed;
    }

    /**
     * Removes and returns the item at the back of the deque (or null if empty).
     */
    public Type removeLast() {
        if (size == 0) {
            return null;
        }

        Type removed = tail.item;

        if (size == 1) {
            head = null;
            tail = null;
        } else {
            DLNode newTail = tail.prev;
            newTail.link(null, newTail.prev);
            tail = newTail;
        }

        size--;
        return removed;
    }

    /**
     * Returns (without removing) the item at the front of the deque (or null if empty).
     */
    public Type peekFirst() {
        if (size == 0) {
            return null;
        }
        return head.item;
    }

    /**
     * Returns (without removing) the item at the back of the deque (or null if empty).
     */
    public Type peekLast() {
        if (size == 0) {
            return null;
        }
        return tail.item;
    }

    /**
     * Returns the current number of items in the deque.
     */
    public int size() {
        return size;
    }

    /**
     * Converts the contents of the deque to a String for display.
     * Format: [a, b, c]
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        DLNode cur = head;
        while (cur != null) {
            sb.append(cur.item);
            if (cur.next != null) {
                sb.append(", ");
            }
            cur = cur.next;
        }

        sb.append("]");
        return sb.toString();
    }

    /**
     * Local doubly-linked node class (must be local to MyDeque).
     */
    private class DLNode {
        private DLNode next;
        private DLNode prev;
        private Type item;

        /**
         * Creates a node containing the given item.
         */
        DLNode(Type item) {
            this.item = item;
            this.next = null;
            this.prev = null;
        }

        /**
         * Links this node forward and back (either can be null).
         */
        void link(DLNode next, DLNode prev) {
            this.next = next;
            this.prev = prev;
        }
    }
}
