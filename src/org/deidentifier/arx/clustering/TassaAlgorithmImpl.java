package org.deidentifier.arx.clustering;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;

import org.deidentifier.arx.ARXInterface;

class TassaAlgorithmImpl {

    private final ARXInterface arxinterface;
    private double             inititalInformationLoss;
    private double             finalInformationLoss;
    private Set<TassaCluster>  result;
    private final int[][]      outputBuffer;
    private TassaCluster[]     recordToCluster;
    private int                numRecords;

	TassaAlgorithmImpl(ARXInterface iface) throws IOException {
        this.arxinterface = iface;
        this.outputBuffer = iface.getBuffer();
        this.recordToCluster = new TassaCluster[arxinterface.getDataQI().length];
        this.numRecords = iface.getDataQI().length;
    }

    /**
     * 
     * @param alpha modifier for the initial size of clusters. has to be 0 < alpha <= 1
     * @param omega modifier for the maximum size of clusters. has to be 1 < omega <= 2
     */
    void execute(double alpha, double omega, Set<TassaCluster> input) {

        // check for correct arguments
        if (alpha <= 0 || alpha > 1) {
            throw new IllegalArgumentException("executeTassa: Argument 'alpha' is out of bound: " + alpha);
        }
        if (omega <= 1 || omega > 2) {
            throw new IllegalArgumentException("executeTassa: Argument 'omega' is out of bound: " + omega);
        }
        
        // Prepare
        final int k = arxinterface.getK();
        final int k_0 = (int) Math.floor(alpha * k) > 0 ? (int) Math.floor(alpha * k) : 1;
        
        if (input == null) {
            result = this.getRandomPartitioning(arxinterface.getGeneralizationManager(), 
                                                numRecords, k_0);
        }
        else {
            result = input;
        }
        
        for (TassaCluster cluster : result) {
            for (int recordId : cluster.getRecords()) {
                this.setCluster(recordId, cluster);
            }
        }
        
        for (int i=0; i<numRecords; i++) {
            if (recordToCluster[i] == null) {
                System.out.println(i);
                System.exit(0);
            }
        }
        
        double lastIL = getAverageGeneralizationCost(result);
        inititalInformationLoss = lastIL;
        
        if (arxinterface.isLogging()) {
            System.out.println("Initial average information loss: " + inititalInformationLoss + ", Initial cluster count: " + result.size());
        }
        
        // Helper variable to check, if records were changed
        boolean recordsChanged = true;
        int recordChangeCount = 0;

        HashSet<TassaCluster> tempOutput = new HashSet<>(result);
        HashSet<TassaCluster> modifiedClusters = new HashSet<>(tempOutput);
        
        while (recordsChanged) {
            
            // reset recordsChanged flag
            recordsChanged = false;
            HashSet<TassaCluster> clustersToCheck = new HashSet<>(modifiedClusters);
            modifiedClusters.clear();
            
            int recordCount = 1;
            final long initTime = System.nanoTime();
            long startTime = initTime;
            
            // Loop: check all records for improvement of information loss
            for (int recordId = 0; recordId < arxinterface.getDataQI().length; recordId++) {
                if (arxinterface.isLogging() && (recordCount % arxinterface.getLogNumberOfRecords() == 0 || recordCount >= numRecords)) {
                    final long stopTime = System.nanoTime();
                    System.out.println("#Clusters: " + clustersToCheck.size() +"/"+ tempOutput.size() + ", Record number: " + recordCount + ", Execution time: " + Math.round((stopTime - startTime) / 1000000.0) + " ms, Average time: " + Math.round((stopTime - initTime) * arxinterface.getLogNumberOfRecords() / (recordCount * 1000000.0)) + " ms");
                    startTime = stopTime;
                }
                recordCount++;
                final TassaCluster sourceCluster = getCluster(recordId);
                TassaCluster targetCluster = null;
                double deltaIL = Double.MAX_VALUE;
                
                // find cluster with minimal change of information loss
                if (clustersToCheck.contains(sourceCluster)) {
                    for (final TassaCluster cluster : tempOutput) {
                        if (cluster != sourceCluster) {
                            final double tempDelta = getChangeOfInformationLoss(recordId, cluster, numRecords);
                            if (tempDelta < deltaIL) {
                                deltaIL = tempDelta;
                                targetCluster = cluster;
                            }
                        }
                    }
                }
                else {
                    for (final TassaCluster cluster : clustersToCheck) {
                        if (cluster != sourceCluster) {
                            final double tempDelta = getChangeOfInformationLoss(recordId, cluster, numRecords);
                            if (tempDelta < deltaIL) {
                                deltaIL = tempDelta;
                                targetCluster = cluster;
                            }
                        }
                    }
                }
                
                if (sourceCluster.getSize() == 1 && targetCluster != null)
                {
                    // move record to target cluster
                    tempOutput.remove(targetCluster);
                    modifiedClusters.remove(targetCluster);
                    clustersToCheck.remove(targetCluster);
                    targetCluster.addRecord(recordId);
                    setCluster(recordId, targetCluster);
                    tempOutput.add(targetCluster);
                    modifiedClusters.add(targetCluster);
                    clustersToCheck.add(targetCluster);
                    
                    // remove empty source cluster from all containing collections
                    tempOutput.remove(sourceCluster);
                    modifiedClusters.remove(sourceCluster);
                    clustersToCheck.remove(sourceCluster);
                    
                    // log the change for the next loop
                    /*if (!modifiedClusters.contains(targetCluster)) {
                        modifiedClusters.add(targetCluster);
                    }*/
                    recordsChanged = true;
                    recordChangeCount++;
                }
                
                // If change in information loss is negative, move record to new cluster
                else if (deltaIL < -0.0000000001 && targetCluster != null) {
                    
                    // remove record from source cluster
                    tempOutput.remove(sourceCluster);
                    modifiedClusters.remove(sourceCluster);
                    clustersToCheck.remove(sourceCluster);
                    sourceCluster.removeRecord(recordId);
                    tempOutput.add(sourceCluster);
                    modifiedClusters.add(sourceCluster);
                    clustersToCheck.add(sourceCluster);
                    
                    // move record to target cluster
                    tempOutput.remove(targetCluster);
                    modifiedClusters.remove(targetCluster);
                    clustersToCheck.remove(targetCluster);
                    targetCluster.addRecord(recordId);
                    setCluster(recordId, targetCluster);
                    tempOutput.add(targetCluster);
                    modifiedClusters.add(targetCluster);
                    clustersToCheck.add(targetCluster);
                    
                    // log the change for the next loop
                    /*if (!modifiedClusters.contains(targetCluster)) {
                        modifiedClusters.add(targetCluster);
                    }
                    if (!modifiedClusters.contains(sourceCluster)) {
                        modifiedClusters.add(sourceCluster);
                    }*/
                    recordsChanged = true;
                    recordChangeCount++;
                }
            }
            
            // Check for clusters greater w*k, split them and add them back to tempOutput
            final LinkedList<TassaCluster> bigClusters = new LinkedList<>();
            for (final Iterator<TassaCluster> itr = tempOutput.iterator(); itr.hasNext();) {
                final TassaCluster cluster = itr.next();
                if (cluster.getSize() > omega * k) {
                    itr.remove();
                    bigClusters.add(cluster);
                }
            }
            
            clustersToCheck.removeAll(bigClusters);
            modifiedClusters.removeAll(bigClusters);
            
            final LinkedList<TassaCluster> newClusters = new LinkedList<>();
            while (bigClusters.size() > 0) {
                Iterator<TassaCluster> itr = bigClusters.iterator();
                while (itr.hasNext()) {
                    TassaCluster next = itr.next();
                    newClusters.add(next.splitCluster());
                    newClusters.add(next);
                    itr.remove();
                }
                itr = newClusters.iterator();
                while (itr.hasNext()) {
                    TassaCluster c = itr.next();
                    if (c.getSize() > omega * k) {
                        itr.remove();
                        bigClusters.add(c);
                    }
                }
            }
            
            modifiedClusters.addAll(newClusters);
            tempOutput.addAll(newClusters);
            clustersToCheck.addAll(newClusters);
            
            final double IL = getAverageGeneralizationCost(tempOutput);
            if (arxinterface.isLogging()) {
                System.out.println("Current average information loss: " + IL + ", DeltaIL: " + (IL-lastIL) + ", Records changed: " + recordChangeCount + ", Clusters to check: " + modifiedClusters.size() +"/"+ tempOutput.size());
            }
            
            clustersToCheck.clear();
            recordChangeCount = 0;
            lastIL = IL;
        }
        
        // put small clusters into smallClusters collection
        final Set<TassaCluster> smallClusters = new HashSet<TassaCluster>();
        
        for (final TassaCluster cluster : tempOutput) {
            if (cluster.getSize() < k) {
                smallClusters.add(cluster);
            }
        }
        // remove small clusters from tempOutput
        tempOutput.removeAll(smallClusters);
        
        // As long as there are clusters with size < k
        // merge closest two clusters and either
        // if size >= k, add them to output, or
        // if size < k, add them back to smallClusters

        final long initTime = System.nanoTime();
        long startTime = initTime;
        int mergeNumber = 1;
        
        // Add temporary clustering to output and merge clusters < k
        result.clear();
        result.addAll(tempOutput);
        
        while (smallClusters.size() > 1) {

            if (arxinterface.isLogging() && (mergeNumber % arxinterface.getLogNumberOfClusters() == 0 || smallClusters.size() == 2)) {
                final long stopTime = System.nanoTime();
                System.out.println("Merged clusters: " + mergeNumber + ", Execution time: " + Math.round((stopTime - startTime) / 1000000.0) + " ms, Average time: " + Math.round((stopTime - initTime) / (mergeNumber * 1000000.0)) + " ms");
                startTime = stopTime;
            }
            mergeNumber++;
            
            Set<TassaCluster> closestPair = getClosestTwoClusters(smallClusters);
            Iterator<TassaCluster> iter = closestPair.iterator();
            TassaCluster cluster1 = iter.next();
            TassaCluster cluster2 = iter.next();
            for (int record : cluster2.getRecords()) {
                setCluster(record, cluster1);
            }
            cluster1.addCluster(cluster2);
            
            smallClusters.remove(cluster2);
            
            if (cluster1.getSize() >= k) {
                result.add(cluster1);
                smallClusters.remove(cluster1);
            }
        }
        
        if (smallClusters.size() == 1) {

            if (arxinterface.isLogging()) {
                final long stopTime = System.nanoTime();
                System.out.println("Merged clusters: " + mergeNumber + ", Execution time: " + Math.round((stopTime - startTime) / 1000000.0) + " ms, Average time: " + Math.round((stopTime - initTime) / (mergeNumber * 1000000.0)) + " ms");
                startTime = stopTime;
            }
            
            TassaCluster cluster1 = smallClusters.iterator().next();
            TassaCluster cluster2 = getClosestCluster(smallClusters, cluster1);
            for (int record : cluster2.getRecords()) {
                setCluster(record, cluster1);
            }
            cluster1.addCluster(cluster2);
            smallClusters.remove(cluster2);
            
        }
        
        finalInformationLoss = getAverageGeneralizationCost(result);
        
        if (arxinterface.isLogging()) {
            System.out.println("Final average information loss: " + finalInformationLoss);
        }
        
        for (TassaCluster cluster : result) {
            int[] tuple = cluster.getTransformation();
            for (int record : cluster.getRecords()) {
                for (int i = 0; i<tuple.length; i++) {
                    outputBuffer[record][i] = tuple[i];
                }
            }
        }
    }

    private double getAverageGeneralizationCost(Set<TassaCluster> clusters) {
        double result = 0.0;
        int numRecords = 0;
        for (final TassaCluster c : clusters) {
            numRecords += c.getSize();
            result += c.getCost() * c.getSize();
        }
        return result / numRecords;
        
    }

    double getFinalInformationLoss() {
        return finalInformationLoss;
    }
    
    double getInititalInformationLoss() {
        return inititalInformationLoss;
    }
    
    int[][] getOutputBuffer() {
		return outputBuffer;
	}

	Set<TassaCluster> getTassaClustering() {
		return result;
	}
    
    private double getChangeOfInformationLoss(int movedRecord, TassaCluster targetCluster, int n) {
        
        final TassaCluster sourceCluster = getCluster(movedRecord);
        
        double removedGC = sourceCluster.getCostWhenRemovingRecord(movedRecord);
        double sourceGC = sourceCluster.getCost();
        double targetGC = targetCluster.getCost();
        if (removedGC >= sourceGC && removedGC > 0 && targetGC >= sourceGC ) {
            return 1.0;
        }
        
        
        double deltaIL = (removedGC * (sourceCluster.getSize() - 1)
                  + targetCluster.getCostWhenAddingRecord(movedRecord) * (targetCluster.getSize() + 1))
                  - (sourceGC * sourceCluster.getSize()
                  + targetGC * targetCluster.getSize());
        deltaIL /= n;
        
        return deltaIL;
    }
    
    
    private Random random = new Random();

    private Set<TassaCluster> getRandomPartitioning(GeneralizationManager manager, int records, int k) {

        // Prepare
        int[] recordIds = new int[records];
        for (int i = 0; i < records; i++) {
            recordIds[i] = i;
        }
        shuffle(recordIds);
        int offset = 0;
        
        // Calculate
        final int numberOfClusters = (int) Math.floor(records / k);
        final int additionalRecords = records % k;
        
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
    
    private void shuffle(int[] array) {
        int count = array.length;
        for (int i = count; i > 1; i--) {
            swap(array, i - 1, random.nextInt(i));
        }
    }

    private void swap(int[] array, int i, int j) {
        int temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

    private TassaCluster getClosestCluster(Set<TassaCluster> clusters, TassaCluster cluster1) {

        double loss = Double.MAX_VALUE;
        TassaCluster result = null;

        for (TassaCluster cluster2 : clusters) {
            if (cluster1 != cluster2) {
                double value = cluster1.getCostWhenAddingCluster(cluster2);
                if (value < loss) {
                    loss = value;
                    result = cluster2;
                }
            }
        }
        if (result == null) { throw new IllegalStateException("Should not happen!"); }
        return result;
    }
    
    private Set<TassaCluster> getClosestTwoClusters(Set<TassaCluster> clusters) {
        
        double loss = Double.MAX_VALUE;
        Set<TassaCluster> result = new HashSet<TassaCluster>();
        
        for (TassaCluster cluster1 : clusters) {
            for (TassaCluster cluster2 : clusters) {
                if (cluster1 != cluster2) {
                    double value = cluster1.getCostWhenAddingCluster(cluster2);
                    if (value < loss) {
                        loss = value;
                        result.clear();
                        result.add(cluster1);
                        result.add(cluster2);
                    }
                }
            }
        }
        if (result.isEmpty()) {
            throw new IllegalStateException("Should not happen!");
        }
        return result;
    }
    
    private void setCluster(int recordId, TassaCluster cluster) {
        this.recordToCluster[recordId] = cluster;
    }
    
    private TassaCluster getCluster(int recordId) {
        return this.recordToCluster[recordId];
    }
}
