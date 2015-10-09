package ru.lj.alamar.microbe;

import java.util.Random;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;

/**
 * @author ilyak
 */
public class Main {
    public static int MICROBES = 10;
    public static int GENES = 10;
    public static float GENE_MUTATION_CHANCE = 0.05f;
    public static float MUTATION_POSITIVE_CHANCE = 0.1f;
    public static float NEGATIVE_EFFECT = 0.05f;
    public static float POSITIVE_EFFECT = 0.01f;
    
    public static void main(String[] args) {
        Random r = new Random();
        ListF<Microbe> microbes = Cf.arrayList();
        for (int i = 0; i < MICROBES; i++) {
            microbes.add(new Microbe(GENES));
        }
        for (int s = 0; s < 20; s++) {
            float totalFitness = 0f;
            for (Microbe microbe : microbes) {
                microbe.mutate(r, GENE_MUTATION_CHANCE, NEGATIVE_EFFECT, MUTATION_POSITIVE_CHANCE, POSITIVE_EFFECT);
                totalFitness += microbe.fitness();
            }
            System.out.println(totalFitness / microbes.size());
        }
    }
}
