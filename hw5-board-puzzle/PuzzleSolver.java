/*
 * TCSS 342 - Assignment 5 - Board Puzzle
 * Puzzle solver using bidirectional breadth-first search.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class PuzzleSolver {
    public int[] rowdisp;
    public int[] coldisp;
    public int[] obsts;
    public int[] aliens;

    private int myN;
    private final Random myRandom;

    public PuzzleSolver() {
        myRandom = new Random();
    }

    public void randomize(final int theN, final int theH, final boolean theDebug) {
        if (theN < 1) {
            throw new IllegalArgumentException("Board size must be positive.");
        }
        if (theH < 0 || theH > theN * theN - theN) {
            throw new IllegalArgumentException("Invalid obstacle count.");
        }
        myN = theN;
        rowdisp = new int[myN];
        coldisp = new int[myN];
        obsts = new int[theH];
        aliens = new int[myN];

        for (int i = 0; i < myN; i++) {
            rowdisp[i] = myRandom.nextInt(myN);
            coldisp[i] = myRandom.nextInt(myN);
        }

        boolean[][] obstacleBoard = new boolean[myN + 1][myN + 1];
        int count = 0;
        while (count < theH) {
            final int row = 1 + myRandom.nextInt(myN);
            final int col = 1 + myRandom.nextInt(myN);
            if (isAntiDiagonal(row, col) || obstacleBoard[row][col]) {
                continue;
            }
            obstacleBoard[row][col] = true;
            obsts[count] = encode(row, col);
            count++;
        }

        final int[] target = buildTargetAliens(myN);
        final int[] scrambled = scrambleFromTarget(target, obstacleBoard, theDebug);
        aliens = scrambled;

        Arrays.sort(obsts);
        Arrays.sort(aliens);

        if (theDebug) {
            System.out.println("Random puzzle created:");
            displayBoard(aliens);
        }
    }

    public void specify(final int[] theRowdisp, final int[] theColdisp, final int[] theObsts,
                        final int[] theAliens, final boolean theDebug) {
        if (theRowdisp == null || theColdisp == null || theAliens == null || theObsts == null) {
            throw new IllegalArgumentException("Input arrays must not be null.");
        }
        if (theRowdisp.length != theColdisp.length || theAliens.length != theRowdisp.length) {
            throw new IllegalArgumentException("Board size mismatch.");
        }
        myN = theRowdisp.length;
        rowdisp = Arrays.copyOf(theRowdisp, theRowdisp.length);
        coldisp = Arrays.copyOf(theColdisp, theColdisp.length);
        obsts = Arrays.copyOf(theObsts, theObsts.length);
        aliens = Arrays.copyOf(theAliens, theAliens.length);
        validateBoard();
        Arrays.sort(obsts);
        Arrays.sort(aliens);

        if (theDebug) {
            System.out.println("Specified puzzle loaded:");
            displayBoard(aliens);
        }
    }

    public void solve(final boolean theDebug) {
        ensureInitialized();
        final long startTime = System.nanoTime();
        final int[] targetAliens = buildTargetAliens(myN);
        final String startKey = stateKey(aliens);
        final String goalKey = stateKey(targetAliens);

        if (startKey.equals(goalKey)) {
            System.out.println("shift summary: ");
            System.out.println("graph size at completion: 1-> <-1 (total: 2 vertices visited)");
            System.out.println("total distance: 0 + 0 = 0");
            final double elapsed = (System.nanoTime() - startTime) / 1_000_000_000.0;
            System.out.printf("elapsed time for closure: %.3fs.%n", elapsed);
            if (theDebug) {
                displayBoard(aliens);
            }
            return;
        }

        final MyQueue<StateRecord> forwardQueue = new MyQueue<StateRecord>();
        final MyQueue<StateRecord> backwardQueue = new MyQueue<StateRecord>();
        final MyHashTable<String, VisitRecord> forwardSeen = new MyHashTable<String, VisitRecord>();
        final MyHashTable<String, VisitRecord> backwardSeen = new MyHashTable<String, VisitRecord>();

        final StateRecord forwardStart = new StateRecord(Arrays.copyOf(aliens, aliens.length), null, null, 0);
        final StateRecord backwardStart = new StateRecord(Arrays.copyOf(targetAliens, targetAliens.length), null, null, 0);
        forwardQueue.put(forwardStart);
        backwardQueue.put(backwardStart);
        forwardSeen.put(startKey, new VisitRecord(forwardStart));
        backwardSeen.put(goalKey, new VisitRecord(backwardStart));

        StateRecord meetForward = null;
        StateRecord meetBackward = null;
        boolean found = false;

        while (!found && (forwardQueue.size() > 0 || backwardQueue.size() > 0)) {
            found = expandFrontier(forwardQueue, forwardSeen, backwardSeen, true, theDebug);
            if (found) {
                final Meeting meeting = myLastMeeting;
                meetForward = meeting.fromThisSide;
                meetBackward = meeting.fromOtherSide;
                break;
            }

            found = expandFrontier(backwardQueue, backwardSeen, forwardSeen, false, theDebug);
            if (found) {
                final Meeting meeting = myLastMeeting;
                meetBackward = meeting.fromThisSide;
                meetForward = meeting.fromOtherSide;
                break;
            }
        }

        final double elapsed = (System.nanoTime() - startTime) / 1_000_000_000.0;

        if (!found) {
            System.out.println("No solution exists.");
            System.out.println("graph size at completion: " + forwardSeen.size() + "-> <-"
                + backwardSeen.size() + " (total: " + (forwardSeen.size() + backwardSeen.size())
                + " vertices visited)");
            System.out.println("total distance: unavailable");
            System.out.printf("elapsed time for closure: %.3fs.%n", elapsed);
            return;
        }

        final ArrayList<String> solution = buildSolution(meetForward, meetBackward);
        final int forwardDistance = meetForward.depth;
        final int backwardDistance = meetBackward.depth;
        System.out.println("distance from root to intersection: " + forwardDistance);
        System.out.println("distance from intersection to goal: " + backwardDistance);
        System.out.println("================================");

        if (theDebug) {
            displayBoard(aliens);
            int[] current = Arrays.copyOf(aliens, aliens.length);
            for (int i = 0; i < solution.size(); i++) {
                final String move = solution.get(i);
                System.out.println("#### shift " + (move.startsWith("R") ? "row " : "col ")
                    + move.substring(1));
                current = applyNamedMove(current, move, true);
                displayBoard(current);
            }
        }

        System.out.println("shift summary: " + joinMoves(solution));
        System.out.println("graph size at completion: " + forwardSeen.size() + "-> <-"
            + backwardSeen.size() + " (total: " + (forwardSeen.size() + backwardSeen.size())
            + " vertices visited)");
        System.out.println("total distance: " + forwardDistance + " + " + backwardDistance
            + " = " + solution.size());
        System.out.printf("elapsed time for closure: %.3fs.%n", elapsed);
    }

    private Meeting myLastMeeting;

    private boolean expandFrontier(final MyQueue<StateRecord> theQueue,
                                   final MyHashTable<String, VisitRecord> theThisSeen,
                                   final MyHashTable<String, VisitRecord> theOtherSeen,
                                   final boolean theForwardDirection,
                                   final boolean theDebug) {
        final int nodesThisLayer = theQueue.size();
        for (int count = 0; count < nodesThisLayer; count++) {
            final StateRecord current = theQueue.get();
            if (current == null) {
                continue;
            }
            for (int i = 1; i <= myN; i++) {
                final int[] rowState = moveState(current.aliens, true, i, theForwardDirection ? 1 : -1);
                if (rowState != null) {
                    final String rowKey = stateKey(rowState);
                    if (!theThisSeen.containsKey(rowKey)) {
                        final StateRecord next = new StateRecord(rowState, current, "R" + i, current.depth + 1);
                        theThisSeen.put(rowKey, new VisitRecord(next));
                        theQueue.put(next);
                        if (theOtherSeen.containsKey(rowKey)) {
                            myLastMeeting = new Meeting(next, theOtherSeen.get(rowKey).record);
                            return true;
                        }
                    }
                }

                final int[] colState = moveState(current.aliens, false, i, theForwardDirection ? 1 : -1);
                if (colState != null) {
                    final String colKey = stateKey(colState);
                    if (!theThisSeen.containsKey(colKey)) {
                        final StateRecord next = new StateRecord(colState, current, "C" + i, current.depth + 1);
                        theThisSeen.put(colKey, new VisitRecord(next));
                        theQueue.put(next);
                        if (theOtherSeen.containsKey(colKey)) {
                            myLastMeeting = new Meeting(next, theOtherSeen.get(colKey).record);
                            return true;
                        }
                    }
                }
            }
        }
        if (theDebug) {
            // Debug hook intentionally light so output stays readable.
        }
        return false;
    }

    private ArrayList<String> buildSolution(final StateRecord theForwardMeet,
                                            final StateRecord theBackwardMeet) {
        final ArrayList<String> forwardMoves = new ArrayList<String>();
        StateRecord cursor = theForwardMeet;
        while (cursor != null && cursor.moveFromParent != null) {
            forwardMoves.add(0, cursor.moveFromParent);
            cursor = cursor.parent;
        }

        final ArrayList<String> backwardMoves = new ArrayList<String>();
        cursor = theBackwardMeet;
        while (cursor != null && cursor.moveFromParent != null) {
            backwardMoves.add(invertMove(cursor.moveFromParent));
            cursor = cursor.parent;
        }

        forwardMoves.addAll(backwardMoves);
        return forwardMoves;
    }

    private String invertMove(final String theMove) {
        return theMove;
    }

    private String joinMoves(final ArrayList<String> theMoves) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < theMoves.size(); i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(theMoves.get(i));
        }
        return sb.toString();
    }

    private int[] buildTargetAliens(final int theN) {
        final int[] target = new int[theN];
        for (int row = 1; row <= theN; row++) {
            target[row - 1] = encode(row, theN - row + 1);
        }
        Arrays.sort(target);
        return target;
    }

    private int[] scrambleFromTarget(final int[] theTarget, final boolean[][] theObstacleBoard,
                                     final boolean theDebug) {
        int[] current = Arrays.copyOf(theTarget, theTarget.length);
        final int scrambleSteps = Math.max(10, myN * myN * 2);
        int successful = 0;
        int guard = 0;
        while (successful < scrambleSteps && guard < scrambleSteps * 20) {
            guard++;
            final boolean rowMove = myRandom.nextBoolean();
            final int index = 1 + myRandom.nextInt(myN);
            final int[] next = moveState(current, rowMove, index, -1);
            if (next != null) {
                current = next;
                successful++;
                if (theDebug) {
                    System.out.println("scramble step " + successful + ": "
                        + (rowMove ? "R" : "C") + index);
                }
            }
        }
        return current;
    }

    private int[] applyNamedMove(final int[] theState, final String theMove, final boolean theForward) {
        final boolean rowMove = theMove.charAt(0) == 'R';
        final int index = Integer.parseInt(theMove.substring(1));
        return moveState(theState, rowMove, index, theForward ? 1 : -1);
    }

    private int[] moveState(final int[] theState, final boolean theRowMove, final int theIndex,
                            final int theDirection) {
        final int delta = (theRowMove ? rowdisp[theIndex - 1] : coldisp[theIndex - 1]) * theDirection;
        if (mod(delta, myN) == 0) {
            return null;
        }

        final boolean[][] occupied = new boolean[myN + 1][myN + 1];
        for (int i = 0; i < theState.length; i++) {
            occupied[rowOf(theState[i])][colOf(theState[i])] = true;
        }

        boolean foundPieceOnLine = false;
        final int[] moved = Arrays.copyOf(theState, theState.length);
        for (int i = 0; i < moved.length; i++) {
            final int row = rowOf(moved[i]);
            final int col = colOf(moved[i]);
            if (theRowMove && row == theIndex) {
                foundPieceOnLine = true;
                final int newCol = mod(col - 1 + delta, myN) + 1;
                if (isObstacle(row, newCol)) {
                    return null;
                }
                moved[i] = encode(row, newCol);
            } else if (!theRowMove && col == theIndex) {
                foundPieceOnLine = true;
                final int newRow = mod(row - 1 + delta, myN) + 1;
                if (isObstacle(newRow, col)) {
                    return null;
                }
                moved[i] = encode(newRow, col);
            }
        }

        if (!foundPieceOnLine) {
            return null;
        }

        final boolean[][] seen = new boolean[myN + 1][myN + 1];
        for (int i = 0; i < moved.length; i++) {
            final int row = rowOf(moved[i]);
            final int col = colOf(moved[i]);
            if (seen[row][col]) {
                return null;
            }
            seen[row][col] = true;
        }

        Arrays.sort(moved);
        if (Arrays.equals(moved, theState)) {
            return null;
        }
        return moved;
    }

    private void validateBoard() {
        if (myN < 1) {
            throw new IllegalArgumentException("Board size must be positive.");
        }

        final boolean[][] obstacleBoard = new boolean[myN + 1][myN + 1];
        for (int i = 0; i < obsts.length; i++) {
            final int row = rowOf(obsts[i]);
            final int col = colOf(obsts[i]);
            validatePosition(row, col);
            if (isAntiDiagonal(row, col)) {
                throw new IllegalArgumentException("Obstacle cannot be on the anti-diagonal.");
            }
            if (obstacleBoard[row][col]) {
                throw new IllegalArgumentException("Duplicate obstacle.");
            }
            obstacleBoard[row][col] = true;
        }

        final boolean[][] alienBoard = new boolean[myN + 1][myN + 1];
        for (int i = 0; i < aliens.length; i++) {
            final int row = rowOf(aliens[i]);
            final int col = colOf(aliens[i]);
            validatePosition(row, col);
            if (alienBoard[row][col] || obstacleBoard[row][col]) {
                throw new IllegalArgumentException("Invalid alien placement.");
            }
            alienBoard[row][col] = true;
        }

        for (int i = 0; i < myN; i++) {
            if (rowdisp[i] < 0 || rowdisp[i] >= myN || coldisp[i] < 0 || coldisp[i] >= myN) {
                throw new IllegalArgumentException("Displacements must be in the range 0..n-1.");
            }
        }
    }

    private void ensureInitialized() {
        if (rowdisp == null || coldisp == null || obsts == null || aliens == null) {
            throw new IllegalStateException("Call randomize() or specify() before solve().");
        }
    }

    private void validatePosition(final int theRow, final int theCol) {
        if (theRow < 1 || theRow > myN || theCol < 1 || theCol > myN) {
            throw new IllegalArgumentException("Position out of bounds: (" + theRow + ", " + theCol + ")");
        }
    }

    private boolean isObstacle(final int theRow, final int theCol) {
        final int code = encode(theRow, theCol);
        for (int i = 0; i < obsts.length; i++) {
            if (obsts[i] == code) {
                return true;
            }
        }
        return false;
    }

    private boolean isAntiDiagonal(final int theRow, final int theCol) {
        return theRow + theCol == myN + 1;
    }

    private int encode(final int theRow, final int theCol) {
        return theRow * 10 + theCol;
    }

    private int rowOf(final int theCode) {
        return theCode / 10;
    }

    private int colOf(final int theCode) {
        return theCode % 10;
    }

    private int mod(final int theValue, final int theMod) {
        int result = theValue % theMod;
        if (result < 0) {
            result += theMod;
        }
        return result;
    }

    private String stateKey(final int[] theState) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < theState.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(theState[i]);
        }
        return sb.toString();
    }

    private void displayBoard(final int[] theAliens) {
        final boolean[][] alienBoard = new boolean[myN + 1][myN + 1];
        final boolean[][] obstacleBoard = new boolean[myN + 1][myN + 1];
        for (int i = 0; i < theAliens.length; i++) {
            alienBoard[rowOf(theAliens[i])][colOf(theAliens[i])] = true;
        }
        for (int i = 0; i < obsts.length; i++) {
            obstacleBoard[rowOf(obsts[i])][colOf(obsts[i])] = true;
        }

        System.out.print("     ");
        for (int col = 1; col <= myN; col++) {
            if (coldisp[col - 1] == 0) {
                System.out.print("    ");
            } else {
                System.out.printf("%4d", coldisp[col - 1]);
            }
        }
        System.out.println();

        System.out.print("     ");
        for (int col = 1; col <= myN; col++) {
            if (coldisp[col - 1] == 0) {
                System.out.print("    ");
            } else {
                System.out.print("   v");
            }
        }
        System.out.println();

        for (int row = 1; row <= myN; row++) {
            if (rowdisp[row - 1] == 0) {
                System.out.print("    ");
            } else {
                System.out.printf("%2d> ", rowdisp[row - 1]);
            }
            for (int col = 1; col <= myN; col++) {
                final char symbol;
                if (obstacleBoard[row][col]) {
                    symbol = 'X';
                } else if (alienBoard[row][col]) {
                    symbol = 'O';
                } else {
                    symbol = ' ';
                }
                System.out.print("[" + symbol + "] ");
            }
            System.out.println();
        }
    }

    private static final class VisitRecord {
        private final StateRecord record;

        private VisitRecord(final StateRecord theRecord) {
            record = theRecord;
        }
    }

    private static final class Meeting {
        private final StateRecord fromThisSide;
        private final StateRecord fromOtherSide;

        private Meeting(final StateRecord theFromThisSide, final StateRecord theFromOtherSide) {
            fromThisSide = theFromThisSide;
            fromOtherSide = theFromOtherSide;
        }
    }

    private static final class StateRecord {
        private final int[] aliens;
        private final StateRecord parent;
        private final String moveFromParent;
        private final int depth;

        private StateRecord(final int[] theAliens, final StateRecord theParent,
                            final String theMoveFromParent, final int theDepth) {
            aliens = theAliens;
            parent = theParent;
            moveFromParent = theMoveFromParent;
            depth = theDepth;
        }
    }
}