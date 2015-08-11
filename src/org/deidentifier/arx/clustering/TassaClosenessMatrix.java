package org.deidentifier.arx.clustering;

import java.util.Arrays;
import java.util.Set;

public class TassaClosenessMatrix {

    /** Matrix with distances. A value of Double.MAX_VALUE marks an empty slot **/
    private double[][]     matrix;
    /** Cluster with id x is at clusters[x] */
    private TassaCluster[] clusters;
    
    /**
     * Creates a new instance
     * @param clustering
     */
    public TassaClosenessMatrix(Set<TassaCluster> clustering) {
    
        // Assign ids
        int count = 0;
        for (TassaCluster cluster : clustering) {
            cluster.id = count++;
        }
        clusters = new TassaCluster[clustering.size()];
        for (TassaCluster cluster : clustering) {
            clusters[cluster.id] = cluster;
        }
        
        // Create matrix
        this.matrix = new double[count][];
        for (int i = 0; i < matrix.length; i++) {
            matrix[i] = new double[count];
            Arrays.fill(matrix[i], Double.MAX_VALUE);
        }
        
        // Initialize
        for (int x=0; x < clusters.length; x++) {
            for (int y=x+1; y<clusters.length; y++) {
                double delta = clusters[x].getInformationLossWhenAdding(clusters[y]) - 
                               (clusters[x].getInformationLoss() + clusters[y].getInformationLoss());
                matrix[x][y] = delta;
            }
        }
    }

    /**
     * Returns the closest two clusters in the given clustering
     * @param clustering
     * @return
     */
    public TassaPair<TassaCluster, TassaCluster> getClosestTwoClusters() {
        
        double deltaMin = Double.MAX_VALUE;
        TassaCluster cluster1 = null;
        TassaCluster cluster2 = null;
        
        for (int x=0; x < clusters.length; x++) {
            for (int y=x+1; y<clusters.length; y++) {
                double delta = matrix[x][y];
                if (delta < deltaMin) {
                    cluster1 = clusters[x];
                    cluster2 = clusters[y];
                    deltaMin = delta;
                }
            }
        }
        
        if (deltaMin == Double.MAX_VALUE) {
            return null;
        } else {
            return new TassaPair<TassaCluster, TassaCluster>(cluster1, cluster2);
        }
    }
    
    /**
     * Second has been merged into source
     * @param first
     * @param second
     */
    public void setMerged(TassaCluster first, TassaCluster second) {
        
        // Remove second cluster
        for (int x = 0; x < clusters.length; x++) {
            matrix[x][second.id] = Double.MAX_VALUE;
            matrix[second.id][x] = Double.MAX_VALUE;
        }
        
        // Update all relationships for first cluster
        for (int x=0; x < clusters.length; x++) {
            int xIndex = x < first.id ? x : first.id;
            int yIndex = first.id > x ? first.id : x;
            if (x != first.id && matrix[xIndex][yIndex] != Double.MAX_VALUE) {
                double delta = clusters[x].getInformationLossWhenAdding(clusters[first.id]) - 
                               (clusters[x].getInformationLoss() + clusters[first.id].getInformationLoss());
                matrix[xIndex][yIndex] = delta;
            }
        }
    }

    /**
     * Cluster has been removed
     * @param cluster
     */
    public void setRemoved(TassaCluster cluster) {

        // Remove cluster
        for (int x = 0; x < clusters.length; x++) {
            matrix[x][cluster.id] = Double.MAX_VALUE;
            matrix[cluster.id][x] = Double.MAX_VALUE;
        }
    }
}
