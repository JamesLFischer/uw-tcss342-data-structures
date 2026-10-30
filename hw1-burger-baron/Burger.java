// This work was completed by James Fischer and Kalsoom Babakarkhail
public class Burger {

    private MyDeque<String> burger;

    private int pattyCount;
    private String pattyType;

    // ingredient flags (allowed: primitives/Strings)
    private boolean pickles;
    private boolean topBun;
    private boolean bottomBun;

    private boolean mayonnaise;
    private boolean baronSauce;
    private boolean lettuce;
    private boolean tomato;
    private boolean onions;

    private boolean brie;
    private boolean pepperjack;
    private boolean mozzarella;
    private boolean cheddar;

    private boolean mushrooms;
    private boolean mustard;
    private boolean ketchup;

    Burger(boolean theWorks) {
        burger = new MyDeque<String>();
        pattyCount = 1;
        pattyType = "Beef";

        topBun = true;
        bottomBun = true;

        if (theWorks) {
            // Baron Burger has everything
            pickles = true;

            mayonnaise = true;
            baronSauce = true;

            lettuce = true;
            tomato = true;
            onions = true;

            brie = true;
            pepperjack = true;
            mozzarella = true;
            cheddar = true;

            mushrooms = true;
            mustard = true;
            ketchup = true;
        } else {
            // basic burger: buns + patty only
            pickles = false;

            mayonnaise = false;
            baronSauce = false;

            lettuce = false;
            tomato = false;
            onions = false;

            brie = false;
            pepperjack = false;
            mozzarella = false;
            cheddar = false;

            mushrooms = false;
            mustard = false;
            ketchup = false;
        }

        rebuild();
    }

    void changePatties(String type) {
        if (isPattyType(type)) {
            pattyType = canonicalPatty(type);
            rebuild();
        }
    }

    void addPatty() {
        if (pattyCount < 4) {
            pattyCount++;
            rebuild();
        }
    }

    void addCategoryOrIngredient(String type) {
        if (type == null) return;

        if (equalsIgnoreCase(type, "Cheese")) {
            brie = true; pepperjack = true; mozzarella = true; cheddar = true;
        } else if (equalsIgnoreCase(type, "Veggies")) {
            lettuce = true; tomato = true; onions = true; pickles = true; mushrooms = true;
        } else if (equalsIgnoreCase(type, "Sauce")) {
            mayonnaise = true; baronSauce = true; mustard = true; ketchup = true;
        } else {
            setIngredientFlag(type, true);
        }

        rebuild();
    }

    void removeCategoryOrIngredient(String type) {
        if (type == null) return;

        if (equalsIgnoreCase(type, "Cheese")) {
            brie = false; pepperjack = false; mozzarella = false; cheddar = false;
        } else if (equalsIgnoreCase(type, "Veggies")) {
            lettuce = false; tomato = false; onions = false; pickles = false; mushrooms = false;
        } else if (equalsIgnoreCase(type, "Sauce")) {
            mayonnaise = false; baronSauce = false; mustard = false; ketchup = false;
        } else {
            setIngredientFlag(type, false);
        }

        rebuild();
    }

    private void rebuild() {
        burger = new MyDeque<String>();

        // TOP -> BOTTOM, matching the official order & trace format
        if (pickles) burger.addLast("Pickles");
        if (topBun) burger.addLast("Top-Bun");

        if (mayonnaise) burger.addLast("Mayonnaise");
        if (baronSauce) burger.addLast("Baron-Sauce");

        if (lettuce) burger.addLast("Lettuce");
        if (tomato) burger.addLast("Tomato");
        if (onions) burger.addLast("Onions");

        // Cheese placement rule:
        // - If 1 patty: cheeses go above the patty.
        // - If >1 patties: cheeses go on the bottom patty => place top patties first,
        //   then cheeses, then bottom patty last.
        if (pattyCount == 1) {
            addCheesesInOrder();
            burger.addLast(pattyType);
        } else {
            // add top patties first (pattyCount - 1)
            for (int i = 0; i < pattyCount - 1; i++) {
                burger.addLast(pattyType);
            }
            // then cheese stack (on bottom patty)
            addCheesesInOrder();
            // then the bottom patty
            burger.addLast(pattyType);
        }

        if (mushrooms) burger.addLast("Mushrooms");
        if (mustard) burger.addLast("Mustard");
        if (ketchup) burger.addLast("Ketchup");

        if (bottomBun) burger.addLast("Bottom-Bun");
    }

    private void addCheesesInOrder() {
        if (brie) burger.addLast("Brie");
        if (pepperjack) burger.addLast("Pepperjack");
        if (mozzarella) burger.addLast("Mozzarella");
        if (cheddar) burger.addLast("Cheddar");
    }

    private void setIngredientFlag(String type, boolean value) {
        // Ingredients only (categories handled elsewhere)
        if (equalsIgnoreCase(type, "Pickles")) pickles = value;
        else if (equalsIgnoreCase(type, "Mayonnaise")) mayonnaise = value;
        else if (equalsIgnoreCase(type, "Baron-Sauce")) baronSauce = value;
        else if (equalsIgnoreCase(type, "Lettuce")) lettuce = value;
        else if (equalsIgnoreCase(type, "Tomato")) tomato = value;
        else if (equalsIgnoreCase(type, "Onions")) onions = value;
        else if (equalsIgnoreCase(type, "Brie")) brie = value;
        else if (equalsIgnoreCase(type, "Pepperjack")) pepperjack = value;
        else if (equalsIgnoreCase(type, "Mozzarella")) mozzarella = value;
        else if (equalsIgnoreCase(type, "Cheddar")) cheddar = value;
        else if (equalsIgnoreCase(type, "Mushrooms")) mushrooms = value;
        else if (equalsIgnoreCase(type, "Mustard")) mustard = value;
        else if (equalsIgnoreCase(type, "Ketchup")) ketchup = value;
        // invalid words ignored by spec
    }

    private boolean isPattyType(String s) {
        return equalsIgnoreCase(s, "Beef")
            || equalsIgnoreCase(s, "Chicken")
            || equalsIgnoreCase(s, "Pork")
            || equalsIgnoreCase(s, "Veggie");
    }

    private String canonicalPatty(String s) {
        if (equalsIgnoreCase(s, "Chicken")) return "Chicken";
        if (equalsIgnoreCase(s, "Pork")) return "Pork";
        if (equalsIgnoreCase(s, "Veggie")) return "Veggie";
        return "Beef";
    }

    private boolean equalsIgnoreCase(String a, String b) {
        return a != null && b != null && a.equalsIgnoreCase(b);
    }

    public String toString() {
        return burger.toString();
    }
}