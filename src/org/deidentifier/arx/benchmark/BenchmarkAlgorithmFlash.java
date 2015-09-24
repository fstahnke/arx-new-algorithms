package org.deidentifier.arx.benchmark;

import java.io.IOException;
import java.util.Arrays;

import org.deidentifier.arx.ARXAnonymizer;
import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.ARXResult;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.DataHandle;
import org.deidentifier.arx.utility.DataConverter;

/**
 * Benchmark algorithm that executes FLASH and notifies a listener of the
 * results.
 * 
 * @author Fabian
 *
 */
public class BenchmarkAlgorithmFlash extends BenchmarkAlgorithm {

    private final Data             data;
    private final ARXConfiguration config;
    private final ARXAnonymizer    anonymizer;

    /**
     * @param observer
     * @param data
     * @param config
     */
    public BenchmarkAlgorithmFlash(IBenchmarkObserver observer,
                                   final Data data,
                                   final ARXConfiguration config) {
        super(observer);
        anonymizer = new ARXAnonymizer();
        this.data = data;
        this.config = config;
    }

    @Override
    public String[][] execute() throws IOException {

        super.start();

        // Execute anonymization
        ARXResult result = anonymizer.anonymize(data, config);

        // Get handle for result
        DataHandle outHandle = result.getOutput(false);

        // Get transformations
        int[] transformation = result.getGlobalOptimum().getTransformation();
        int[] maxTransformation = new int[transformation.length];
        Arrays.fill(maxTransformation, -1);
        int[][] transformations = new int[2][transformation.length];
        transformations[0] = transformation;
        transformations[1] = maxTransformation;
        
        // Get outliers
        int numRows = data.getHandle().getNumRows();
        int numOutliers = 0;
        for (int i = 0; i < numRows; i++) {
            if (outHandle.isOutlier(i)) {
                numOutliers++;
            }
        }
        int[] weights = new int[] { (numRows - numOutliers), numOutliers };

        // Convert input and output to array of string arrays
        DataConverter converter = new DataConverter();
        String[][] output = converter.toArray(outHandle);

        outHandle.release();
        data.getHandle().release();

        // Notify listenener and return output
        super.updated(output, transformation);
        super.finished(output, transformations, weights);
        return output;
    }

}
