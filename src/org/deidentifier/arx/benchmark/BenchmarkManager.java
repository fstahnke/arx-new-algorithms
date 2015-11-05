package org.deidentifier.arx.benchmark;

import java.io.IOException;
import java.util.Arrays;

import org.deidentifier.arx.exceptions.RollbackRequiredException;

public class BenchmarkManager {

    public static void main(String[] args) throws IOException, RollbackRequiredException {

        /*
         * Results so far: Utility & Variance (TODO: Rerun to fix variance
         * values. TODO: Rerun with good value for dynamic gsScaling.) k-scaling
         * (TODO: fix values for utility & variance of Tassa in k-Scaling)
         * QI-scaling record scaling suppressionLimit-Scaling: Split plots into
         * two graphs (utility & runtime) gsFactor-scaling: graph to visualize
         * flaw of initial approach dynamic gsFactor-scaling: adult (TODO: Rerun
         * (fix runtime outlier). TODO: run for other datasets)
         * stepSize-scaling: adult (TODO: other datasets) suppressionScaling2:
         * adult (TODO: other datasets) RGR Iteration Analysis: (TODO: Rerun
         * with smaller gsFactor. TODO: How to analyse the dynamic gsFactor?
         * TODO: Add utility gain?)
         * 
         * TODO: All, check boundaries for plots
         */

        boolean iterationAnalysis = false;
        String[] configFiles = Arrays.copyOf(args, args.length);

        if (args.length == 0) {
            System.out.println("No configuration specified. Using default values.");
            new BenchmarkExperimentUtilityAndRuntime(null).execute();
        } else if (args[0].toLowerCase().equals("-iteration") || args[0].toLowerCase().equals("-i")) {
            iterationAnalysis = true;
            configFiles = Arrays.copyOfRange(args, 1, args.length);
        }

        double startTime = System.currentTimeMillis();

        if (iterationAnalysis) {
            for (String config : configFiles) {
                new BenchmarkExperimentRGRIterations(config).execute();
            }

        } else {
            for (String config : configFiles) {
                new BenchmarkExperimentUtilityAndRuntime(config).execute();
            }
        }

        double elapsedTime = System.currentTimeMillis() - startTime;
        System.out.println("Total Runtime: " + (elapsedTime / 1000.0) + " sec (" +
                           (elapsedTime / 60000) + " min)");

    }

}
