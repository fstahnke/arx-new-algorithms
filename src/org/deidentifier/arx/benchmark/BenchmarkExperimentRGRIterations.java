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
import org.deidentifier.arx.exceptions.RollbackRequiredException;
import org.deidentifier.arx.recursive.BenchmarkAlgorithmRGR;
import org.deidentifier.arx.utility.AggregateFunction;
import org.deidentifier.arx.utility.DataConverter;
import org.deidentifier.arx.utility.UtilityMeasureDiscernibility;
import org.deidentifier.arx.utility.UtilityMeasureLoss;

import de.linearbits.subframe.Benchmark;
import de.linearbits.subframe.analyzer.ValueBuffer;

/**
 * BenchmarkExperiment analysing the development of generalization degrees,
 * utility, suppressed records and the ratio of suppressed records throughout
 * the iterations of RGR.
 * 
 * @author Fabian Stahnke
 */
public class BenchmarkExperimentRGRIterations {

    /** The benchmark instance */
    private final Benchmark          BENCHMARK              = new Benchmark(new String[] {
            "Dataset",
            "UtilityMeasure",
            "PrivacyModel",
            "Algorithm",
            "SuppressionLimit",
            "gsFactor",
            "gsFactorStepSize",
            "K",
            "Records",
            "QIs"                                          });

    /** UTILITY */
    private final int                UTILITY                  = BENCHMARK.addMeasure("Utility");
    /** RUNTIME */
    private final int                RUNTIME                  = BENCHMARK.addMeasure("Runtime");
    /** ITERATION OF THE RECURSIVE ALGORITHM */
    private final int                STEP                     = BENCHMARK.addMeasure("Step");
    /** NUMBER OF SUPPRESSED TUPLES */
    private final int                SUPPRESSED               = BENCHMARK.addMeasure("Suppressed");
    /** RATIO OF SUPPRESSED TUPLES */
    private final int                SUPPRESSED_RATIO         = BENCHMARK.addMeasure("SuppressedRatio");
    /** GENERALIZATION VARIANCE */
    private final int                VARIANCE                 = BENCHMARK.addMeasure("Variance");
    /** GENERALIZATION VARIANCE WITHOUT SUPPRESSED TUPLES */
    private final int                VARIANCE_NOTSUPPRESSED   = BENCHMARK.addMeasure("VarianceWithoutSuppressed");
    /** Number of runs for each benchmark setting */
    private int                      numberOfRuns;
    /** Number of warmup runs */
    private int                      numberOfWarmups          = 0;
    /** The setup of this experiment */
    private BenchmarkSetup           setup;
    /** The metadata of this experiment */
    private BenchmarkMetadataUtility metadata;

    /** AVERAGE DEGREE OF GENERALIZATION */
    private final int                GENERALIZATION_DEGREE1   = BENCHMARK.addMeasure("GeneralizationDegree1");
    private final int                GENERALIZATION_DEGREE2   = BENCHMARK.addMeasure("GeneralizationDegree2");
    private final int                GENERALIZATION_DEGREE3   = BENCHMARK.addMeasure("GeneralizationDegree3");
    private final int                GENERALIZATION_DEGREE4   = BENCHMARK.addMeasure("GeneralizationDegree4");
    private final int                GENERALIZATION_DEGREE5   = BENCHMARK.addMeasure("GeneralizationDegree5");
    private final int                GENERALIZATION_DEGREE6   = BENCHMARK.addMeasure("GeneralizationDegree6");
    private final int                GENERALIZATION_DEGREE7   = BENCHMARK.addMeasure("GeneralizationDegree7");
    private final int                GENERALIZATION_DEGREE8   = BENCHMARK.addMeasure("GeneralizationDegree8");
    private final int                GENERALIZATION_DEGREE9   = BENCHMARK.addMeasure("GeneralizationDegree9");

    private final int[]              DEGREE_ARRAY             = new int[] {
            GENERALIZATION_DEGREE1,
            GENERALIZATION_DEGREE2,
            GENERALIZATION_DEGREE3,
            GENERALIZATION_DEGREE4,
            GENERALIZATION_DEGREE5,
            GENERALIZATION_DEGREE6,
            GENERALIZATION_DEGREE7,
            GENERALIZATION_DEGREE8,
            GENERALIZATION_DEGREE9                           };

    /** AVERAGE DEGREE OF GENERALIZATION */
    private final int                GENERALIZATION_VARIANCE1 = BENCHMARK.addMeasure("GeneralizationVariance1");
    private final int                GENERALIZATION_VARIANCE2 = BENCHMARK.addMeasure("GeneralizationVariance2");
    private final int                GENERALIZATION_VARIANCE3 = BENCHMARK.addMeasure("GeneralizationVariance3");
    private final int                GENERALIZATION_VARIANCE4 = BENCHMARK.addMeasure("GeneralizationVariance4");
    private final int                GENERALIZATION_VARIANCE5 = BENCHMARK.addMeasure("GeneralizationVariance5");
    private final int                GENERALIZATION_VARIANCE6 = BENCHMARK.addMeasure("GeneralizationVariance6");
    private final int                GENERALIZATION_VARIANCE7 = BENCHMARK.addMeasure("GeneralizationVariance7");
    private final int                GENERALIZATION_VARIANCE8 = BENCHMARK.addMeasure("GeneralizationVariance8");
    private final int                GENERALIZATION_VARIANCE9 = BENCHMARK.addMeasure("GeneralizationVariance9");

    private final int[]              VARIANCE_ARRAY           = new int[] {
            GENERALIZATION_VARIANCE1,
            GENERALIZATION_VARIANCE2,
            GENERALIZATION_VARIANCE3,
            GENERALIZATION_VARIANCE4,
            GENERALIZATION_VARIANCE5,
            GENERALIZATION_VARIANCE6,
            GENERALIZATION_VARIANCE7,
            GENERALIZATION_VARIANCE8,
            GENERALIZATION_VARIANCE9                         };

    public BenchmarkExperimentRGRIterations(String benchmarkConfig) throws IOException {
        // Init
        BENCHMARK.addAnalyzer(UTILITY, new ValueBuffer());
        BENCHMARK.addAnalyzer(RUNTIME, new ValueBuffer());
        BENCHMARK.addAnalyzer(SUPPRESSED, new ValueBuffer());
        BENCHMARK.addAnalyzer(SUPPRESSED_RATIO, new ValueBuffer());
        BENCHMARK.addAnalyzer(VARIANCE, new ValueBuffer());
        BENCHMARK.addAnalyzer(VARIANCE_NOTSUPPRESSED, new ValueBuffer());

        BENCHMARK.addAnalyzer(STEP, new ValueBuffer());
        for (int degree : DEGREE_ARRAY) {
            BENCHMARK.addAnalyzer(degree, new ValueBuffer());
        }
        for (int variance : VARIANCE_ARRAY) {
            BENCHMARK.addAnalyzer(variance, new ValueBuffer());
        }

        setup = new BenchmarkSetup(benchmarkConfig);
        metadata = new BenchmarkMetadataUtility(setup);

    }

    /**
     * /** Main entry point
     * 
     * @param args
     * @throws IOException
     * @throws RollbackRequiredException
     */
    public void execute() throws IOException, RollbackRequiredException {

        File resultFile = new File(setup.getOutputFile());
        resultFile.getParentFile().mkdirs();

        // Do an initial warmup
        initialWarmup(BenchmarkDataset.ADULT,
                      BenchmarkUtilityMeasure.LOSS,
                      BenchmarkPrivacyModel.K5_ANONYMITY,
                      BenchmarkAlgorithm.RECURSIVE_GLOBAL_RECODING,
                      0.1,
                      0.0,
                      0.05);

        // Repeat for each data set
        for (BenchmarkAlgorithm algorithm : setup.getAlgorithms()) {
            for (BenchmarkDataset dataset : setup.getDatasets()) {
                for (BenchmarkPrivacyModel model : setup.getPrivacyModels()) {
                    for (BenchmarkUtilityMeasure measure : setup.getUtilityMeasures()) {
                        for (double suppressionLimit : setup.getSuppressionLimits()) {
                            for (double gsStepSize : setup.getGsStepSizes()) {
                                for (double gsFactor : setup.getGsFactors()) {

                                    System.out.println("Performing run: " + dataset.name() + " / " +
                                                       measure + " / " + model + " / " + algorithm +
                                                       " / suppLimit: " + suppressionLimit +
                                                       " / gsFactor: " + gsFactor +
                                                       " / gsStepSize: " + gsStepSize + " / QIs: " +
                                                       dataset.getNumQIs() + " / Records: " +
                                                       dataset.getNumRecords());

                                    // New run
                                    performExperiment(dataset,
                                                      measure,
                                                      model,
                                                      algorithm,
                                                      suppressionLimit,
                                                      gsFactor,
                                                      gsStepSize);

                                    // Write after each experiment
                                    BENCHMARK.getResults().write(resultFile);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * @param adult
     * @param loss
     * @param k5Anonymity
     * @param recursiveGlobalRecoding
     * @param d
     * @param e
     * @param f
     * @throws IOException
     * @throws RollbackRequiredException
     */
    private void initialWarmup(BenchmarkDataset dataset,
                               BenchmarkUtilityMeasure measure,
                               BenchmarkPrivacyModel model,
                               BenchmarkAlgorithm algorithm,
                               double suppressionLimit,
                               double gsFactor,
                               double gsStepSize) throws IOException, RollbackRequiredException {

        if (numberOfWarmups < 1) { return; }

        Data data = BenchmarkSetup.getData(dataset, model);
        ARXConfiguration config = BenchmarkSetup.getConfiguration(dataset,
                                                                  measure,
                                                                  model,
                                                                  suppressionLimit,
                                                                  gsFactor);

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
            IBenchmarkListener listener = new IBenchmarkListener() {

                @Override
                public void setWarmup(boolean isWarmup) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void notify(long timestamp, String[][] output, int[] transformation) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void notifyFinished(long timestamp, String[][] output) {
                    // TODO Auto-generated method stub

                }
            };

            org.deidentifier.arx.benchmark.BenchmarkAlgorithm algorithmImplementation = null;
            if (algorithm == BenchmarkAlgorithm.TASSA) {
                algorithmImplementation = new TassaAlgorithm(listener, data, config);
            } else if (algorithm == BenchmarkAlgorithm.RECURSIVE_GLOBAL_RECODING) {
                algorithmImplementation = new BenchmarkAlgorithmRGR(listener,
                                                                    data,
                                                                    config,
                                                                    gsStepSize);
            } else if (algorithm == BenchmarkAlgorithm.FLASH) {
                algorithmImplementation = new BenchmarkAlgorithmFlash(listener, data, config);
            }

            // Execute warmup
            System.out.println("Initial warmup phase for " + algorithm.toString());
            System.out.print("Warmup iteration: ");
            for (int i = 1; i <= 20; i++) {
                algorithmImplementation.execute();
                if (i % 2 == 0 || i == 20) {
                    System.out.print(i + " ");
                }
            }
            System.out.println(">> done!");

        } else {
            throw new UnsupportedOperationException("Unimplemented Algorithm: " + algorithm);
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
     * @param suppressionLimit
     * @throws IOException
     * @throws RollbackRequiredException
     */
    private void performExperiment(final BenchmarkDataset dataset,
                                   final BenchmarkUtilityMeasure measure,
                                   final BenchmarkPrivacyModel model,
                                   final BenchmarkAlgorithm algorithm,
                                   final double suppressionLimit,
                                   final double gsFactor,
                                   final double gsStepSize) throws IOException,
                                                           RollbackRequiredException {

        final Data data = BenchmarkSetup.getData(dataset, model);
        ARXConfiguration config = BenchmarkSetup.getConfiguration(dataset,
                                                                  measure,
                                                                  model,
                                                                  suppressionLimit);

        final Map<String, String[][]> hierarchies = new DataConverter().toMap(data.getDefinition());
        final String[] header = new DataConverter().getHeader(data.getHandle());

        // Calculate max generalization levels
        final int maxGeneralizationLevels[] = new int[header.length];
        for (int i = 0; i < maxGeneralizationLevels.length; i++) {
            maxGeneralizationLevels[i] = data.getDefinition()
                                             .getHierarchy(data.getHandle().getAttributeName(i))[0].length - 1;
        }

        if (algorithm == BenchmarkAlgorithm.RECURSIVE_GLOBAL_RECODING) {

            IBenchmarkListener listener = new IBenchmarkListener() {

                private int      step               = 0;
                /**
                 * The number of tuples that are generalized and not suppressed.
                 */
                private int      generalizedRecords = 0;
                /**
                 * The average generalization degree of each attributes.
                 */
                private double[] generalizationDegrees;
                /**
                 * The generalization variance of each attribute.
                 */
                private double[] generalizationVariances;

                @Override
                public void notify(long timestamp, String[][] output, int[] transformation) {

                    // init
                    if (step == 0) {
                        generalizationDegrees = new double[transformation.length];
                        generalizationVariances = new double[transformation.length];
                        Arrays.fill(generalizationDegrees, 1d);
                        Arrays.fill(generalizationVariances, 0d);
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

                    // Calculate how many records have been generalized during
                    // this run
                    int suppressed = getSuppressedRecords(output);
                    // get number of newly generalized tuples from this run
                    int newGeneralized = output.length - generalizedRecords - suppressed;

                    // Update the generalization degree for each attribute
                    for (int i = 0; i < transformation.length; i++) {

                        // Obtain generalization degree for current run
                        int generalizationLevel = transformation[i];
                        int maxGeneralizationLevel = maxGeneralizationLevels[i];
                        double currentDegree = 1.0 * generalizationLevel / maxGeneralizationLevel;

                        // Calculate new generalization degree:
                        // De-normalize degree from last run
                        double updatedDegree = generalizationDegrees[i] * output.length;
                        // Remove suppressed tuples from the last run
                        updatedDegree -= (output.length - generalizedRecords);
                        // Add generalized tuples from this run
                        updatedDegree += newGeneralized * currentDegree;
                        // Add the suppressed tuples from this run
                        updatedDegree += suppressed;
                        // Normalize
                        updatedDegree /= output.length;

                        // De-normalize variance from last run
                        double updatedVariance = generalizationVariances[i] * output.length;
                        // Remove suppressed tuples from the last run
                        updatedVariance -= (output.length - generalizedRecords) *
                                           Math.pow((1 - generalizationDegrees[i]), 2);
                        // Add generalized tuples from this run
                        updatedVariance += newGeneralized *
                                           Math.pow((currentDegree - updatedDegree), 2);
                        // Add the suppressed tuples from this run
                        updatedVariance += suppressed * Math.pow((1 - updatedDegree), 2);
                        // Normalize
                        updatedVariance /= output.length;

                        // Update output values
                        generalizationDegrees[i] = updatedDegree;
                        generalizationVariances[i] = updatedVariance;
                    }

                    // Update number of generalized records
                    generalizedRecords = output.length - suppressed;
                    
                    // Add results to Benchmark run
                    BENCHMARK.addRun(dataset,
                                     measure,
                                     model,
                                     algorithm,
                                     suppressionLimit,
                                     gsFactor,
                                     gsStepSize,
                                     model.getStrength(),
                                     output.length,
                                     output[0].length);

                    // Step complete
                    if (step >= 1) {
                        System.out.print(step + " ");
                    }
                    
                    // Write
                    BENCHMARK.addValue(STEP, step++);
                    BENCHMARK.addValue(SUPPRESSED, suppressed);
                    BENCHMARK.addValue(SUPPRESSED_RATIO, suppressed * 1.0 / output.length);
                    BENCHMARK.addValue(UTILITY, utility);
                    BENCHMARK.addValue(VARIANCE, BenchmarkHelper.calculateVariance(output, header, hierarchies, false));
                    BENCHMARK.addValue(VARIANCE_NOTSUPPRESSED, BenchmarkHelper.calculateVariance(output, header, hierarchies, true));
                    BENCHMARK.addValue(RUNTIME, timestamp);

                    for (int i = 0; i < transformation.length; i++) {
                        BENCHMARK.addValue(DEGREE_ARRAY[i], generalizationDegrees[i]);
                        BENCHMARK.addValue(VARIANCE_ARRAY[i], generalizationVariances[i]);
                    }

                    if (transformation.length < DEGREE_ARRAY.length) {
                        for (int i = transformation.length; i < DEGREE_ARRAY.length; i++) {
                            BENCHMARK.addValue(DEGREE_ARRAY[i], -0.1d);
                            BENCHMARK.addValue(VARIANCE_ARRAY[i], -0.1d);
                        }
                    }
                }

                @Override
                public void notifyFinished(long timestamp, String[][] output) {
                    System.out.println(">>> done!");
                }

                @Override
                public void setWarmup(boolean isWarmup) {
                }

            };

            BenchmarkAlgorithmRGR implementation = new BenchmarkAlgorithmRGR(listener,
                                                                             data,
                                                                             config,
                                                                             gsStepSize);
            System.out.print("Step: ");
            implementation.execute();

        } else {
            throw new UnsupportedOperationException("Algorithm not supported: " + algorithm);
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
