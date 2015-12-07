package ru.lj.alamar.microbe;

import java.io.File;
import java.io.IOException;
import java.awt.BasicStroke;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTitleAnnotation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleAnchor;

import ru.yandex.bolts.collection.ListF;

/**
 * @author ilyak
 */
public class Chart {

    public static void drawChart(String model, String title, ListF<Float> dataset, boolean padRight) throws IOException {
        XYSeries series = new XYSeries(title);
        for (int i = 0; i < dataset.size(); i++) {
            series.add(i, dataset.get(i));
        }
        XYDataset data = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart("", "generation #", "average fitness", data);
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.getRenderer().setSeriesStroke(0, new BasicStroke(3.5f));
        plot.getDomainAxis().setLabelFont(plot.getDomainAxis().getLabelFont().deriveFont(25f));
        plot.getDomainAxis().setTickLabelFont(plot.getDomainAxis().getTickLabelFont().deriveFont(20f));
        plot.getRangeAxis().setLabelFont(plot.getRangeAxis().getLabelFont().deriveFont(25f));
        plot.getRangeAxis().setTickLabelFont(plot.getRangeAxis().getTickLabelFont().deriveFont(20f));
        chart.getLegend().setItemFont(chart.getLegend().getItemFont().deriveFont(20f));
        plot.addAnnotation(new XYTitleAnnotation(0.01f, 0.01f, chart.getLegend(), RectangleAnchor.BOTTOM_LEFT));
        chart.removeLegend();
        File output = new File("models/" + model + ".png");
        if (output.exists()) {
            System.err.println("Creating back-up copy of simulation chart");
            output.renameTo(new File(output.getPath() + ".bak"));
        }
        ChartUtilities.saveChartAsPNG(output, chart, 1200, 900);
    }
}
