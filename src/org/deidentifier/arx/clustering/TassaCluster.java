package org.deidentifier.arx.clustering;

import java.util.Arrays;

import cern.colt.list.IntArrayList;

public class TassaCluster {

    /** The number of attributes. */
    private final int                   numAttributes;
    /** Identifiers of records */
    private IntArrayList                recordIdentifiers;
    /** Generalization levels of the cluster */
    private int[]                       generalizationLevels;
    /** Costs */
    private double                      informationLoss;
    /** Manager */
    private final GeneralizationManager generalizationManager;
    /** Id */
    public int                          id;
    /** Cache */
    private final double[]              cache;

    /**
     * Creates a new cluster
     * @param manager
     * @param recordIdentifiers
     */
    public TassaCluster(GeneralizationManager manager, IntArrayList recordIdentifiers) {
        this.generalizationManager = manager;
        this.numAttributes = manager.getNumAttributes();
        this.generalizationLevels = new int[numAttributes];
        this.recordIdentifiers = recordIdentifiers;
        this.cache = new double[numAttributes];
        Arrays.fill(this.cache, -1d);
        this.update();
    }
    
    public void addCluster(TassaCluster cluster) {
        
        this.recordIdentifiers.addAllOf(cluster.recordIdentifiers);
        this.update();
    }
    
    public void addRecord(int recordId) {
        this.recordIdentifiers.add(recordId);
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
        return generalizationManager.getInformationLossWhenAddingCluster(this.recordIdentifiers, this.generalizationLevels, 
                                                           cluster.recordIdentifiers, cluster.generalizationLevels);
    }
    
    /**
     * Returns the total (weighted) generalization cost when adding a record.
     * @param Added record.
     * @return Weighted generalization cost.
     */
    public double getInformationLossWhenAdding(int record) {
        return generalizationManager.getInformationLossWhenAddingRecord(this.recordIdentifiers, 
                                                                        this.generalizationLevels, 
                                                                        record,
                                                                        this.cache);
    }
    
    /**
     * Returns the total (weighted) generalization cost when removing a record.    
     * @param Removed record.
     * @return Weighted generalization cost.
     */
    public double getInformationLossWhenRemoving(int record) {
    	if (this.recordIdentifiers.size() == 0) {
            throw new IllegalStateException("Cannot remove element from empty cluster");
        } else if (this.recordIdentifiers.size() == 1) {
            return 0;
        } else {
             return generalizationManager.getInformationLossWhenRemovingRecord(this.recordIdentifiers, record);
        }
    }
    
    public IntArrayList getRecords() {
        return this.recordIdentifiers;
    }
    
    public int getSize() {
        return this.recordIdentifiers.size();
    }

    public int[] getTransformation() {
        return generalizationManager.getTransformation(recordIdentifiers.getQuick(0), generalizationLevels);
    }

    public void removeRecord(int recordId) {
        this.recordIdentifiers.remove(this.recordIdentifiers.indexOf(recordId));
        this.update();
    }

    /**
     * Splits this cluster into a new cluster
     * @return
     */
    public TassaCluster splitCluster() {
        int splitSize = (int)(this.recordIdentifiers.size() / 2d);
        IntArrayList newRecordIdentifiers = new IntArrayList();
        for (int i=splitSize; i<this.recordIdentifiers.size(); i++) {
            newRecordIdentifiers.add(this.recordIdentifiers.elements()[i]);
        }
        this.recordIdentifiers.setSize(splitSize);
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
    		this.informationLoss = generalizationManager.getInformationLoss(this.recordIdentifiers, this.generalizationLevels, this.cache);
    	}
    }
}
