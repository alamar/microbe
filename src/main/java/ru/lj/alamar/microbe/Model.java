package ru.lj.alamar.microbe;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import java.util.Random;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.bolts.collection.Tuple2List;

/**
 * @author ilyak
 */
public class Model {

    private static class PropertiesFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return name.toLowerCase().endsWith(".properties");
        }
    }

    public enum Stat {
        AVG,
        DEV,
        BOX;
    }

    private static final DecimalFormat FMT = new DecimalFormat("0.#####", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: model {model-name} [RNG-seed] [key=value]...");
            System.err.println("See MODELS directory");
            System.exit(1);
        }
        String firstArg = args[0];
        if (args.length > 1 && args[1] == null) {
            args = new String[] { firstArg }; /* anti-maven */
        }

        File maybeDir = new File("models/" + firstArg);
        if (!maybeDir.isDirectory()) {
            runOneModel(args);
            return;
        }
        for (String fileName : maybeDir.list(new PropertiesFilter())) {
            String[] params = Arrays.copyOf(args, args.length);
            params[0] = firstArg + "/" + fileName;
            runOneModel(params);
        }
    }

    public static void runOneModel(String[] params) throws Exception {
        params[0] = params[0].replace(".properties", "");
        String modelFullName = modelWithParameters(params);
        PrintWriter out = output(modelFullName);
        String title = params[0].replaceAll(".*[/\\\\]", "").replaceAll("_", " ") + " " +
                Cf.list(params).drop(1).mkString(" ");
        try {
            Properties model = loadModel(params, out);
            print(out, "model = " + title);
            Random r = new XorShiftRandom(Long.parseLong(model.getProperty("seed")));
            int steps = Integer.parseInt(model.getProperty("steps"));
            ListF<Float> avgFitness = runSimulation(r, model, steps, out);
            Chart.drawChart(modelFullName, title, avgFitness, avgFitness.size() < steps);
        } finally {
            out.close();
            System.out.println("Simulation complete for model: " + title);
        }
    }

    static ListF<Float> runSimulation(Random r, Properties model, int steps, PrintWriter out) throws IOException {
        ListF<Microbe> microbes = Cf.arrayList();
        int population = Integer.parseInt(model.getProperty("population"));
        float normalFitness = Float.parseFloat(model.getProperty("normal.fitness"));
        int chromosomes = Integer.parseInt(model.getProperty("chromosomes"));
        int genes = Integer.parseInt(model.getProperty("genes"));
        for (int i = 0; i < population; i++) {
            microbes.add(new Microbe(normalFitness, chromosomes, genes, false, i));
        }
        int variploidPopulation = Integer.parseInt(model.getProperty("variploid.population"));
        for (int i = 0; i < variploidPopulation; i++) {
            microbes.add(new Microbe(normalFitness, chromosomes, genes, true, i + population));
        }

        float geneMutationChance = Float.parseFloat(model.getProperty("gene.mutation.chance"));
        float negativeEffect = Float.parseFloat(model.getProperty("negative.effect"));
        float mutationPositiveChance = Float.parseFloat(model.getProperty("mutation.positive.chance"));
        float positiveEffect = Float.parseFloat(model.getProperty("positive.effect"));
        float luckRatio = Float.parseFloat(model.getProperty("luck.ratio"));

        float conversionRatio = Float.parseFloat(model.getProperty("conversion.ratio"));
        float crossingRatio = Float.parseFloat(model.getProperty("crossing.ratio"));

        int maxVariploidChromosomes = Integer.parseInt(model.getProperty("max.variploid.chromosomes"));
        float unequalDivisionChance = Float.parseFloat(model.getProperty("unequal.division.chance"));
        float downsizeChance = Float.parseFloat(model.getProperty("downsize.chance"));

        float horizontalTransferRatio = Float.parseFloat(model.getProperty("horizontal.transfer.ratio"));
        float chromosomeSubstitutionRatio = Float.parseFloat(model.getProperty("chromosome.substitution.ratio"));
        float chromosomeExchangeRatio = Float.parseFloat(model.getProperty("chromosome.exchange.ratio"));
        float pairingRatio = Float.parseFloat(model.getProperty("pairing.ratio"));
        boolean homology = "true".equalsIgnoreCase(model.getProperty("homology"));

        boolean inexactDuplication = "true".equalsIgnoreCase(model.getProperty("inexact.chromosome.duplication"));
        boolean mitosis = "true".equalsIgnoreCase(model.getProperty("mitosis"));

        Stat stat = Stat.valueOf(model.getProperty("stat").toUpperCase());

        if (stat == Stat.DEV) {
            print(out, "step\tpopulation\taverage fitness\tstandard deviation");
        } else if (stat == Stat.BOX) {
            print(out, "step\tpopulation\tmedian\t10%\t25%\t75%\t90%");
        } else {
            print(out, "step\tpopulation\taverage fitness");
        }
        ListF<Float> dataset = Cf.arrayList();
        for (int s = 0; s < steps; s++) {
            float totalFitness = 0f;
            float totalChromosomes = 0;
            float[] fitnesses = new float[microbes.size()];
            int i = 0;
            for (Microbe microbe : microbes) {
                float fitness = microbe.fitness();
                totalFitness += fitness;
                fitnesses[i++] = fitness;
                microbe.mutate(r, geneMutationChance, negativeEffect, mutationPositiveChance, positiveEffect);
                totalChromosomes += microbe.getChromosomes().length;
            }

            for (int t = 0; t < conversionRatio * totalChromosomes; t++) {
                Microbe target = microbes.get(r.nextInt(microbes.size()));
                target.conversion(r);
            }
            for (int t = 0; t < crossingRatio * totalChromosomes; t++) {
                Microbe target = microbes.get(r.nextInt(microbes.size()));
                target.crossing(r);
            }
            for (int t = 0; t < horizontalTransferRatio * totalChromosomes; t++) {
                Microbe donor = microbes.get(r.nextInt(microbes.size()));
                Microbe recipient = microbes.get(r.nextInt(microbes.size()));
                recipient.horizontalTransfer(r, donor, homology);
            }
            for (int t = 0; t < chromosomeSubstitutionRatio * totalChromosomes; t++) {
                Microbe donor = microbes.get(r.nextInt(microbes.size()));
                Microbe recipient = microbes.get(r.nextInt(microbes.size()));
                recipient.chromosomeSubstitution(r, donor, homology);
            }
            for (int t = 0; t < chromosomeExchangeRatio * totalChromosomes; t++) {
                Microbe donor = microbes.get(r.nextInt(microbes.size()));
                Microbe recipient = microbes.get(r.nextInt(microbes.size()));
                recipient.chromosomeExchange(r, donor, homology);
            }
            for (int t = 0; t < pairingRatio * totalChromosomes; t++) {
                Microbe donor = microbes.get(r.nextInt(microbes.size()));
                Microbe recipient = microbes.get(r.nextInt(microbes.size()));
                recipient.pairing(r, donor, homology);
            }
            int[] ploidy = new int[10];
            for (Microbe microbe : microbes) {
                if (microbe.getPloidy() <= 9) {
                    ploidy[microbe.isChangePloidy() ? microbe.getPloidy() : 0]++;
                }
            }
            microbes = Microbe.selectOffspring(r, microbes, luckRatio, maxVariploidChromosomes, inexactDuplication,
                    unequalDivisionChance, downsizeChance, mitosis);
            if (microbes.isEmpty()) {
                break;
            }

            float avgFitness = totalFitness / (float) microbes.size();
            String statCols = computeStat(stat, avgFitness, fitnesses);
            print(out, s + "\t" + microbes.size() + "\t" + statCols);
            if (variploidPopulation > 0) {
                printPloidy(out, ploidy, microbes.size());
            }
            if (microbes.size() == (population + variploidPopulation)) {
                dataset.add(avgFitness);
            }
            if (s % 10 == 0) {
                out.flush();
            }
        }

        if (microbes.isNotEmpty()) {
            outputChromosomeStats(out, microbes, normalFitness, genes);
            if ("true".equalsIgnoreCase(model.getProperty("dump"))) {
                for (Microbe microbe : microbes.shuffle()) {
                    for (float[] chromosome : microbe.getChromosomes()) {
                        out.print(FMT.format(chromosome[0]));
                        out.print("\t");
                        for (int g = 1; g < genes; g++) {
                            out.print(FMT.format(chromosome[g]));
                            out.print("\t");
                        }
                        out.println();
                    }
                    out.println();
                }
            }
        }
        return dataset;
    }

    private static void outputChromosomeStats(PrintWriter out, ListF<Microbe> microbes,
            float normalFitness, int genes)
    {
        Tuple2List<Integer, float[]> idxChromosomes = Tuple2List.arrayList();
        for (Microbe microbe : microbes) {
            for (float[] chromosome : microbe.getChromosomes()) {
                int idx = (int) chromosome[0];

                idxChromosomes.add(idx, chromosome);
            }
        }
        MapF<Integer, ListF<float[]>> idxToChromosomes = idxChromosomes.groupBy1();
        print(out, "Chromosome histogram: ");
        print(out, idxToChromosomes.mapValues(Cf.List.sizeF()).entries().sortBy2().take(20).reverse().mkString("\n", "\t"));
        for (Tuple2<Integer, ListF<float[]>> entry :
                idxToChromosomes.entries().sortBy2(Cf.List.sizeF().andThenNaturalComparator().invert()).take(20))
        {
            int count = entry.get2().size();
            out.println("#");
            out.println("#" + entry.get1());
            out.println("chromosome\tgene\tavg\tdev");
            if (count <= 1) continue;
            float avgAvgGene = 0f;
            float[] avgGenes = new float[genes];
            float[] geneAcrossChromosomes = new float[count];
            for (int g = 1; g <= genes; g++) {
                int i = 0;
                float avgGene = 0f;
                for (float[] chromosome : entry.get2()) {
                    avgGene += chromosome[g] / normalFitness;
                    geneAcrossChromosomes[i++] = chromosome[g] / normalFitness;
                }
                avgGene = avgGene / (float) count;
                avgGenes[g - 1] = avgGene;
                avgAvgGene += avgGene;
                out.println("c" + entry.get1() + "\tg" + g + "\t" + computeStat(Stat.DEV, avgGene, geneAcrossChromosomes));
            }
            avgAvgGene = avgAvgGene / (float) genes;
            out.println("c" + entry.get1() + "\tavg\t" + computeStat(Stat.DEV, avgAvgGene, avgGenes));
        }
    }

    static String computeStat(Stat stat, float avgFitness, float[] fitnesses) {
        if (stat == Stat.DEV) {
            float sdev = 0f;
            for (float fitness : fitnesses) {
                sdev += (avgFitness - fitness) * (avgFitness - fitness);
            }
            float devFitness = (float) Math.sqrt(sdev / (fitnesses.length - 1f));
            return FMT.format(avgFitness) + "\t" + FMT.format(devFitness);
        }
        if (stat == Stat.BOX) {
            Arrays.sort(fitnesses);
            float median = fitnesses[fitnesses.length / 2];
            float lowBox = fitnesses[fitnesses.length / 4];
            float highBox = fitnesses[(fitnesses.length / 4) * 3];
            float lowWhisker = fitnesses[fitnesses.length / 10];
            float highWhisker = fitnesses[(fitnesses.length / 10) * 9];
            return FMT.format(median) + "\t" + FMT.format(lowWhisker) + "\t" + FMT.format(lowBox)
                    + "\t" + FMT.format(highBox) + "\t" + FMT.format(highWhisker);
        }
        return FMT.format(avgFitness);
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
        File output = new File("models/" + modelName + ".csv");
        if (output.exists()) {
            System.err.println("Creating back-up copy of simulation results");
            output.renameTo(new File(output.getPath() + ".bak"));
        }
        System.err.println("Writing simulation results to: " + output.getPath());
        return new PrintWriter(output);
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
