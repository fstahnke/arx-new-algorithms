package org.deidentifier.arx.benchmark;

import java.io.IOException;

import org.deidentifier.arx.exceptions.RollbackRequiredException;

public class BenchmarkManager {

    public static void main(String[] args) throws IOException, RollbackRequiredException {

        double startTime = System.currentTimeMillis();

//      new BenchmarkExperimentUtilityAndRuntime().execute("benchmarkConfig/QIScaling.xml");
//      new BenchmarkExperimentUtilityAndRuntime().execute("benchmarkConfig/QIScaling_short.xml");
//      new BenchmarkExperimentUtilityAndRuntime().execute("benchmarkConfig/recordScaling.xml");
//      new BenchmarkExperimentUtilityAndRuntime().execute("benchmarkConfig/kScaling.xml");
//      new BenchmarkExperimentUtilityAndRuntime().execute("benchmarkConfig/utilityVariance.xml");
//         new BenchmarkExperimentGsScaling().execute("benchmarkConfig/gsFactorScaling.xml");
//      new BenchmarkExperimentUtilityAndRuntime().execute("benchmarkConfig/ruleOut.xml");
//      new BenchmarkExperimentUtilityAndRuntime().execute("benchmarkConfig/suppressionScaling.xml");
//      new BenchmarkExperimentUtilityAndRuntime().execute("benchmarkConfig/suppressionScaling2.xml");
//        new BenchmarkExperimentUtilityAndRuntime().execute("benchmarkConfig/gsFactorDynamicScaling.xml");
        new BenchmarkExperimentUtilityAndRuntime().execute("benchmarkConfig/gsStepSizeScaling.xml");

//      new BenchmarkExperimentRGRIterations().execute("benchmarkConfig/rgrIterationAnalysis.xml");

         double elapsedTime = System.currentTimeMillis() - startTime;
         System.out.println("Total Runtime: " + (elapsedTime / 1000.0) + " sec (" +
                            (elapsedTime / 60000) + " min)");

    }

}
