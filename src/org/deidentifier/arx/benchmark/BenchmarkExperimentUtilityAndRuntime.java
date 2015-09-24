/*
 * Benchmark of risk-based anonymization in ARX 3.0.0 Copyright 2015 - Fabian
 * Prasser
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
import java.util.Arrays;
import java.util.Map;

import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkAlgorithm;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkDataset;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkPrivacyModel;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkUtilityMeasure;
import org.deidentifier.arx.clustering.TassaAlgorithm;
import org.deidentifier.arx.recursive.BenchmarkAlgorithmRGR;
import org.deidentifier.arx.utility.AggregateFunction;
import org.deidentifier.arx.utility.DataConverter;
import org.deidentifier.arx.utility.UtilityMeasureDiscernibility;
import org.deidentifier.arx.utility.UtilityMeasureLoss;

import de.linearbits.subframe.Benchmark;
import de.linearbits.subframe.analyzer.ValueBuffer;

/**
 * BenchmarkExperiment analysing utility and runtime (with multiple runs).
 * 
 * @author Fabian Stahnke
 */
public class BenchmarkExperimentUtilityAndRuntime {
    

    /** The benchmark instance */
    private static final Benchmark BENCHMARK        = new Benchmark(new String[] {
            "Dataset",
            "UtilityMeasure",
            "PrivacyModel",
            "Algorithm",
            "Suppression"                          });

    /**
     * Choose benchmarkConfig to run and comment others out.
     */
    // private static final String benchmarkConfig =
    // "benchmarkConfig/tassaRGR-RecordScaling.xml";
    // private static final String benchmarkConfig =
    // "benchmarkConfig/tassaRGR-QIScaling.xml";
    // private static final String benchmarkConfig =
    // "benchmarkConfig/tassaRGR-QIScaling_short.xml";
    // private static final String benchmarkConfig =
    // "benchmarkConfig/tassaRGR-KScaling.xml";
    // private static final String benchmarkConfig =
    // "benchmarkConfig/tassaRGRFlash-Utility.xml";
    private static final String    benchmarkConfig  = "benchmarkConfig/testConfig.xml";

    /** PRIVACY STRENGTH */
    private static final int       PRIVACY_STRENGTH = BENCHMARK.addMeasure("Privacy Strength");
    /** NUMBER OF RECORDS */
    private static final int       RECORDS          = BENCHMARK.addMeasure("Records");
    /** NUMBER OF QIs */
    private static final int       QIS              = BENCHMARK.addMeasure("QIs");
    /** UTILITY */
    private static final int       UTILITY          = BENCHMARK.addMeasure("Utility");
    /** RUNTIME */
    private static final int       RUNTIME          = BENCHMARK.addMeasure("Runtime");
    /** Number of runs for each benchmark setting */
    private static int             numberOfRuns;
    /** Number of warmup runs */
    private static int             numberOfWarmups;

    /**
     * Main entry point
     * 
     * @param args
     * @throws IOException
     */
    public static void executeBenchmark(String benchmarkConfig) throws IOException {

        // Init
        BENCHMARK.addAnalyzer(PRIVACY_STRENGTH, new ValueBuffer());
        BENCHMARK.addAnalyzer(RECORDS, new ValueBuffer());
        BENCHMARK.addAnalyzer(QIS, new ValueBuffer());
        BENCHMARK.addAnalyzer(UTILITY, new ValueBuffer());
        BENCHMARK.addAnalyzer(RUNTIME, new ValueBuffer());

        BenchmarkSetup setup = new BenchmarkSetup(benchmarkConfig);
        BenchmarkMetadataUtility metadata = new BenchmarkMetadataUtility(setup);
        File resultFile = new File(setup.getOutputFile());
        resultFile.getParentFile().mkdirs();
        numberOfRuns = setup.getNumberOfRuns();
        numberOfWarmups = setup.getNumberOfWarmups();

        // Repeat for each data set
        for (BenchmarkPrivacyModel model : setup.getPrivacyModels()) {
            for (BenchmarkUtilityMeasure measure : setup.getUtilityMeasures()) {
                for (BenchmarkAlgorithm algorithm : setup.getAlgorithms()) {
                    for (BenchmarkDataset dataset : setup.getDatasets()) {
                        for (double suppression : setup.getSuppressionLimits()) {

                            // Tassa doesn't support suppression limits
                            if (algorithm == BenchmarkAlgorithm.TASSA) {
                                suppression = 0.0;
                            }

                            System.out.println("Performing run: " + dataset.name() + " / " +
                                               measure + " / " + model + " / " + algorithm + " / " +
                                               suppression);

                            performExperiment(metadata,
                                              dataset,
                                              measure,
                                              model,
                                              algorithm,
                                              suppression);
                            // Write after each experiment
                            BENCHMARK.getResults().write(resultFile);
                            // Break suppression limit loop for Tassa
                            if (algorithm == BenchmarkAlgorithm.TASSA) {
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Perform experiments
     * 
     * @param metadata
     * @param dataset
     * @param measure
     * @param model
     * @param algorithm
     * @param suppressed
     * @throws IOException
     */
    private static void performExperiment(final BenchmarkMetadataUtility metadata,
                                          final BenchmarkDataset dataset,
                                          final BenchmarkUtilityMeasure measure,
                                          final BenchmarkPrivacyModel model,
                                          final BenchmarkAlgorithm algorithm,
                                          final double suppressed) throws IOException {

        Data data = BenchmarkSetup.getData(dataset, model);
        ARXConfiguration config = BenchmarkSetup.getConfiguration(dataset,
                                                                  measure,
                                                                  model,
                                                                  suppressed);

        final Map<String, String[][]> hierarchies = new DataConverter().toMap(data.getDefinition());
        final String[] header = new DataConverter().getHeader(data.getHandle());

        // Calculate max generalization levels
        final int maxGeneralizationLevels[] = new int[header.length];
        for (int i = 0; i < maxGeneralizationLevels.length; i++) {
            maxGeneralizationLevels[i] = data.getDefinition()
                                             .getHierarchy(data.getHandle().getAttributeName(i))[0].length - 1;
        }

        if (algorithm == BenchmarkAlgorithm.TASSA ||
            algorithm == BenchmarkAlgorithm.RECURSIVE_GLOBAL_RECODING ||
            algorithm == BenchmarkAlgorithm.FLASH) {
            IBenchmarkObserver observer = new IBenchmarkObserver() {

                private boolean  isWarmup       = false;
                private int      run            = 0;
                private double[] utilityResults = new double[numberOfRuns];
                private double[] runtimes       = new double[numberOfRuns];

                @Override
                public void notify(long timestamp, String[][] output, int[] transformation) {
                    // Empty by design. We only want final results.
                }

                @Override
                public void notifyFinished(long timestamp, String[][] output, int[] transformation) {

                    if (!isWarmup) {
                        // Obtain utility
                        double utility = 0d;
                        if (measure == BenchmarkUtilityMeasure.LOSS) {
                            utility = new UtilityMeasureLoss<Double>(header,
                                                                     hierarchies,
                                                                     AggregateFunction.GEOMETRIC_MEAN).evaluate(output)
                                                                                                      .getUtility();
                        } else if (measure == BenchmarkUtilityMeasure.DISCERNIBILITY) {
                            utility = new UtilityMeasureDiscernibility().evaluate(output)
                                                                        .getUtility();
                        }

                        // Normalize
                        utility -= metadata.getLowerBound(dataset, measure);
                        utility /= (metadata.getUpperBound(dataset, measure) - metadata.getLowerBound(dataset,
                                                                                                      measure));

                        // Save intermediary results
                        utilityResults[run] = utility;
                        runtimes[run] = timestamp;

                        // Write
                        if (run == numberOfRuns - 1) {

                            double utilityMean = calculateArithmeticMean(utilityResults);
                            double runtime = calculateArithmeticMean(runtimes);

                            BENCHMARK.addRun(dataset, measure, model, algorithm, suppressed);
                            BENCHMARK.addValue(PRIVACY_STRENGTH, model.getStrength());
                            BENCHMARK.addValue(RECORDS, output.length);
                            BENCHMARK.addValue(QIS, output[0].length);
                            BENCHMARK.addValue(UTILITY, utilityMean);
                            BENCHMARK.addValue(RUNTIME, runtime);
                        }

                        run++;
                        // Run complete
                        if (numberOfRuns > 1 && (run % numberOfWarmups == 0 || run == numberOfRuns)) {
                            System.out.print(run + " ");
                        }
                    }
                }

                @Override
                public void setWarmup(boolean isWarmup) {
                    this.isWarmup = isWarmup;
                }

                @Override
                public void notifyTransformations(int[][] transformations, int[] weight) {
                    // TODO Auto-generated method stub

                }
            };

            org.deidentifier.arx.benchmark.BenchmarkAlgorithm algorithmImplementation = null;
            if (algorithm == BenchmarkAlgorithm.TASSA) {
                algorithmImplementation = new TassaAlgorithm(observer, data, config);
            } else if (algorithm == BenchmarkAlgorithm.RECURSIVE_GLOBAL_RECODING) {
                algorithmImplementation = new BenchmarkAlgorithmRGR(observer, data, config);
            } else if (algorithm == BenchmarkAlgorithm.FLASH) {
                algorithmImplementation = new BenchmarkAlgorithmFlash(observer, data, config);
            }

            if (numberOfWarmups > 0) {
                System.out.print("Warmup... ");
                observer.setWarmup(true);
                for (int i = 0; i < numberOfWarmups; i++) {
                    algorithmImplementation.execute();
                }
                observer.setWarmup(false);
                System.out.println("done!");

                System.out.print("Iteration: ");
            }
            for (int i = 0; i < numberOfRuns; i++) {
                algorithmImplementation.execute();
            }
            System.out.println(">> done!");

        } else {
            throw new UnsupportedOperationException("Unimplemented Algorithm: " + algorithm);
        }
    }

    /**
     * Get the arithmetic mean for a set of values.
     * 
     * @param values
     * @return The arithmetic mean.
     */
    private static double calculateArithmeticMean(double[] values) {
        if (values.length == 1) { return values[0]; }
        double arithmeticMean = 0d;
        for (double value : values) {
            arithmeticMean += value;
        }
        arithmeticMean /= values.length;
        return arithmeticMean;
    }

    /**
     * Calculates the variance of the generalization of each attribute.
     * 
     * @param transformations
     *            The transformations of the result.
     * @param weight
     *            The number of records for each transformation.
     * @param maxGeneralizationLevels
     *            The maximum generalization level of each attribute.
     * @return
     */
    private static double[] calculateGeneralizationVariances(int[][] transformations,
                                                             int[] weight,
                                                             int[] maxGeneralizationLevels) {
        // Add up all generalization levels
        double[] averageDegrees = new double[transformations[0].length];
        Arrays.fill(averageDegrees, 0.0);
        for (int[] row : transformations) {
            for (int i = 0; i < row.length; i++) {
                averageDegrees[i] += row[i];
            }
        }
        // Normalize
        for (int i = 0; i < averageDegrees.length; i++) {
            averageDegrees[i] /= transformations.length;
        }
        // Add up all variances
        double[] variances = new double[transformations[0].length];
        Arrays.fill(variances, 0.0);
        for (int[] row : transformations) {
            for (int i = 0; i < row.length; i++) {
                variances[i] += Math.pow(row[i], 2);
            }
        }
        // Normalize
        for (int i = 0; i < variances.length; i++) {
            variances[i] /= transformations.length;
        }
        return variances;
    }

}
