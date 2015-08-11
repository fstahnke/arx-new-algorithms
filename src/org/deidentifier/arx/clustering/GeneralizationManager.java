package org.deidentifier.arx.clustering;

import org.deidentifier.arx.ARXInterface;
import org.deidentifier.arx.framework.data.GeneralizationHierarchy;
import org.deidentifier.arx.metric.v2.DomainShareMaterialized;

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
     * Once cluster
     * @param cluster
     * @param generalization
     * @return
     */
    public double getInformationLoss(int[] cluster, int[] generalization) {
        
        double cost = 0d;
        int[] record = data[cluster[0]];
        for (int i = 0; i < record.length; i++) {
            int[][] hierarchy = hierarchies[i];
            int value = hierarchy[record[i]][generalization[i]];
            cost += getDomainShare(i, generalization[i], value);
        }
        cost /= (double) numAttributes;
        cost *= cluster.length;
        return cost;
    }


    /**
     * Cluster and record
     * @param cluster
     * @param _generalization
     * @param additionalRecord
     * @return
     */
    public double getInformationLossWhenAddingRecord(int[] cluster, int[] _generalization, int additionalRecord) {

        int[] generalization = new int[numAttributes];
        for (int i = 0; i <generalization.length; i++) {
            generalization[i] = getGeneralizationLevel(i, cluster, additionalRecord, _generalization[0]);
        }
        
        double cost = 0d;
        int[] record = data[additionalRecord];
        for (int i = 0; i < record.length; i++) {
            int[][] hierarchy = hierarchies[i];
            int value = hierarchy[record[i]][generalization[i]];
            cost += getDomainShare(i, generalization[i], value);
        }

        cost /= (double) numAttributes;
        cost *= (cluster.length + 1);
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
    public double getInformationLossWhenAddingCluster(int[] cluster1, int[] generalization1, int[] cluster2, int[] generalization2) {
        
        int[] generalization = new int[numAttributes];
        for (int i = 0; i < generalization.length; i++) {
            generalization[i] = getGeneralizationLevel(i, cluster1, cluster2, Math.max(generalization1[i], generalization2[i]));
        }

        double cost = 0d;
        int[] record = data[cluster1[0]];
        for (int i = 0; i < record.length; i++) {
            int[][] hierarchy = hierarchies[i];
            int value = hierarchy[record[i]][generalization[i]];
            cost += getDomainShare(i, generalization[i], value);
        }

        cost /= (double) numAttributes;
        cost *= (cluster1.length + cluster2.length);
        return cost;
    }

    /**
     * Cluster without record
     * @param cluster
     * @param record
     * @return
     */
    public double getInformationLossWithoutRecord(int[] cluster, int record) {

        int[] generalization = new int[numAttributes];
        for (int i = 0; i < generalization.length; i++) {
            generalization[i] = getGeneralizationLevelWithoutRecord(i, cluster, record);
        }

        double cost = 0d;
        int index = cluster[0] != record ? cluster[0] : cluster[1];
        int[] tuple = data[index];
        for (int i = 0; i < tuple.length; i++) {
            int[][] hierarchy = hierarchies[i];
            int value = hierarchy[tuple[i]][generalization[i]];
            cost += getDomainShare(i, generalization[i], value);
        }

        cost /= (double) numAttributes;
        cost *= (cluster.length - 1);
        return cost;
    }

    /**
     * Returns a generalization level
     * @param dimension
     * @param records
     * @return
     */
    public int getGeneralizationLevel(int dimension, int[] records) {
        return getGeneralizationLevel(dimension, records, 0);
    }

    /**
     * Cluster
     * @param records
     * @return
     */
    public int getGeneralizationLevel(int dimension, int[] records, int lvl) {

        int[][] hierarchy = hierarchies[dimension];
        int val = hierarchy[data[records[0]][dimension]][lvl];
        for (int i = 1; i < records.length && lvl != hierarchy[0].length - 1; i++) {
            while (hierarchy[data[records[i]][dimension]][lvl] != val) {
                val = hierarchy[data[records[i - 1]][dimension]][++lvl];
            }
        }
        return lvl;
    }
    
    /**
     * Cluster and record
     */
    public int getGeneralizationLevel(int dimension, int[] records1, int record, int lvl) {

        int[][] hierarchy = hierarchies[dimension];
        int val = hierarchy[data[records1[0]][dimension]][lvl];
        for (int i = 1; i < records1.length + 1 && lvl != hierarchy[0].length - 1; i++) {
            int current = i < records1.length ? records1[i] : record;
            int previous = i < records1.length ? records1[i - 1] : records1[records1.length - 1];
            while (hierarchy[data[current][dimension]][lvl] != val) {
                val = hierarchy[data[previous][dimension]][++lvl];
            }
        }
        return lvl;
    }
    
    /**
     * Two clusters
     */
    public int getGeneralizationLevel(int dimension, int[] records1, int[] records2, int lvl) {

        int[][] hierarchy = hierarchies[dimension];
        int val = hierarchy[data[records1[0]][dimension]][lvl];
        for (int i = 1; i < records1.length + records2.length && lvl != hierarchy[0].length - 1; i++) {
            
            int current = i < records1.length  ? records1[i] : 
                          i == records1.length ? records2[0] : 
                                                 records2[i - records1.length];
            int previous = i < records1.length  ? records1[i - 1] : 
                           i == records1.length ? records1[records1.length - 1] :
                                                  records2[i - records1.length - 1];
            
            while (hierarchy[data[current][dimension]][lvl] != val) {
                val = hierarchy[data[previous][dimension]][++lvl];
            }
        }
        return lvl;
    }

    /**
     * Cluster and record
     */
    public int getGeneralizationLevelWithoutRecord(int dimension, int[] records, int record) {

        // Prepare
        int[][] hierarchy = hierarchies[dimension];
        int lvl = 0;
        int idx = records[0] == record ? 1 : 0;
        int val = hierarchy[data[records[idx]][dimension]][lvl];
        
        for (int i = idx + 1; i < records.length && lvl != hierarchy[0].length - 1; i++) {
            
            if (records[i] == record && i == records.length-1) {
                break;
            }
            
            int current = records[i] != record ? records[i] : records[i+1];
            int previous = records[i-1] != record ? records[i-1] : records[i-2];
            
            while (hierarchy[data[current][dimension]][lvl] != val) {
                val = hierarchy[data[previous][dimension]][++lvl];
            }
        }
        return lvl;
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
