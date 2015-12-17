package org.deidentifier.arx.benchmark;

import java.io.IOException;

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
     * @param listener
     * @param data
     * @param config
     */
    public BenchmarkAlgorithmFlash(IBenchmarkListener listener,
                                   final Data data,
                                   final ARXConfiguration config) {
        super(listener);
        this.anonymizer = new ARXAnonymizer();
        this.data = data;
        this.config = config;
    }

    @Override
    public void execute() throws IOException {

        super.start();

        // Execute anonymization
        ARXResult result = anonymizer.anonymize(data, config);
        DataHandle outHandle = result.getOutput(false);
        DataConverter converter = new DataConverter();
        String[][] output = converter.toArray(outHandle);
        outHandle.release();
        data.getHandle().release();

        // Notify listenener and return output
        super.finished(output);
    }

}
