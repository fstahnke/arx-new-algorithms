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

import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkAlgorithm;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkDataset;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkPrivacyModel;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkUtilityMeasure;
import org.deidentifier.arx.clustering.TassaAlgorithm;
import org.deidentifier.arx.recursive.BenchmarkAlgorithmRGR;

import de.linearbits.subframe.Benchmark;
import de.linearbits.subframe.analyzer.ValueBuffer;

/**
 * BenchmarkExperiment analysing the development of generalization degrees of
 * RGR.
 * 
 * @author Fabian Prasser
 */
public class BenchmarkExperiment2 {

    /** The benchmark instance */
    private static final Benchmark BENCHMARK              = new Benchmark(new String[] {
            "Dataset",
            "UtilityMeasure",
            "PrivacyModel",
            "Algorithm",
            "Suppression"                                });

    /** Iteration of the recursive algorithm */
    private static final int       STEP                   = BENCHMARK.addMeasure("Step");

    /** AVERAGE PERCENTAGE_OF_GENERALIZATION */
    private static final int       GENERALIZATION_DEGREE  = BENCHMARK.addMeasure("Generalization degree");

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

        BENCHMARK.addAnalyzer(GENERALIZATION_DEGREE, new ValueBuffer());
        for (int degree : DEGREE_ARRAY) {
            BENCHMARK.addAnalyzer(degree, new ValueBuffer());
        }

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

        if (algorithm == BenchmarkAlgorithm.RECURSIVE_GLOBAL_RECODING) {

            IBenchmarkObserver listener = new IBenchmarkObserver() {

                private int step = 0;

                @Override
                public void notify(long timestamp, String[][] output, int[] transformation) {

                    // Obtain relative generalization
                    double[] generalizationDegrees = new double[transformation.length];
                    double averageGeneralizationDegree = 0;

                    for (int i = 0; i < transformation.length; i++) {
                        int generalizationLevel = transformation[i];
                        int maxGeneralizationLevel = data.getDefinition()
                                                         .getHierarchy(data.getHandle()
                                                                           .getAttributeName(i))[0].length - 1;
                        double degree = 1.0 * generalizationLevel / maxGeneralizationLevel;
                        generalizationDegrees[i] = degree;
                        averageGeneralizationDegree += generalizationDegrees[i];
                    }

                    averageGeneralizationDegree /= transformation.length;

                    BENCHMARK.addRun(dataset, measure, model, algorithm, suppression);

                    // Write
                    BENCHMARK.addValue(STEP, step++);
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

        } else if (algorithm == BenchmarkAlgorithm.TASSA) {
            config.setMaxOutliers(0);
            IBenchmarkObserver observer = new IBenchmarkObserver() {

                private int step = 0;

                @Override
                public void notify(long timestamp, String[][] output, int[] transformation) {
                    BENCHMARK.addRun(dataset, measure, model, algorithm, suppression);

                    // Write
                    BENCHMARK.addValue(STEP, step++);
                }

                @Override
                public void notifyFinished(long timestamp, String[][] output, int[] transformation) {
                    // TODO Auto-generated method stub
                    
                }

                @Override
                public boolean isWarmup() {
                    // TODO Auto-generated method stub
                    return false;
                }

                @Override
                public void setWarmup(boolean isWarmup) {
                    // TODO Auto-generated method stub
                    
                }
            };

            TassaAlgorithm implementation = new TassaAlgorithm(observer, data, config);
            implementation.execute();
        } else {
            throw new UnsupportedOperationException("TODO: Implement");
        }
    }
}
