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
public class BenchmarkAnalysisRecordScaling {

    /**
     * Choose benchmarkConfig to run and comment others out.
     */
    private static final String benchmarkConfig = "benchmarkConfig/recordScaling.xml";

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

        groups.add(analyzeRuntime(file,
                                  BenchmarkDataset.ADULT,
                                  BenchmarkUtilityMeasure.LOSS,
                                  BenchmarkPrivacyModel.K5_ANONYMITY,
                                  null,
                                  0.1,
                                  0.0,
                                  0.05));
        groups.add(analyzeUtility(file,
                                  BenchmarkDataset.ADULT,
                                  BenchmarkUtilityMeasure.LOSS,
                                  BenchmarkPrivacyModel.K5_ANONYMITY,
                                  null,
                                  0.1,
                                  0.0,
                                  0.05));
        groups.add(analyzeRuntime(file,
                                  BenchmarkDataset.ADULT,
                                  BenchmarkUtilityMeasure.LOSS,
                                  BenchmarkPrivacyModel.K5_ANONYMITY,
                                  null,
                                  0.95,
                                  0.0,
                                  0.05));
        groups.add(analyzeUtility(file,
                                  BenchmarkDataset.ADULT,
                                  BenchmarkUtilityMeasure.LOSS,
                                  BenchmarkPrivacyModel.K5_ANONYMITY,
                                  null,
                                  0.95,
                                  0.0,
                                  0.05));
        LaTeX.plot(groups, setup.getPlotFile(), true);

    }

    /**
     * Performs the analysis
     * 
     * @param file
     * @param suppressionLimit
     * @param algorithm
     * @param model
     * @param measure
     * @param data
     * @return
     * @throws ParseException
     */
    private static PlotGroup analyzeRuntime(CSVFile file,
                                            BenchmarkDataset data,
                                            BenchmarkUtilityMeasure measure,
                                            BenchmarkPrivacyModel model,
                                            BenchmarkAlgorithm algorithm,
                                            double suppressionLimit,
                                            double gsFactor,
                                            double gsStepSize) throws ParseException {

        // Selects according rows
        Selector<String[]> selectorRGR = file.getSelectorBuilder()
                                             .field("Dataset")
                                             .equals(data.toString())
                                             .and()
                                             .field("UtilityMeasure")
                                             .equals(measure.toString())
                                             .and()
                                             .field("PrivacyModel")
                                             .equals(model.toString())
                                             .and()
                                             .field("Algorithm")
                                             .equals(BenchmarkAlgorithm.RECURSIVE_GLOBAL_RECODING.toString())
                                             .and()
                                             .field("SuppressionLimit")
                                             .equals(String.valueOf(suppressionLimit))
                                             .build();

        // Selects according rows
        Selector<String[]> selectorFlash = file.getSelectorBuilder()
                                               .field("Dataset")
                                               .equals(data.toString())
                                               .and()
                                               .field("UtilityMeasure")
                                               .equals(measure.toString())
                                               .and()
                                               .field("PrivacyModel")
                                               .equals(model.toString())
                                               .and()
                                               .field("Algorithm")
                                               .equals(BenchmarkAlgorithm.FLASH.toString())
                                               .and()
                                               .field("SuppressionLimit")
                                               .equals(String.valueOf(suppressionLimit))
                                               .build();

        // Selects according rows
        Selector<String[]> selectorTassa = file.getSelectorBuilder()
                                               .field("Dataset")
                                               .equals(data.toString())
                                               .and()
                                               .field("UtilityMeasure")
                                               .equals(measure.toString())
                                               .and()
                                               .field("PrivacyModel")
                                               .equals(model.toString())
                                               .and()
                                               .field("Algorithm")
                                               .equals(BenchmarkAlgorithm.TASSA.toString())
                                               .and()
                                               .field("SuppressionLimit")
                                               .equals("0.0")
                                               .build();

        // Read data into 2D series
        Series2D rgrSeries = new Series2D(file,
                                          selectorRGR,
                                          new Field("Records"),
                                          new Field("Runtime", Analyzer.VALUE));

        // Read data into 2D series
        Series2D flashSeries = new Series2D(file,
                                            selectorFlash,
                                            new Field("Records"),
                                            new Field("Runtime", Analyzer.VALUE));

        // Read data into 2D series
        Series2D tassaSeries = new Series2D(file,
                                            selectorTassa,
                                            new Field("Records"),
                                            new Field("Runtime", Analyzer.VALUE));

        // Dirty hack for creating a 3D series from two 2D series'
        Series3D series = new Series3D(file, selectorRGR, new Field("Dataset"), // Cluster
                                       new Field("UtilityMeasure"), // Type
                                       new Field("PrivacyModel")); // Value
        series.getData().clear();
        for (Point2D point : rgrSeries.getData()) {
            series.getData().add(new Point3D(point.x,
                                             "RGR",
                                             String.valueOf((Double.valueOf(point.y) / 1000))));
        }
        for (Point2D point : flashSeries.getData()) {
            series.getData().add(new Point3D(point.x,
                                             "Flash",
                                             String.valueOf((Double.valueOf(point.y) / 1000))));
        }
        int tassaScalingFactor = 1;
        for (Point2D point : tassaSeries.getData()) {
            series.getData().add(new Point3D(point.x,
                                             "Clustering",
                                             String.valueOf((Double.valueOf(point.y) / tassaScalingFactor / 1000))));
        }

        // Plot
        List<Plot<?>> plots = new ArrayList<Plot<?>>();
        plots.add(new PlotLinesClustered(data.toString() + " / " + measure.toString() + " / " +
                                                 model.toString() + " / " + suppressionLimit,
                                         new Labels("Records", "Time [s]"),
                                         series));

        GnuPlotParams params = new GnuPlotParams();
        params.colorize = true;
        params.rotateXTicks = 0;
        params.keypos = KeyPos.TOP_LEFT;
        params.size = 1.0d;
        params.ratio = 0.5d;
        return new PlotGroup("Scaling of anonymization algorithms with number of records. ",
                             plots,
                             params,
                             1.0d);
    }

    /**
     * Performs the analysis
     * 
     * @param file
     * @param suppressionLimit
     * @param algorithm
     * @param model
     * @param measure
     * @param data
     * @return
     * @throws ParseException
     */
    private static PlotGroup analyzeUtility(CSVFile file,
                                            BenchmarkDataset data,
                                            BenchmarkUtilityMeasure measure,
                                            BenchmarkPrivacyModel model,
                                            BenchmarkAlgorithm algorithm,
                                            double suppressionLimit,
                                            double gsFactor,
                                            double gsStepSize) throws ParseException {

        // Selects according rows
        Selector<String[]> selectorRGR = file.getSelectorBuilder()
                                             .field("Dataset")
                                             .equals(data.toString())
                                             .and()
                                             .field("UtilityMeasure")
                                             .equals(measure.toString())
                                             .and()
                                             .field("PrivacyModel")
                                             .equals(model.toString())
                                             .and()
                                             .field("Algorithm")
                                             .equals(BenchmarkAlgorithm.RECURSIVE_GLOBAL_RECODING.toString())
                                             .and()
                                             .field("SuppressionLimit")
                                             .equals(String.valueOf(suppressionLimit))
                                             .build();

        // Selects according rows
        Selector<String[]> selectorFlash = file.getSelectorBuilder()
                                               .field("Dataset")
                                               .equals(data.toString())
                                               .and()
                                               .field("UtilityMeasure")
                                               .equals(measure.toString())
                                               .and()
                                               .field("PrivacyModel")
                                               .equals(model.toString())
                                               .and()
                                               .field("Algorithm")
                                               .equals(BenchmarkAlgorithm.FLASH.toString())
                                               .and()
                                               .field("SuppressionLimit")
                                               .equals(String.valueOf(suppressionLimit))
                                               .build();

        // Selects according rows
        Selector<String[]> selectorTassa = file.getSelectorBuilder()
                                               .field("Dataset")
                                               .equals(data.toString())
                                               .and()
                                               .field("UtilityMeasure")
                                               .equals(measure.toString())
                                               .and()
                                               .field("PrivacyModel")
                                               .equals(model.toString())
                                               .and()
                                               .field("Algorithm")
                                               .equals(BenchmarkAlgorithm.TASSA.toString())
                                               .and()
                                               .field("SuppressionLimit")
                                               .equals("0.0")
                                               .build();

        // Read data into 2D series
        Series2D rgrSeries = new Series2D(file,
                                          selectorRGR,
                                          new Field("Records"),
                                          new Field("Utility", Analyzer.VALUE));

        // Read data into 2D series
        Series2D flashSeries = new Series2D(file,
                                            selectorFlash,
                                            new Field("Records"),
                                            new Field("Utility", Analyzer.VALUE));

        // Read data into 2D series
        Series2D tassaSeries = new Series2D(file,
                                            selectorTassa,
                                            new Field("Records"),
                                            new Field("Utility", Analyzer.VALUE));

        // Dirty hack for creating a 3D series from two 2D series'
        Series3D series = new Series3D(file, selectorRGR, new Field("Dataset"), // Cluster
                                       new Field("UtilityMeasure"), // Type
                                       new Field("PrivacyModel")); // Value
        series.getData().clear();
        for (Point2D point : rgrSeries.getData()) {
            series.getData().add(new Point3D(point.x,
                                             "RGR",
                                             String.valueOf((1 - Double.valueOf(point.y)) * 100)));
        }
        for (Point2D point : flashSeries.getData()) {
            series.getData().add(new Point3D(point.x,
                                             "Flash",
                                             String.valueOf((1 - Double.valueOf(point.y)) * 100)));
        }
        for (Point2D point : tassaSeries.getData()) {
            series.getData().add(new Point3D(point.x,
                                             "Clustering",
                                             String.valueOf((1 - Double.valueOf(point.y)) * 100)));
        }

        // Plot
        List<Plot<?>> plots = new ArrayList<Plot<?>>();
        plots.add(new PlotLinesClustered(data.toString() + " / " + measure.toString() + " / " +
                                                 model.toString() + " / " + suppressionLimit,
                                         new Labels("Records", "Utility [%]"),
                                         series));

        GnuPlotParams params = new GnuPlotParams();
        params.colorize = true;
        params.rotateXTicks = 0;
        params.keypos = KeyPos.BOTTOM_RIGHT;
        params.size = 1.0d;
        params.ratio = 0.5d;
        params.minY = 0d;
        params.maxY = 100d;
        return new PlotGroup("Scaling of anonymization algorithms with number of records. ",
                             plots,
                             params,
                             1.0d);
    }
}
