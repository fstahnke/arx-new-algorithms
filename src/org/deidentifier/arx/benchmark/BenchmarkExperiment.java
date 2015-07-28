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

import org.deidentifier.arx.ARXAnonymizer;
import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.ARXLattice;
import org.deidentifier.arx.ARXLattice.ARXNode;
import org.deidentifier.arx.ARXResult;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkDataset;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkPrivacyModel;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkUtilityMeasure;
import org.deidentifier.arx.recursive.RecursiveAlgorithm;
import org.deidentifier.arx.utility.AggregateFunction;
import org.deidentifier.arx.utility.DataConverter;
import org.deidentifier.arx.utility.UtilityMeasureLoss;

import de.linearbits.subframe.Benchmark;
import de.linearbits.subframe.analyzer.ValueBuffer;
import de.linearbits.subframe.analyzer.buffered.BufferedArithmeticMeanAnalyzer;
import de.linearbits.subframe.analyzer.buffered.BufferedStandardDeviationAnalyzer;

/**
 * Main benchmark class.
 * 
 * @author Fabian Prasser
 */
public class BenchmarkExperiment {

    /** The benchmark instance */
    private static final Benchmark BENCHMARK   = new Benchmark(new String[] { "Dataset", "UtilityMeasure", "Algorithm", "Suppression" });

    /** TOTAL */
    public static final int        TOTAL       = BENCHMARK.addMeasure("Total");

    /** UTILITY */
    public static final int        UTILITY     = BENCHMARK.addMeasure("Utility");

    /** Repetitions */
    private static final int       REPETITIONS = 1;

    /**
     * Main entry point
     * 
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        // Init
        BENCHMARK.addAnalyzer(UTILITY, new BufferedArithmeticMeanAnalyzer());
        BENCHMARK.addAnalyzer(UTILITY, new BufferedStandardDeviationAnalyzer());
        BENCHMARK.addAnalyzer(TOTAL, new ValueBuffer());
        
        // Repeat for each data set
        for (BenchmarkDataset data : BenchmarkSetup.getDatasets()) {

            // New run
            BENCHMARK.addRun(data.toString(), BenchmarkPrivacyModel.K_ANONYMITY, "RGR", 0.3d);
            anonymize(data);

            // Write after each experiment
            File resultFile = new File("results/experiment.csv");
            resultFile.mkdirs();
            BENCHMARK.getResults().write(resultFile);
        }
    }
    
    /**
     * Performs the experiments
     * 
     * @param dataset
     * @throws IOException
     */
    private static void anonymize(BenchmarkDataset dataset) throws IOException {
        
        Data data = BenchmarkSetup.getData(dataset, BenchmarkPrivacyModel.K_ANONYMITY);
        ARXConfiguration config = BenchmarkSetup.getConfiguration(dataset, BenchmarkUtilityMeasure.LOSS, BenchmarkPrivacyModel.K_ANONYMITY, 0.01d);
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        RecursiveAlgorithm rgr = new RecursiveAlgorithm();
        
        // Warmup
        //ARXResult result = anonymizer.anonymize(data, config);
        
        String[][] result = rgr.execute(data, config, anonymizer);
        
        data.getHandle().release();
        
        // Benchmark
        long time = System.currentTimeMillis();
        for (int i = 0; i < REPETITIONS; i++) {
            System.out.println(" - Run-1 " + (i + 1) + " of " + REPETITIONS);
            data.getHandle().release();
            result = rgr.execute(data, config, anonymizer);
        }
        time = System.currentTimeMillis() - time;

        DataConverter converter = new DataConverter();
        double outputLoss = new UtilityMeasureLoss<Double>(converter.getHeader(data.getHandle()), converter.toMap(data.getDefinition()), AggregateFunction.GEOMETRIC_MEAN).evaluate(result).getUtility();
        
        BENCHMARK.addValue(UTILITY, outputLoss);
        BENCHMARK.addValue(TOTAL, (int) time);
    }
}
