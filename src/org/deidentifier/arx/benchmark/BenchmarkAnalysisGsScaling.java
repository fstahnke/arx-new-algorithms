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
public class BenchmarkAnalysisGsScaling {

    /**
     * Choose benchmarkConfig to run and comment others out.
     */
    // private static final String benchmarkConfig =
    // "benchmarkConfig/recordScaling.xml";
    // private static final String benchmarkConfig =
    // "benchmarkConfig/QIScaling.xml";
    private static final String benchmarkConfig = "benchmarkConfig/gsFactorScaling.xml";

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

        groups.add(analyze(file,
                           BenchmarkDataset.ADULT,
                           BenchmarkUtilityMeasure.LOSS,
                           BenchmarkPrivacyModel.K5_ANONYMITY,
                           BenchmarkAlgorithm.RECURSIVE_GLOBAL_RECODING,
                           0.0));
        groups.add(analyze(file,
                           BenchmarkDataset.ADULT,
                           BenchmarkUtilityMeasure.LOSS,
                           BenchmarkPrivacyModel.K5_ANONYMITY,
                           BenchmarkAlgorithm.FLASH,
                           0.0));
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
    private static PlotGroup analyze(CSVFile file,
                                     BenchmarkDataset data,
                                     BenchmarkUtilityMeasure measure,
                                     BenchmarkPrivacyModel model,
                                     BenchmarkAlgorithm algorithm,
                                     double suppression) throws ParseException {

        // Selects according rows
        Selector<String[]> selector5Percent = file.getSelectorBuilder()
                                                  .field("Algorithm")
                                                  .equals(algorithm.toString())
                                                  .and()
                                                  .field("Suppression Limit")
                                                  .equals("0.05")
                                                  .build();

        // Selects according rows
        Selector<String[]> selector100Percent = file.getSelectorBuilder()
                                                    .field("Algorithm")
                                                    .equals(algorithm.toString())
                                                    .and()
                                                    .field("Suppression Limit")
                                                    .equals("1.0")
                                                    .build();

        // Read data into 2D series
        Series2D fiveSeriesRuntime = new Series2D(file,
                                                  selector5Percent,
                                                  new Field("Suppression Weight"),
                                                  new Field("Runtime", Analyzer.VALUE));

        // Read data into 2D series
        Series2D fiveSeriesUtility = new Series2D(file,
                                                  selector5Percent,
                                                  new Field("Suppression Weight"),
                                                  new Field("Utility", Analyzer.VALUE));

        // Read data into 2D series
        Series2D hundredSeriesRuntime = new Series2D(file,
                                                     selector100Percent,
                                                     new Field("Suppression Weight"),
                                                     new Field("Runtime", Analyzer.VALUE));

        // Read data into 2D series
        Series2D hundredSeriesUtility = new Series2D(file,
                                                     selector100Percent,
                                                     new Field("Suppression Weight"),
                                                     new Field("Utility", Analyzer.VALUE));

        // Dirty hack for creating a 3D series from two 2D series'
        Series3D series = new Series3D(file,
                                       selector5Percent,
                                       new Field("Dataset"), // Cluster
                                       new Field("UtilityMeasure"), // Type
                                       new Field("PrivacyModel")); // Value
        series.getData().clear();
        for (Point2D point : fiveSeriesUtility.getData()) {
            series.getData()
                  .add(new Point3D(point.x,
                                   "Suppression Limit 0.05 (Utility)",
                                   String.valueOf(1 - Double.valueOf(point.y))));
        }
        for (Point2D point : hundredSeriesUtility.getData()) {
            series.getData()
                  .add(new Point3D(point.x,
                                   "Suppression Limit 1.0 (Utility)",
                                   String.valueOf(1 - Double.valueOf(point.y))));
        }
        int runtimeScalingFactor = 10000;
        for (Point2D point : fiveSeriesRuntime.getData()) {
            series.getData()
                  .add(new Point3D(point.x,
                                   "Suppression Limit 0.05 (Runtime)",
                                   String.valueOf(Double.valueOf(point.y) / runtimeScalingFactor)));
        }
        for (Point2D point : hundredSeriesRuntime.getData()) {
            series.getData()
                  .add(new Point3D(point.x,
                                   "Suppression Limit 1.0 (Runtime)",
                                   String.valueOf(Double.valueOf(point.y) / runtimeScalingFactor)));
        }

        // Plot
        List<Plot<?>> plots = new ArrayList<Plot<?>>();
        plots.add(new PlotLinesClustered(algorithm.toString() + " / " + data.toString() + " / " +
                                         measure.toString(),
                                         new Labels("Factor: Generalization / Suppression", "Utility"),
                                         series));

        GnuPlotParams params = new GnuPlotParams();
        params.colorize = true;
        params.rotateXTicks = 0;
        params.keypos = KeyPos.TOP_LEFT;
        params.size = 1.0d;
        params.ratio = 0.5d;
        params.minY = 0d;
        params.maxY = 1d;
        return new PlotGroup("Development of utility and runtime with different generalization/suppression weights. ",
                             plots,
                             params,
                             1.0d);
    }
}
