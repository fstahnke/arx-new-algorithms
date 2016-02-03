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
import de.linearbits.subframe.graph.PlotHistogramClustered;
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
public class BenchmarkAnalysisBoxPlots {

    /**
     * Choose benchmarkConfig
     */
    private static final String benchmarkConfig = "benchmarkConfig/utilityVarianceSuppression.xml";

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

        // Each iteration in this loop is another Figure
        for (BenchmarkDataset dataset : setup.getDatasets()) {
            for (BenchmarkUtilityMeasure measure : setup.getUtilityMeasures()) {
                // for (BenchmarkPrivacyModel model : setup.getPrivacyModels())
                // {
                for (double suppressionLimit : setup.getSuppressionLimits()) {
                    for (double gsFactor : setup.getGsFactors()) {
//                        for (double gsFactorStepSize : setup.getGsStepSizes()) {
                        double gsFactorStepSize = 0d;
                            groups.add(analyzeUtility(file,
                                                      dataset,
                                                      measure,
                                                      null,
                                                      null,
                                                      suppressionLimit,
                                                      gsFactor,
                                                      gsFactorStepSize));
                            groups.add(analyzeRuntime(file,
                                                      dataset,
                                                      measure,
                                                      null,
                                                      null,
                                                      suppressionLimit,
                                                      gsFactor,
                                                      gsFactorStepSize));
//                            groups.add(analyzeVariance(file,
//                                                       dataset,
//                                                       measure,
//                                                       null,
//                                                       null,
//                                                       suppressionLimit,
//                                                       gsFactor,
//                                                       gsFactorStepSize));
                            groups.add(analyzeTransformations(file,
                                                       dataset,
                                                       measure,
                                                       null,
                                                       null,
                                                       suppressionLimit,
                                                       gsFactor,
                                                       gsFactorStepSize));
//                        }
                    }
                }
                // }
            }
        }

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
    private static PlotGroup analyzeUtility(CSVFile file,
                                            BenchmarkDataset data,
                                            BenchmarkUtilityMeasure measure,
                                            BenchmarkPrivacyModel model,
                                            BenchmarkAlgorithm algorithm,
                                            double suppressionLimit,
                                            double gsFactor,
                                            double gsFactorStepSize) throws ParseException {
        
        double flashSuppressionLimit = suppressionLimit;

        // Selects according rows
        Selector<String[]> selectorRGR = file.getSelectorBuilder()
                                             .field("Dataset")
                                             .equals(data.toString())
                                             .and()
                                             .field("UtilityMeasure")
                                             .equals(measure.toString())
                                             // .and()
                                             // .field("PrivacyModel")
                                             // .equals(model.toString())
                                             .and()
                                             .field("Algorithm")
                                             .equals(BenchmarkAlgorithm.RECURSIVE_GLOBAL_RECODING.toString())
                                             .and()
                                             .field("SuppressionLimit")
                                             .equals(String.valueOf(suppressionLimit))
                                             .and()
                                             .field("gsFactor")
                                             .equals(String.valueOf(gsFactor))
//                                             .and()
//                                             .field("gsFactorStepSize")
//                                             .equals(String.valueOf(gsFactorStepSize))
                                             .build();

        // Selects according rows
        Selector<String[]> selectorFlash = file.getSelectorBuilder()
                                               .field("Dataset")
                                               .equals(data.toString())
                                               .and()
                                               .field("UtilityMeasure")
                                               .equals(measure.toString())
                                               // .and()
                                               // .field("PrivacyModel")
                                               // .equals(model.toString())
                                               .and()
                                               .field("Algorithm")
                                               .equals(BenchmarkAlgorithm.FLASH.toString())
                                               .and()
                                               .field("SuppressionLimit")
                                               .equals(String.valueOf(flashSuppressionLimit))
                                               .and()
                                               .field("gsFactor")
                                               .equals("0.5")
//                                               .and()
//                                               .field("gsFactorStepSize")
//                                               .equals("0.0")
                                               .build();

        // Selects according rows
        Selector<String[]> selectorTassa = file.getSelectorBuilder()
                                               .field("Dataset")
                                               .equals(data.toString())
                                               .and()
                                               .field("UtilityMeasure")
                                               .equals(measure.toString())
                                               // .and()
                                               // .field("PrivacyModel")
                                               // .equals(model.toString())
                                               .and()
                                               .field("Algorithm")
                                               .equals(BenchmarkAlgorithm.TASSA.toString())
                                               .and()
                                               .field("SuppressionLimit")
                                               .equals("0.0")
                                               .and()
                                               .field("gsFactor")
                                               .equals("0.0")
//                                               .and()
//                                               .field("gsFactorStepSize")
//                                               .equals("0.0")
                                               .build();

        // Read data into 2D series
        Series2D rgrSeries = new Series2D(file,
                                          selectorRGR,
                                          new Field("K"),
                                          new Field("Utility", Analyzer.VALUE));

        // Read data into 2D series
        Series2D flashSeries = new Series2D(file,
                                            selectorFlash,
                                            new Field("K"),
                                            new Field("Utility", Analyzer.VALUE));

        // Read data into 2D series
        Series2D tassaSeries = new Series2D(file,
                                            selectorTassa,
                                            new Field("K"),
                                            new Field("Utility", Analyzer.VALUE));

        // Dirty hack for creating a 3D series from two 2D series'
        Series3D series = new Series3D(file, selectorRGR, new Field("Dataset"), // Cluster
                                       new Field("UtilityMeasure"), // Type
                                       new Field("PrivacyModel")); // Value
        series.getData().clear();
        for (Point2D point : tassaSeries.getData()) {
            series.getData().add(new Point3D(point.x,
                                             BenchmarkAlgorithm.TASSA.toString(),
                                             String.valueOf(1 - Double.valueOf(point.y))));
        }
        for (Point2D point : rgrSeries.getData()) {
            series.getData()
                  .add(new Point3D(point.x,
                                   BenchmarkAlgorithm.RECURSIVE_GLOBAL_RECODING.toString(),
                                   String.valueOf(1 - Double.valueOf(point.y))));
        }
        for (Point2D point : flashSeries.getData()) {
            series.getData().add(new Point3D(point.x,
                                             BenchmarkAlgorithm.FLASH.toString(),
                                             String.valueOf(1 - Double.valueOf(point.y))));
        }

        // Plot
        List<Plot<?>> plots = new ArrayList<Plot<?>>();

        plots.add(new PlotHistogramClustered("Dataset: " + data.toString() + " / Measure: " +
                                                     measure.toString() + " / Suppression Limit: " +
                                                     suppressionLimit + " / gsFactor: " + gsFactor +
                                                     " / gsStepSize: " + gsFactorStepSize,
                                             new Labels("minGroupSize", "Utility [%]"),
                                             series));

        GnuPlotParams params = new GnuPlotParams();
        params.printValues = false;
        params.colorize = true;
        params.rotateXTicks = 0;
        params.keypos = KeyPos.TOP_RIGHT;
        params.size = 1.0d;
        params.ratio = 0.5d;
        if (measure != BenchmarkUtilityMeasure.DISCERNIBILITY) {
            params.minY = 0d;
        }
        params.maxY = 1d;
        return new PlotGroup("Comparison of utility of RGR, Flash and Tassa with different K. gsFactor and gsStepSize only apply to RGR.",
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
    private static PlotGroup analyzeTransformations(CSVFile file,
                                                    BenchmarkDataset data,
                                                    BenchmarkUtilityMeasure measure,
                                                    BenchmarkPrivacyModel model,
                                                    BenchmarkAlgorithm algorithm,
                                                    double suppressionLimit,
                                                    double gsFactor,
                                                    double gsFactorStepSize) throws ParseException {
        

        double flashSuppressionLimit = suppressionLimit;

        // Selects according rows
        Selector<String[]> selectorRGR = file.getSelectorBuilder()
                                             .field("Dataset")
                                             .equals(data.toString())
                                             .and()
                                             .field("UtilityMeasure")
                                             .equals(measure.toString())
                                             // .and()
                                             // .field("PrivacyModel")
                                             // .equals(model.toString())
                                             .and()
                                             .field("Algorithm")
                                             .equals(BenchmarkAlgorithm.RECURSIVE_GLOBAL_RECODING.toString())
                                             .and()
                                             .field("SuppressionLimit")
                                             .equals(String.valueOf(suppressionLimit))
                                             .and()
                                             .field("gsFactor")
                                             .equals(String.valueOf(gsFactor))
//                                             .and()
//                                             .field("gsFactorStepSize")
//                                             .equals(String.valueOf(gsFactorStepSize))
                                             .build();

        // Selects according rows
        Selector<String[]> selectorFlash = file.getSelectorBuilder()
                                               .field("Dataset")
                                               .equals(data.toString())
                                               .and()
                                               .field("UtilityMeasure")
                                               .equals(measure.toString())
                                               // .and()
                                               // .field("PrivacyModel")
                                               // .equals(model.toString())
                                               .and()
                                               .field("Algorithm")
                                               .equals(BenchmarkAlgorithm.FLASH.toString())
                                               .and()
                                               .field("SuppressionLimit")
                                               .equals(String.valueOf(String.valueOf(flashSuppressionLimit)))
                                               .and()
                                               .field("gsFactor")
                                               .equals("0.5")
//                                               .and()
//                                               .field("gsFactorStepSize")
//                                               .equals("0.0")
                                               .build();

        // Selects according rows
        Selector<String[]> selectorTassa = file.getSelectorBuilder()
                                               .field("Dataset")
                                               .equals(data.toString())
                                               .and()
                                               .field("UtilityMeasure")
                                               .equals(measure.toString())
                                               // .and()
                                               // .field("PrivacyModel")
                                               // .equals(model.toString())
                                               .and()
                                               .field("Algorithm")
                                               .equals(BenchmarkAlgorithm.TASSA.toString())
                                               .and()
                                               .field("SuppressionLimit")
                                               .equals("0.0")
                                               .and()
                                               .field("gsFactor")
                                               .equals("0.0")
//                                               .and()
//                                               .field("gsFactorStepSize")
//                                               .equals("0.0")
                                               .build();

        // Read data into 2D series
        Series2D rgrSeries = new Series2D(file,
                                          selectorRGR,
                                          new Field("K"),
                                          new Field("Transformations", Analyzer.VALUE));

        // Read data into 2D series
        Series2D flashSeries = new Series2D(file,
                                            selectorFlash,
                                            new Field("K"),
                                            new Field("Transformations", Analyzer.VALUE));

        // Read data into 2D series
        Series2D tassaSeries = new Series2D(file,
                                            selectorTassa,
                                            new Field("K"),
                                            new Field("Transformations", Analyzer.VALUE));

        // Dirty hack for creating a 3D series from two 2D series'
        Series3D series = new Series3D(file, selectorRGR, new Field("Dataset"), // Cluster
                                       new Field("UtilityMeasure"), // Type
                                       new Field("PrivacyModel")); // Value
        series.getData().clear();
        for (Point2D point : tassaSeries.getData()) {
            series.getData()
                  .add(new Point3D(point.x, BenchmarkAlgorithm.TASSA.toString(), point.y));
        }
        for (Point2D point : rgrSeries.getData()) {
            series.getData()
                  .add(new Point3D(point.x,
                                   BenchmarkAlgorithm.RECURSIVE_GLOBAL_RECODING.toString(),
                                   point.y));
        }
        for (Point2D point : flashSeries.getData()) {
            series.getData()
                  .add(new Point3D(point.x, BenchmarkAlgorithm.FLASH.toString(), point.y));
        }

        // Plot
        List<Plot<?>> plots = new ArrayList<Plot<?>>();

        plots.add(new PlotHistogramClustered("Dataset: " + data.toString() + " / Measure: " +
                                                     measure.toString() + " / Suppression Limit: " +
                                                     suppressionLimit + " / gsFactor: " + gsFactor +
                                                     " / gsStepSize: " + gsFactorStepSize,
                                             new Labels("minGroupSize", "Number of transformations"),
                                             series));

        GnuPlotParams params = new GnuPlotParams();
        params.printValues = false;
        params.colorize = true;
        params.rotateXTicks = 0;
        params.keypos = KeyPos.TOP_RIGHT;
        params.size = 1.0d;
        params.ratio = 0.5d;
        params.minY = 0d;
//        params.maxY = 600d;
        return new PlotGroup("Comparison of standard deviation of RGR, Flash and Tassa with different K. gsFactor and gsStepSize only apply to RGR.",
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
    private static PlotGroup analyzeVariance(CSVFile file,
                                             BenchmarkDataset data,
                                             BenchmarkUtilityMeasure measure,
                                             BenchmarkPrivacyModel model,
                                             BenchmarkAlgorithm algorithm,
                                             double suppressionLimit,
                                             double gsFactor,
                                             double gsFactorStepSize) throws ParseException {
        

        double flashSuppressionLimit = suppressionLimit;

        // Selects according rows
        Selector<String[]> selectorRGR = file.getSelectorBuilder()
                                             .field("Dataset")
                                             .equals(data.toString())
                                             .and()
                                             .field("UtilityMeasure")
                                             .equals(measure.toString())
                                             // .and()
                                             // .field("PrivacyModel")
                                             // .equals(model.toString())
                                             .and()
                                             .field("Algorithm")
                                             .equals(BenchmarkAlgorithm.RECURSIVE_GLOBAL_RECODING.toString())
                                             .and()
                                             .field("SuppressionLimit")
                                             .equals(String.valueOf(suppressionLimit))
                                             .and()
                                             .field("gsFactor")
                                             .equals(String.valueOf(gsFactor))
//                                             .and()
//                                             .field("gsFactorStepSize")
//                                             .equals(String.valueOf(gsFactorStepSize))
                                             .build();

        // Selects according rows
        Selector<String[]> selectorFlash = file.getSelectorBuilder()
                                               .field("Dataset")
                                               .equals(data.toString())
                                               .and()
                                               .field("UtilityMeasure")
                                               .equals(measure.toString())
                                               // .and()
                                               // .field("PrivacyModel")
                                               // .equals(model.toString())
                                               .and()
                                               .field("Algorithm")
                                               .equals(BenchmarkAlgorithm.FLASH.toString())
                                               .and()
                                               .field("SuppressionLimit")
                                               .equals(String.valueOf(flashSuppressionLimit))
                                               .and()
                                               .field("gsFactor")
                                               .equals("0.5")
//                                               .and()
//                                               .field("gsFactorStepSize")
//                                               .equals("0.0")
                                               .build();

        // Selects according rows
        Selector<String[]> selectorTassa = file.getSelectorBuilder()
                                               .field("Dataset")
                                               .equals(data.toString())
                                               .and()
                                               .field("UtilityMeasure")
                                               .equals(measure.toString())
                                               // .and()
                                               // .field("PrivacyModel")
                                               // .equals(model.toString())
                                               .and()
                                               .field("Algorithm")
                                               .equals(BenchmarkAlgorithm.TASSA.toString())
                                               .and()
                                               .field("SuppressionLimit")
                                               .equals("0.0")
                                               .and()
                                               .field("gsFactor")
                                               .equals("0.0")
//                                               .and()
//                                               .field("gsFactorStepSize")
//                                               .equals("0.0")
                                               .build();

        // Read data into 2D series
        Series2D rgrSeries = new Series2D(file,
                                          selectorRGR,
                                          new Field("K"),
                                          new Field("Variance", Analyzer.VALUE));

        // Read data into 2D series
        Series2D flashSeries = new Series2D(file,
                                            selectorFlash,
                                            new Field("K"),
                                            new Field("Variance", Analyzer.VALUE));

        // Read data into 2D series
        Series2D tassaSeries = new Series2D(file,
                                            selectorTassa,
                                            new Field("K"),
                                            new Field("Variance", Analyzer.VALUE));

        // Dirty hack for creating a 3D series from two 2D series'
        Series3D series = new Series3D(file, selectorRGR, new Field("Dataset"), // Cluster
                                       new Field("UtilityMeasure"), // Type
                                       new Field("PrivacyModel")); // Value
        series.getData().clear();
//        for (Point2D point : tassaSeries.getData()) {
//            series.getData().add(new Point3D(point.x,
//                                             BenchmarkAlgorithm.TASSA.toString(),
//                                             String.valueOf(Math.sqrt(Double.valueOf(point.y)))));
//        }
        for (Point2D point : rgrSeries.getData()) {
            series.getData()
                  .add(new Point3D(point.x,
                                   BenchmarkAlgorithm.RECURSIVE_GLOBAL_RECODING.toString(),
                                   String.valueOf(Math.sqrt(Double.valueOf(point.y)))));
        }
        for (Point2D point : flashSeries.getData()) {
            series.getData().add(new Point3D(point.x,
                                             BenchmarkAlgorithm.FLASH.toString(),
                                             String.valueOf(Math.sqrt(Double.valueOf(point.y)))));
        }

        // Plot
        List<Plot<?>> plots = new ArrayList<Plot<?>>();

        plots.add(new PlotHistogramClustered("Dataset: " + data.toString() + " / Measure: " +
                                                     measure.toString() + " / Suppression Limit: " +
                                                     suppressionLimit + " / gsFactor: " + gsFactor +
                                                     " / gsStepSize: " + gsFactorStepSize,
                                             new Labels("minGroupSize", "Standard deviation"),
                                             series));

        GnuPlotParams params = new GnuPlotParams();
        params.printValues = false;
        params.colorize = true;
        params.rotateXTicks = 0;
        params.keypos = KeyPos.TOP_RIGHT;
        params.size = 1.0d;
        params.ratio = 0.5d;
        if (measure != BenchmarkUtilityMeasure.DISCERNIBILITY) {
            params.minY = 0d;
        }
        return new PlotGroup("Comparison of standard deviation of RGR, Flash and Tassa with different K. gsFactor and gsStepSize only apply to RGR.",
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
    private static PlotGroup analyzeRuntime(CSVFile file,
                                            BenchmarkDataset data,
                                            BenchmarkUtilityMeasure measure,
                                            BenchmarkPrivacyModel model,
                                            BenchmarkAlgorithm algorithm,
                                            double suppressionLimit,
                                            double gsFactor,
                                            double gsFactorStepSize) throws ParseException {
        

        double flashSuppressionLimit = suppressionLimit;

        // Selects according rows
        Selector<String[]> selectorRGR = file.getSelectorBuilder()
                                             .field("Dataset")
                                             .equals(data.toString())
                                             .and()
                                             .field("UtilityMeasure")
                                             .equals(measure.toString())
                                             // .and()
                                             // .field("PrivacyModel")
                                             // .equals(model.toString())
                                             .and()
                                             .field("Algorithm")
                                             .equals(BenchmarkAlgorithm.RECURSIVE_GLOBAL_RECODING.toString())
                                             .and()
                                             .field("SuppressionLimit")
                                             .equals(String.valueOf(suppressionLimit))
                                             .and()
                                             .field("gsFactor")
                                             .equals(String.valueOf(gsFactor))
//                                             .and()
//                                             .field("gsFactorStepSize")
//                                             .equals(String.valueOf(gsFactorStepSize))
                                             .build();

        // Selects according rows
        Selector<String[]> selectorFlash = file.getSelectorBuilder()
                                               .field("Dataset")
                                               .equals(data.toString())
                                               .and()
                                               .field("UtilityMeasure")
                                               .equals(measure.toString())
                                               // .and()
                                               // .field("PrivacyModel")
                                               // .equals(model.toString())
                                               .and()
                                               .field("Algorithm")
                                               .equals(BenchmarkAlgorithm.FLASH.toString())
                                               .and()
                                               .field("SuppressionLimit")
                                               .equals(String.valueOf(flashSuppressionLimit))
                                               .and()
                                               .field("gsFactor")
                                               .equals("0.5")
//                                               .and()
//                                               .field("gsFactorStepSize")
//                                               .equals("0.0")
                                               .build();

        // Selects according rows
        Selector<String[]> selectorTassa = file.getSelectorBuilder()
                                               .field("Dataset")
                                               .equals(data.toString())
                                               .and()
                                               .field("UtilityMeasure")
                                               .equals(measure.toString())
                                               // .and()
                                               // .field("PrivacyModel")
                                               // .equals(model.toString())
                                               .and()
                                               .field("Algorithm")
                                               .equals(BenchmarkAlgorithm.TASSA.toString())
                                               .and()
                                               .field("SuppressionLimit")
                                               .equals("0.0")
                                               .and()
                                               .field("gsFactor")
                                               .equals("0.0")
//                                               .and()
//                                               .field("gsFactorStepSize")
//                                               .equals("0.0")
                                               .build();

        // Read data into 2D series
        Series2D rgrSeries = new Series2D(file,
                                          selectorRGR,
                                          new Field("K"),
                                          new Field("Runtime", Analyzer.VALUE));

        // Read data into 2D series
        Series2D flashSeries = new Series2D(file,
                                            selectorFlash,
                                            new Field("K"),
                                            new Field("Runtime", Analyzer.VALUE));

        // Read data into 2D series
        Series2D tassaSeries = new Series2D(file,
                                            selectorTassa,
                                            new Field("K"),
                                            new Field("Runtime", Analyzer.VALUE));

        // Dirty hack for creating a 3D series from two 2D series'
        Series3D series = new Series3D(file, selectorRGR, new Field("Dataset"), // Cluster
                                       new Field("UtilityMeasure"), // Type
                                       new Field("PrivacyModel")); // Value
        series.getData().clear();
        for (Point2D point : tassaSeries.getData()) {
            series.getData()
                  .add(new Point3D(point.x, BenchmarkAlgorithm.TASSA.toString(), String.valueOf(Double.valueOf(point.y) / 1000)));
        }
        for (Point2D point : rgrSeries.getData()) {
            series.getData()
                  .add(new Point3D(point.x,
                                   BenchmarkAlgorithm.RECURSIVE_GLOBAL_RECODING.toString(),
                                   String.valueOf(Double.valueOf(point.y) / 1000)));
        }
        for (Point2D point : flashSeries.getData()) {
            series.getData()
                  .add(new Point3D(point.x, BenchmarkAlgorithm.FLASH.toString(), String.valueOf(Double.valueOf(point.y) / 1000)));
        }

        // Plot
        List<Plot<?>> plots = new ArrayList<Plot<?>>();

        plots.add(new PlotHistogramClustered("Dataset: " + data.toString() + " / Measure: " +
                                                     measure.toString() + " / Suppression Limit: " +
                                                     suppressionLimit + " / gsFactor: " + gsFactor +
                                                     " / gsStepSize: " + gsFactorStepSize,
                                             new Labels("minGroupSize", "Execution time [s]"),
                                             series));

        GnuPlotParams params = new GnuPlotParams();
        params.colorize = true;
        params.rotateXTicks = 0;
        params.keypos = KeyPos.TOP_RIGHT;
        params.size = 1.0d;
        params.ratio = 0.5d;
        if (measure != BenchmarkUtilityMeasure.DISCERNIBILITY) {
            params.minY = 0d;
        }
        return new PlotGroup("Comparison of utility of RGR, Flash and Tassa with different K. gsFactor and gsStepSize only apply to RGR.",
                             plots,
                             params,
                             1.0d);
    }
}
