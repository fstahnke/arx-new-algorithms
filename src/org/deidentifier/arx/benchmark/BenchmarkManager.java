package org.deidentifier.arx.benchmark;

import java.io.IOException;

public class BenchmarkManager {

    /**
     * Choose benchmarkConfig to run.
     */
    private static final String recordScalingConfig   = "benchmarkConfig/recordScaling.xml";
    private static final String qiScalingConfig       = "benchmarkConfig/QIScaling.xml";
    private static final String qiScalingConfigS      = "benchmarkConfig/QIScaling_short.xml";
    private static final String kScalingConfig        = "benchmarkConfig/kScaling.xml";
    private static final String utilityVarianceConfig = "benchmarkConfig/utilityVariance.xml";
    private static final String ruleOutConfig         = "benchmarkConfig/ruleOut.xml";
    
    private static final String iterationConfig = "benchmarkConfig/rgrIterationAnalysis.xml";

    public static void main(String[] args) throws IOException {
        BenchmarkExperimentUtilityAndRuntime utilityBenchmark = new BenchmarkExperimentUtilityAndRuntime();

        utilityBenchmark.execute(kScalingConfig);
        utilityBenchmark.execute(recordScalingConfig);
        utilityBenchmark.execute(qiScalingConfig);
        utilityBenchmark.execute(qiScalingConfigS);
//        utilityBenchmark.execute(utilityVarianceConfig);
//        utilityBenchmark.execute(ruleOutConfig);
        
        BenchmarkExperimentRGRIterations iterationsBenchmark = new BenchmarkExperimentRGRIterations();
        
        iterationsBenchmark.execute(iterationConfig);
        
        
        

    }

}
