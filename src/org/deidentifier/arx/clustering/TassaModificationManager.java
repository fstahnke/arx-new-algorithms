package org.deidentifier.arx.clustering;

import java.util.HashSet;
import java.util.Set;

public class TassaModificationManager {

    /** Clusters modified in the current iteration*/
    private Set<TassaCluster> current = new HashSet<TassaCluster>();
    /** Clusters modified in the last iteration*/
    private Set<TassaCluster> last = new HashSet<TassaCluster>();
    
    /**
     * Creates a new instance
     */
    public TassaModificationManager(){
        // Empty by design
    }
    
    /**
     * Sets a cluster modified
     * @param cluster
     */
    public void setModified(TassaCluster cluster) {
        this.current.add(cluster);
        this.last.add(cluster);
    }

    /**
     * Sets a clustering modified
     * @param clustering
     */
    public void setModified(Set<TassaCluster> clustering) {
        this.current.addAll(clustering);
        this.last.addAll(clustering);
    }
    
    /**
     * Returns whether a cluster must be seen as modified
     * @param cluster
     * @return
     */
    public boolean isModified(TassaCluster cluster) {
        return this.last.contains(cluster);
    }
    
    /**
     * Prepare the next iteration
     */
    public void prepareNextIteration() {
        Set<TassaCluster> temp = this.last;
        this.last = this.current;
        this.current = temp;
        this.current.clear();
    }
}
