package ru.lj.alamar.microbe;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Properties;
import java.util.Random;
import java.util.Arrays;
import java.awt.BasicStroke;

import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.axis.NumberAxis;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;

/**
 * @author ilyak
 */
public class Model {

    private static final DecimalFormat FMT = new DecimalFormat("0.#####");

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: model {model-name} [RNG-seed] [key=value]...");
            System.err.println("See MODELS directory");
            System.exit(1);
        }
        if (args.length > 1 && args[1] == null) {
            args = new String[] { args[0] }; /* anti-maven */
        }
        args[0] = args[0].replace(".properties", "");
        String modelFullName = modelWithParameters(args);
        PrintWriter out = output(modelFullName);
        String title = args[0] + " " + Cf.list(args).drop(1).mkString(" ");
        try {
            Properties model = loadModel(args, out);
            print(out, "model = " + args[0]);
            Random r = new Random(Integer.parseInt(model.getProperty("seed")));
            drawChart(modelFullName, title, runSimulation(r, model, out));
        } finally {
            out.close();
            System.out.println("Simulation complete for model: " + title);
        }
    }

    static ListF<Float> runSimulation(Random r, Properties model, PrintWriter out) throws IOException {
        ListF<Microbe> microbes = Cf.arrayList();
        int population = Integer.parseInt(model.getProperty("population"));
        float normalFitness = Float.parseFloat(model.getProperty("normal.fitness"));
        int chromosomes = Integer.parseInt(model.getProperty("chromosomes"));
        int genes = Integer.parseInt(model.getProperty("genes"));
        for (int i = 0; i < population; i++) {
            microbes.add(new Microbe(normalFitness, chromosomes, genes, false));
        }
        int variploidPopulation = Integer.parseInt(model.getProperty("variploid.population"));
        for (int i = 0; i < variploidPopulation; i++) {
            microbes.add(new Microbe(normalFitness, chromosomes, genes, true));
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
        float horizontalTransferRatio = Float.parseFloat(model.getProperty("horizontal.transfer.ratio"));
        float chromosomeSubstitutionRatio = Float.parseFloat(model.getProperty("chromosome.substitution.ratio"));
        float chromosomeExchangeRatio = Float.parseFloat(model.getProperty("chromosome.exchange.ratio"));

        boolean inexactDuplication = "true".equalsIgnoreCase(model.getProperty("inexact.chromosome.duplication"));
        boolean mitosis = "true".equalsIgnoreCase(model.getProperty("mitosis"));

        int steps = Integer.parseInt(model.getProperty("steps"));
        print(out, "step\tpopulation\taverage fitness");
        ListF<Float> dataset = Cf.arrayList();
        for (int s = 0; s < steps; s++) {
            float totalChromosomes = 0;
            for (Microbe microbe : microbes) {
                microbe.mutate(r, geneMutationChance, negativeEffect, mutationPositiveChance, positiveEffect, conversionChance, crossingChance);
                totalChromosomes += microbe.getChromosomes().length;
            }
            for (int t = 0; t < horizontalTransferRatio * totalChromosomes; t++) {
                Microbe donor = microbes.get(r.nextInt(microbes.size()));
                Microbe recipient = microbes.get(r.nextInt(microbes.size()));
                recipient.horizontalTransfer(r, donor);
            }
            for (int t = 0; t < chromosomeSubstitutionRatio * totalChromosomes; t++) {
                Microbe donor = microbes.get(r.nextInt(microbes.size()));
                Microbe recipient = microbes.get(r.nextInt(microbes.size()));
                recipient.chromosomeSubstitution(r, donor);
            }
            for (int t = 0; t < chromosomeExchangeRatio * totalChromosomes; t++) {
                Microbe donor = microbes.get(r.nextInt(microbes.size()));
                Microbe recipient = microbes.get(r.nextInt(microbes.size()));
                recipient.chromosomeExchange(r, donor);
            }
            float totalFitness = 0f;
            int[] ploidy = new int[10];
            for (Microbe microbe : microbes) {
                totalFitness += microbe.fitness();
                if (microbe.getPloidy() <= 9) {
                    ploidy[microbe.isChangePloidy() ? microbe.getPloidy() : 0]++;
                }
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
            dataset.add(avgFitness);
            if (s % 10 == 0) {
                out.flush();
            }
        }
        return dataset;
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

    static PrintWriter output(String modelName) throws IOException {
        File output = new File("models/" + modelName + ".txt");
        if (output.exists()) {
            System.err.println("Creating back-up copy of simulation results");
            output.renameTo(new File(output.getPath() + ".bak"));
        }
        return new PrintWriter(output);
    }

    static void drawChart(String model, String title, ListF<Float> dataset) throws IOException {
        XYSeries series = new XYSeries(title);
        for (int i = 0; i < dataset.size(); i++) {
            series.add(i, dataset.get(i));
        }
        XYDataset data = new XYSeriesCollection(series);
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setShapesVisible(false);
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        JFreeChart chart = new JFreeChart(new XYPlot(data, new NumberAxis(), new NumberAxis(), renderer));
        File output = new File("models/" + model + ".png");
        if (output.exists()) {
            System.err.println("Creating back-up copy of simulation chart");
            output.renameTo(new File(output.getPath() + ".bak"));
        }
        ChartUtilities.saveChartAsPNG(output, chart, 800, 600);
    }

    static String modelWithParameters(String[] args) {
        String modelName = args[0];
        for (int a = 1; a < args.length; a++) {
            modelName += "-" + args[a].replaceAll(" ", "").replaceAll("=", "-").replaceAll("\\.", "");
        }
        return modelName;
    }

    static Properties loadModel(String[] args, final PrintWriter out) throws IOException {
        Properties model = new Properties() {
            public String getProperty(String name) {
                String value = super.getProperty(name);
                print(out, name + " = " + value);
                return value;
            }
        };

        loadPropertiesFile(model, "default");
        loadPropertiesFile(model, args[0]);
        String baseModelName = model.getProperty("base.model");
        if (baseModelName != null) {
            // No support for nesting!
            // XXX do we need it at all when we have command-line properties?
            loadPropertiesFile(model, baseModelName);
            loadPropertiesFile(model, args[0]);
        }
        for (int a = 1; a < args.length; a++) {
            String arg = args[a];
            if (arg.matches("^[0-9]+$")) {
                model.setProperty("seed", arg);
                continue;
            }
            int eq = arg.indexOf("=");
            if (eq <= 0) {
                throw new RuntimeException("Cannot parse key=value: " + arg);
            }
            model.setProperty(arg.substring(0, eq).trim(), arg.substring(eq + 1).trim());
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
