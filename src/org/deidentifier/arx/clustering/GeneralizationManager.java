package org.deidentifier.arx.clustering;

import org.deidentifier.arx.ARXInterface;
import org.deidentifier.arx.framework.data.GeneralizationHierarchy;
import org.deidentifier.arx.metric.v2.DomainShareMaterialized;

import cern.colt.list.IntArrayList;

public class GeneralizationManager {

    /** TODO */
    private final DomainShareMaterialized[] shares;
    /** TODO */
    private final int                       numAttributes;
    /** TODO */
    private final int[][][]                 hierarchies;
    /** TODO */
    private final int[][]                   data;

    /**
     * Creates a new instance
     * @param arxInterface
     */
    public GeneralizationManager(ARXInterface arxInterface) {
        GeneralizationHierarchy[] generalizationHierarchies = arxInterface.getDataManager().getHierarchies();
        this.numAttributes = generalizationHierarchies.length;
        this.shares = new DomainShareMaterialized[numAttributes];
        this.hierarchies = new int[numAttributes][][];
        this.data = arxInterface.getDataQI();
        for (int i = 0; i < generalizationHierarchies.length; i++) {
            this.shares[i] = arxInterface.getDomainShares()[i];
            this.hierarchies[i] = generalizationHierarchies[i].getArray();
        }
    }

    /**
     * Returns a domain share
     * @param dimension
     * @param level
     * @param value
     * @return
     */
    public double getDomainShare(int dimension, int level, int value) {
        return shares[dimension].getShare(value, level);
    }
    
    /**
     * Returns the domain size
     * @param dimension
     * @return
     */
    public double getDomainSize(int dimension) {
        return shares[dimension].getDomainSize();
    }
    
    /**
     * Returns a generalization level
     * @param dimension
     * @param records
     * @return
     */
    public int getGeneralizationLevel(int dimension, IntArrayList records) {
        return getGeneralizationLevel(dimension, records, 0);
    }


    /**
     * Cluster
     * @param records
     * @return
     */
    public int getGeneralizationLevel(int dimension, IntArrayList records, int lvl) {

        int[][] hierarchy = hierarchies[dimension];
        int val = hierarchy[data[records.getQuick(0)][dimension]][lvl];
        for (int i = 1; i < records.size() && lvl != hierarchy[0].length - 1; i++) {
            while (hierarchy[data[records.getQuick(i)][dimension]][lvl] != val) {
                val = hierarchy[data[records.getQuick(i - 1)][dimension]][++lvl];
            }
        }
        return lvl;
    }
    
    /**
     * Two clusters
     */
    public int getGeneralizationLevelWhenAddingCluster(int dimension, IntArrayList records1, IntArrayList records2, int lvl) {


        int[][] hierarchy = hierarchies[dimension];
        int[] current = hierarchy[data[records2.getQuick(0)][dimension]];
        int[] previous = hierarchy[data[records1.getQuick(0)][dimension]];
        int val = previous[lvl];
        while (current[lvl] != val) {
            val = previous[++lvl];
        }
        return lvl;
    }

    /**
     * Cluster and record
     */
    public int getGeneralizationLevelWhenRemovingRecord(int dimension, IntArrayList records, int record) {

        // Prepare
        int[][] hierarchy = hierarchies[dimension];
        int lvl = 0;
        int idx = records.getQuick(0) == record ? 1 : 0;
        int val = hierarchy[data[records.getQuick(idx)][dimension]][lvl];
        
        for (int i = idx + 1; i < records.size() && lvl != hierarchy[0].length - 1; i++) {
            
            if (records.getQuick(i) == record && i == records.size()-1) {
                break;
            }
            
            int current = records.getQuick(i) != record ? records.getQuick(i) : records.getQuick(i+1);
            int previous = records.getQuick(i-1) != record ? records.getQuick(i-1) : records.getQuick(i-2);
            
            while (hierarchy[data[current][dimension]][lvl] != val) {
                val = hierarchy[data[previous][dimension]][++lvl];
            }
        }
        return lvl;
    }

    /**
     * Once cluster. This method has two side effects: it updates the generalization and cache arrays
     * @param cluster
     * @param generalization
     * @param cache 
     * @return
     */
    public double getInformationLoss(IntArrayList cluster, int[] generalization, double[] cache) {

        double cost = 0d;
        int[] record = data[cluster.getQuick(0)];
        
        for (int i = 0; i < numAttributes; i++) {
            int level = getGeneralizationLevel(i, cluster);
            if (level != generalization[i] || cache[i] == -1d) {
                generalization[i] = level;
                int[][] hierarchy = hierarchies[i];
                int value = hierarchy[record[i]][level];
                double share = getDomainShare(i, level, value);
                cost += share;
                cache[i] = share;
            } else {
                cost += cache[i];
            }
        }
        
        cost /= (double) numAttributes;
        cost *= cluster.size();
        return cost;
    }

    /**
     * Two clusters
     * @param cluster1
     * @param generalization1
     * @param cluster2
     * @param generalization2
     * @return
     */
    public double getInformationLossWhenAddingCluster(IntArrayList cluster1, 
                                                      int[] generalization1,
                                                      IntArrayList cluster2, 
                                                      int[] generalization2) {
        
        

        double cost = 0d;
        int[] record = data[cluster1.getQuick(0)];

        for (int i = 0; i < numAttributes; i++) {
            int level = getGeneralizationLevelWhenAddingCluster(i,
                                                                cluster1,
                                                                cluster2,
                                                                Math.max(generalization1[i],
                                                                         generalization2[i]));
            int[][] hierarchy = hierarchies[i];
            int value = hierarchy[record[i]][level];
            cost += getDomainShare(i, level, value);
        }

        cost /= (double) numAttributes;
        cost *= (cluster1.size() + cluster2.size());
        return cost;
    }
    
    /**
     * Cluster and record
     * @param cluster
     * @param generalization
     * @param record
     * @return
     */
    public double getInformationLossWhenAddingRecord(IntArrayList cluster, 
                                                     int[] generalization, 
                                                     int record,
                                                     double[] cache) {

        double cost = 0d;
        int[] tuple = data[record];
        
        for (int dimension = 0; dimension <numAttributes; dimension++) {
            
            int inputvalue = tuple[dimension];
            int level = generalization[dimension];
            int[][] hierarchy = hierarchies[dimension];
            int[] current = hierarchy[inputvalue];
            int[] previous = hierarchy[data[cluster.getQuick(0)][dimension]];
            int tempvalue = previous[level];
            while (current[level] != tempvalue) {
                tempvalue = previous[++level];
            }
            
            if (level != generalization[dimension]) {
                int generalizedvalue = hierarchy[inputvalue][level];
                cost += getDomainShare(dimension, level, generalizedvalue);
            } else {
                cost += cache[dimension];
            }
        }

        cost /= (double) numAttributes;
        cost *= (cluster.size() + 1);
        return cost;
    }

    /**
     * Cluster without record
     * @param cluster
     * @param record
     * @return
     */
    public double getInformationLossWhenRemovingRecord(IntArrayList cluster, int record) {

        

        double cost = 0d;
        int index = cluster.getQuick(0) != record ? cluster.getQuick(0) : cluster.getQuick(1);
        int[] tuple = data[index];
        
        for (int i = 0; i < numAttributes; i++) {
            int level = getGeneralizationLevelWhenRemovingRecord(i, cluster, record);
            int[][] hierarchy = hierarchies[i];
            int value = hierarchy[tuple[i]][level];
            cost += getDomainShare(i, level, value);
        }

        cost /= (double) numAttributes;
        cost *= (cluster.size() - 1);
        return cost;
    }
    

    /**
     * Returns the number of attributes
     * @return
     */
    public int getNumAttributes() {
        return numAttributes;
    }
    
    /**
     * Returns the transformed record
     */
    public int[] getTransformation(int record, int[] generalization) {
        int[] result = new int[generalization.length];
        for (int i=0; i<result.length; i++) {
            result[i] = hierarchies[i][data[record][i]][generalization[i]];
        }
        return result;
    }
}
