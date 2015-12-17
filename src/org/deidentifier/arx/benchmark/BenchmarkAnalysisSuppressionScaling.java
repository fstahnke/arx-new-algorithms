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
public class BenchmarkAnalysisSuppressionScaling {

    /**
     * Choose benchmarkConfig to run.
     */
    private static final String benchmarkConfig = "benchmarkConfig/suppressionScaling.xml";

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
            for (double gsStepSize : setup.getGsStepSizes()) {
                for (double gsFactor : setup.getGsFactors()) {
                    groups.add(analyzeUtility(file,
                                              dataset,
                                              BenchmarkUtilityMeasure.LOSS,
                                              BenchmarkPrivacyModel.K5_ANONYMITY,
                                              BenchmarkAlgorithm.RECURSIVE_GLOBAL_RECODING,
                                              0.0,
                                              gsFactor,
                                              gsStepSize));
                    groups.add(analyzeRuntime(file,
                                              dataset,
                                              BenchmarkUtilityMeasure.LOSS,
                                              BenchmarkPrivacyModel.K5_ANONYMITY,
                                              BenchmarkAlgorithm.RECURSIVE_GLOBAL_RECODING,
                                              0.0,
                                              gsFactor,
                                              gsStepSize));
                    groups.add(analyzeHeterogeneity(file,
                                              dataset,
                                              BenchmarkUtilityMeasure.LOSS,
                                              BenchmarkPrivacyModel.K5_ANONYMITY,
                                              BenchmarkAlgorithm.RECURSIVE_GLOBAL_RECODING,
                                              0.0,
                                              gsFactor,
                                              gsStepSize));
                }
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
     * @param dataset
     * @return
     * @throws ParseException
     */
    private static PlotGroup analyzeUtility(CSVFile file,
                                            BenchmarkDataset dataset,
                                            BenchmarkUtilityMeasure measure,
                                            BenchmarkPrivacyModel model,
                                            BenchmarkAlgorithm algorithm,
                                            double suppression,
                                            double gsFactor,
                                            double gsStepSize) throws ParseException {

        // Selects according rows
        Selector<String[]> selectorRGR = file.getSelectorBuilder()
                                             .field("Algorithm")
                                             .equals(algorithm.toString())
                                             .and()
                                             .field("Dataset")
                                             .equals(dataset.toString())
                                             .and()
                                             .field("gsFactor")
                                             .equals(String.valueOf(gsFactor))
                                             .and()
                                             .field("gsFactorStepSize")
                                             .equals(String.valueOf(gsStepSize))
                                             .and()
                                             .field("SuppressionLimit")
                                             .greater(String.valueOf(0d))
                                             .build();

        // Read data into 2D series
        Series2D rgrSeriesUtility = new Series2D(file,
                                                 selectorRGR,
                                                 new Field("SuppressionLimit"),
                                                 new Field("Utility", Analyzer.VALUE));

        // Dirty hack for creating a 3D series from two 2D series'
        Series3D series = new Series3D(file, selectorRGR, new Field("Dataset"), // Cluster
                                       new Field("UtilityMeasure"), // Type
                                       new Field("PrivacyModel")); // Value
        series.getData().clear();
        for (Point2D point : rgrSeriesUtility.getData()) {
            series.getData().add(new Point3D(point.x,
                                             "RGR (Utility)",
                                             String.valueOf(1 - Double.valueOf(point.y))));
        }

        // Plot
        List<Plot<?>> plots = new ArrayList<Plot<?>>();
        plots.add(new PlotLinesClustered("Dataset " + dataset.toString() + " / Measure: " +
                                         measure.toString() + " / gsFactor: " + gsFactor +
                                         " / gsStepSize: " + gsStepSize + " / Algorithm: " +
                                         algorithm.toString(), new Labels("Suppression Limit",
                                                                          "Utility"), series));

        GnuPlotParams params = new GnuPlotParams();
        params.colorize = true;
        params.rotateXTicks = 0;
        params.keypos = KeyPos.BOTTOM_RIGHT;
        params.size = 1.0d;
        params.ratio = 0.5d;
        params.minY = 0d;
        params.maxY = 1d;
        return new PlotGroup("Development of utility with different suppression limits. ",
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
     * @param dataset
     * @return
     * @throws ParseException
     */
    private static PlotGroup analyzeRuntime(CSVFile file,
                                            BenchmarkDataset dataset,
                                            BenchmarkUtilityMeasure measure,
                                            BenchmarkPrivacyModel model,
                                            BenchmarkAlgorithm algorithm,
                                            double suppression,
                                            double gsFactor,
                                            double gsStepSize) throws ParseException {

        // Selects according rows
        Selector<String[]> selectorRGR = file.getSelectorBuilder()
                                             .field("Algorithm")
                                             .equals(algorithm.toString())
                                             .and()
                                             .field("Dataset")
                                             .equals(dataset.toString())
                                             .and()
                                             .field("gsFactor")
                                             .equals(String.valueOf(gsFactor))
                                             .and()
                                             .field("gsFactorStepSize")
                                             .equals(String.valueOf(gsStepSize))
                                             .and()
                                             .field("SuppressionLimit")
                                             .greater(String.valueOf(0d))
                                             .build();

        // Read data into 2D series
        Series2D rgrSeriesRuntime = new Series2D(file,
                                                 selectorRGR,
                                                 new Field("SuppressionLimit"),
                                                 new Field("Runtime", Analyzer.VALUE));

        // Dirty hack for creating a 3D series from two 2D series'
        Series3D series = new Series3D(file, selectorRGR, new Field("Dataset"), // Cluster
                                       new Field("UtilityMeasure"), // Type
                                       new Field("PrivacyModel")); // Value
        series.getData().clear();

        for (Point2D point : rgrSeriesRuntime.getData()) {
            series.getData().add(new Point3D(point.x, "RGR (Runtime)", point.y));
        }

        // Plot
        List<Plot<?>> plots = new ArrayList<Plot<?>>();
        plots.add(new PlotLinesClustered("Dataset " + dataset.toString() + " / Measure: " +
                                         measure.toString() + " / gsFactor: " + gsFactor +
                                         " / gsStepSize: " + gsStepSize + " / Algorithm: " +
                                         algorithm.toString(), new Labels("Suppression Limit",
                                                                          "Runtime [ms]"), series));

        GnuPlotParams params = new GnuPlotParams();
        params.colorize = true;
        params.rotateXTicks = 0;
        params.keypos = KeyPos.BOTTOM_RIGHT;
        params.size = 1.0d;
        params.ratio = 0.5d;
        params.minY = 0d;
        return new PlotGroup("Development of runtime with different suppression limits. ",
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
     * @param dataset
     * @return
     * @throws ParseException
     */
    private static PlotGroup analyzeHeterogeneity(CSVFile file,
                                            BenchmarkDataset dataset,
                                            BenchmarkUtilityMeasure measure,
                                            BenchmarkPrivacyModel model,
                                            BenchmarkAlgorithm algorithm,
                                            double suppression,
                                            double gsFactor,
                                            double gsStepSize) throws ParseException {

        // Selects according rows
        Selector<String[]> selectorRGR = file.getSelectorBuilder()
                                             .field("Algorithm")
                                             .equals(algorithm.toString())
                                             .and()
                                             .field("Dataset")
                                             .equals(dataset.toString())
                                             .and()
                                             .field("gsFactor")
                                             .equals(String.valueOf(gsFactor))
                                             .and()
                                             .field("gsFactorStepSize")
                                             .equals(String.valueOf(gsStepSize))
                                             .and()
                                             .field("SuppressionLimit")
                                             .greater(String.valueOf(0d))
                                             .build();

        // Read data into 2D series
        Series2D rgrSeriesRuntime = new Series2D(file,
                                                 selectorRGR,
                                                 new Field("SuppressionLimit"),
                                                 new Field("Transformations", Analyzer.VALUE));

        // Dirty hack for creating a 3D series from two 2D series'
        Series3D series = new Series3D(file, selectorRGR, new Field("Dataset"), // Cluster
                                       new Field("UtilityMeasure"), // Type
                                       new Field("PrivacyModel")); // Value
        series.getData().clear();

        for (Point2D point : rgrSeriesRuntime.getData()) {
            series.getData().add(new Point3D(point.x, "RGR (Transformations)", point.y));
        }

        // Plot
        List<Plot<?>> plots = new ArrayList<Plot<?>>();
        plots.add(new PlotLinesClustered("Dataset " + dataset.toString() + " / Measure: " +
                                         measure.toString() + " / gsFactor: " + gsFactor +
                                         " / gsStepSize: " + gsStepSize + " / Algorithm: " +
                                         algorithm.toString(), new Labels("Suppression Limit",
                                                                          "Heterogeneity"), series));

        GnuPlotParams params = new GnuPlotParams();
        params.colorize = true;
        params.rotateXTicks = 0;
        params.keypos = KeyPos.BOTTOM_RIGHT;
        params.size = 1.0d;
        params.ratio = 0.5d;
        params.minY = 0d;
        return new PlotGroup("Development of heterogeneity with different suppression limits. ",
                             plots,
                             params,
                             1.0d);
    }
}
