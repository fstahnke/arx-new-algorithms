package org.deidentifier.arx.recursive;

import java.io.IOException;

import org.deidentifier.arx.ARXAnonymizer;
import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.ARXResult;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.DataHandle;
import org.deidentifier.arx.benchmark.BenchmarkAlgorithm;
import org.deidentifier.arx.benchmark.BenchmarkHelper;
import org.deidentifier.arx.benchmark.IBenchmarkListener;
import org.deidentifier.arx.exceptions.RollbackRequiredException;
import org.deidentifier.arx.utility.DataConverter;

public class BenchmarkAlgorithmRGR extends BenchmarkAlgorithm {

    private final Data     data;
    final ARXConfiguration config;
    final ARXAnonymizer    anonymizer;
    private final double   minOptimizationThreshold;

    public BenchmarkAlgorithmRGR(IBenchmarkListener listener,
                                 final Data data,
                                 final ARXConfiguration config,
                                 final double minOptimizationThreshold) {
        super(listener);
        this.anonymizer = new ARXAnonymizer();
        this.data = data;
        this.config = config;
        if (minOptimizationThreshold < 0d || minOptimizationThreshold > 1d) {
            throw new IllegalArgumentException("Optimization threshold must be in [0, 1]");
        } else {
            this.minOptimizationThreshold = minOptimizationThreshold;
        }
    }

    public void execute() throws IOException, RollbackRequiredException {
        super.start();

        double maxOutliers;

        if (config.getMaxOutliers() == 1 && minOptimizationThreshold == 0) {
            // If there is no restriction by suppression limit nor by
            // optimization threshold,
            // we want to optimize at least 1 record.
            double minGeneralization = 1d / data.getHandle().getNumRows();
            maxOutliers = 1 - minGeneralization;
        } else {
            // If there is a restriction by either suppression limit or
            // optimization threshold,
            // take the value that optimizes more records.
            maxOutliers = Math.min(1 - minOptimizationThreshold, config.getMaxOutliers());
        }
        config.setMaxOutliers(maxOutliers);

        // Execute the first anonymization
        ARXResult result = anonymizer.anonymize(data, config);

        // Optimize result
        double gsFactor = config.getMetric().getGeneralizationSuppressionFactor();
        optimizeIterative(result, gsFactor, minOptimizationThreshold);

        data.getHandle().release();
    }

    /**
     * This method optimizes the given data output with local recoding to
     * improve its utility
     * 
     * @param result
     * @param outHandle
     * @param gsFactor
     *            A factor [0,1] weighting generalization and suppression. The
     *            default value is 0.5, which means that generalization and
     *            suppression will be treated equally. A factor of 0 will favor
     *            suppression, and a factor of 1 will favor generalization. The
     *            values in between can be used for balancing both methods.
     * @param minOptimizationThreshold
     *            The minimum number of records that is optimized by each
     *            iteration.
     * @throws RollbackRequiredException
     */
    private void
            optimizeIterative(final ARXResult result,
                              double gsFactor,
                              final double minOptimizationThreshold) throws RollbackRequiredException {

        if (gsFactor < 0d || gsFactor > 1d) { throw new IllegalArgumentException("Generalization/suppression factor must be in [0, 1]"); }
        if (minOptimizationThreshold < 0d || minOptimizationThreshold > 1d) { throw new IllegalArgumentException("Min equivalence class size must be in [0, 1]"); }

        DataHandle outHandle = result.getOutput(false);

        if (outHandle == null) {
            super.finished(new String[][] { new String[] { "*" } });
            System.err.println("No result found. Increase time limit for heuristic. Current limit: " +
                               config.getHeuristicSearchTimeLimit() + " ms");
            return;
        }

        DataConverter converter = new DataConverter();
        String[][] output = converter.toArray(outHandle);
        super.updated(output, result.getGlobalOptimum().getTransformation());
        double originalSuppressionLimit = config.getMaxOutliers();

        // Outer loop
        int tuplesChanged = Integer.MAX_VALUE;

        while (result.isOptimizable(outHandle)) {

            int optimizableRecords = BenchmarkHelper.getNumSuppressed(output);

            if (optimizableRecords <= 0) {
                System.err.println("Result not optimizable but isOptimizable() returned true!");
            }
            if (tuplesChanged <= 0) {
                System.err.println("Result hasn't changed in the last iteration.");
            }

            // Adapt suppression limit
            double localSuppressionLimit = 1 - (minOptimizationThreshold * output.length / optimizableRecords);
            // We want to optimize at least 1 record
            localSuppressionLimit = Math.min(localSuppressionLimit, 1 - (1d / optimizableRecords));
            // We don't want to have a suppression limit below 0
            localSuppressionLimit = Math.max(localSuppressionLimit, 0);
            config.setMaxOutliers(localSuppressionLimit);

            // Perform individual optimization
            tuplesChanged = result.optimize(outHandle, gsFactor);

//             System.out.println("Suppression Limit: " +
//             config.getMaxOutliers() +
//             ", changed tuples: " + tuplesChanged);

            // Convert result and call listener
            output = converter.toArray(outHandle);
            super.updated(output, result.getGlobalOptimum().getTransformation());
        }

        super.finished(output);
        // reset suppression limit for next run
        result.getConfiguration().setMaxOutliers(originalSuppressionLimit);
        outHandle.release();
    }
}
