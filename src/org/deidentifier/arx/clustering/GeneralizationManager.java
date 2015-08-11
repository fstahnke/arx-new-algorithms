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
        
        // TODO: It is enough to include the loss for one record, because the loss for all records is equal 
        
        double cost = 0d;
        for (int recordId : cluster) {
            int[] record = data[recordId];
            for (int i = 0; i < record.length; i++) {
                int[][] hierarchy = hierarchies[i];
                int value = hierarchy[record[i]][generalization[i]];
                cost += getDomainShare(i, generalization[i], value);
            }
        }
        cost /= (double)numAttributes;
        return cost;
    }


    /**
     * Cluster and record
     * @param cluster
     * @param _generalization
     * @param additionalRecord
     * @return
     */
    public double getInformationLossWhenAdding(int[] cluster, int[] _generalization, int additionalRecord) {

        // TODO: It is enough to include the loss for one record, because the loss for all records is equal
        
        int[] generalization = new int[numAttributes];
        for (int i = 0; i <generalization.length; i++) {
            generalization[i] = getGeneralizationLevel(i, cluster, additionalRecord, generalization[i]);
        }
        
        double cost = 0d;
        for (int recordId : cluster) {
            int[] record = data[recordId];
            for (int i = 0; i < record.length; i++) {
                int[][] hierarchy = hierarchies[i];
                int value = hierarchy[record[i]][generalization[i]];
                cost += getDomainShare(i, generalization[i], value);
            }
        }
        int[] record = data[additionalRecord];
        for (int i = 0; i < record.length; i++) {
            int[][] hierarchy = hierarchies[i];
            int value = hierarchy[record[i]][generalization[i]];
            cost += getDomainShare(i, generalization[i], value);
        }
        cost /= (double) numAttributes;
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
    public double getInformationLossWhenAdding(int[] cluster1, int[] generalization1, int[] cluster2, int[] generalization2) {
        
        // TODO: It is enough to include the loss for one record, because the loss for all records is equal
        
        int[] generalization = new int[numAttributes];
        for (int i = 0; i <generalization.length; i++) {
            generalization[i] = getGeneralizationLevel(i, cluster1, cluster2, Math.max(generalization1[i], generalization2[i]));
        }
        
        double cost = 0d;
        for (int recordId : cluster1) {
            int[] record = data[recordId];
            for (int i = 0; i < record.length; i++) {
                int[][] hierarchy = hierarchies[i];
                int value = hierarchy[record[i]][generalization[i]];
                cost += getDomainShare(i, generalization[i], value);
            }
        }
        for (int recordId : cluster2) {
            int[] record = data[recordId];
            for (int i = 0; i < record.length; i++) {
                int[][] hierarchy = hierarchies[i];
                int value = hierarchy[record[i]][generalization[i]];
                cost += getDomainShare(i, generalization[i], value);
            }
        }
        cost /= (double) numAttributes;
        return cost;
    }

    /**
     * Cluster without record
     * @param cluster1
     * @param generalization1
     * @param record2
     * @return
     */
    public double getInformationLossWithoutRecord(int[] cluster1, int[] generalization1, int record2) {

        // TODO: It is enough to include the loss for one record, because the loss for all records is equal
        
        int[] generalization = new int[numAttributes];
        for (int i = 0; i <generalization.length; i++) {
            generalization[i] = getGeneralizationLevelWithoutRecord(i, cluster1, record2, 0);
        }
        
        double cost = 0d;
        for (int recordId : cluster1) {
            if (recordId != record2) {
                int[] record = data[recordId];
                for (int i = 0; i < record.length; i++) {
                    int[][] hierarchy = hierarchies[i];
                    int value = hierarchy[record[i]][generalization[i]];
                    cost += getDomainShare(i, generalization[i], value);
                }
            }
        }
        cost /= (double)numAttributes;
        return cost;
    }

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
        for (int i = 1; i < records1.length && lvl != hierarchy[0].length - 1; i++) {
            while (hierarchy[data[records1[i]][dimension]][lvl] != val) {
                val = hierarchy[data[records1[i - 1]][dimension]][++lvl];
            }
        }
        while (hierarchy[data[record][dimension]][lvl] != val) { // TODO: Is this correct?
                val = hierarchy[data[record][dimension]][++lvl];
            }
        return lvl;
    }
    
    /**
     * Two clusters
     */
    public int getGeneralizationLevel(int dimension, int[] records1, int[] records2, int lvl) {

        int[][] hierarchy = hierarchies[dimension];
        int val = hierarchy[data[records1[0]][dimension]][lvl];
        for (int i = 1; i < records1.length && lvl != hierarchy[0].length - 1; i++) {
            while (hierarchy[data[records1[i]][dimension]][lvl] != val) {
                val = hierarchy[data[records1[i - 1]][dimension]][++lvl];
            }
        }
        for (int i = 1; i < records2.length && lvl != hierarchy[0].length - 1; i++) {
            while (hierarchy[data[records2[i]][dimension]][lvl] != val) {
                val = hierarchy[data[records2[i - 1]][dimension]][++lvl];
            }
        }
        return lvl;
    }

    /**
     * Cluster and record
     */
    public int getGeneralizationLevelWithoutRecord(int dimension, int[] records1, int record, int lvl) {

        int[][] hierarchy = hierarchies[dimension];
        
        int val = 0;
        int start = 0;
        if (records1[0] != record) {
            val = hierarchy[data[records1[0]][dimension]][lvl];
        } else {
            if (records1.length == 1) {
                return 0;
            }
            val = hierarchy[data[records1[1]][dimension]][lvl];
            start = 1;
        }
        for (int i = start + 1; i < records1.length && lvl != hierarchy[0].length - 1; i++) {
            if (records1[i] != record) {
                while (hierarchy[data[records1[i]][dimension]][lvl] != val) {
                    val = hierarchy[data[records1[i - 1]][dimension]][++lvl];
                }
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
