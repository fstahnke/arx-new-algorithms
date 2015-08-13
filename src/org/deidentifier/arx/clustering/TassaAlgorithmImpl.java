package org.deidentifier.arx.clustering;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.deidentifier.arx.ARXInterface;
import org.deidentifier.arx.clustering.TassaLogger.TassaStep;

import cern.colt.list.IntArrayList;

public class TassaAlgorithmImpl {

    /** TODO */
    private final TassaLogger        logger              = new TassaLogger(this);
    /** TODO */
    private final ARXInterface       arxinterface;
    /** TODO */
    private double                   inititalInformationLoss;
    /** TODO */
    private double                   finalInformationLoss;
    /** TODO */
    private Set<TassaCluster>        currentClustering;
    /** TODO */
    private final int[][]            outputBuffer;
    /** TODO */
    private TassaCluster[]           recordToCluster;
    /** TODO */
    private int                      numRecords;
    /** TODO */
    private TassaStatistics          statistics          = new TassaStatistics();
    /** TODO */
    private TassaModificationManager modificationManager = new TassaModificationManager();

    /**
	 * Creates a new instance
	 * @param iface
	 * @throws IOException
	 */
    TassaAlgorithmImpl(ARXInterface iface) throws IOException {
        this.arxinterface = iface;
        this.outputBuffer = iface.getBuffer();
        this.recordToCluster = new TassaCluster[arxinterface.getDataQI().length];
        this.numRecords = iface.getDataQI().length;
    }
    
    /**
     * Assigns all records to the cluster
     * @param records
     * @param cluster
     */
    private void assignRecordsToCluster(IntArrayList records, TassaCluster cluster) {
        for (int i = 0; i < records.size(); i++) {
            assignRecordToCluster(records.getQuick(i), cluster);
        }
    }
    
    /**
     * Assigns the given record to the given cluster
     * @param record
     * @param cluster
     */
    private void assignRecordToCluster(int record, TassaCluster cluster) {
        this.recordToCluster[record] = cluster;
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
        
        TassaClosenessMatrix matrix = new TassaClosenessMatrix(smallClusters);
        TassaPair<TassaCluster, TassaCluster> pair = matrix.getClosestTwoClusters();

        // As long as there are clusters with size < k
        // merge closest two clusters and either
        // if size >= k, add them to output, or
        // if size < k, process them further
        while (pair != null) {

            // Log
            logger.log ();
            
            // Merge closest pair
            assignRecordsToCluster(pair.second.getRecords(), pair.first);
            pair.first.addCluster(pair.second);
            smallClusters.remove(pair.second);
            matrix.setMerged(pair.first, pair.second);
            
            if (pair.first.getSize() >= clusterSize) {
                largeClusters.add(pair.first);
                smallClusters.remove(pair.first);
                matrix.setRemoved(pair.first);
            }

            // Update statistics
            statistics.incClustersMerged();
            
            // Update
            pair = matrix.getClosestTwoClusters();
        }
        
        // If there is one cluster left, merge it with the closest cluster from the large clusters
        if (smallClusters.size() == 1) {

            // Log
            logger.log ();
            
            // Perform
            TassaCluster cluster1 = smallClusters.iterator().next();
            TassaCluster cluster2 = getClosestClusterForCluster(largeClusters, cluster1);
            assignRecordsToCluster(cluster1.getRecords(), cluster2);
            cluster2.addCluster(cluster1);
            smallClusters.remove(cluster1);

            // Update statistics
            statistics.incClustersMerged();
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
    private TassaCluster getClosestClusterForCluster(Set<TassaCluster> clustering, TassaCluster cluster) {

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
    private TassaPair<TassaCluster, Double> getClosestClusterForRecord(Set<TassaCluster> clustering, TassaCluster source, int record) {

        double delta = Double.MAX_VALUE;
        double loss = Double.MAX_VALUE;
        TassaCluster result = null;
        
        for (TassaCluster cluster : clustering) {
            
            if (cluster != source &&  (modificationManager.isModified(source) || modificationManager.isModified(cluster))) {

                // Skip if lower bound is already higher then the current optimum
                if (delta != Double.MAX_VALUE && cluster.getLowerBoundForAdditionalInformationLoss() > delta) {
                    continue;
                }
                
                
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
            assignRecordsToCluster(cluster.getRecords(), cluster);
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
        IntArrayList recordIds = new IntArrayList(numRecords);
        for (int i = 0; i < numRecords; i++) {
            recordIds.add(i);
        }
        recordIds.shuffle();
        int offset = 0;
        
        // Calculate
        final int numberOfClusters = (int) Math.floor(numRecords / k);
        final int additionalRecords = numRecords % k;
        
        // Build
        Set<TassaCluster> result = new HashSet<TassaCluster>();
        for (int i = 0; i<numberOfClusters; i++) {
            int clusterSize = i < additionalRecords ? k + 1 : k;
            TassaCluster cluster = new TassaCluster(manager, (IntArrayList)recordIds.partFromTo(offset, offset + clusterSize - 1));
            result.add(cluster);
            offset += clusterSize;
        }
        
        // Return
        return result;
    }

    /**
     * 
     * @param oldValue
     * @param newValue
     * @param normalizationFactor
     * @return
     */
    private boolean isSignficantlySmaller(double oldValue, double newValue, double normalizationFactor) {
        return newValue / normalizationFactor - oldValue / normalizationFactor < -0.0001d;
    }
    
    /**
     * Moves all records within the given clustering, if it decreases the average information loss
     * @param clustering
     * @return
     */
    private boolean moveRecords(Set<TassaCluster> clustering) {

        // Log
        logger.log();
        
        // Flag to detect modification
        boolean modified = false;
        
        // Loop
        for (int record = 0; record < numRecords; record++) {

            // Log
            logger.log();
            
            // Find closest cluster
            TassaCluster sourceCluster = getCluster(record);
            TassaPair<TassaCluster, Double> targetCluster = getClosestClusterForRecord(clustering, sourceCluster, record);

            // Check if it improves the overall costs. Take cluster sizes into account.
            double inputGC = sourceCluster.getInformationLoss() + targetCluster.first.getInformationLoss();
            double outputGC = sourceCluster.getInformationLossWhenRemoving(record) + targetCluster.second;
            
            // If yes or if source cluster is singleton, move
            if (isSignficantlySmaller(inputGC, outputGC, sourceCluster.getSize() + targetCluster.first.getSize()) || sourceCluster.getSize() == 1) {
                
                // Update statistics
                statistics.incRecordsMoved();
                
                // Move
                targetCluster.first.addRecord(record);
                sourceCluster.removeRecord(record);
                assignRecordToCluster(record, targetCluster.first);
                
                // Remove if empty
                if (sourceCluster.getSize() == 0) {
                    clustering.remove(sourceCluster);
                }
                
                // Set modified
                this.modificationManager.setModified(sourceCluster);
                this.modificationManager.setModified(targetCluster.first);
                modified = true;
            }
        }
        
        // Return
        return modified;
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
        Iterator<TassaCluster> iter = clustering.iterator();
        while (iter.hasNext()) {
            TassaCluster cluster = iter.next();
            if (cluster.getSize() > omega * arxinterface.getK()) {
                largeClusters.add(cluster);
                iter.remove();
            }
        }
        
        // Split them
        while (!largeClusters.isEmpty()) {

            // Log
            logger.log();
            
            // Split one cluster
            TassaCluster cluster1 = largeClusters.iterator().next();
            TassaCluster cluster2 = cluster1.splitCluster();
            
            // Set modified
            modified = true;
            this.modificationManager.setModified(cluster1);
            this.modificationManager.setModified(cluster2);

            // Update statistics
            statistics.incClustersSplit();
            
            // Check first cluster
            if (cluster1.getSize() <= omega * arxinterface.getK()) {
                largeClusters.remove(cluster1);
                clustering.add(cluster1);
                assignRecordsToCluster(cluster1.getRecords(), cluster1);
            }
            
            // Check second cluster
            if (cluster2.getSize() <= omega * arxinterface.getK()) {
                clustering.add(cluster2);
                assignRecordsToCluster(cluster2.getRecords(), cluster2);
            } else {
                largeClusters.add(cluster2);
            }
        }

        // Return 
        return modified;
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
        long time = System.currentTimeMillis();
        this.currentClustering = this.getInitialPartitioning(alpha, omega, input);
        this.inititalInformationLoss =  getTotalInformationLoss();
        this.modificationManager.setModified(this.currentClustering);
        
        // Log
        logger.next(TassaStep.INITIALIZE);
        
        // Intermediate steps: move and split
        boolean modified = true;
        while (modified) {
            modified = false;
            
            // Log
            double previousLoss = getTotalInformationLoss();
            logger.next(TassaStep.MOVE_RECORDS);
            modified |= moveRecords(this.currentClustering);
            double newLoss = getTotalInformationLoss();
            
            // Log
            logger.next(TassaStep.SPLIT_CLUSTERS);
            modified |= splitClusters(this.currentClustering, omega);
            
            // Break
            if (!isSignficantlySmaller(previousLoss, newLoss, this.numRecords)) {
                break;
            }
            
            // Prepare
            this.modificationManager.prepareNextIteration();
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
            for (int j = 0; j <cluster.getRecords().size(); j++) {
                int record = cluster.getRecords().getQuick(j);
                for (int i = 0; i<tuple.length; i++) {
                    outputBuffer[record][i] = tuple[i];
                }
            }
        }
        
        statistics.setFinalInformationLoss(this.getFinalInformationLoss());
        statistics.setInitialInformationLoss(this.getInititalInformationLoss());
        statistics.setNumberOfClusters(this.getNumberOfClusters());
        statistics.setExecutionTime(System.currentTimeMillis() - time);
    }
    
    /**
     * Return TODO
     * @return
     */
    Set<TassaCluster> getClustering() {
		return currentClustering;
	}
    
    /**
     * Return TODO
     * @return
     */
    double getFinalInformationLoss() {
        return finalInformationLoss / this.numRecords;
    }

    /**
     * Return TODO
     * @return
     */
    double getInititalInformationLoss() {
        return inititalInformationLoss / this.numRecords;
    }

    /**
     * Returns the number of clusters
     * @return
     */
	int getNumberOfClusters() {
        if (currentClustering != null) {
            return currentClustering.size();
        } else {
            return 0;
        }
    }

    /**
     * Returns the number of records
     * @return
     */
    double getNumberOfRecords() {
        return this.numRecords;
    }
    
    /**
     * Return TODO
     * @return
     */
    int[][] getOutputBuffer() {
		return outputBuffer;
	}

    /**
     * Returns statistics
     */
    TassaStatistics getStatistics() {
        return this.statistics;
    }

    /**
     * Returns the total information loss
     * @return
     */
    double getTotalInformationLoss() {
        
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
    
    /**
     * Enable/disable logging
     * @param logging
     */
    void setLogging(boolean logging) {
        this.logger.setLogging(logging);
    }
}
