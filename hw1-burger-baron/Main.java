// This work was completed by James Fischer and Kalsoom Babakarkhail
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Main {

    private int orderNumber = 0;

    public static void main(String[] args) {
        Main m = new Main();

        // Optional: run tests first
        // m.testMyDeque();
        // m.testBurger();

        if (args == null || args.length == 0) {
            System.out.println("ERROR: Missing input filename argument (args[0]).");
            return;
        }

        Scanner fileScanner = null;
        try {
            fileScanner = new Scanner(new File(args[0]));
        } catch (FileNotFoundException e) {
            System.out.println("ERROR: File not found: " + args[0]);
            return;
        }

        while (fileScanner.hasNextLine()) {
            String line = fileScanner.nextLine().trim();
            if (line.length() > 0) {
                m.parseLine(line);
            }
        }
        fileScanner.close();
    }

    public void parseLine(String line) {
        int currentOrder = orderNumber;
        orderNumber = (orderNumber + 1) % 1000;

        MyDeque<String> tokens = tokenize(line);
        boolean isBaronOrder = line.indexOf("Baron Burger") >= 0;
        Burger burger = new Burger(isBaronOrder);

        // Parse patty count (defaults to Single)
        if (isPattyCount(tokens.peekFirst())) {
            int count = pattyCountToInt(tokens.removeFirst());
            for (int i = 1; i < count; i++) burger.addPatty();
        }

        // Parse patty type (defaults to Beef)
        if (isPattyType(tokens.peekFirst())) {
            burger.changePatties(tokens.removeFirst());
        }

        // Consume "Baron Burger" or "Burger"
        if ("Baron".equals(tokens.peekFirst())) tokens.removeFirst();
        if ("Burger".equals(tokens.peekFirst())) tokens.removeFirst();

        // Parse modifiers
        if ("with".equals(tokens.peekFirst())) {
            tokens.removeFirst();

            if (isBaronOrder) {
                // Baron: "with no <omissions> but <exceptions>"
                if ("no".equals(tokens.peekFirst())) {
                    tokens.removeFirst();
                    while (tokens.size() > 0 && !"but".equals(tokens.peekFirst())) {
                        burger.removeCategoryOrIngredient(tokens.removeFirst());
                    }
                }
                if ("but".equals(tokens.peekFirst())) {
                    tokens.removeFirst();
                    while (tokens.size() > 0) {
                        String exception = tokens.removeFirst();
                        if (!isCategory(exception)) {
                            burger.addCategoryOrIngredient(exception);
                        }
                    }
                }
            } else {
                // Basic: "with <additions> but no <exceptions>"
                while (tokens.size() > 0 && !"but".equals(tokens.peekFirst())) {
                    burger.addCategoryOrIngredient(tokens.removeFirst());
                }
                if ("but".equals(tokens.peekFirst())) {
                    tokens.removeFirst();
                    if ("no".equals(tokens.peekFirst())) tokens.removeFirst();
                    while (tokens.size() > 0) {
                        String exception = tokens.removeFirst();
                        if (!isCategory(exception)) {
                            burger.removeCategoryOrIngredient(exception);
                        }
                    }
                }
            }
        }

        System.out.println("Processing Order " + currentOrder + ": " + line);
        System.out.println(burger.toString());
        System.out.println();
    }

    private MyDeque<String> tokenize(String line) {
        MyDeque<String> out = new MyDeque<String>();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == ' ') {
                if (sb.length() > 0) {
                    out.addLast(sb.toString());
                    sb.setLength(0);
                }
            } else {
                sb.append(c);
            }
        }
        if (sb.length() > 0) out.addLast(sb.toString());
        return out;
    }

    private boolean isPattyCount(String s) {
        return "Single".equals(s) || "Double".equals(s) || 
               "Triple".equals(s) || "Quadruple".equals(s);
    }

    private int pattyCountToInt(String count) {
        if ("Double".equals(count)) return 2;
        if ("Triple".equals(count)) return 3;
        if ("Quadruple".equals(count)) return 4;
        return 1;
    }

    private boolean isPattyType(String s) {
        return "Beef".equals(s) || "Chicken".equals(s) || 
               "Pork".equals(s) || "Veggie".equals(s);
    }

    private boolean isCategory(String s) {
        return "Cheese".equals(s) || "Veggies".equals(s) || "Sauce".equals(s);
    }

    public void testMyDeque() {
        System.out.println("=== testMyDeque() ===");
        MyDeque<String> d = new MyDeque<String>();
        
        System.out.println("Empty size (expect 0): " + d.size());
        System.out.println("Empty peekFirst (expect null): " + d.peekFirst());
        System.out.println("Empty peekLast  (expect null): " + d.peekLast());
        System.out.println("Empty removeFirst (expect null): " + d.removeFirst());
        System.out.println("Empty removeLast  (expect null): " + d.removeLast());

        d.addFirst("B");
        d.addFirst("A");
        d.addLast("C");
        System.out.println("After addFirst/addLast: " + d.toString());
        System.out.println("size (expect 3): " + d.size());
        System.out.println("peekFirst (expect A): " + d.peekFirst());
        System.out.println("peekLast  (expect C): " + d.peekLast());

        System.out.println("removeFirst (expect A): " + d.removeFirst());
        System.out.println("removeLast  (expect C): " + d.removeLast());
        System.out.println("Now: " + d.toString());
        System.out.println("removeFirst (expect B): " + d.removeFirst());
        System.out.println("Now empty size (expect 0): " + d.size());
        System.out.println();
    }

    public void testBurger() {
        System.out.println("=== testBurger() ===");

        Burger basic = new Burger(false);
        System.out.println("Basic default: " + basic.toString());

        basic.changePatties("Chicken");
        basic.addCategoryOrIngredient("Cheese");
        basic.addCategoryOrIngredient("Onions");
        System.out.println("Basic w/ Cheese + Onions + Chicken: " + basic.toString());

        Burger baron = new Burger(true);
        System.out.println("Baron default: " + baron.toString());

        baron.removeCategoryOrIngredient("Veggies");
        baron.removeCategoryOrIngredient("Sauce");
        baron.addCategoryOrIngredient("Baron-Sauce");
        System.out.println("Baron no Veggies/Sauce but Baron-Sauce: " + baron.toString());
        System.out.println();
    }
}