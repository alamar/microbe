package ru.lj.alamar.microbe;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.File;
import java.io.IOException;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTitleAnnotation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleInsets;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.bolts.collection.Tuple2List;

/**
 * @author ilyak
 */
public class Chart {

    private static Color[][] colorSets = new Color[][] {
        // "brights"
        { new Color(0x96, 0x4B, 0x00), new Color(0xFF, 0x95, 0x0E), new Color(0x4B, 0x1F, 0x6F), new Color(0xFF, 0x33, 0x99) },
        // reds
        { new Color(0xE1, 0x33, 0x0C), new Color(0xE1, 0x7E, 0x54), new Color(0xC5, 0x00, 0x0B), new Color(0x86, 0x0F, 0x07) },
        // yellows
        { new Color(0xE1, 0xB7, 0x0C), new Color(0xE1, 0xD8, 0x54), new Color(0xC5, 0xB8, 0x00), new Color(0x86, 0x7E, 0x07) },
        // greens
        { new Color(0x70, 0xE1, 0x0C), new Color(0xC2, 0xFF, 0x5F), new Color(0x45, 0xC5, 0x00), new Color(0x33, 0x86, 0x07) },
        // blues
        { new Color(0x0C, 0x93, 0xE1), new Color(0x54, 0xB0, 0xE1), new Color(0x00, 0x66, 0xC5), new Color(0x07, 0x46, 0x86) }
    };

    public static void drawChart(String model, String title, ListF<Float> dataset, boolean padRight) throws IOException {
        drawChart(model, Cf.<Tuple2List<String, ListF<Float>>>list(Tuple2List.fromPairs(title, dataset)), padRight);
    }

    public static void drawChart(String model, ListF<Tuple2List<String, ListF<Float>>> dataset, boolean padRight) throws IOException {
        XYSeriesCollection data = new XYSeriesCollection();
        float min = 1f;
        float max = 1f;
        for (Tuple2List<String, ListF<Float>> segment : dataset) {
            for (Tuple2<String, ListF<Float>> run : segment) {
                XYSeries series = new XYSeries(run.get1());
                ListF<Float> values = run.get2();
                for (int i = 0; i < values.size(); i++) {
                    float value = values.get(i);
                    series.add(i, value);
                    if (min > value) min = value;
                    if (max < value) max = value;
                }
                data.addSeries(series);
            }
        }

        JFreeChart chart = ChartFactory.createXYLineChart("", "generation #", "average fitness", data);

        XYPlot plot = (XYPlot) chart.getPlot();
        int i = 0;
        int s = 0;
        for (Tuple2List<String, ListF<Float>> segment : dataset) {
            for (Tuple2<String, ListF<Float>> run : segment) {
                plot.getRenderer().setSeriesPaint(i,
                        s < 5 ? colorSets[s % 5][(i - s) % 4] : colorSets[i % 5][(i / 4) % 4]);
                plot.getRenderer().setSeriesStroke(i, new BasicStroke(3.5f));
                i++;
            }
            s++;
        }
        min = ((float) Math.floor(min * 20.0f) - (i > 5 ? 2.0f : 1.0f)) / 20.0f;
        max = ((float) Math.ceil(max * 20.0f) + 1.0f) / 20.0f;

        plot.setBackgroundPaint(new Color(0xF8, 0xF8, 0xF8));
        BasicStroke gridlineStroke = (BasicStroke) plot.getDomainGridlineStroke();
        gridlineStroke = new BasicStroke(2.0f, gridlineStroke.getEndCap(), gridlineStroke.getLineJoin(), gridlineStroke.getMiterLimit(),
                gridlineStroke.getDashArray(), gridlineStroke.getDashPhase());
        plot.setDomainGridlineStroke(gridlineStroke);
        plot.setDomainGridlinePaint(new Color(0xC0, 0xC0, 0xC0));
        plot.getDomainAxis().setLabelFont(plot.getDomainAxis().getLabelFont().deriveFont(25f));
        plot.getDomainAxis().setTickLabelFont(plot.getDomainAxis().getTickLabelFont().deriveFont(20f));
        if (!padRight) {
            plot.getDomainAxis().setUpperMargin(0);
        }
        plot.getRangeAxis().setLabelFont(plot.getRangeAxis().getLabelFont().deriveFont(27f));
        plot.getRangeAxis().setTickLabelFont(plot.getRangeAxis().getTickLabelFont().deriveFont(22f));
        plot.getRangeAxis().setUpperBound(max);
        plot.getRangeAxis().setLowerBound(min);
        plot.setRangeGridlineStroke(gridlineStroke);
        plot.setRangeGridlinePaint(new Color(0xC0, 0xC0, 0xC0));
        if (max > 1.0f) {
            ValueMarker fit = new ValueMarker(1.0f);
            fit.setPaint(Color.black);
            fit.setStroke(new BasicStroke(2.0f));
            plot.addRangeMarker(fit);
        }
        if (min < 0.5f) {
            ValueMarker degenerate = new ValueMarker(0.5f);
            degenerate.setPaint(Color.black);
            degenerate.setStroke(new BasicStroke(2.0f));
            plot.addRangeMarker(degenerate);
        }

        chart.getLegend().setItemFont(chart.getLegend().getItemFont().deriveFont(20f));
        chart.getLegend().setItemLabelPadding(new RectangleInsets(2, 2, 2, 20));
        XYTitleAnnotation legend = new XYTitleAnnotation(0.01f, 0.01f, chart.getLegend(), RectangleAnchor.BOTTOM_LEFT);
        legend.setMaxWidth(0.98f);
        plot.addAnnotation(legend);
        chart.removeLegend();
        File output = new File("models/" + model + ".png");
        if (output.exists()) {
            System.err.println("Creating back-up copy of simulation chart");
            output.renameTo(new File(output.getPath() + ".bak"));
        }
        ChartUtilities.saveChartAsPNG(output, chart, 1200, 900);
        System.err.println("Wrote chart: " + output.getPath());
    }
}
