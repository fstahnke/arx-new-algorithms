package org.deidentifier.arx.benchmark;

import java.io.IOException;

import org.deidentifier.arx.exceptions.RollbackRequiredException;

public class BenchmarkManager {

    public static void main(String[] args) throws IOException, RollbackRequiredException {
        
        /*
         * Results so far:
         * Utility & Variance (TODO: Rerun to fix variance values. TODO: Rerun with good value for dynamic gsScaling.)
         * k-scaling (TODO: fix values for utility & variance of Tassa in k-Scaling)
         * QI-scaling
         * record scaling
         * suppressionLimit-Scaling: Split plots into two graphs (utility & runtime)
         * gsFactor-scaling: graph to visualize flaw of initial approach
         * dynamic gsFactor-scaling: adult (TODO: Rerun (fix runtime outlier). TODO: run for other datasets)
         * stepSize-scaling: adult (TODO: other datasets)
         * suppressionScaling2: adult (TODO: other datasets)
         * RGR Iteration Analysis: (TODO: Rerun with smaller gsFactor. TODO: How to analyse the dynamic gsFactor? TODO: Add utility gain?)
         * 
         * TODO: All, check boundaries for plots
         * 
         */
        
        if (args.length == 0) {
            System.out.println("No configuration specified.");
            return;
        }

        double startTime = System.currentTimeMillis();
        
        for (String config : args) {
            new BenchmarkExperimentUtilityAndRuntime(config).execute();
        }

        double elapsedTime = System.currentTimeMillis() - startTime;
        System.out.println("Total Runtime: " + (elapsedTime / 1000.0) + " sec (" +
                           (elapsedTime / 60000) + " min)");

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
//        new BenchmarkExperimentUtilityAndRuntime().execute("benchmarkConfig/gsStepSizeScaling.xml");
//      new BenchmarkExperimentUtilityAndRuntime().execute("benchmarkConfig/utilityVarianceSuppressionRGR.xml"); //TODO

//      new BenchmarkExperimentRGRIterations().execute("benchmarkConfig/rgrIterationAnalysis.xml");

    }

}
