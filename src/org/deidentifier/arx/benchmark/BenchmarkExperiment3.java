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
 * BenchmarkExperiment comparing RGR and Tassa and analysing variance of Tassa
 * results.
 * 
 * @author Fabian Prasser
 */
public class BenchmarkExperiment3 {

    /** The benchmark instance */
    private static final Benchmark BENCHMARK      = new Benchmark(new String[] {
            "Dataset",
            "UtilityMeasure",
            "PrivacyModel",
            "Algorithm"                          });

    /** UTILITY */
    private static final int       UTILITY        = BENCHMARK.addMeasure("Utility");
    /** VARIANCE */
    private static final int       VARIANCE       = BENCHMARK.addMeasure("Variance");
    /** RUNTIME */
    private static final int       RUNTIME        = BENCHMARK.addMeasure("Runtime");
    /** Number of runs for each benchmark setting */
    private static final int       NUMBER_OF_RUNS = 5;

    /**
     * Main entry point
     * 
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        // Init
        BENCHMARK.addAnalyzer(UTILITY, new ValueBuffer());
        BENCHMARK.addAnalyzer(VARIANCE, new ValueBuffer());
        BENCHMARK.addAnalyzer(RUNTIME, new ValueBuffer());

        BenchmarkSetup setup = new BenchmarkSetup("benchmarkConfig/tassaRGR.xml");
        BenchmarkMetadataUtility metadata = new BenchmarkMetadataUtility(setup);
        File resultFile = new File(setup.getOutputFile());
        resultFile.getParentFile().mkdirs();

        // Repeat for each data set
        for (BenchmarkDataset data : setup.getDatasets()) {
            for (BenchmarkAlgorithm algorithm : setup.getAlgorithms()) {
                for (BenchmarkPrivacyModel model : setup.getPrivacyModels()) {
                    for (BenchmarkUtilityMeasure measure : setup.getUtilityMeasures()) {
                        if (algorithm != BenchmarkAlgorithm.TASSA) {
                            for (double suppression : setup.getSuppressionLimits()) {
                                System.out.println("Performing run: " + data + "/" + measure + "/" +
                                                   model + "/" + algorithm + "/" + suppression);

                                // New run
                                performExperiment(metadata,
                                                  data,
                                                  measure,
                                                  model,
                                                  algorithm,
                                                  suppression);

                                // Write after each experiment
                                BENCHMARK.getResults().write(resultFile);
                            }
                            // For Tassa we don't need the suppression limit
                        } else {
                            System.out.println("Performing run: " + data + "/" + measure + "/" +
                                               model + "/" + algorithm + "/(n/a)");

                            // New run
                            performExperiment(metadata, data, measure, model, algorithm, 0.0);

                            // Write after each experiment
                            BENCHMARK.getResults().write(resultFile);
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
     * @param suppression
     * @throws IOException
     */
    private static void performExperiment(final BenchmarkMetadataUtility metadata,
                                          final BenchmarkDataset dataset,
                                          final BenchmarkUtilityMeasure measure,
                                          final BenchmarkPrivacyModel model,
                                          final BenchmarkAlgorithm algorithm,
                                          double suppression) throws IOException {

        Data data = BenchmarkSetup.getData(dataset, model);
        ARXConfiguration config = BenchmarkSetup.getConfiguration(dataset,
                                                                  measure,
                                                                  model,
                                                                  suppression);

        final Map<String, String[][]> hierarchies = new DataConverter().toMap(data.getDefinition());
        final String[] header = new DataConverter().getHeader(data.getHandle());

        if (algorithm == BenchmarkAlgorithm.TASSA ||
            algorithm == BenchmarkAlgorithm.RECURSIVE_GLOBAL_RECODING) {
            IBenchmarkObserver observer = new IBenchmarkObserver() {

                private boolean  isWarmup       = false;
                private int      run            = 0;
                private double[] utilityResults = new double[NUMBER_OF_RUNS];
                private double[] runtimes       = new double[NUMBER_OF_RUNS];

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
                        if (run == NUMBER_OF_RUNS - 1) {

                            double utilityMean = calculateArithmeticMean(utilityResults);
                            double variance = calculateVariance(utilityResults);
                            double runtime = calculateArithmeticMean(runtimes);

                            BENCHMARK.addRun(dataset, measure, model, algorithm);
                            BENCHMARK.addValue(UTILITY, utilityMean);
                            BENCHMARK.addValue(RUNTIME, runtime);
                            BENCHMARK.addValue(VARIANCE, variance);
                        }

                        // Run complete
                        run++;
                        System.out.print(run + " ");
                    }
                }

                @Override
                public boolean isWarmup() {
                    return isWarmup;
                }

                @Override
                public void setWarmup(boolean isWarmup) {
                    this.isWarmup = isWarmup;
                }
            };

            org.deidentifier.arx.benchmark.BenchmarkAlgorithm algorithmImplementation = null;
            if (algorithm == BenchmarkAlgorithm.TASSA) {
                algorithmImplementation = new TassaAlgorithm(observer, data, config);
            } else if (algorithm == BenchmarkAlgorithm.RECURSIVE_GLOBAL_RECODING) {
                algorithmImplementation = new BenchmarkAlgorithmRGR(observer, data, config);
            }
            
            System.out.print("Warmup... ");
            observer.setWarmup(true);
            algorithmImplementation.execute();
            observer.setWarmup(false);
            System.out.println("done!");

            System.out.print("Iteration: ");
            for (int i = 0; i < NUMBER_OF_RUNS; i++) {
                algorithmImplementation.execute();
            }
            System.out.println("done!");

        } else {
            throw new UnsupportedOperationException("Unimplemented Algorithm: " + algorithm);
        }
    }

    /**
     * Get the variance for a set of values (without Bessel's correction).
     * 
     * @param values
     *            A set of sample values.
     * @return The sample variance.
     */
    private static double calculateVariance(double[] values) {

        double arithmeticMean = calculateArithmeticMean(values);

        double variance = 0;
        for (double value : values) {
            variance += Math.pow(value - arithmeticMean, 2);
        }
        variance /= values.length;

        return variance;

    }

    /**
     * Get the arithmetic mean for a set of values.
     * 
     * @param values
     * @return The arithmetic mean.
     */
    private static double calculateArithmeticMean(double[] values) {
        double arithmeticMean = 0d;
        for (double value : values) {
            arithmeticMean += value;
        }
        arithmeticMean /= values.length;
        return arithmeticMean;
    }

}
