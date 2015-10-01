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
    private static final String suppressionScaling    = "benchmarkConfig/suppressionScaling.xml";

    private static final String iterationConfig       = "benchmarkConfig/rgrIterationAnalysis.xml";

    public static void main(String[] args) throws IOException {

        double startTime = System.currentTimeMillis();

//         new BenchmarkExperimentUtilityAndRuntime().execute(qiScalingConfig);
//         new BenchmarkExperimentUtilityAndRuntime().execute(qiScalingConfigS);
//         new BenchmarkExperimentUtilityAndRuntime().execute(recordScalingConfig);
//         new BenchmarkExperimentUtilityAndRuntime().execute(kScalingConfig);
         new BenchmarkExperimentUtilityAndRuntime().execute(utilityVarianceConfig);
//         new BenchmarkExperimentUtilityAndRuntime().execute(ruleOutConfig);
        // new BenchmarkExperimentUtilityAndRuntime().execute(suppressionScaling);

//         new BenchmarkExperimentRGRIterations().execute(iterationConfig);

         double elapsedTime = System.currentTimeMillis() - startTime;
         System.out.println("Total Runtime: " + (elapsedTime / 1000.0) + " sec (" +
                            (elapsedTime / 60000) + " min)");

    }

}
