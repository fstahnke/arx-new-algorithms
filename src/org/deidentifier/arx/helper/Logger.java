package org.deidentifier.arx.helper;

import java.util.ArrayList;

import org.deidentifier.arx.clustering.*;

public class Logger {

    // Log settings
    private static final int    LOGGING_STEP = 10000;
    private ArrayList<LogLevel> logLevel;
    private TassaAlgorithmImpl  algorithm;

    // Log states
    /** TODO */
    private long                logTimeStart = -1;
    /** TODO */
    private long                logTimeLast  = -1;
    /** TODO */
    private int                 logTickLast  = -1;
    /** TODO */
    private int                 logTick;

    // Algorithm states
    private TassaStep           currentStep;
    private TassaMethodCall     currentMethodCall;

    /**
     * @param algorithm
     */
    public Logger(TassaAlgorithmImpl algorithm) {
        this.algorithm = algorithm;
        this.logLevel = new ArrayList<Logger.LogLevel>(4);
        logLevel.add(LogLevel.STEPS);
        logLevel.add(LogLevel.CALLS);
    }

    public void setCurrentClusteringStep(TassaStep currentStep) {
        this.currentStep = currentStep;
        if (currentStep == TassaStep.INITIALIZE) {
            logTimeLast = System.currentTimeMillis();
        }
        if (logLevel.contains(LogLevel.STEPS)) {
            log();
        }
    }

    public void setCurrentMethodCall(TassaMethodCall currentMethodCall) {
        this.currentMethodCall = currentMethodCall;
        if (logLevel.contains(LogLevel.CALLS)) {
            log();
        }
    }

    /**
     * Logging
     */
    public void log() {

        // Tick
        this.logTick++;

        // Print
        int clusters = this.algorithm.getNumberOfClusters();
        double averageCost = this.algorithm.getAverageGeneralizationCost();
        long time = System.currentTimeMillis();

        // Print phase, step and events
        StringBuilder logOutput = new StringBuilder("Step: " + this.currentStep);
        logOutput.append(", Events since last logging: " + (logTick - logTickLast));
        System.out.println(logOutput);

        // Print clusters, cost and time
        logOutput = new StringBuilder();

        logOutput.append(" - Clusters: " + clusters + ", Cost: " + averageCost);
        logOutput.append(", Time: " + (time - logTimeLast) + "[ms], Total: " +
                         (time - logTimeStart) + "[ms]");
        System.out.println(logOutput);

        // Set time and tick for next logging
        logTickLast = logTick;
        this.logTimeLast = System.currentTimeMillis();
        if (this.logTimeStart == -1) {
            this.logTimeStart = this.logTimeLast;
        }
    }

    public static enum TassaStep {
        INITIALIZE {
            @Override
            public String toString() {
                return "Initializing";
            }
        },
        MOVE_RECORDS {
            @Override
            public String toString() {
                return "Moving records";
            }
        },
        SPLIT_CLUSTERS {
            @Override
            public String toString() {
                return "Splitting clusters";
            }
        },
        FINALIZE {
            @Override
            public String toString() {
                return "Finalizing";
            }
        }
    }

    public static enum TassaMethodCall {
        ENSURE_CLUSTERSIZE,
        GET_CLOSEST_TO_RECORD,
        GET_CLOSEST_TO_CLUSTER,
        GET_CLOSEST_CLUSTERS,
        SET_CLUSTER
    }

    public static enum LogLevel {
        OFF,
        STEPS,
        CALLS,
        VERBOSE
    }

    public static enum LogElement {
        NUM_CLUSTERS,
        AVG_COST,
        AVG_COST_DELTA,
        TIME,

    }

}
