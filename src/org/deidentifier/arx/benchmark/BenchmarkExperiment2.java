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
import java.util.Iterator;
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
 * Main benchmark class.
 * 
 * @author Fabian Prasser
 */
public class BenchmarkExperiment2 {

    /** The benchmark instance */
    private static final Benchmark BENCHMARK              = new Benchmark(new String[] { "Dataset",
            "UtilityMeasure",
            "PrivacyModel",
            "Algorithm",
            "Suppression"                                });

    /** TOTAL */
    public static final int        TIME                   = BENCHMARK.addMeasure("Time");

    /** AVERAGE PERCENTAGE_OF_GENERALIZATION */
    public static final int        GENERALIZATION_DEGREE  = BENCHMARK.addMeasure("Generalization degree");

    public static final int        GENERALIZATION_DEGREE1 = BENCHMARK.addMeasure("Generalization degree 1");
    public static final int        GENERALIZATION_DEGREE2 = BENCHMARK.addMeasure("Generalization degree 2");
    public static final int        GENERALIZATION_DEGREE3 = BENCHMARK.addMeasure("Generalization degree 3");
    public static final int        GENERALIZATION_DEGREE4 = BENCHMARK.addMeasure("Generalization degree 4");
    public static final int        GENERALIZATION_DEGREE5 = BENCHMARK.addMeasure("Generalization degree 5");
    public static final int        GENERALIZATION_DEGREE6 = BENCHMARK.addMeasure("Generalization degree 6");
    public static final int        GENERALIZATION_DEGREE7 = BENCHMARK.addMeasure("Generalization degree 7");
    public static final int        GENERALIZATION_DEGREE8 = BENCHMARK.addMeasure("Generalization degree 8");
    public static final int        GENERALIZATION_DEGREE9 = BENCHMARK.addMeasure("Generalization degree 9");

    public static final int[]      DEGREE_ARRAY           = new int[] { GENERALIZATION_DEGREE1,
                                                                        GENERALIZATION_DEGREE2,
                                                                        GENERALIZATION_DEGREE3,
                                                                        GENERALIZATION_DEGREE4,
                                                                        GENERALIZATION_DEGREE5,
                                                                        GENERALIZATION_DEGREE6,
                                                                        GENERALIZATION_DEGREE7,
                                                                        GENERALIZATION_DEGREE8,
                                                                        GENERALIZATION_DEGREE9
                                                                        };

    /**
     * /** Main entry point
     * 
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        // Init
        BENCHMARK.addAnalyzer(TIME, new ValueBuffer());

        BENCHMARK.addAnalyzer(GENERALIZATION_DEGREE, new ValueBuffer());
        for (int degree : DEGREE_ARRAY) {
            BENCHMARK.addAnalyzer(degree, new ValueBuffer());
        }

        File resultFile = new File("results/experiment2.csv");
        resultFile.getParentFile().mkdirs();
        BenchmarkMetadataUtility metadata = new BenchmarkMetadataUtility();

        // Repeat for each data set
        for (BenchmarkAlgorithm algorithm : BenchmarkSetup.getAlgorithms()) {
            for (BenchmarkDataset data : BenchmarkSetup.getDatasets()) {
                for (BenchmarkPrivacyModel model : BenchmarkSetup.getPrivacyModels()) {
                    for (BenchmarkUtilityMeasure measure : BenchmarkSetup.getUtilityMeasures()) {
                        for (double suppression : BenchmarkSetup.getSuppressionLimits()) {
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

        final Data data = BenchmarkSetup.getData(dataset, model);
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
                    double utilityMeasure = 0d;
                    if (measure == BenchmarkUtilityMeasure.LOSS) {
                        utilityMeasure = new UtilityMeasureLoss<Double>(header,
                                                                        hierarchies,
                                                                        AggregateFunction.GEOMETRIC_MEAN).evaluate(output)
                                                                                                         .getUtility();
                    } else if (measure == BenchmarkUtilityMeasure.DISCERNIBILITY) {
                        utilityMeasure = new UtilityMeasureDiscernibility().evaluate(output)
                                                                           .getUtility();
                    }

                    // Normalize
                    utilityMeasure -= metadata.getLowerBound(dataset, measure);
                    utilityMeasure /= (metadata.getUpperBound(dataset, measure) - metadata.getLowerBound(dataset,
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

                    // Obtain relative generalization
                    double[] generalizationDegrees = new double[transformation.length];
                    double averageGeneralizationDegree = 0;

                    for (int i = 0; i < transformation.length; i++) {
                        int generalizationLevel = transformation[i];
                        int maxGeneralizationLevel = data.getDefinition().getHierarchy(data.getHandle().getAttributeName(i))[0].length - 1;
                        double degree = 1.0 * generalizationLevel / maxGeneralizationLevel;
                        generalizationDegrees[i] = degree;
                        averageGeneralizationDegree += generalizationDegrees[i];
                    }

                    averageGeneralizationDegree /= transformation.length;

                    BENCHMARK.addRun(dataset, measure, model, algorithm, suppression);

                    // Write
                    BENCHMARK.addValue(TIME, timestamp);
                    BENCHMARK.addValue(GENERALIZATION_DEGREE, averageGeneralizationDegree);

                    for (int i = 0; i < transformation.length; i++) {
                        BENCHMARK.addValue(DEGREE_ARRAY[i], generalizationDegrees[i]);
                    }
                    
                    if (transformation.length < DEGREE_ARRAY.length) {
                        for (int i = transformation.length; i < DEGREE_ARRAY.length; i++) {
                            BENCHMARK.addValue(DEGREE_ARRAY[i], -0.1d);
                        }
                    }
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
                }
            };

            TassaAlgorithm implementation = new TassaAlgorithm(observer, data, config);
            implementation.execute();
        } else {
            throw new UnsupportedOperationException("TODO: Implement");
        }
    }
}
