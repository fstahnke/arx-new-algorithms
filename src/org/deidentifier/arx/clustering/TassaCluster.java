package org.deidentifier.arx.clustering;

import java.util.Arrays;

public class TassaCluster {
    
    /** The number of attributes. */
    private final int                          numbAttributes;
    /** Identifiers of records*/
    private int[]                              recordIdentifiers;
    /** Generalization levels of the cluster*/
    private int[]                              generalizationLevels;
    /** Costs*/
    private double                             informationLoss;
    /** Manager*/
    private final GeneralizationManager        generalizationManager;

    /**
     * Creates a new cluster
     * @param manager
     * @param recordIdentifiers
     */
    public TassaCluster(GeneralizationManager manager, int[] recordIdentifiers) {
        this.generalizationManager = manager;
        this.numbAttributes = manager.getNumAttributes();
        this.generalizationLevels = new int[numbAttributes];
        this.recordIdentifiers = recordIdentifiers;
        this.update();
    }
    
    public void addCluster(TassaCluster cluster) {
        int offset = recordIdentifiers.length;
        this.recordIdentifiers = Arrays.copyOf(recordIdentifiers, recordIdentifiers.length + cluster.recordIdentifiers.length);
        System.arraycopy(cluster.recordIdentifiers, 0, this.recordIdentifiers, offset, cluster.recordIdentifiers.length);
        this.update();
    }
    
    public void addRecord(int recordId) {
        // TODO: Use array list?
        this.recordIdentifiers = Arrays.copyOf(recordIdentifiers, recordIdentifiers.length + 1);
        this.recordIdentifiers[recordIdentifiers.length - 1] = recordId;
        this.update();
    }
    
    public double getInformationLoss() {
        return this.informationLoss;
    }
    
    /**
     * Returns the total (weighted) generalization cost when adding another cluster.
     * @param Added cluster.
     * @return Weighted generalization cost.
     */
    public double getInformationLossWhenAdding(TassaCluster cluster) {
        return generalizationManager.getInformationLossWhenAdding(this.recordIdentifiers, this.generalizationLevels, 
                                                           cluster.recordIdentifiers, cluster.generalizationLevels);
    }
    
    /**
     * Returns the total (weighted) generalization cost when adding a record.
     * @param Added record.
     * @return Weighted generalization cost.
     */
    public double getInformationLossWhenAdding(int record) {
        return generalizationManager.getInformationLossWhenAdding(this.recordIdentifiers, this.generalizationLevels, record);
    }
    
    /**
     * Returns the total (weighted) generalization cost when removing a record.    
     * @param Removed record.
     * @return Weighted generalization cost.
     */
    public double getInformationLossWhenRemoving(int record) {
    	if (this.recordIdentifiers.length == 0) {
            throw new IllegalStateException("This may never happen");
        } else if (this.recordIdentifiers.length == 1) {
            return 0;
        } else {
            return generalizationManager.getInformationLossWithoutRecord(this.recordIdentifiers, this.generalizationLevels, record);
        }
    }
    
    public int[] getRecords() {
        return this.recordIdentifiers;
    }
    
    public int getSize() {
        return this.recordIdentifiers.length;
    }

    public int[] getTransformation() {
        return generalizationManager.getTransformation(recordIdentifiers[0], generalizationLevels);
    }

    public void removeRecord(int recordId) {
        int[] newRecordIdentifiers = new int[recordIdentifiers.length - 1];
        // If cluster is not empty, add all remaining recordIDs to the array
        if (newRecordIdentifiers.length > 0) {
            int idx = 0;
            for (int id : this.recordIdentifiers) {
                if (id != recordId) {
                    newRecordIdentifiers[idx++] = id;
                }
            }
        }
        this.recordIdentifiers = newRecordIdentifiers;
        this.update();
    }

    /**
     * Splits this cluster into a new cluster
     * @return
     */
    public TassaCluster splitCluster() {
        final int splitSize = (int)(this.recordIdentifiers.length / 2d);
        int[] newRecordIdentifiers = Arrays.copyOfRange(this.recordIdentifiers, splitSize, this.recordIdentifiers.length);
        this.recordIdentifiers = Arrays.copyOf(this.recordIdentifiers, splitSize);
        this.update();
        return new TassaCluster(generalizationManager, newRecordIdentifiers);        
    }

    /**
     * 
     * @param changedObject
     * 
     * We want to update:
     * - generalizationLevels
     * - generalizationCost
     * - transformation
     * - the hash code
     * - the removedGC cache
     */
    private void update() {
    	// If cluster is empty
    	if (this.getSize() == 0) {
    	    this.informationLoss = 0d;
    	// Else, update
    	} else {
    		for (int i = 0; i < numbAttributes; i++) {
    			this.generalizationLevels[i] = generalizationManager.getGeneralizationLevel(i, this.recordIdentifiers);
    		}
    		this.informationLoss = generalizationManager.getInformationLoss(this.recordIdentifiers, this.generalizationLevels);
    	}
    }
}
