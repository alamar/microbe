package ru.lj.alamar.microbe;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Properties;
import java.util.Random;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Tuple2List;

/**
 * @author ilyak
 */
public class Main {

    private static final DecimalFormat FMT = new DecimalFormat("0.#####");

    public static void main(String[] args) throws Exception {
        Properties model = loadModel(args[0]);
        String cmdlineSeed = (args.length >= 2 && args[1] != null && !args[1].isEmpty()) ? args[1] : "";
        PrintWriter out = output(args[0], cmdlineSeed);
        Random r = new Random(Integer.parseInt(cmdlineSeed.isEmpty() ? model.getProperty("seed") : cmdlineSeed));
        ListF<Microbe> microbes = Cf.arrayList();
        int population = Integer.parseInt(model.getProperty("population"));
        int chromosomes = Integer.parseInt(model.getProperty("chromosomes"));
        int genes = Integer.parseInt(model.getProperty("genes"));
        for (int i = 0; i < population; i++) {
            microbes.add(new Microbe(chromosomes, genes, true));
        }
        String fixploidPopulation = model.getProperty("fixploid.population");
        if (fixploidPopulation != null) {
            int fp = Integer.parseInt(fixploidPopulation);
            for (int i = 0; i < fp; i++) {
                microbes.add(new Microbe(chromosomes, genes, false));
            }
        }

        float geneMutationChance = Float.parseFloat(model.getProperty("gene.mutation.chance"));
        float negativeEffect = Float.parseFloat(model.getProperty("negative.effect"));
        float mutationPositiveChance = Float.parseFloat(model.getProperty("mutation.positive.chance"));
        float positiveEffect = Float.parseFloat(model.getProperty("positive.effect"));
        float luckRatio = Float.parseFloat(model.getProperty("luck.ratio"));
        boolean inexactDuplication = "true".equalsIgnoreCase(model.getProperty("inexact.chromosome.duplication"));
        String downsizeChanceString = model.getProperty("downsize.chance");
        float downsizeChance = (downsizeChanceString == null) ? 0f : Float.parseFloat(downsizeChanceString);

        print(out, "Running model: " + model.getProperty("title"));
        print(out, "step\tpopulation\taverage fitness");
        int steps = Integer.parseInt(model.getProperty("steps"));
        for (int s = 0; s < steps; s++) {
            float totalFitness = 0f;
            int[] ploidy = new int[10];
            for (Microbe microbe : microbes) {
                microbe.mutate(r, geneMutationChance, negativeEffect, mutationPositiveChance, positiveEffect);
                totalFitness += microbe.fitness();
                if (microbe.getPloidy() <= 9) {
                    ploidy[microbe.isChangePloidy() ? microbe.getPloidy() : 0]++;
                }
            }
            float avgFitness = totalFitness / (float) microbes.size();
            microbes = selectOffspring(r, microbes, luckRatio, inexactDuplication, downsizeChance);
            if (microbes.isEmpty()) {
                break;
            }
            print(out, s + "\t" + microbes.size() + "\t" + FMT.format(avgFitness));
            printPloidy(out, ploidy, microbes.size());
        }
        out.close();
    }

    static ListF<Microbe> selectOffspring(Random r, ListF<Microbe> population, Float luckRatio, boolean inexactDuplication, Float downsizeChance) {
        Tuple2List<Float, Microbe> withFitnessAndLuck = Tuple2List.arrayList();
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
            float fitness = microbe.fitness();
            withFitnessAndLuck.add(fitness * (1f - luckRatio) + r.nextFloat() * luckRatio, microbe);
            withFitnessAndLuck.add(fitness * (1f - luckRatio) + r.nextFloat() * luckRatio,
                    microbe.replicate(r, inexactDuplication, downsizeChance));
        }
        return withFitnessAndLuck.sortBy1().reverse().get2().take(population.size());
    }

    private static final int BAR_WIDTH = 50;
    static void printPloidy(PrintWriter out, int[] ploidy, int population) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            for (int b = 0; b < (ploidy[i] * BAR_WIDTH + population / 2) / population; b++) {
                sb.append(i == 0 ? "M" : Integer.toString(i));
            }
        }
        print(out, sb.toString());
    }

    static void print(PrintWriter out, String line) throws IOException {
        System.out.println(line);
        out.println(line);
    }

    static PrintWriter output(String modelName, String cmdlineSeed) throws IOException {
        return new PrintWriter(new File(modelName + (cmdlineSeed.isEmpty() ? "" : ("-" + cmdlineSeed)) + ".txt"));
    }

    static Properties loadModel(String modelName) throws IOException {
        FileInputStream stream = new FileInputStream(new File(modelName + ".properties"));
        try {
            Properties model = new Properties();
            model.load(stream);
            return model;
        } finally {
            try {
                stream.close();
            } catch (Exception e) {
                System.err.println(e);
            }
        }
    }
}
