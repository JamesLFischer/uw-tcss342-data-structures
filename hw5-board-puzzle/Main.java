/*
 * TCSS 342 - Assignment 5 - Board Puzzle
 * Controller and manual tests.
 */

public class Main {
    public static void main(final String[] args) {
        final Main driver = new Main();
        driver.testBFS();
        System.out.println();
        driver.testPuzzleSolver();

        if (args.length >= 2) {
            final int n = Integer.parseInt(args[0]);
            final int h = Integer.parseInt(args[1]);
            final boolean debug = args.length >= 3 && Boolean.parseBoolean(args[2]);
            System.out.println();
            System.out.println("Random puzzle requested from command line:");
            final PuzzleSolver solver = new PuzzleSolver();
            solver.randomize(n, h, debug);
            solver.solve(debug);
        }
    }

    public void testBFS() {
        System.out.println("=== testBFS ===");

        final MyQueue<String> queue = new MyQueue<String>();
        queue.put("A");
        queue.put("B");
        queue.put("C");
        System.out.println("Queue size after 3 inserts: " + queue.size());
        System.out.println("Queue get #1: " + queue.get());
        System.out.println("Queue get #2: " + queue.get());
        System.out.println("Queue size now: " + queue.size());

        final MyHashTable<String, Integer> table = new MyHashTable<String, Integer>();
        table.put("start", 0);
        table.put("middle", 1);
        table.put("goal", 2);
        System.out.println("Hash table contains 'middle': " + table.containsKey("middle"));
        System.out.println("Hash table get 'goal': " + table.get("goal"));
        System.out.println("Hash table size: " + table.size());
    }

    public void testPuzzleSolver() {
        System.out.println("=== testPuzzleSolver ===");

        final PuzzleSolver solver = new PuzzleSolver();
        solver.specify(
            new int[] {2, 1, 4, 0, 3},
            new int[] {0, 3, 1, 0, 2},
            new int[] {22, 34},
            new int[] {13, 25, 31, 43, 52},
            false
        );
        solver.solve(true);

        System.out.println();
        System.out.println("=== random puzzle demo ===");
        final PuzzleSolver randomSolver = new PuzzleSolver();
        randomSolver.randomize(5, 2, false);
        randomSolver.solve(false);
    }
}
