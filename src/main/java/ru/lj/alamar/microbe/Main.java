package ru.lj.alamar.microbe;

import java.util.Random;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Tuple2List;

/**
 * @author ilyak
 */
public class Main {
    public static int MICROBES = 100000;
    public static int CHROMOSOMES = 1;
    public static int GENES = 100;
    public static float GENE_MUTATION_CHANCE = 0.00867f;
    public static float MUTATION_POSITIVE_CHANCE = 0.1f;
    public static float NEGATIVE_EFFECT = 0.05f;
    public static float POSITIVE_EFFECT = 0.01f;
    public static float LUCK_RATIO = 0.3f;

    public static void main(String[] args) {
        Random r = new Random(1444556354);
        ListF<Microbe> microbes = Cf.arrayList();
        for (int i = 0; i < MICROBES; i++) {
            microbes.add(new Microbe(CHROMOSOMES, GENES));
        }
        for (int s = 0; s < 10000; s++) {
            float totalFitness = 0f;
            for (Microbe microbe : microbes) {
                microbe.mutate(r, GENE_MUTATION_CHANCE, NEGATIVE_EFFECT, MUTATION_POSITIVE_CHANCE, POSITIVE_EFFECT);
                totalFitness += microbe.fitness();
            }
            float avgFitness = totalFitness / (float) microbes.size();
            microbes = selectOffspring(r, microbes);
            if (microbes.isEmpty()) {
                break;
            }
            System.out.println(s + "\t" + microbes.size() + "\t" + avgFitness);
        }
    }

    static ListF<Microbe> selectOffspring(Random r, ListF<Microbe> population) {
        Tuple2List<Float, Microbe> withFitnessAndLuck = Tuple2List.arrayList();
        for (Microbe microbe : population) {
            if (microbe.isDead()) continue;
            float fitness = microbe.fitness();
            withFitnessAndLuck.add(fitness * (1f - LUCK_RATIO) + r.nextFloat() * LUCK_RATIO, microbe);
            withFitnessAndLuck.add(fitness * (1f - LUCK_RATIO) + r.nextFloat() * LUCK_RATIO, microbe.replicate(r));
        }
        return withFitnessAndLuck.sortBy1().reverse().get2().take(population.size());
    }
}
