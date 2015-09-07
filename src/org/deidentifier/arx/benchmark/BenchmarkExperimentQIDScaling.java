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
 * BenchmarkExperiment comparing RGR's and Tassa's ability to scale
 * 
 * @author Fabian Stahnke
 */
public class BenchmarkExperimentQIDScaling {

    /** The benchmark instance */
    private static final Benchmark BENCHMARK      = new Benchmark(new String[] {
            "Dataset",
            "UtilityMeasure",
            "PrivacyModel",
            "Algorithm",
            "Suppression"                        });

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
        BENCHMARK.addAnalyzer(RUNTIME, new ValueBuffer());

        BenchmarkSetup setup = new BenchmarkSetup("benchmarkConfig/tassaRGRscaling.xml");
        BenchmarkMetadataUtility metadata = new BenchmarkMetadataUtility(setup);
        File resultFile = new File(setup.getOutputFile());
        resultFile.getParentFile().mkdirs();
        final double SUPPRESSION = 0.5;

        // Repeat for each data set
        for (BenchmarkPrivacyModel model : setup.getPrivacyModels()) {
            for (BenchmarkUtilityMeasure measure : setup.getUtilityMeasures()) {
                for (BenchmarkAlgorithm algorithm : setup.getAlgorithms()) {
                    for (int subsetCount = 1000; subsetCount <= 30000; subsetCount += 1000) {
                        System.out.println("Performing run: " + measure + " / " + model + " / " +
                                           algorithm + " / subset-" + subsetCount);

                        // New run
                        if (algorithm == BenchmarkAlgorithm.TASSA) {
                            performExperiment(metadata,
                                              BenchmarkDataset.ADULT,
                                              measure,
                                              model,
                                              algorithm,
                                              0.0,
                                              subsetCount);
                        } else {
                            performExperiment(metadata,
                                              BenchmarkDataset.ADULT,
                                              measure,
                                              model,
                                              algorithm,
                                              SUPPRESSION,
                                              subsetCount);
                        }

                        // Write after each experiment
                        BENCHMARK.getResults().write(resultFile);
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
                                          final double suppressed,
                                          int subsetCount) throws IOException {

        Data data = BenchmarkSetup.getDataSubset(dataset, model, subsetCount);
        ARXConfiguration config = BenchmarkSetup.getConfiguration(dataset,
                                                                  measure,
                                                                  model,
                                                                  suppressed);

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
                            double runtime = calculateArithmeticMean(runtimes);

                            BENCHMARK.addRun(dataset, measure, model, algorithm, suppressed);
                            BENCHMARK.addValue(UTILITY, utilityMean);
                            BENCHMARK.addValue(RUNTIME, runtime);
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
