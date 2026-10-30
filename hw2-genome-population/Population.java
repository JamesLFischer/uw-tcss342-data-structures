// James Fischer 1/30/2026

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class Population {

    public Genome mostFit;
    private final List<Genome> population;
    private final Random rand = new Random();

    public Population(Integer numGenomes, Double mutationRate) {
        population = new ArrayList<>();
        for (int i = 0; i < numGenomes; i++) {
            population.add(new Genome(mutationRate));
        }
        mostFit = population.get(0);
    }

    public void day() {

        population.sort(Comparator.comparingInt(Genome::fitness));
        mostFit = population.get(0);

        int survivors = population.size() / 2;
        List<Genome> nextGen = new ArrayList<>(population.subList(0, survivors));

        while (nextGen.size() < population.size()) {
            Genome parent1 = nextGen.get(rand.nextInt(survivors));
            Genome child = new Genome(parent1);

            if (rand.nextBoolean()) {
                Genome parent2 = nextGen.get(rand.nextInt(survivors));
                child.crossover(parent2);
            }

            child.mutate();
            nextGen.add(child);
        }

        population.clear();
        population.addAll(nextGen);
    }

    public void testPopulation() {
        System.out.println("Population test:");
        for (Genome g : population) {
            System.out.println(g);
        }
    }
}
