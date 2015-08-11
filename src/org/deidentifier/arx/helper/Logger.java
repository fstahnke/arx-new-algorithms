package org.deidentifier.arx.helper;

import org.deidentifier.arx.clustering.TassaAlgorithmImpl;

public class Logger {

    /**
     * Tassa step
     *
     */
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
    /** TODO */
    private int                ticks     = 0;
    /** TODO */
    private TassaStep          step      = null;
    /** TODO */
    private long               time      = 0;
    /** TODO */
    private long               start      = 0;
    
    /** TODO */
    private TassaAlgorithmImpl algorithm = null;

    /**
     * @param algorithm
     */
    public Logger(TassaAlgorithmImpl algorithm) {
        this.algorithm = algorithm;
        this.start = System.currentTimeMillis();
    }

    /**
     * Done
     */
    public void done() {
        if (this.step != null) {
            System.out.println("Step done: " + step);
            System.out.println(" - Ticks: " + ticks + ", Time: " + (System.currentTimeMillis() - time) + ", Total: " + (System.currentTimeMillis() - start));
            System.out.println(" - Clusters: " + algorithm.getNumberOfClusters() + ", Information Loss: " + algorithm.getTotalInformationLoss() / algorithm.getNumberOfRecords());
        }
    }

    /**
     * Logging
     */
    public void log() {

        // Tick
        this.ticks++;
    }
    
    /**
     * Next teo
     * @param step
     */
    public void next(TassaStep step) {
        done();
        this.step = step;
        this.time = System.currentTimeMillis();
        this.ticks = 0;
    }
}
