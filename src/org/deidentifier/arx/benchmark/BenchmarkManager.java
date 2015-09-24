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

        double startTime = System.currentTimeMillis();
        
        utilityBenchmark.execute(kScalingConfig);
        utilityBenchmark.execute(recordScalingConfig);
        utilityBenchmark.execute(qiScalingConfig);
        utilityBenchmark.execute(qiScalingConfigS);
//        utilityBenchmark.execute(utilityVarianceConfig);
//        utilityBenchmark.execute(ruleOutConfig);
        
        double elapsedTime = System.currentTimeMillis() - startTime;
        System.out.println("Total Runtime: " + elapsedTime + " ms");
        double estimatedTime = elapsedTime * 6.0 / 1000.0;
        System.out.println("Estimated time for long tests: " + estimatedTime + " sec");
        
        BenchmarkExperimentRGRIterations iterationsBenchmark = new BenchmarkExperimentRGRIterations();
        
//        iterationsBenchmark.execute(iterationConfig);
        
        
        

    }

}
