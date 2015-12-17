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
import org.deidentifier.arx.exceptions.RollbackRequiredException;
import org.deidentifier.arx.recursive.BenchmarkAlgorithmRGR;
import org.deidentifier.arx.utility.AggregateFunction;
import org.deidentifier.arx.utility.DataConverter;
import org.deidentifier.arx.utility.UtilityMeasureDiscernibility;
import org.deidentifier.arx.utility.UtilityMeasureGenericNonUniformEntropyWithLowerBound;
import org.deidentifier.arx.utility.UtilityMeasureLoss;
import org.deidentifier.arx.utility.UtilityMeasureNonUniformEntropy;

import de.linearbits.subframe.Benchmark;
import de.linearbits.subframe.analyzer.ValueBuffer;

/**
 * BenchmarkExperiment analysing utility and runtime (with multiple runs).
 * 
 * @author Fabian Stahnke
 */
public class BenchmarkExperimentUtilityAndRuntime {

    /** The benchmark instance */
    private final Benchmark          BENCHMARK                 = new Benchmark(new String[] {
            "Dataset",
            "UtilityMeasure",
            "PrivacyModel",
            "Algorithm",
            "SuppressionLimit",
            "gsFactor",
            "gsFactorStepSize",
            "K",
            "Records",
            "QIs"                                             });

    /** UTILITY */
    private final int                UTILITY                   = BENCHMARK.addMeasure("Utility");
    /** RUNTIME */
    private final int                RUNTIME                   = BENCHMARK.addMeasure("Runtime");
    /** NUMBER OF SUPPRESSED TUPLES */
    private final int                SUPPRESSED                = BENCHMARK.addMeasure("Suppressed");
    /** RATIO OF SUPPRESSED TUPLES */
    private final int                SUPPRESSED_RATIO          = BENCHMARK.addMeasure("SuppressedRatio");
    /** GENERALIZATION VARIANCE */
    private final int                VARIANCE                  = BENCHMARK.addMeasure("Variance");
    /** GENERALIZATION VARIANCE WITHOUT SUPPRESSED TUPLES */
    private final int                VARIANCE_NOTSUPPRESSED    = BENCHMARK.addMeasure("VarianceWithoutSuppressed");
    /** NUMBER OF DISTINCT TRANSFORMATIONS */
    private final int                NUMBER_OF_TRANSFORMATIONS = BENCHMARK.addMeasure("Transformations");
    /** Number of runs for each benchmark setting */
    private int                      numberOfRuns;
    /** Number of warmup runs */
    private int                      numberOfWarmups;
    /** The setup of this experiment */
    private BenchmarkSetup           setup;
    /** The metadata of this experiment */
    private BenchmarkMetadataUtility metadata;
    /** The file to save results */
    private File                     resultFile;

    public BenchmarkExperimentUtilityAndRuntime(String benchmarkConfig) throws IOException {
        // Init
        BENCHMARK.addAnalyzer(UTILITY, new ValueBuffer());
        BENCHMARK.addAnalyzer(RUNTIME, new ValueBuffer());
        BENCHMARK.addAnalyzer(SUPPRESSED, new ValueBuffer());
        BENCHMARK.addAnalyzer(SUPPRESSED_RATIO, new ValueBuffer());
        BENCHMARK.addAnalyzer(VARIANCE, new ValueBuffer());
        BENCHMARK.addAnalyzer(VARIANCE_NOTSUPPRESSED, new ValueBuffer());
        BENCHMARK.addAnalyzer(NUMBER_OF_TRANSFORMATIONS, new ValueBuffer());

        setup = new BenchmarkSetup(benchmarkConfig);
        metadata = new BenchmarkMetadataUtility(setup);
        resultFile = new File(setup.getOutputFile());
        resultFile.getParentFile().mkdirs();
        numberOfRuns = setup.getNumberOfRuns();
        numberOfWarmups = setup.getNumberOfWarmups();

    }

    /**
     * Main entry point
     * 
     * @param args
     * @throws IOException
     * @throws RollbackRequiredException
     */
    public void execute() throws IOException, RollbackRequiredException {

        // Do an initial warmup
        initialWarmup(BenchmarkDataset.ADULT,
                      BenchmarkUtilityMeasure.LOSS,
                      BenchmarkPrivacyModel.K5_ANONYMITY,
                      BenchmarkAlgorithm.RECURSIVE_GLOBAL_RECODING,
                      0.9,
                      0.0,
                      0.9);

        // Repeat for each data set
            for (BenchmarkUtilityMeasure measure : setup.getUtilityMeasures()) {
                for (BenchmarkAlgorithm algorithm : setup.getAlgorithms()) {
                    for (BenchmarkDataset dataset : setup.getDatasets()) {
                        for (BenchmarkPrivacyModel model : setup.getPrivacyModels()) {
                        if (algorithm == BenchmarkAlgorithm.RECURSIVE_GLOBAL_RECODING) {
                            for (double suppressionLimit : setup.getSuppressionLimits()) {
                                for (double gsStepSize : setup.getGsStepSizes()) {
                                    for (double gsFactor : setup.getGsFactors()) {

                                        performExperiment(dataset,
                                                          measure,
                                                          model,
                                                          algorithm,
                                                          suppressionLimit,
                                                          gsFactor,
                                                          gsStepSize);
                                    }
                                }
                            }

                        } else {
                            // We take default values for Flash and Clustering
                            double gsFactor = 0.5;
                            double gsStepSize = 0.0;
                            if (algorithm == BenchmarkAlgorithm.TASSA) {
                                double suppressionLimit = 0.0;

                                performExperiment(dataset,
                                                  measure,
                                                  model,
                                                  algorithm,
                                                  suppressionLimit,
                                                  gsFactor,
                                                  gsStepSize);
                            } else {
                                // double suppressionLimit = 1.0;
                                for (double suppressionLimit : setup.getSuppressionLimits()) {

                                    performExperiment(dataset,
                                                      measure,
                                                      model,
                                                      algorithm,
                                                      suppressionLimit,
                                                      gsFactor,
                                                      gsStepSize);
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
     * @param gsFactor
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

        System.out.println("Performing run: " + dataset.name() + " / " + measure + " / " + model +
                           " / " + algorithm + " / suppLimit: " + suppressionLimit +
                           " / gsFactor: " + gsFactor + " / gsStepSize: " + gsStepSize +
                           " / QIs: " + dataset.getNumQIs() + " / Records: " +
                           dataset.getNumRecords());

        final Data data = BenchmarkSetup.getData(dataset, model);
        ARXConfiguration config = BenchmarkSetup.getConfiguration(dataset,
                                                                  measure,
                                                                  model,
                                                                  suppressionLimit,
                                                                  gsFactor);

        final Map<String, String[][]> hierarchies = new DataConverter().toMap(data.getDefinition());
        final String[] header = new DataConverter().getHeader(data.getHandle());
        final String[][] input = new DataConverter().toArray(data.getHandle());

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

                private boolean  isWarmup       = false;
                private int      run            = 0;
                private double[] utilityResults = new double[numberOfRuns];
                private double[] runtimes       = new double[numberOfRuns];

                @Override
                public void notify(long timestamp, String[][] output, int[] transformation) {
                    // Empty by design. We only want final results.
                }

                @Override
                public void notifyFinished(long timestamp, String[][] output) {

                    // If warmup, don't do anything.
                    if (isWarmup) { return; }

                    // Obtain utility
                    double utility = 0d;
                    if (measure == BenchmarkUtilityMeasure.LOSS) {
                        utility = new UtilityMeasureLoss<Double>(header,
                                                                 hierarchies,
                                                                 AggregateFunction.GEOMETRIC_MEAN).evaluate(output)
                                                                                                  .getUtility();
                    } else if (measure == BenchmarkUtilityMeasure.DISCERNIBILITY) {
                        utility = new UtilityMeasureDiscernibility().evaluate(output).getUtility();
                    } else if (measure == BenchmarkUtilityMeasure.NMENTROPY) {
                        utility = new UtilityMeasureGenericNonUniformEntropyWithLowerBound<Double>(header,
                                                                              input,
                                                                              hierarchies,
                                                                              AggregateFunction.ARITHMETIC_MEAN).evaluate(output)
                                                                                                               .getUtility();
                    }

                    // Normalize
                    utility -= metadata.getLowerBound(dataset, measure);
                    utility /= (metadata.getUpperBound(dataset, measure) - metadata.getLowerBound(dataset,
                                                                                                  measure));

                    // Save intermediary results
                    utilityResults[run] = utility;
                    runtimes[run] = timestamp;

                    run++;
                    // Run complete
                    if (numberOfRuns > 1 && (run % numberOfWarmups == 0 || run == numberOfRuns)) {
                        System.out.print(run + " ");
                    }

                    // Write
                    if (run == numberOfRuns) {

                        // Calculate results
                        int suppressedTuples = BenchmarkHelper.getNumSuppressed(output);
                        double suppressedRatio = BenchmarkHelper.divideInts(suppressedTuples,
                                                                            output.length);
                        double utilityMean = BenchmarkHelper.calculateArithmeticMean(utilityResults);
                        long runtime = Math.round(BenchmarkHelper.calculateArithmeticMean(runtimes));
                        double variance = BenchmarkHelper.calculateVariance(output,
                                                                            header,
                                                                            hierarchies,
                                                                            false);
                        double varianceNotSuppressed = BenchmarkHelper.calculateVariance(output,
                                                                                         header,
                                                                                         hierarchies,
                                                                                         true);
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
                        BENCHMARK.addValue(UTILITY, utilityMean);
                        BENCHMARK.addValue(RUNTIME, runtime);
                        BENCHMARK.addValue(SUPPRESSED, suppressedTuples);
                        BENCHMARK.addValue(SUPPRESSED_RATIO, suppressedRatio);
                        BENCHMARK.addValue(VARIANCE, variance);
                        BENCHMARK.addValue(VARIANCE_NOTSUPPRESSED, varianceNotSuppressed);
                        BENCHMARK.addValue(NUMBER_OF_TRANSFORMATIONS,
                                           BenchmarkHelper.calculateNumberOfTransformations(output,
                                                                                            header,
                                                                                            hierarchies));
                    }
                }

                @Override
                public void setWarmup(boolean isWarmup) {
                    this.isWarmup = isWarmup;
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

            if (numberOfWarmups > 0) {
                System.out.print("Warmup... ");
                double time = System.currentTimeMillis();
                listener.setWarmup(true);
                for (int i = 0; i < numberOfWarmups; i++) {
                    algorithmImplementation.execute();
                }
                listener.setWarmup(false);
                double elapsed = (System.currentTimeMillis() - time) / 1000d;
                System.out.println("done in " + (int) elapsed + "s!");

                System.out.print("Iteration: ");
            }
            for (int i = 0; i < numberOfRuns; i++) {
                double time = System.currentTimeMillis();
                algorithmImplementation.execute();
                double elapsed = (System.currentTimeMillis() - time) / 1000d;
                System.out.print("(" + (int) elapsed + " s), ");
            }
            System.out.println(">> done!");

            // Write after each experiment
            BENCHMARK.getResults().write(resultFile);

        } else {
            throw new UnsupportedOperationException("Unimplemented Algorithm: " + algorithm);
        }
    }
}
