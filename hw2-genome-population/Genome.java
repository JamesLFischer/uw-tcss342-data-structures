 // James Fischer 1/30/2026

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Genome {

    public static final String target = "PAULO LICCIARDI BARRETO";
    private static final char[] ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ -'".toCharArray();

    private final Random rand = new Random();
    private final double mutationRate;
    private List<Character> genes;

    public Genome(double mutationRate) {
        this.mutationRate = mutationRate;
        genes = new ArrayList<>();
        genes.add('A');
    }

    public Genome(Genome gene) {
        this.mutationRate = gene.mutationRate;
        this.genes = new ArrayList<>(gene.genes);
    }

    public void mutate() {

        // add character
        if (rand.nextDouble() < mutationRate) {
            int pos = rand.nextInt(genes.size() + 1);
            genes.add(pos, randomChar());
        }

        // delete character
        if (rand.nextDouble() < mutationRate && genes.size() >= 2) {
            int pos = rand.nextInt(genes.size());
            genes.remove(pos);
        }

        // change characters
        for (int i = 0; i < genes.size(); i++) {
            if (rand.nextDouble() < mutationRate) {
                genes.set(i, randomChar());
            }
        }
    }

    public void crossover(Genome other) {
        List<Character> child = new ArrayList<>();

        int maxLen = Math.max(this.genes.size(), other.genes.size());

        for (int i = 0; i < maxLen; i++) {
            Genome parent = rand.nextBoolean() ? this : other;
            if (i < parent.genes.size()) {
                child.add(parent.genes.get(i));
            } else {
                break;
            }
        }

        this.genes = child;
    }

    public Integer fitness() {
        int n = genes.size();
        int m = target.length();
        int L = Math.max(n, m);

        int f = Math.abs(m - n);

        for (int i = 0; i < L; i++) {
            char g = (i < n) ? genes.get(i) : 0;
            char t = (i < m) ? target.charAt(i) : 0;
            if (g != t) {
                f++;
            }
        }
        return f;
    }

    private char randomChar() {
        return ALPHABET[rand.nextInt(ALPHABET.length)];
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (char c : genes) sb.append(c);
        return "(\"" + sb + "\", " + fitness() + ")";
    }
}
