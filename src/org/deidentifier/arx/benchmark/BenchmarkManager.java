package org.deidentifier.arx.benchmark;

import java.io.IOException;

public class BenchmarkManager {

    /**
     * Choose benchmarkConfig to run and comment others out.
     */
    private static final String recordScalingConfig   = "benchmarkConfig/tassaRGR-RecordScaling.xml";
    private static final String qiScalingConfig       = "benchmarkConfig/tassaRGR-QIScaling.xml";
    private static final String qiScalingConfigS      = "benchmarkConfig/tassaRGR-QIScaling_short.xml";
    private static final String kScalingConfig        = "benchmarkConfig/tassaRGR-KScaling.xml";
    private static final String utilityVarianceConfig = "benchmarkConfig/tassaRGRFlash-Utility.xml";
    private static final String ruleOutConfig         = "benchmarkConfi/tassaRGR-ruleOut.xml";

    public static void main(String[] args) throws IOException {
        BenchmarkExperimentUtilityAndRuntime utility = new BenchmarkExperimentUtilityAndRuntime();

        utility.executeBenchmark(kScalingConfig);

    }

}
