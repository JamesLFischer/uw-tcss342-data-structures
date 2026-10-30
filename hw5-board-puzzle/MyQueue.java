/*
 * TCSS 342 - Assignment 5 - Board Puzzle
 * Queue implementation for BFS.
 */

import java.util.ArrayList;

public class MyQueue<T> {
    private final ArrayList<T> myData;
    private int myFront;

    public MyQueue() {
        myData = new ArrayList<T>();
        myFront = 0;
    }

    public void put(final T theItem) {
        myData.add(theItem);
    }

    public T get() {
        if (size() == 0) {
            return null;
        }
        final T result = myData.get(myFront);
        myFront++;

        if (myFront > 64 && myFront * 2 >= myData.size()) {
            myData.subList(0, myFront).clear();
            myFront = 0;
        }
        return result;
    }

    public int size() {
        return myData.size() - myFront;
    }
}
