package org.deidentifier.arx.clustering;

import java.util.Arrays;

import cern.colt.list.IntArrayList;

public class TassaCluster {

    /** The number of attributes. */
    private final int                   numAttributes;
    /** Identifiers of records */
    private IntArrayList                records;
    /** Generalization levels of the cluster */
    private int[]                       generalizationLevels;
    /** Costs */
    private double                      informationLoss;
    /** Costs */
    private double                      lowerBoundForAdditionalInformationLoss;
    /** Manager */
    private final GeneralizationManager generalizationManager;
    /** Id */
    public int                          id;
    /** Cache */
    private double[]                    cache;

    /**
     * Creates a new cluster
     * 
     * @param manager
     * @param recordIdentifiers
     */
    public TassaCluster(GeneralizationManager manager, IntArrayList recordIdentifiers) {
        this.generalizationManager = manager;
        this.numAttributes = manager.getNumAttributes();
        this.generalizationLevels = new int[numAttributes];
        this.records = recordIdentifiers;
        this.cache = new double[numAttributes];
        Arrays.fill(this.cache, -1d);
        this.update();
    }

    public void addCluster(TassaCluster cluster) {

        this.records.addAllOf(cluster.records);
        this.update();
    }

    public void addRecord(int recordId) {
        this.records.add(recordId);
        this.update();
    }

    public double getInformationLoss() {
        return this.informationLoss;
    }

    /**
     * Returns the total (weighted) generalization cost when adding a record.
     * 
     * @param Added
     *            record.
     * @return Weighted generalization cost.
     */
    public double getInformationLossWhenAdding(int record) {

        return generalizationManager.getInformationLossWhenAddingRecord(this.records,
                                                                        this.generalizationLevels,
                                                                        record,
                                                                        this.cache);
    }

    /**
     * Returns the total (weighted) generalization cost when adding another
     * cluster.
     * 
     * @param Added
     *            cluster.
     * @return Weighted generalization cost.
     */
    public double getInformationLossWhenAdding(TassaCluster cluster) {
        return generalizationManager.getInformationLossWhenAddingCluster(this.records,
                                                                         this.generalizationLevels,
                                                                         cluster.records,
                                                                         cluster.generalizationLevels);
    }

    /**
     * Returns the total (weighted) generalization cost when removing a record.
     * 
     * @param Removed
     *            record.
     * @return Weighted generalization cost.
     */
    public double getInformationLossWhenRemoving(int record) {
        if (this.records.size() == 0) {
            throw new IllegalStateException("Cannot remove element from empty cluster");
        } else if (this.records.size() == 1) {
            return 0;
        } else {
            return generalizationManager.getInformationLossWhenRemovingRecord(this.records,
                                                                              record);
        }
    }

    public IntArrayList getRecords() {
        return this.records;
    }

    public int getSize() {
        return this.records.size();
    }

    public int[] getTransformation() {
        return generalizationManager.getTransformation(records.getQuick(0),
                                                       generalizationLevels);
    }

    public void removeRecord(int recordId) {
        this.records.remove(this.records.indexOf(recordId));
        this.update();
    }

    /**
     * Splits this cluster into a new cluster
     * 
     * @return
     */
    public TassaCluster splitCluster() {
        int splitSize = (int) (this.records.size() / 2d);
        IntArrayList newRecordIdentifiers = new IntArrayList();
        for (int i = splitSize; i < this.records.size(); i++) {
            newRecordIdentifiers.add(this.records.elements()[i]);
        }
        this.records.setSize(splitSize);
        this.update();
        return new TassaCluster(generalizationManager, newRecordIdentifiers);
    }
    
    /**
     * Returns a lower bound on the additional information loss
     */
    public double getLowerBoundForAdditionalInformationLoss() {
        return this.lowerBoundForAdditionalInformationLoss;
    }
    

    /**
     * Updates the cluster
     */
    private void update() {
        // If cluster is empty
        if (this.getSize() == 0) {
            this.informationLoss = 0d;
            // Else, update
        } else {
            this.informationLoss = generalizationManager.getInformationLoss(this.records,
                                                                            this.generalizationLevels,
                                                                            this.cache);
            this.lowerBoundForAdditionalInformationLoss = this.informationLoss / (double)this.records.size();
        }
    }
}
