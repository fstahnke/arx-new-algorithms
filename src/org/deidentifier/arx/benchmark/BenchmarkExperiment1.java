/*
 * Benchmark of risk-based anonymization in ARX 3.0.0
 * Copyright 2015 - Fabian Prasser
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * BenchmarkExperiment analyzing utility and suppression of RGR.
 * 
 * @author Fabian Prasser
 */
public class BenchmarkExperiment1 {

    /** The benchmark instance */
    private static final Benchmark BENCHMARK  = new Benchmark(new String[] { "Dataset",
            "UtilityMeasure",
            "PrivacyModel",
            "Algorithm",
            "Suppression"                    });

    /** TOTAL */
    public static final int        TIME       = BENCHMARK.addMeasure("Time");

    /** UTILITY */
    public static final int        UTILITY    = BENCHMARK.addMeasure("Utility");

    /** UTILITY */
    public static final int        SUPPRESSED = BENCHMARK.addMeasure("Suppressed");

    /**
     * Main entry point
     * 
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        // Init
        BENCHMARK.addAnalyzer(TIME, new ValueBuffer());
        BENCHMARK.addAnalyzer(UTILITY, new ValueBuffer());
        BENCHMARK.addAnalyzer(SUPPRESSED, new ValueBuffer());

        BenchmarkSetup setup = new BenchmarkSetup("utilityAndSuppressionRGR.xml");
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
                                          final double suppression) throws IOException {

        Data data = BenchmarkSetup.getData(dataset, model);
        ARXConfiguration config = BenchmarkSetup.getConfiguration(dataset,
                                                                  measure,
                                                                  model,
                                                                  suppression);

        final Map<String, String[][]> hierarchies = new DataConverter().toMap(data.getDefinition());
        final String[] header = new DataConverter().getHeader(data.getHandle());

        if (algorithm == BenchmarkAlgorithm.RECURSIVE_GLOBAL_RECODING) {

            IBenchmarkObserver listener = new IBenchmarkObserver() {

                @Override
                public void notify(long timestamp, String[][] output, int[] transformation) {

                    // Obtain utility
                    double utility = 0d;
                    if (measure == BenchmarkUtilityMeasure.LOSS) {
                        utility = new UtilityMeasureLoss<Double>(header,
                                                                 hierarchies,
                                                                 AggregateFunction.GEOMETRIC_MEAN).evaluate(output)
                                                                                                  .getUtility();
                    } else if (measure == BenchmarkUtilityMeasure.DISCERNIBILITY) {
                        utility = new UtilityMeasureDiscernibility().evaluate(output).getUtility();
                    }

                    // Normalize
                    utility -= metadata.getLowerBound(dataset, measure);
                    utility /= (metadata.getUpperBound(dataset, measure) - metadata.getLowerBound(dataset,
                                                                                                  measure));

                    // Obtain suppressed tuples
                    double suppressed = 0d;
                    for (String[] row : output) {

                        boolean cellSuppressed = true;
                        for (String cell : row) {
                            if (!cell.equals("*")) {
                                cellSuppressed = false;
                                break;
                            }
                        }
                        if (cellSuppressed) {
                            suppressed++;
                        }
                    }

                    // Normalize
                    suppressed /= (double) output.length;

                    BENCHMARK.addRun(dataset, measure, model, algorithm, suppression);

                    // Write
                    BENCHMARK.addValue(TIME, timestamp);
                    BENCHMARK.addValue(UTILITY, utility);
                    BENCHMARK.addValue(SUPPRESSED, suppressed);
                }

            };

            BenchmarkAlgorithmRGR implementation = new BenchmarkAlgorithmRGR(listener, data, config);
            implementation.execute();

        } else if (algorithm == BenchmarkAlgorithm.TASSA) {
            config.setMaxOutliers(0);
            IBenchmarkObserver observer = new IBenchmarkObserver() {

                @Override
                public void notify(long timestamp, String[][] output, int[] transformation) {

                    // Obtain utility
                    double utility = 0d;
                    if (measure == BenchmarkUtilityMeasure.LOSS) {
                        utility = new UtilityMeasureLoss<Double>(header,
                                                                 hierarchies,
                                                                 AggregateFunction.GEOMETRIC_MEAN).evaluate(output)
                                                                                                  .getUtility();
                    } else if (measure == BenchmarkUtilityMeasure.DISCERNIBILITY) {
                        utility = new UtilityMeasureDiscernibility().evaluate(output).getUtility();
                    }

                    // Normalize
                    utility -= metadata.getLowerBound(dataset, measure);
                    utility /= (metadata.getUpperBound(dataset, measure) - metadata.getLowerBound(dataset,
                                                                                                  measure));

                    // Obtain suppressed tuples
                    double suppressed = 0d;
                    for (String[] row : output) {

                        boolean cellSuppressed = true;
                        for (String cell : row) {
                            if (!cell.equals("*")) {
                                cellSuppressed = false;
                                break;
                            }
                        }
                        if (cellSuppressed) {
                            suppressed++;
                        }
                    }

                    // Normalize
                    suppressed /= (double) output.length;

                    BENCHMARK.addRun(dataset, measure, model, algorithm, suppression);

                    // Write
                    BENCHMARK.addValue(TIME, timestamp);
                    BENCHMARK.addValue(UTILITY, utility);
                    BENCHMARK.addValue(SUPPRESSED, suppressed);
                }
            };

            TassaAlgorithm implementation = new TassaAlgorithm(observer, data, config);
            implementation.execute();
        } else {
            throw new UnsupportedOperationException("TODO: Implement");
        }
    }
}
