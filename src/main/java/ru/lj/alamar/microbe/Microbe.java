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
    boolean changePloidy;
    private float fitness = -1f;

    protected Microbe(float[][] inheritedChromosomes, boolean changePloidy) {
        this.changePloidy = changePloidy;
        chromosomes = inheritedChromosomes;
    }

    public Microbe(int ploidy, int numGenes, boolean changePloidy) {
        this.changePloidy = changePloidy;
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
    private static final int MAX_CHANGING_PLOIDY = 6;
    // XXX Mutates (not in biological sense :)
    public Microbe replicate(Random r, boolean inexact, float downsizeChance) {
        ListF<float[]> copies = Cf.arrayList();
        int targetPloidy = chromosomes.length;
        if (targetPloidy > 1 && downsizeChance > r.nextFloat()) {
            targetPloidy = (targetPloidy + 1) / 2;
        }
        for (float[] chromosome : chromosomes) {
            copies.add(chromosome.clone());
            if (inexact || changePloidy) {
                copies.add(chromosome.clone());
            }
        }
        if (inexact || changePloidy) {
            Collections.shuffle(copies, r);
        }
        ListF<float[]> doubled = copies.take(Math.max(targetPloidy, 2)).plus(chromosomes);
        Collections.shuffle(doubled, r);
        int splitAt = targetPloidy;
        if (changePloidy) {
            float adjust = r.nextFloat();
            if (splitAt > 1 && adjust < 0.1f) {
                splitAt--;
            }
            if (adjust > 0.9f && splitAt < MAX_CHANGING_PLOIDY) {
                splitAt++;
            }
        }

        chromosomes = doubled.take(splitAt).toArray(OF_CHROMOSOMES);
        fitness = -1f;
        return new Microbe(doubled.drop(splitAt)
                .take(Math.min(Math.max(targetPloidy * 2 - splitAt, 1), MAX_CHANGING_PLOIDY))
                .toArray(OF_CHROMOSOMES), changePloidy);
    }

    public int getPloidy() {
        return chromosomes.length;
    }

    public float[][] getChromosomes() {
        return chromosomes;
    }

    public boolean isChangePloidy() {
        return changePloidy;
    }
}
