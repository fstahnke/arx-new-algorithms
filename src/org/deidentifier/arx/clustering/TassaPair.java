package org.deidentifier.arx.clustering;

public class TassaPair<T, U> {

    /** Immutable, so public is ok*/
    public final T first;
    /** Immutable, so public is ok*/
    public final U second;
    
    /**
     * Creates a new instance
     * @param first
     * @param second
     */
    public TassaPair(T first, U second) {
        this.first = first;
        this.second = second;
    }
}
