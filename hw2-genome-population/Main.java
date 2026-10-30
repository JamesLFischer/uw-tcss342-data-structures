// James Fischer 1/30/2026

public class Main {

    public static void main(String[] args) {

        long start = System.currentTimeMillis();

        Population pop = new Population(100, 0.05);
        int generation = 0;

        while (pop.mostFit.fitness() != 0) {
            System.out.println("Gen " + generation + ": " + pop.mostFit);
            pop.day();
            generation++;
        }

        System.out.println("Gen " + generation + ": " + pop.mostFit);

        long end = System.currentTimeMillis();
        System.out.println("Generations: " + generation);
        System.out.println("Running time: " + (end - start) + " milliseconds.");
    }

    public static void testGenome() {
        Genome g = new Genome(0.05);
        System.out.println(g);
        g.mutate();
        System.out.println(g);
    }

    public static void testPopulation() {
        Population p = new Population(10, 0.05);
        p.testPopulation();
    }
}
