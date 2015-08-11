package org.deidentifier.arx.clustering;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.deidentifier.arx.ARXInterface;
import org.deidentifier.arx.helper.Logger;
import org.deidentifier.arx.helper.Logger.TassaStep;

public class TassaAlgorithmImpl {

    private final Logger       logger;
    /** TODO */
    private final ARXInterface arxinterface;
    /** TODO */
    private double             inititalInformationLoss;
    /** TODO */
    private double             finalInformationLoss;
    /** TODO */
    private Set<TassaCluster>  currentClustering;
    /** TODO */
    private final int[][]      outputBuffer;
    /** TODO */
    private TassaCluster[]     recordToCluster;
    /** TODO */
    private int                numRecords;
    /** TODO */
    private Random             random = new Random(0xDEADBEEF); // TODO: Remove when not debugging any more

    /**
	 * Creates a new instance
	 * @param iface
	 * @throws IOException
	 */
    TassaAlgorithmImpl(ARXInterface iface) throws IOException {
        this.arxinterface = iface;
        this.logger = new Logger(this);
        this.outputBuffer = iface.getBuffer();
        this.recordToCluster = new TassaCluster[arxinterface.getDataQI().length];
        this.numRecords = iface.getDataQI().length;
    }
    
	/**
     * Returns the total information loss
     * @return
     */
    public double getTotalInformationLoss() {
        
        if (this.currentClustering == null) {
            return 0;
        } else {
            double result = 0.0;
            for (TassaCluster cluster : this.currentClustering) {
                result += cluster.getInformationLoss();
            }
            return result;
        }
    }
    
    public int getNumberOfClusters() {
        if (currentClustering != null) {
            return currentClustering.size();
        } else {
            return 0;
        }
    }

    /**
     * Checks the given parameters
     * @param alpha
     * @param omega
     */
    private void checkParameters(double alpha, double omega) {
        if (alpha <= 0 || alpha > 1) {
            throw new IllegalArgumentException("Argument 'alpha' is out of bounds: " + alpha);
        }
        if (omega <= 1 || omega > 2) {
            throw new IllegalArgumentException("Argument 'omega' is out of bounds: " + omega);
        }
    }
    
    /**
     * Modifies the clustering to ensure that all clusters have a given minimal size
     * @param clustering
     * @param clusterSize
     * @return
     */
    private void ensureClustersHaveSize(Set<TassaCluster> clustering, int clusterSize) {

        // Log
        logger.log();
        
        // Prepare
        Set<TassaCluster> smallClusters = new HashSet<TassaCluster>();
        Set<TassaCluster> largeClusters = new HashSet<TassaCluster>();
        for (final TassaCluster cluster : clustering) {
            if (cluster.getSize() < clusterSize) {
                smallClusters.add(cluster);
            } else {
                largeClusters.add(cluster);
            }
        }

        // As long as there are clusters with size < k
        // merge closest two clusters and either
        // if size >= k, add them to output, or
        // if size < k, process them further
        while (smallClusters.size() > 1) {

            // Log
            logger.log ();
            
            // Merge closest pair
            TassaPair<TassaCluster, TassaCluster> pair = getClosestTwoClusters(smallClusters);
            for (int record : pair.second.getRecords()) {
                setCluster(record, pair.first);
            }
            pair.first.addCluster(pair.second);
            smallClusters.remove(pair.second);
            
            if (pair.first.getSize() >= clusterSize) {
                largeClusters.add(pair.first);
                smallClusters.remove(pair.first);
            }
        }
        
        // If there is one cluster left, merge it with the closest cluster from the large clusters
        if (smallClusters.size() == 1) {

            // Log
            logger.log ();
            
            // Perform
            TassaCluster cluster1 = smallClusters.iterator().next();
            TassaCluster cluster2 = getClosestCluster(largeClusters, cluster1);
            for (int record : cluster1.getRecords()) {
                setCluster(record, cluster2);
            }
            cluster2.addCluster(cluster1);
            smallClusters.remove(cluster1);
        }
        
        // Return
        clustering.clear();
        clustering.addAll(largeClusters);
    }

    /**
     * Returns the cluster which is closest to the given one
     * @param clustering
     * @param cluster
     * @return
     */
    private TassaCluster getClosestCluster(Set<TassaCluster> clustering, TassaCluster cluster) {

        double loss = Double.MAX_VALUE;
        TassaCluster result = null;

        for (TassaCluster cluster2 : clustering) {
            if (cluster != cluster2) {
            	// Calculate weighted cost.
                double value = cluster.getInformationLossWhenAdding(cluster2);
                if (value < loss) {
                    loss = value;
                    result = cluster2;
                }
            }
        }
        if (result == null) { throw new IllegalStateException("Should not happen!"); }
        return result;
    }

    /**
     * Returns the cluster which is closest to the given record
     * @param clustering
     * @param source
     * @param record
     * @return
     */
    private TassaPair<TassaCluster, Double> getClosestCluster(Set<TassaCluster> clustering, TassaCluster source, int record) {

        double delta = Double.MAX_VALUE;
        double loss = Double.MAX_VALUE;
        TassaCluster result = null;

        for (TassaCluster cluster : clustering) {
            if (cluster != source) {

                double _loss = cluster.getInformationLossWhenAdding(record);
                double _delta = _loss - cluster.getInformationLoss();
                if (_delta < 0d) {
                    throw new IllegalStateException("Delta may never be <0");
                }
                if (_delta < delta) {
                    loss = _loss;
                    delta = _delta;
                    result = cluster;
                }
            }
        }
        if (result == null) { 
            throw new IllegalStateException("There may never be no closest cluster"); 
        }
        return new TassaPair<TassaCluster, Double>(result, loss);
    }

    /**
     * Returns the closest two clusters in the given clustering
     * @param clustering
     * @return
     */
    private TassaPair<TassaCluster, TassaCluster> getClosestTwoClusters(Set<TassaCluster> clustering) {
        
        double loss = Double.MAX_VALUE;
        TassaPair<TassaCluster, TassaCluster> result = null;
        
        for (TassaCluster cluster1 : clustering) {
            for (TassaCluster cluster2 : clustering) {
                if (cluster1 != cluster2) {
                	// Calculate weighted cost.
                    double value = cluster1.getInformationLossWhenAdding(cluster2);
                    if (value < loss) {
                        loss = value;
                        result = new TassaPair<TassaCluster, TassaCluster>(cluster1, cluster2);
                    }
                }
            }
        }
        if (result == null) {
            throw new IllegalStateException("Should not happen!");
        }
        return result;
    }

    /**
     * Returns the cluster to which the given record is assigned
     * @param record
     * @return
     */
	private TassaCluster getCluster(int record) {
        return this.recordToCluster[record];
    }

    /**
     * Returns the initial partitioning
     * @param alpha
     * @param omega
     * @param input
     * @return
     */
    private Set<TassaCluster> getInitialPartitioning(double alpha,
                                                     double omega,
                                                     Set<TassaCluster> input) {

        // Prepare
        Set<TassaCluster> result;
        
        if (input != null) {
            
            // Given
            result = input;
            
        } else  {
            
            // Random
            int k = arxinterface.getK();
            int k_0 = (int) Math.floor(alpha * k) > 0 ? (int) Math.floor(alpha * k) : 1;
            result = this.getRandomPartitioning(arxinterface.getGeneralizationManager(), numRecords, k_0);
        }
            
        // Update cluster assignments
        for (TassaCluster cluster : result) {
            for (int recordId : cluster.getRecords()) {
                this.setCluster(recordId, cluster);
            }
        }
        
        // Return
        return result;
    }

    /**
	 * Returns an initial random partitioning for the given number of records
	 * @param manager
	 * @param numRecords
	 * @param k
	 * @return
	 */
    private Set<TassaCluster> getRandomPartitioning(GeneralizationManager manager, int numRecords, int k) {

        // Prepare
        int[] recordIds = new int[numRecords];
        for (int i = 0; i < numRecords; i++) {
            recordIds[i] = i;
        }
        shuffle(recordIds);
        int offset = 0;
        
        // Calculate
        final int numberOfClusters = (int) Math.floor(numRecords / k);
        final int additionalRecords = numRecords % k;
        
        // Build
        Set<TassaCluster> result = new HashSet<TassaCluster>();
        for (int i = 0; i<numberOfClusters; i++) {
            int clusterSize = i < additionalRecords ? k + 1 : k;
            TassaCluster cluster = new TassaCluster(manager, Arrays.copyOfRange(recordIds, offset, offset + clusterSize));
            result.add(cluster);
            offset += clusterSize;
        }
        
        // Return
        return result;
    }
    
    /**
     * Moves all records within the given clustering, if it decreases the average information loss
     * @param clustering
     * @return
     */
    private boolean moveRecords(Set<TassaCluster> clustering) {

        // Log
        logger.log();
        
        // Prepare
        boolean modified = false;

        // Loop
        for (int record = 0; record < numRecords; record++) {

            // Log
            logger.log();
            
            // Find closest cluster
            TassaCluster sourceCluster = getCluster(record);
            TassaPair<TassaCluster, Double> targetCluster = getClosestCluster(clustering, sourceCluster, record);

            // Check if it improves the overall costs. Take cluster sizes into account.
            double inputGC = sourceCluster.getInformationLoss() + targetCluster.first.getInformationLoss();
            double outputGC = sourceCluster.getInformationLossWhenRemoving(record) + targetCluster.second;
            
            //**********************************
            //**********************************
            boolean DEBUG_singleton = sourceCluster.getSize() == 1;
            double DEBUG_before = this.getTotalInformationLoss();
            String DEBUG_clusterBefore1 = this.getClusterAsString(sourceCluster);
            String DEBUG_clusterBefore2 = this.getClusterAsString(targetCluster.first);
            double DEBUG_input1 = sourceCluster.getInformationLossWhenRemoving(record);
            double DEBUG_input2 = targetCluster.second;
            //**********************************
            //**********************************
            
            // If yes or if source cluster is singleton, move
            if (outputGC < inputGC || sourceCluster.getSize() == 1) {
                
                // Move
                targetCluster.first.addRecord(record);
                sourceCluster.removeRecord(record);
                setCluster(record, targetCluster.first);
                
                // Remove if empty
                if (sourceCluster.getSize() == 0) {
                    clustering.remove(sourceCluster);
                }
                
                // State
                modified = true;
            }
            
            //**********************************
            //**********************************
            double DEBUG_after = this.getTotalInformationLoss();
            String DEBUG_clusterAfter1 = this.getClusterAsString(sourceCluster);
            String DEBUG_clusterAfter2 = this.getClusterAsString(targetCluster.first);
            double DEBUG_output1 = sourceCluster.getInformationLoss();
            double DEBUG_output2 = targetCluster.first.getInformationLoss();
            if (DEBUG_before < DEBUG_after && !DEBUG_singleton) {
                System.out.println("Before: " + DEBUG_before);
                System.out.println("After: " + DEBUG_after);
                System.out.println("Size: " + sourceCluster.getSize());
                System.out.println("Size: " + targetCluster.first.getSize());
                System.out.println(DEBUG_clusterBefore1);
                System.out.println(DEBUG_clusterBefore2);
                System.out.println(DEBUG_clusterAfter1);
                System.out.println(DEBUG_clusterAfter2);
                System.out.println("Input:" + DEBUG_input1+"+"+DEBUG_input2);
                System.out.println("Output:" + DEBUG_output1+"+"+DEBUG_output2);
                throw new IllegalStateException("Information loss increased, but this was not a singleton");
            }
            //**********************************
            //**********************************
        }
        
        // Return
        return modified;
    }
    
    /**
     * Assigns the given record to the given cluster
     * @param record
     * @param cluster
     */
    private void setCluster(int record, TassaCluster cluster) {
        this.recordToCluster[record] = cluster;
    }
    
    /**
     * Helper: shuffles the given array
     * @param array
     */
    private void shuffle(int[] array) {
        int count = array.length;
        for (int i = count; i > 1; i--) {
            swap(array, i - 1, random.nextInt(i));
        }
    }

    /**
     * Splits all clusters
     * @param clustering
     * @return
     */
    private boolean splitClusters(Set<TassaCluster> clustering, double omega) {

        // Prepare
        boolean modified = false;

        // Collect clusters with size > w*k 
        Set<TassaCluster> largeClusters = new HashSet<TassaCluster>();
        for (TassaCluster cluster : clustering) {
            if (cluster.getSize() > omega * arxinterface.getK()) {
                largeClusters.add(cluster);
            }
        }
        
        // Split them
        while (!largeClusters.isEmpty()) {

            // Log
            logger.log();
            
            // Split one cluster
            TassaCluster cluster1 = largeClusters.iterator().next();
            TassaCluster cluster2 = cluster1.splitCluster();
            modified = true;
            
            // Check first cluster
            if (cluster1.getSize() <= omega * arxinterface.getK()) {
                largeClusters.remove(cluster1);
            }
            
            // Check second cluster
            if (cluster2.getSize() <= omega * arxinterface.getK()) {
                clustering.add(cluster2);
            } else {
                largeClusters.add(cluster2);
            }
        }
        
        // Return 
        return modified;
    }
    /**
     * Helper: swaps elements in the given array
     * @param array
     * @param i
     * @param j
     */
    private void swap(int[] array, int i, int j) {
        int temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }
    
    /**
     * Method for debugging purposes
     * @param cluster
     */
    private String getClusterAsString(TassaCluster cluster) {
        
        StringBuilder builder = new StringBuilder();
        builder.append("Printing cluster:").append("\n");
        builder.append(" - Input:").append("\n");
        for (int id : cluster.getRecords()) {
            builder.append("  * " + Arrays.toString(arxinterface.getDataQI()[id])).append("\n");
        }
        builder.append(" - Output:\n");
        for (int i = 0; i < cluster.getRecords().length; i++) {
            builder.append("  * " + Arrays.toString(cluster.getTransformation())).append("\n");
        }
        builder.append(" - Loss:" + cluster.getInformationLoss()).append("\n");
        return builder.toString();
    }
    
    /**
     * 
     * @param alpha modifier for the initial size of clusters. has to be 0 < alpha <= 1
     * @param omega modifier for the maximum size of clusters. has to be 1 < omega <= 2
     */
    void execute(double alpha, double omega, Set<TassaCluster> input) {

        
        // Check
        this.checkParameters(alpha, omega);
        
        // Initial step: create random clustering
        this.currentClustering = this.getInitialPartitioning(alpha, omega, input);
        this.inititalInformationLoss =  getTotalInformationLoss();
        
        // Log
        logger.next(TassaStep.INITIALIZE);
        
        // Intermediate steps: move and split
        boolean modified = true;
        while (modified) {
            modified = false;
            // Log
            logger.next(TassaStep.MOVE_RECORDS);
            modified |= moveRecords(this.currentClustering);
            // Log
            logger.next(TassaStep.SPLIT_CLUSTERS);
            modified |= splitClusters(this.currentClustering, omega);
        }

        // Log
        logger.next(TassaStep.FINALIZE);
        // Final step: ensure that all clusters have size >= k
        ensureClustersHaveSize(this.currentClustering, this.arxinterface.getK());
        this.finalInformationLoss = getTotalInformationLoss();

        // Log
        logger.done();
        
        // Transform data
        for (TassaCluster cluster : currentClustering) {
            int[] tuple = cluster.getTransformation();
            for (int record : cluster.getRecords()) {
                for (int i = 0; i<tuple.length; i++) {
                    outputBuffer[record][i] = tuple[i];
                }
            }
        }
    }

    /**
     * Return TODO
     * @return
     */
    double getFinalInformationLoss() {
        return finalInformationLoss;
    }

    /**
     * Return TODO
     * @return
     */
    double getInititalInformationLoss() {
        return inititalInformationLoss;
    }

    /**
     * Return TODO
     * @return
     */
    int[][] getOutputBuffer() {
		return outputBuffer;
	}
    
    /**
     * Return TODO
     * @return
     */
    Set<TassaCluster> getTassaClustering() {
		return currentClustering;
	}
    
}
