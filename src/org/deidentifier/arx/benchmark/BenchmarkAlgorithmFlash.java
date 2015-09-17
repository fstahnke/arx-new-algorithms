package org.deidentifier.arx.benchmark;

import java.io.IOException;

import org.deidentifier.arx.ARXAnonymizer;
import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.ARXResult;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.DataHandle;
import org.deidentifier.arx.utility.DataConverter;

/**
 * Benchmark algorithm that executes FLASH and notifies a listener of the results.
 * @author Fabian
 *
 */
public class BenchmarkAlgorithmFlash extends BenchmarkAlgorithm {
    
    private final Data data;
    private final ARXConfiguration config;
    private final ARXAnonymizer anonymizer;

    /**
     * @param observer
     * @param data
     * @param config
     */
    public BenchmarkAlgorithmFlash(IBenchmarkObserver observer, final Data data, final ARXConfiguration config) {
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
        int[] transformation = result.getGlobalOptimum().getTransformation();
        
        // Get handle for input data and result
        DataHandle inHandle = data.getHandle();
        DataHandle outHandle = result.getOutput(false);

        // Convert input and output to array of string arrays
        DataConverter converter = new DataConverter();
        String[][] output = converter.toArray(outHandle);
        
        // Notify listenener and return output
        super.updated(output, transformation);
        super.finished(output, transformation);
        return output;
    }

}
