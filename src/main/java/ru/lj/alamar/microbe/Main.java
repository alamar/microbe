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
            microbes.add(new Microbe(chromosomes, genes, false));
        }
        String variploidPopulation = model.getProperty("variploid.population");
        if (variploidPopulation != null) {
            int vp = Integer.parseInt(variploidPopulation);
            for (int i = 0; i < vp; i++) {
                microbes.add(new Microbe(chromosomes, genes, true));
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
        String conversionChanceString = model.getProperty("conversion.chance");
        float conversionChance = (conversionChanceString == null) ? 0f : Float.parseFloat(conversionChanceString);
        String horizontalTransfersString = model.getProperty("horizontal.transfers");
        int horizontalTransfers = horizontalTransfersString == null ? 0 : Integer.parseInt(horizontalTransfersString);
        boolean mitosis = "true".equalsIgnoreCase(model.getProperty("mitosis"));

        print(out, "Running model: " + args[0]);
        print(out, "step\tpopulation\taverage fitness");
        int steps = Integer.parseInt(model.getProperty("steps"));
        for (int s = 0; s < steps; s++) {
            float totalFitness = 0f;
            int[] ploidy = new int[10];
            for (Microbe microbe : microbes) {
                microbe.mutate(r, geneMutationChance, negativeEffect, mutationPositiveChance, positiveEffect, conversionChance);
                totalFitness += microbe.fitness();
                if (microbe.getPloidy() <= 9) {
                    ploidy[microbe.isChangePloidy() ? microbe.getPloidy() : 0]++;
                }
            }
            for (int t = 0; t < horizontalTransfers; t++) {
                Microbe donor = microbes.get(r.nextInt(microbes.size()));
                Microbe recipient = microbes.get(r.nextInt(microbes.size()));
                recipient.horizontalTransfer(r, donor);
            }
            float avgFitness = totalFitness / (float) microbes.size();
            microbes = Microbe.selectOffspring(r, microbes, luckRatio, inexactDuplication, downsizeChance, mitosis);
            if (microbes.isEmpty()) {
                break;
            }
            print(out, s + "\t" + microbes.size() + "\t" + FMT.format(avgFitness));
            if (variploidPopulation != null) {
                printPloidy(out, ploidy, microbes.size());
            }
        }
        out.close();
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
        return new PrintWriter(new File("models/" + modelName + (cmdlineSeed.isEmpty() ? "" : ("-" + cmdlineSeed)) + ".txt"));
    }

    static Properties loadModel(String modelName) throws IOException {
        FileInputStream stream = new FileInputStream(new File("models/" + modelName + ".properties"));
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
