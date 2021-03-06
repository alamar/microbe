package ru.lj.alamar.microbe;

import java.util.Collections;
import java.util.Random;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Tuple2List;

/**
 * @author ilyak
 */
public class Microbe {
    // fitness: 0.0 to 1.0
    public static final float ALIVE_FITNESS = 0.5f;
    private float normalFitness;
    // [[chromosomeId gene gene gene gene...]]
    // Yes I know it's kind of troublesome
    private float[][] chromosomes;
    private boolean changePloidy;
    // cached; trouble source
    private float fitness = -1f;

    protected Microbe(float normalFitness, float[][] inheritedChromosomes, boolean changePloidy) {
        this.normalFitness = normalFitness;
        this.changePloidy = changePloidy;
        this.chromosomes = inheritedChromosomes;
    }

    public Microbe(float normalFitness, int ploidy, int numGenes, boolean changePloidy, int chromosomeIdBase) {
        this.normalFitness = normalFitness;
        this.changePloidy = changePloidy;
        chromosomes = new float[ploidy][numGenes + 1];
        chromosomeIdBase *= ploidy;
        for (float[] chromosome : chromosomes) {
           for (int g = 1; g <= numGenes; g++) {
               chromosome[g] = normalFitness;
           }
           chromosome[0] = chromosomeIdBase++;
        }
    }

    public float fitness() {
        if (fitness >= 0) return fitness;

        float result = 1.0f;
        for (int g = 1; g < chromosomes[0].length; g++) {
            float geneFitness = chromosomes[0][g];
            for (int p = 1; p < chromosomes.length; p++) {
                if (chromosomes[p][g] > geneFitness) {
                    geneFitness = chromosomes[p][g];
                }
            }
            result *= (geneFitness / normalFitness);
        }
        fitness = result;
        return result;
    }

    public boolean isDead() {
        return fitness() < ALIVE_FITNESS;
    }

    public void mutate(Random r, float geneMutationChance, float negativeModifier, float mutationPositiveChance, float positiveModifier) {
        for (int c = 0; c < chromosomes.length; c++) {
            float[] chromosome = chromosomes[c];
            for (int g = 1; g < chromosome.length; g++) {
                if (r.nextFloat() > geneMutationChance) continue;
                if (r.nextFloat() < mutationPositiveChance) {
                    chromosome[g] = Math.min(1f, 1f - (1f - chromosome[g]) * (1f - positiveModifier));
                } else {
                    chromosome[g] = Math.max(0f, chromosome[g] * (1f - negativeModifier));
                }
            }
        }
        fitness = -1f;
    }

    private float[] otherChromosome(Random r, int current) {
        int index = r.nextInt(chromosomes.length - 1);
        if (index >= current) {
            return chromosomes[index + 1];
        }
        return chromosomes[index];
    }

    // XXX Following 3 methods too similar!
    public void conversion(Random r) {
        int genes = chromosomes[0].length - 1;
        int startingGene = r.nextInt(genes);
        int fragmentLength = 1 + genes / 20 + r.nextInt(genes / 10 + 1);
        int targetChromosomeNo = r.nextInt(chromosomes.length);
        float[] sourceChromosome = otherChromosome(r, targetChromosomeNo);
        for (int g = startingGene; g < startingGene + fragmentLength; g++) {
            chromosomes[targetChromosomeNo][1 + g % genes] = sourceChromosome[1 + g % genes];
        }
        this.fitness = -1f;
    }

    public void crossing(Random r) {
        int genes = chromosomes[0].length - 1;
        int startingGene = r.nextInt(genes);
        int fragmentLength = 1 + genes / 20 + r.nextInt(genes / 10 + 1);
        int oneChromosomeNo = r.nextInt(chromosomes.length);
        float[] twoChromosome = otherChromosome(r, oneChromosomeNo);
        for (int g = startingGene; g < startingGene + fragmentLength; g++) {
            float gswp = chromosomes[oneChromosomeNo][1 + g % genes];
            chromosomes[oneChromosomeNo][1 + g % genes] = twoChromosome[1 + g % genes];
            twoChromosome[1 + g % genes] = gswp;
        }
        this.fitness = -1f;
    }

    public void horizontalTransfer(Random r, Microbe donor, boolean homologic) {
        int genes = chromosomes[0].length - 1;
        int startingGene = r.nextInt(genes);
        int fragmentLength = 1 + genes / 20 + r.nextInt(genes / 10 + 1);
        int targetChromosome = r.nextInt(chromosomes.length);
        int sourceChromosome = selectChromosome(r, donor, homologic, chromosomes[targetChromosome]);
        for (int g = startingGene; g < startingGene + fragmentLength; g++) {
            chromosomes[targetChromosome][1 + g % genes] = donor.getChromosomes()[sourceChromosome][1 + g % genes];
        }
        this.fitness = -1f;
    }

    public void chromosomeSubstitution(Random r, Microbe donor, boolean homologic) {
        int targetChromosome = r.nextInt(chromosomes.length);
        int sourceChromosome = selectChromosome(r, donor, homologic, chromosomes[targetChromosome]);
        chromosomes[targetChromosome] = donor.getChromosomes()[sourceChromosome].clone();
        this.fitness = -1f;
    }

    public void chromosomeExchange(Random r, Microbe peer, boolean homologic) {
        int ownChromosome = r.nextInt(chromosomes.length);
        float[] chromosome = chromosomes[ownChromosome];
        int peerChromosome = selectChromosome(r, peer, homologic, chromosome);
        chromosomes[ownChromosome] = peer.getChromosomes()[peerChromosome];
        peer.getChromosomes()[peerChromosome] = chromosome;
        peer.fitness = -1f;
        this.fitness = -1f;
    }

    // First exchange most similar chromosomes, then do crossing on them
    public void pairing(Random r, Microbe peer, boolean homologic) {
        int ownChromosomeNo = r.nextInt(chromosomes.length);
        float[] chromosome = chromosomes[ownChromosomeNo];
        int peerChromosomeNo = selectChromosome(r, peer, homologic, chromosome);
        chromosomes[ownChromosomeNo] = peer.getChromosomes()[peerChromosomeNo];
        peer.getChromosomes()[peerChromosomeNo] = chromosome;

        int genes = chromosomes[0].length - 1;
        int startingGene = r.nextInt(genes);
        int fragmentLength = 1 + genes / 20 + r.nextInt(genes / 10 + 1);
        for (int g = startingGene; g < startingGene + fragmentLength; g++) {
            float gswp = chromosomes[ownChromosomeNo][1 + g % genes];
            chromosomes[ownChromosomeNo][1 + g % genes] = chromosome[1 + g % genes];
            chromosome[1 + g % genes] = gswp;
        }
        this.fitness = -1f;
        peer.fitness = -1f;
    }

    private static int selectChromosome(Random r, Microbe peer, boolean homologic, float[] ownChromosome) {
        int peerChromosomeNo = r.nextInt(peer.getChromosomes().length);
        if (homologic) {
            int bestMatches = 0;
            for (int c = 0; c < peer.getChromosomes().length; c++) {
                float[] peerChromosome = peer.getChromosomes()[c];
                if (peerChromosome == ownChromosome) {
                    continue;
                }
                int matches = 0;
                for (int i = 1; i < peerChromosome.length; i++) {
                    if (ownChromosome[i] == peerChromosome[i]) {
                        matches++;
                    }
                }
                if (matches > bestMatches) {
                    bestMatches = matches;
                    peerChromosomeNo = c;
                }
            }
        }
        return peerChromosomeNo;
    }

    private static float[][] OF_CHROMOSOMES = new float[0][0];
    // XXX Mutates (not in biological sense :)
    public Microbe replicate(Random r, boolean inexact, int maxChromosomes, float unequalDivisionChance, float downsizeChance) {
        ListF<float[]> copies = Cf.arrayList();
        int targetPloidy = chromosomes.length;
        if (!changePloidy) {
            maxChromosomes = targetPloidy;
        }

        boolean downsize = changePloidy && downsizeChance > 0f && targetPloidy > 1 && downsizeChance > r.nextFloat();
        boolean triplicate = inexact || (targetPloidy == 1 && changePloidy);

        if (downsize) {
            targetPloidy = (targetPloidy + 1) / 2;
        } else {
            for (float[] chromosome : chromosomes) {
                copies.add(chromosome.clone());
                if (triplicate) {
                    copies.add(chromosome.clone());
                }
            }
            if (triplicate) {
                Collections.shuffle(copies, r);
            }
        }
        ListF<float[]> doubled = Cf.arrayList(copies.take(Math.max(targetPloidy, 2)).plus(chromosomes));
        Collections.shuffle(doubled, r);
        int splitAt = targetPloidy;
        if (changePloidy && !downsize && r.nextFloat() < unequalDivisionChance) {
            splitAt++;
        }

        this.chromosomes = doubled.take(Math.min(splitAt, maxChromosomes)).toArray(OF_CHROMOSOMES);
        this.fitness = -1f;

        float[][] siblingChromosomes = doubled.drop(splitAt).take(maxChromosomes).toArray(OF_CHROMOSOMES);
        return new Microbe(normalFitness, siblingChromosomes, changePloidy);
    }

    // XXX Doesn't change the order of chromosomes, which makes conversion biased.
    public Microbe mitosis() {
        float[][] siblingChromosomes = new float[chromosomes.length][];
        for (int c = 0; c < chromosomes.length; c++) {
            siblingChromosomes[c] = chromosomes[c].clone();
        }
        return new Microbe(normalFitness, siblingChromosomes, changePloidy);
    }

    public static ListF<Microbe> selectOffspring(Random r, ListF<Microbe> population, Float luckRatio,
            int maxChromosomes, boolean inexactDuplication, float unequalDivisionChance, float downsizeChance, boolean mitosis) {
        Tuple2List<Float, Microbe> withFitnessAndLuck = Tuple2List.arrayList();
        // Hope we don't run off so far
        float minFitness = 2f;
        float maxFitness = 0f;
        for (Microbe microbe : population) {
            if (microbe.isDead()) continue;
            float fitness = microbe.fitness();
            if (minFitness > fitness) {
                minFitness = fitness;
            }
            if (maxFitness < fitness) {
                maxFitness = fitness;
            }
        }
        for (Microbe microbe : population) {
            if (microbe.isDead()) continue;
            Microbe sibling = mitosis ? microbe.mitosis() : microbe.replicate(r, inexactDuplication, maxChromosomes,
                    unequalDivisionChance, downsizeChance);
            if (!microbe.isDead()) {
                withFitnessAndLuck.add(((microbe.fitness() - minFitness) / (maxFitness - minFitness)) * (1f - luckRatio) + r.nextFloat() * luckRatio, microbe);
            }
            if (!sibling.isDead()) {
                withFitnessAndLuck.add(((sibling.fitness() - minFitness) / (maxFitness - minFitness)) * (1f - luckRatio) + r.nextFloat() * luckRatio, sibling);
            }
        }
        return withFitnessAndLuck.sortBy1().reverse().get2().take(population.size());
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
