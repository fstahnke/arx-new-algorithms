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
    private final double   maxSuppressionLimit;

    public BenchmarkAlgorithmRGR(IBenchmarkListener listener,
                                 final Data data,
                                 final ARXConfiguration config,
                                 final double maxSuppressionLimit) {
        super(listener);
        this.anonymizer = new ARXAnonymizer();
        this.data = data;
        this.config = config;
        if (maxSuppressionLimit < 0d || maxSuppressionLimit > 1d) {
            throw new IllegalArgumentException("Group size must be in [0, 1]");
        } else {
            this.maxSuppressionLimit = maxSuppressionLimit;
        }
    }

    public void execute() throws IOException, RollbackRequiredException {
        super.start();

        config.setMaxOutliers(Math.min(maxSuppressionLimit, config.getMaxOutliers()));

        // Execute the first anonymization
        ARXResult result = anonymizer.anonymize(data, config);

        // Optimize result
        double gsFactor = config.getMetric().getGeneralizationSuppressionFactor();
        optimizeIterative(result, gsFactor, maxSuppressionLimit);

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
     * @param maxSuppressionLimit
     *            The minimum size of a group that is transformed with the same
     *            transformation in relation to the total size of the dataset.
     * @throws RollbackRequiredException
     */
    private void
            optimizeIterative(final ARXResult result,
                                       double gsFactor,
                                       final double maxSuppressionLimit) throws RollbackRequiredException {

        if (gsFactor < 0d || gsFactor > 1d) { throw new IllegalArgumentException("Generalization/suppression factor must be in [0, 1]"); }
        if (maxSuppressionLimit < 0d || maxSuppressionLimit > 1d) { throw new IllegalArgumentException("Min equivalence class size must be in [0, 1]"); }

        DataHandle outHandle = result.getOutput(false);
        if (outHandle == null) {
            String[][] output = new String[][] { new String[] { "*" } };
            super.finished(output);
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

        if (!result.isOptimizable(outHandle)) {
            System.err.println("We never entered the loop");
        }
        while (result.isOptimizable(outHandle) && tuplesChanged > 0) {

            // Adapt suppression limit
            int optimizableRecords = BenchmarkHelper.getNumSuppressed(output);
            if (maxSuppressionLimit < 1 && optimizableRecords > 0) {
                // Calculate the maximum suppression limit for the current subset
                double localMaxSuppressionLimit = (1 - (1 - maxSuppressionLimit) * output.length / optimizableRecords);
                // We don't want to have a negative suppression limit
                localMaxSuppressionLimit = Math.max(localMaxSuppressionLimit, 0);
                // Set the new local suppression limit if it's smaller than the original suppression limit
                double localSuppressionLimit = Math.min(localMaxSuppressionLimit, originalSuppressionLimit);
                config.setMaxOutliers(localSuppressionLimit);
                
//                 System.out.println("Adapting suppressionLimit to: "
//                 + BenchmarkHelper.round(config.getMaxOutliers(), 3));
            }
            // Perform individual optimization
            tuplesChanged = result.optimize(outHandle, gsFactor);

             System.out.println("Suppression Limit: " +
             config.getMaxOutliers() + ", changed tuples: " + tuplesChanged);

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
