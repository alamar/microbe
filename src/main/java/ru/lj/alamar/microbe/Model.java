package ru.lj.alamar.microbe;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Properties;
import java.util.Random;
import java.util.Arrays;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;

/**
 * @author ilyak
 */
public class Model {

    private static final DecimalFormat FMT = new DecimalFormat("0.#####");

    public static void main(String[] args) throws Exception {
        String cmdlineSeed = (args.length >= 2 && args[1] != null && !args[1].isEmpty()) ? args[1] : "";
        PrintWriter out = output(args[0], cmdlineSeed);
        try {
        Properties model = loadModel(args[0], out);
        print(out, "model = " + args[0]);
        Random r = new Random(Integer.parseInt(cmdlineSeed.isEmpty() ? model.getProperty("seed") : cmdlineSeed));
        ListF<Microbe> microbes = Cf.arrayList();
        int population = Integer.parseInt(model.getProperty("population"));
        int chromosomes = Integer.parseInt(model.getProperty("chromosomes"));
        int genes = Integer.parseInt(model.getProperty("genes"));
        for (int i = 0; i < population; i++) {
            microbes.add(new Microbe(chromosomes, genes, false));
        }
        int variploidPopulation = Integer.parseInt(model.getProperty("variploid.population"));
        for (int i = 0; i < variploidPopulation; i++) {
            microbes.add(new Microbe(chromosomes, genes, true));
        }

        float geneMutationChance = Float.parseFloat(model.getProperty("gene.mutation.chance"));
        float negativeEffect = Float.parseFloat(model.getProperty("negative.effect"));
        float mutationPositiveChance = Float.parseFloat(model.getProperty("mutation.positive.chance"));
        float positiveEffect = Float.parseFloat(model.getProperty("positive.effect"));
        float luckRatio = Float.parseFloat(model.getProperty("luck.ratio"));

        float downsizeChance = Float.parseFloat(model.getProperty("downsize.chance"));
        float conversionChance = Float.parseFloat(model.getProperty("conversion.chance"));
        float crossingChance = Float.parseFloat(model.getProperty("crossing.chance"));

        int maxVariploidChromosomes = Integer.parseInt(model.getProperty("max.variploid.chromosomes"));
        int horizontalTransfers = Integer.parseInt(model.getProperty("horizontal.transfers"));
        int chromosomeSubstitutions = Integer.parseInt(model.getProperty("chromosome.substitutions"));
        int chromosomeExchanges = Integer.parseInt(model.getProperty("chromosome.exchanges"));

        boolean inexactDuplication = "true".equalsIgnoreCase(model.getProperty("inexact.chromosome.duplication"));
        boolean mitosis = "true".equalsIgnoreCase(model.getProperty("mitosis"));

        int steps = Integer.parseInt(model.getProperty("steps"));
        print(out, "step\tpopulation\taverage fitness");
        for (int s = 0; s < steps; s++) {
            float totalFitness = 0f;
            int[] ploidy = new int[10];
            for (Microbe microbe : microbes) {
                microbe.mutate(r, geneMutationChance, negativeEffect, mutationPositiveChance, positiveEffect, conversionChance, crossingChance);
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
            for (int t = 0; t < chromosomeSubstitutions; t++) {
                Microbe donor = microbes.get(r.nextInt(microbes.size()));
                Microbe recipient = microbes.get(r.nextInt(microbes.size()));
                recipient.chromosomeSubstitution(r, donor);
            }
            for (int t = 0; t < chromosomeExchanges; t++) {
                Microbe donor = microbes.get(r.nextInt(microbes.size()));
                Microbe recipient = microbes.get(r.nextInt(microbes.size()));
                recipient.chromosomeExchange(r, donor);
            }
            float avgFitness = totalFitness / (float) microbes.size();
            microbes = Microbe.selectOffspring(r, microbes, luckRatio, maxVariploidChromosomes, inexactDuplication, downsizeChance, mitosis);
            if (microbes.isEmpty()) {
                break;
            }
            print(out, s + "\t" + microbes.size() + "\t" + FMT.format(avgFitness));
            if (variploidPopulation > 0) {
                printPloidy(out, ploidy, microbes.size());
            }
        }
        /*for (Microbe microbe : microbes.shuffle()) {
            for (float[] chromosome : microbe.getChromosomes()) {
                for (float gene : chromosome) {
                    out.print(FMT.format(gene));
                    out.print("\t");
                }
                out.println();
            }
            out.println();
        }*/
        } finally {
            out.close();
            System.out.println("Simulation complete for model: " + args[0]);
        }
    }

    private static final int BAR_WIDTH = 50;
    static void printPloidy(PrintWriter out, int[] ploidy, int population) throws IOException {
        StringBuilder bar = new StringBuilder();
        StringBuilder table = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            for (int b = 0; b < (ploidy[i] * BAR_WIDTH + population / 2) / population; b++) {
                bar.append(i == 0 ? "M" : Integer.toString(i));
            }
            table.append("\t").append(ploidy[i]);
        }
        print(out, bar.append(table).toString());
    }

    static void print(PrintWriter out, String line) {
        System.out.println(line);
        out.println(line);
    }

    static PrintWriter output(String modelName, String cmdlineSeed) throws IOException {
        File output = new File("models/" + modelName + (cmdlineSeed.isEmpty() ? "" : ("-" + cmdlineSeed)) + ".txt");
        if (output.exists()) {
            System.err.println("Creating back-up copy of simulation results");
            output.renameTo(new File(output.getPath() + ".bak"));
        }
        return new PrintWriter(new File("models/" + modelName + (cmdlineSeed.isEmpty() ? "" : ("-" + cmdlineSeed)) + ".txt"));
    }

    static Properties loadModel(String modelName, final PrintWriter out) throws IOException {
        Properties model = new Properties() {
            public String getProperty(String name) {
                String value = super.getProperty(name);
                print(out, name + " = " + value);
                return value;
            }
        };

        loadPropertiesFile(model, "default");
        loadPropertiesFile(model, modelName);
        String baseModelName = model.getProperty("base.model");
        if (baseModelName != null) {
            // No support for nesting!
            loadPropertiesFile(model, baseModelName);
            loadPropertiesFile(model, modelName);
        }
        return model;
    }

    static void loadPropertiesFile(Properties model, String modelName) throws IOException {
        FileInputStream stream = new FileInputStream(new File("models/" + modelName + ".properties"));
        try {
            model.load(stream);
        } finally {
            try {
                stream.close();
            } catch (Exception e) {
                System.err.println(e);
            }
        }

    }
}
