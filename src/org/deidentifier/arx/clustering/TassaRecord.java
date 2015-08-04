/**
 * 
 */
package org.deidentifier.arx.clustering;

/**
 * @author Fabian Stahnke
 *
 */
public final class TassaRecord implements IGeneralizable {
    
    private final int[]  recordContent;
    
    private TassaCluster assignedCluster;
    
    public TassaRecord(int[] content)
    {
        recordContent = content;
    }
    
    public int[] getValues() {
        return recordContent;
    }
    
    public TassaCluster getAssignedCluster() {
        return assignedCluster;
    }
    public void setAssignedCluster(TassaCluster cluster) {
        assignedCluster = cluster;
    }
}
