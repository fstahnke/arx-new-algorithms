package org.deidentifier.arx.recursive;

import java.io.IOException;
import java.util.Iterator;

import org.deidentifier.arx.ARXAnonymizer;
import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.ARXListener;
import org.deidentifier.arx.ARXResult;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.DataHandle;
import org.deidentifier.arx.benchmark.BenchmarkAlgorithm;
import org.deidentifier.arx.benchmark.IBenchmarkListener;
import org.deidentifier.arx.criteria.KAnonymity;
import org.deidentifier.arx.criteria.LDiversity;
import org.deidentifier.arx.exceptions.RollbackRequiredException;
import org.deidentifier.arx.metric.v2.MetricMDNMLoss;
import org.deidentifier.arx.utility.DataConverter;

public class BenchmarkAlgorithmRGR extends BenchmarkAlgorithm {

    private final Data             data;
    private final ARXConfiguration config;
    private final ARXAnonymizer    anonymizer;
    private final double           stepSize;

    public BenchmarkAlgorithmRGR(IBenchmarkListener listener,
                                 final Data data,
                                 final ARXConfiguration config,
                                 final double stepSize) {
        super(listener);
        this.anonymizer = new ARXAnonymizer();
        this.data = data;
        this.config = config;
        if (stepSize < 0d || stepSize > 1d) {
            throw new IllegalArgumentException("Step size must be in [0, 1]");
        } else {
            this.stepSize = stepSize;
        }
    }

    public void execute() throws IOException, RollbackRequiredException {
        super.start();

        // Execute the first anonymization
        ARXResult result = anonymizer.anonymize(data, config);

        // Optimize result
        double gsFactor = ((MetricMDNMLoss) config.getMetric()).getGeneralizationSuppressionFactor();
        optimizeIterative(result, gsFactor, stepSize);

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
     * @param adaptationFactor
     *            Is added to the gsFactor when reaching a fixpoint
     * @throws RollbackRequiredException
     */
    private void optimizeIterative(final ARXResult result,
                                  double gsFactor,
                                  final double adaptationFactor) throws RollbackRequiredException {

        if (gsFactor < 0d || gsFactor > 1d) { throw new IllegalArgumentException("Generalization/suppression factor must be in [0, 1]"); }
        if (adaptationFactor < 0d || adaptationFactor > 1d) { throw new IllegalArgumentException("Adaption factor must be in [0, 1]"); }

        DataHandle outHandle = result.getOutput(false);
        DataConverter converter = new DataConverter();
        String[][] output = converter.toArray(outHandle);
        super.updated(output, result.getGlobalOptimum().getTransformation());

        // Outer loop
        boolean tuplesChanged = true;
        while (result.isOptimizable(outHandle) && tuplesChanged && gsFactor <= 0.5d) {

            // Perform individual optimization
            tuplesChanged = result.optimize(outHandle, gsFactor) > 0;

            // Convert result and call listener
            output = converter.toArray(outHandle);
            super.updated(output, result.getGlobalOptimum().getTransformation());

            // Try to adapt, if possible
            if (!tuplesChanged && adaptationFactor > 0d && gsFactor < 0.5d) {
                gsFactor += adaptationFactor;
                tuplesChanged = true;
            }
        }
        
        super.finished(output);
        outHandle.release();
    }
}
