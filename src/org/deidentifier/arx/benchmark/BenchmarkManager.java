package org.deidentifier.arx.benchmark;

import java.io.IOException;
import java.util.Arrays;

import org.deidentifier.arx.exceptions.RollbackRequiredException;

public class BenchmarkManager {

    public static void main(String[] args) throws IOException, RollbackRequiredException {

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
