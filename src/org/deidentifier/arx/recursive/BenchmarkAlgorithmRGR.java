package org.deidentifier.arx.recursive;

import java.io.IOException;
import java.util.Iterator;

import org.deidentifier.arx.ARXAnonymizer;
import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.ARXResult;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.DataHandle;
import org.deidentifier.arx.benchmark.BenchmarkAlgorithm;
import org.deidentifier.arx.benchmark.IBenchmarkObserver;
import org.deidentifier.arx.criteria.KAnonymity;
import org.deidentifier.arx.criteria.LDiversity;
import org.deidentifier.arx.exceptions.RollbackRequiredException;
import org.deidentifier.arx.metric.v2.MetricMDNMLoss;
import org.deidentifier.arx.utility.DataConverter;

public class BenchmarkAlgorithmRGR extends BenchmarkAlgorithm {

    private final Data             data;
    private final ARXConfiguration config;
    private final ARXAnonymizer    anonymizer;
    private final double           stepping;

    public BenchmarkAlgorithmRGR(IBenchmarkObserver listener,
                                 final Data data,
                                 final ARXConfiguration config,
                                 final double stepping) {
        super(listener);
        this.anonymizer = new ARXAnonymizer();
        this.data = data;
        this.config = config;
        this.stepping = stepping;
    }

    public void execute() throws IOException, RollbackRequiredException {
        super.start();

        // Execute the first anonymization
        ARXResult result = anonymizer.anonymize(data, config);

        // Get handle for input data and result
        DataHandle outHandle = result.getOutput(false);
        DataConverter converter = new DataConverter();
        String[][] output = converter.toArray(outHandle);
        super.updated(output, result.getGlobalOptimum().getTransformation());
        
        if (stepping == 0d) {
            
            while (result.isOptimizable(outHandle) && result.optimize(outHandle) != 0){
                output = converter.toArray(outHandle);
                super.updated(output, result.getGlobalOptimum().getTransformation());
            }
            super.finished(output);
            
        } else {
            
            double gsFactor = ((MetricMDNMLoss)config.getMetric()).getGeneralizationSuppressionFactor();
            result.optimizeIterative(outHandle, gsFactor, Integer.MAX_VALUE, stepping);
            output = converter.toArray(outHandle);
            super.finished(output);
        }
        
        outHandle.release();
        data.getHandle().release();
    }
}
