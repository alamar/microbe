package ru.lj.alamar.microbe;

import java.util.Collections;
import java.util.Random;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;

/**
 * @author ilyak
 */
public class Microbe {
    // fitness: 0.0 to 1.0
    public static final float NORMAL_FITNESS = 0.9f;
    public static final float ALIVE_FITNESS = 0.5f;
    private float[][] chromosomes;
    private float fitness = -1f;

    protected Microbe(float[][] inheritedChromosomes) {
        chromosomes = inheritedChromosomes;
    }

    public Microbe(int ploidy, int numGenes) {
        chromosomes = new float[ploidy][numGenes];
        for (float[] chromosome : chromosomes) {
           for (int g = 0; g < numGenes; g++) {
               chromosome[g] = NORMAL_FITNESS;
           }
        }
    }

    public float fitness() {
        if (fitness >= 0) return fitness;

        float result = 1.0f;
        for (int g = 0; g < chromosomes[0].length; g++) {
            float geneFitness = chromosomes[0][g];
            for (int p = 1; p < chromosomes.length; p++) {
                if (chromosomes[p][g] > geneFitness) {
                    geneFitness = chromosomes[p][g];
                }
            }
            result *= (geneFitness / NORMAL_FITNESS);
        }
        fitness = result;
        return result;
    }

    public boolean isDead() {
        return fitness() < ALIVE_FITNESS;
    }

    public void mutate(Random r, float geneMutationChance, float negativeModifier, float mutationPositiveChance, float positiveModifier) {
        for (float[] chromosome : chromosomes) {
            for (int g = 0; g < chromosome.length; g++) {
                if (r.nextFloat() > geneMutationChance) continue;
                if (r.nextFloat() < mutationPositiveChance) {
                    chromosome[g] = Math.min(1f, chromosome[g] + positiveModifier);
                } else {
                    chromosome[g] = Math.max(0f, chromosome[g] - negativeModifier);
                }
            }
        }
        fitness = -1f;
    }

    private static float[][] OF_CHROMOSOMES = new float[0][0];
    // XXX Mutates (not in biological sense :)
    public Microbe replicate(Random r) {
        ListF<float[]> doubled = Cf.arrayList();
        for (float[] chromosome : chromosomes) {
            doubled.add(chromosome.clone());
            doubled.add(chromosome);
        }
        // XXX Require Random r
        Collections.shuffle(doubled, r);
        chromosomes = doubled.take(chromosomes.length).toArray(OF_CHROMOSOMES);
        fitness = -1f;
        return new Microbe(doubled.drop(chromosomes.length).toArray(OF_CHROMOSOMES));
    }
}
