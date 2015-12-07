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

import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.bolts.collection.Tuple2List;

/**
 * @author ilyak
 */
public class Chart {

    public static void drawChart(String model, String title, ListF<Float> dataset, boolean padRight) throws IOException {
        drawChart(model, Tuple2List.fromPairs(title, dataset), padRight);
    }

    public static void drawChart(String model, Tuple2List<String, ListF<Float>> dataset, boolean padRight) throws IOException {
        XYSeriesCollection data = new XYSeriesCollection();
        float min = 1f;
        float max = 1f;
        for (Tuple2<String, ListF<Float>> run : dataset) {
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
        min = ((float) Math.floor(min * 20.0f) - 1.0f) / 20.0f;
        max = ((float) Math.ceil(max * 20.0f) + 1.0f) / 20.0f;

        JFreeChart chart = ChartFactory.createXYLineChart("", "generation #", "average fitness", data);
        XYPlot plot = (XYPlot) chart.getPlot();
        for (int i = 0; i < dataset.size(); i++) {
            plot.getRenderer().setSeriesStroke(i, new BasicStroke(3.5f));
        }

        BasicStroke gridlineStroke = (BasicStroke) plot.getDomainGridlineStroke();
        gridlineStroke = new BasicStroke(2.0f, gridlineStroke.getEndCap(), gridlineStroke.getLineJoin(), gridlineStroke.getMiterLimit(),
                gridlineStroke.getDashArray(), gridlineStroke.getDashPhase());
        plot.getDomainAxis().setLabelFont(plot.getDomainAxis().getLabelFont().deriveFont(25f));
        plot.getDomainAxis().setTickLabelFont(plot.getDomainAxis().getTickLabelFont().deriveFont(20f));
        plot.setDomainGridlineStroke(gridlineStroke);
        if (!padRight) {
            plot.getDomainAxis().setUpperMargin(0);
        }
        plot.getRangeAxis().setLabelFont(plot.getRangeAxis().getLabelFont().deriveFont(27f));
        plot.getRangeAxis().setTickLabelFont(plot.getRangeAxis().getTickLabelFont().deriveFont(22f));
        plot.getRangeAxis().setUpperBound(max);
        plot.getRangeAxis().setLowerBound(min);
        plot.setRangeGridlineStroke(gridlineStroke);
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
        plot.addAnnotation(new XYTitleAnnotation(0.01f, 0.01f, chart.getLegend(), RectangleAnchor.BOTTOM_LEFT));
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
