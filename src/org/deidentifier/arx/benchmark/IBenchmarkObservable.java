package org.deidentifier.arx.benchmark;

public interface IBenchmarkObservable {
    
    /**
     * Sets the observer
     * @param observer
     */
    void setObserver(IBenchmarkObserver observer);
    
    /**
     * Notifies the observer of the changes
     */
    void notifyObserver();
}
