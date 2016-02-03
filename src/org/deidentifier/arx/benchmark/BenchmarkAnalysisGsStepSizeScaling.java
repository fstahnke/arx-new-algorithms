/*
 * Source code of the experiments for the entropy metric
 * 
 * Copyright (C) 2015 Fabian Prasser
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.deidentifier.arx.benchmark;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkAlgorithm;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkDataset;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkPrivacyModel;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkUtilityMeasure;

import de.linearbits.objectselector.Selector;
import de.linearbits.subframe.analyzer.Analyzer;
import de.linearbits.subframe.graph.Field;
import de.linearbits.subframe.graph.Labels;
import de.linearbits.subframe.graph.Plot;
import de.linearbits.subframe.graph.PlotLinesClustered;
import de.linearbits.subframe.graph.Point2D;
import de.linearbits.subframe.graph.Point3D;
import de.linearbits.subframe.graph.Series2D;
import de.linearbits.subframe.graph.Series3D;
import de.linearbits.subframe.io.CSVFile;
import de.linearbits.subframe.render.GnuPlotParams;
import de.linearbits.subframe.render.GnuPlotParams.KeyPos;
import de.linearbits.subframe.render.LaTeX;
import de.linearbits.subframe.render.PlotGroup;

/**
 * BenchmarkAnalysis analyzing scaling of the different algorithms. x-Axis:
 * Scaling factor (records, QIs, K-value) y-Axis: Time Plot-Type: Line Plot
 * 
 * @author Fabian Prasser
 */
public class BenchmarkAnalysisGsStepSizeScaling {

    /**
     * Choose benchmarkConfig to run and comment others out.
     */
    // private static final String benchmarkConfig =
    // "benchmarkConfig/recordScaling.xml";
    // private static final String benchmarkConfig =
    // "benchmarkConfig/QIScaling.xml";
    private static final String benchmarkConfig = "benchmarkConfig/minOptimizationThresholdScaling.xml";

    /**
     * Main
     * 
     * @param args
     * @throws IOException
     * @throws ParseException
     */
    public static void main(String[] args) throws IOException, ParseException {

        List<PlotGroup> groups = new ArrayList<PlotGroup>();
        BenchmarkSetup setup = new BenchmarkSetup(benchmarkConfig);
        CSVFile file = new CSVFile(new File(setup.getOutputFile()));

        for (BenchmarkDataset dataset : setup.getDatasets()) {
            for (BenchmarkAlgorithm algorithm : setup.getAlgorithms()) {
                // for (double gsStepSize : setup.getGsStepSizes()) {
                groups.add(analyzeUtility(file,
                                          setup,
                                          dataset,
                                          BenchmarkUtilityMeasure.LOSS,
                                          BenchmarkPrivacyModel.K5_ANONYMITY,
                                          algorithm,
                                          1,
                                          0.05));

                groups.add(analyzeRuntime(file,
                                          setup,
                                          dataset,
                                          BenchmarkUtilityMeasure.LOSS,
                                          BenchmarkPrivacyModel.K5_ANONYMITY,
                                          algorithm,
                                          1,
                                          0.05));
                // }
            }
        }
        LaTeX.plot(groups, setup.getPlotFile(), true);

    }

    /**
     * Performs the analysis
     * 
     * @param file
     * @param suppression
     * @param algorithm
     * @param model
     * @param measure
     * @param data
     * @return
     * @throws ParseException
     */
    private static PlotGroup analyzeUtility(CSVFile file,
                                            BenchmarkSetup setup,
                                            BenchmarkDataset data,
                                            BenchmarkUtilityMeasure measure,
                                            BenchmarkPrivacyModel model,
                                            BenchmarkAlgorithm algorithm,
                                            double suppression,
                                            double gsStepSize) throws ParseException {

        Selector<String[]> tmpSelector = null;
        ArrayList<Series2D> seriesList = new ArrayList<>();

        for (double suppress : setup.getSuppressionLimits()) {

            // Selects according rows
            Selector<String[]> selector = file.getSelectorBuilder()
                                              .field("UtilityMeasure")
                                              .equals(measure.toString())
                                              .and()
                                              .field("PrivacyModel")
                                              .equals(model.toString())
                                              .and()
                                              .field("Algorithm")
                                              .equals(algorithm.toString())
                                              .and()
                                              .field("SuppressionLimit")
                                              .equals(String.valueOf(suppress))
                                              .and()
                                              .field("Dataset")
                                              .equals(data.toString())
                                              .build();

            Series2D series = new Series2D(file,
                                           selector,
                                           new Field("gsFactorStepSize"),
                                           new Field("Utility", Analyzer.VALUE));

            seriesList.add(series);
            tmpSelector = selector;
        }

        // Dirty hack for creating a 3D series from two 2D series'
        Series3D series3D = new Series3D(file, tmpSelector, new Field("Dataset"), // Cluster
                                         new Field("UtilityMeasure"), // Type
                                         new Field("PrivacyModel")); // Value
        series3D.getData().clear();

        Iterator<Series2D> itr = seriesList.listIterator();
        for (double suppress : setup.getSuppressionLimits()) {
            Series2D series2D = itr.next();
            for (Point2D point : series2D.getData()) {
                series3D.getData().add(new Point3D(point.x,
                                                   "Suppression Limit " + suppress + " (Utility)",
                                                   String.valueOf(1 - Double.valueOf(point.y))));
            }
        }

        // Plot
        List<Plot<?>> plots = new ArrayList<Plot<?>>();
        plots.add(new PlotLinesClustered("Algorithm: " + algorithm.toString() + " / Dataset: " +
                                                 data.toString() + " / Measure: " +
                                                 measure.toString() + " / Model: " +
                                                 model.toString() + " / gsStepSize: " + gsStepSize,
                                         new Labels("Factor: Generalization / Suppression",
                                                    "Utility"), series3D));

        GnuPlotParams params = new GnuPlotParams();
        params.colorize = true;
        params.rotateXTicks = 0;
        params.keypos = KeyPos.TOP_LEFT;
        params.size = 1.0d;
        params.ratio = 0.5d;
        params.minY = 0d;
        params.maxY = 1d;
        return new PlotGroup("Development of utility with different generalization/suppression weights. ",
                             plots,
                             params,
                             1.0d);
    }

    /**
     * Performs the analysis
     * 
     * @param file
     * @param suppression
     * @param algorithm
     * @param model
     * @param measure
     * @param data
     * @return
     * @throws ParseException
     */
    private static PlotGroup analyzeRuntime(CSVFile file,
                                            BenchmarkSetup setup,
                                            BenchmarkDataset data,
                                            BenchmarkUtilityMeasure measure,
                                            BenchmarkPrivacyModel model,
                                            BenchmarkAlgorithm algorithm,
                                            double suppression,
                                            double gsStepSize) throws ParseException {

        Selector<String[]> tmpSelector = null;
        ArrayList<Series2D> seriesList = new ArrayList<>();

        for (double suppress : setup.getSuppressionLimits()) {

            // Selects according rows
            Selector<String[]> selector = file.getSelectorBuilder()
                                              .field("UtilityMeasure")
                                              .equals(measure.toString())
                                              .and()
                                              .field("PrivacyModel")
                                              .equals(model.toString())
                                              .and()
                                              .field("Algorithm")
                                              .equals(algorithm.toString())
                                              .and()
                                              .field("SuppressionLimit")
                                              .equals(String.valueOf(suppress))
                                              .and()
                                              .field("Dataset")
                                              .equals(data.toString())
                                              .build();

            Series2D series = new Series2D(file,
                                           selector,
                                           new Field("gsFactorStepSize"),
                                           new Field("Runtime", Analyzer.VALUE));

            tmpSelector = selector;
            seriesList.add(series);
        }

        // Dirty hack for creating a 3D series from two 2D series'
        Series3D series3D = new Series3D(file, tmpSelector, new Field("Dataset"), // Cluster
                                         new Field("UtilityMeasure"), // Type
                                         new Field("PrivacyModel")); // Value
        series3D.getData().clear();

        Iterator<Series2D> itr = seriesList.listIterator();
        for (double suppress : setup.getSuppressionLimits()) {
            Series2D series2D = itr.next();
            for (Point2D point : series2D.getData()) {
                series3D.getData().add(new Point3D(point.x, "Suppression Limit " + suppress +
                                                            " (Runtime)", point.y));
            }
        }

        // Plot
        List<Plot<?>> plots = new ArrayList<Plot<?>>();
        plots.add(new PlotLinesClustered("Algorithm: " + algorithm.toString() + " / Dataset: " +
                                                 data.toString() + " / Measure: " +
                                                 measure.toString() + " / Model: " +
                                                 model.toString() + " / gsStepSize: " + gsStepSize,
                                         new Labels("Factor: Generalization / Suppression",
                                                    "Runtime"), series3D));

        GnuPlotParams params = new GnuPlotParams();
        params.colorize = true;
        params.rotateXTicks = 0;
        params.keypos = KeyPos.TOP_LEFT;
        params.size = 1.0d;
        params.ratio = 0.5d;
        params.minY = 0d;
        return new PlotGroup("Development of runtime with different generalization/suppression weights. ",
                             plots,
                             params,
                             1.0d);
    }
}
