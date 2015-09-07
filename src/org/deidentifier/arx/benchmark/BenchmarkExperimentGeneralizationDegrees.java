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
import java.util.Arrays;
import java.util.Map;

import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkAlgorithm;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkDataset;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkPrivacyModel;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkUtilityMeasure;
import org.deidentifier.arx.recursive.BenchmarkAlgorithmRGR;
import org.deidentifier.arx.utility.AggregateFunction;
import org.deidentifier.arx.utility.DataConverter;
import org.deidentifier.arx.utility.UtilityMeasureDiscernibility;
import org.deidentifier.arx.utility.UtilityMeasureLoss;

import de.linearbits.subframe.Benchmark;
import de.linearbits.subframe.analyzer.ValueBuffer;

/**
 * BenchmarkExperiment analysing the development of generalization degrees of
 * RGR.
 * 
 * @author Fabian Prasser
 */
public class BenchmarkExperimentGeneralizationDegrees {

    /** The benchmark instance */
    private static final Benchmark BENCHMARK              = new Benchmark(new String[] {
            "Dataset",
            "UtilityMeasure",
            "PrivacyModel",
            "Algorithm",
            "Suppression"                                });

    /** TOTAL TIME ELAPSED */
    public static final int        TIME       = BENCHMARK.addMeasure("Time");

    /** UTILITY */
    public static final int        UTILITY    = BENCHMARK.addMeasure("Utility");

    /** SUPPRESSED RECORDS */
    public static final int        SUPPRESSED = BENCHMARK.addMeasure("Suppressed");

    /** ITERATION OF THE RECURSIVE ALGORITHM */
    private static final int       STEP                   = BENCHMARK.addMeasure("Step");

    /** AVERAGE DEGREE OF GENERALIZATION */
    private static final int       GENERALIZATION_DEGREE1 = BENCHMARK.addMeasure("Generalization degree 1");
    private static final int       GENERALIZATION_DEGREE2 = BENCHMARK.addMeasure("Generalization degree 2");
    private static final int       GENERALIZATION_DEGREE3 = BENCHMARK.addMeasure("Generalization degree 3");
    private static final int       GENERALIZATION_DEGREE4 = BENCHMARK.addMeasure("Generalization degree 4");
    private static final int       GENERALIZATION_DEGREE5 = BENCHMARK.addMeasure("Generalization degree 5");
    private static final int       GENERALIZATION_DEGREE6 = BENCHMARK.addMeasure("Generalization degree 6");
    private static final int       GENERALIZATION_DEGREE7 = BENCHMARK.addMeasure("Generalization degree 7");
    private static final int       GENERALIZATION_DEGREE8 = BENCHMARK.addMeasure("Generalization degree 8");
    private static final int       GENERALIZATION_DEGREE9 = BENCHMARK.addMeasure("Generalization degree 9");

    private static final int[]     DEGREE_ARRAY           = new int[] {
            GENERALIZATION_DEGREE1,
            GENERALIZATION_DEGREE2,
            GENERALIZATION_DEGREE3,
            GENERALIZATION_DEGREE4,
            GENERALIZATION_DEGREE5,
            GENERALIZATION_DEGREE6,
            GENERALIZATION_DEGREE7,
            GENERALIZATION_DEGREE8,
            GENERALIZATION_DEGREE9                       };

    /**
     * /** Main entry point
     * 
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        // Init
        BENCHMARK.addAnalyzer(STEP, new ValueBuffer());
        for (int degree : DEGREE_ARRAY) {
            BENCHMARK.addAnalyzer(degree, new ValueBuffer());
        }
        BENCHMARK.addAnalyzer(SUPPRESSED, new ValueBuffer());
        BENCHMARK.addAnalyzer(UTILITY, new ValueBuffer());
        BENCHMARK.addAnalyzer(TIME, new ValueBuffer());

        BenchmarkSetup setup = new BenchmarkSetup("benchmarkConfig/generalizationDegreeRGR.xml");
        BenchmarkMetadataUtility metadata = new BenchmarkMetadataUtility(setup);
        File resultFile = new File(setup.getOutputFile());
        resultFile.getParentFile().mkdirs();

        // Repeat for each data set
        for (BenchmarkAlgorithm algorithm : setup.getAlgorithms()) {
            for (BenchmarkDataset data : setup.getDatasets()) {
                for (BenchmarkPrivacyModel model : setup.getPrivacyModels()) {
                    for (BenchmarkUtilityMeasure measure : setup.getUtilityMeasures()) {
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

                private int step = 0;
                /**
                 * The number of tuples that are generalized and not suppressed.
                 */
                private int generalizedRecords = 0;
                /**
                 * The average generalization degrees of all records.
                 */
                private double[] generalizationDegrees;

                @Override
                public void notify(long timestamp, String[][] output, int[] transformation) {
                    
                    // init
                    if (step == 0) {
                        generalizationDegrees = new double[transformation.length];
                        Arrays.fill(generalizationDegrees, 1d);
                    }

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
                    
                    // Calculate how many records have been generalized during this run
                    int suppressed = getSuppressedRecords(output);
                    int newGeneralized = output.length - generalizedRecords - suppressed; // get number of newly generalized tuples from this run
                    
                    // Update the generalization degree for each attribute
                    for (int i = 0; i < transformation.length; i++) {

                        // Obtain generalization degree for current run
                        int generalizationLevel = transformation[i];
                        int maxGeneralizationLevel = data.getDefinition()
                                                         .getHierarchy(data.getHandle()
                                                                           .getAttributeName(i))[0].length - 1;
                        double currentDegree = 1.0 * generalizationLevel / maxGeneralizationLevel;
                        
                        // De-normalize degree from last run
                        double updatedDegree = generalizationDegrees[i] * output.length;
                        // Remove suppressed tuples from the last run
                        updatedDegree -= (output.length - generalizedRecords);
                        // Add generalized tuples from this run
                        updatedDegree += currentDegree * newGeneralized;
                        // Add the suppressed tuples from this run
                        updatedDegree += suppressed;
                        // Normalize and add updated value to output
                        updatedDegree /= output.length;
                        generalizationDegrees[i] = updatedDegree;
                    }
                    
                    // Update number of generalized records
                    generalizedRecords = output.length - suppressed;

                    BENCHMARK.addRun(dataset, measure, model, algorithm, suppression);

                    // Write
                    BENCHMARK.addValue(STEP, step++);
                    BENCHMARK.addValue(SUPPRESSED, suppressed);
                    BENCHMARK.addValue(UTILITY, utility);
                    BENCHMARK.addValue(TIME, timestamp);

                    for (int i = 0; i < transformation.length; i++) {
                        BENCHMARK.addValue(DEGREE_ARRAY[i], generalizationDegrees[i]);
                    }

                    if (transformation.length < DEGREE_ARRAY.length) {
                        for (int i = transformation.length; i < DEGREE_ARRAY.length; i++) {
                            BENCHMARK.addValue(DEGREE_ARRAY[i], -0.1d);
                        }
                    }
                }

                @Override
                public void notifyFinished(long timestamp, String[][] output, int[] transformation) {
                }

                @Override
                public boolean isWarmup() {
                    return false;
                }

                @Override
                public void setWarmup(boolean isWarmup) {
                }

            };

            BenchmarkAlgorithmRGR implementation = new BenchmarkAlgorithmRGR(listener, data, config);
            implementation.execute();

        } else {
            throw new UnsupportedOperationException("TODO: Implement");
        }
    }
    
    private static int getSuppressedRecords(String[][] output) {

        // Obtain suppressed tuples
        int suppressed = 0;
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
        
        return suppressed;
    }
}
