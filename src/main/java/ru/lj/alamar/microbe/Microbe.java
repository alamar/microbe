package ru.lj.alamar.microbe;

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
    private float[] genes;

    protected Microbe(float[] parentGenesCopy) {
        genes = parentGenesCopy;
    }

    public Microbe(int numGenes) {
        genes = new float[numGenes];
        for (int i = 0; i < numGenes; i++) {
            genes[i] = NORMAL_FITNESS;
        }
    }

    public float fitness() {
        float result = 1.0f;
        for (int i = 0; i < genes.length; i++) {
            result *= (genes[i] / NORMAL_FITNESS);
        }
        return result;
    }

    public boolean isDead() {
        return fitness() < ALIVE_FITNESS;
    }

    public void mutate(Random r, float geneMutationChance, float negativeModifier, float mutationPositiveChance, float positiveModifier) {
        for (int i = 0; i < genes.length; i++) {
            if (r.nextFloat() > geneMutationChance) continue;
            if (r.nextFloat() < mutationPositiveChance) {
                genes[i] = Math.min(1f, genes[i] + positiveModifier);
            } else {
                genes[i] = Math.max(0f, genes[i] - negativeModifier);
            }
        }
    }

    public Microbe replicate() {
        return new Microbe(genes.clone());
    }
}
