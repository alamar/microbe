package ru.lj.alamar.microbe;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Tuple2List;

/**
 * @author ilyak
 */
public class Merge {

    private static final DecimalFormat FMT = new DecimalFormat("0.#####", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

    public static void main(String[] argsArray) throws Exception {
        ListF<String> args = Cf.list(argsArray);
        int steps = 0;
        if (args.isNotEmpty() && args.first().matches("[0-9]+")) {
            steps = Integer.parseInt(args.first());
            args = args.drop(1);
        }
        if (args.size() < 3) {
            System.err.println("Usage: merge [steps] {model-name[,model-name...]} [{model-name[,model-name...]}] {target-name}");
            System.exit(1);
        }
        ListF<Tuple2List<String, ListF<Float>>> dataset = Cf.arrayList();
        for (String arg : args.rdrop(1)) {
            Tuple2List<String, ListF<Float>> segment = Tuple2List.arrayList();
            for (String model : arg.split(",")) {
                BufferedReader reader = openResults(model);
                try {
                    String line;
                    ListF<Float> values = Cf.arrayList();
                    int startingPopulation = 0;
                    while ((line = reader.readLine()) != null) {
                        int indexOfEq = line.indexOf('=');
                        if (indexOfEq > 0 && line.substring(0, indexOfEq).trim().equals("model")) {
                            model = line.substring(indexOfEq + 1).trim();
                        }
                        int indexOfTab = line.indexOf('\t');
                        if (indexOfTab > 0) {
                            int indexOfSecondTab = line.indexOf('\t', indexOfTab + 1);
                            if (indexOfSecondTab > 0) {
                                String col = line.substring(indexOfTab + 1, indexOfSecondTab).trim();
                                if (col.matches("[0-9]+")) {
                                    int population = Integer.parseInt(col);
                                    if (startingPopulation == 0) {
                                        startingPopulation = population;
                                    }
                                    if (startingPopulation <= population) {
                                        values.add(FMT.parse(line.substring(indexOfSecondTab + 1)).floatValue());
                                        if (values.size() == steps) {
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    segment.add(model, values);
                } finally {
                    reader.close();
                }
            }
            dataset.add(segment);
        }

        Chart.drawChart(args.last(), dataset, steps == 0);

        int written;
        int i = 0;
        PrintWriter out = output(args.last());
        do {
            written = 0;
            StringBuilder line = new StringBuilder();
            for (Tuple2List<String, ListF<Float>> segment : dataset) {
                for (ListF<Float> values : segment.get2()) {
                    if (values.size() > i) {
                        line.append(FMT.format(values.get(i)));
                        written++;
                    }
                    line.append('\t');
                }
            }
            out.println(line.toString());
            i++;
        } while (written > 0);
        out.close();
    }

    static BufferedReader openResults(String modelName) throws IOException {
        File input = new File("models/" + modelName + ".csv");
        return new BufferedReader(new FileReader(input));
    }

    static PrintWriter output(String modelName) throws IOException {
        File output = new File("models/" + modelName + ".csv");
        if (output.exists()) {
            System.err.println("Creating back-up copy of merge results");
            output.renameTo(new File(output.getPath() + ".bak"));
        }
        System.err.println("Writing simulations merge to: " + output.getPath());
        return new PrintWriter(output);
    }
}
