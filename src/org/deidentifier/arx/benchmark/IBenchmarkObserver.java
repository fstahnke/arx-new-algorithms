package org.deidentifier.arx.benchmark;

public interface IBenchmarkObserver {
    
    public void setWarmup(boolean isWarmup);
    
    public void notify(long timestamp, String[][] output, int[] transformation);
    
    public void notifyFinished(long timestamp, String[][] output, int[] transformation);
    
    public void notifyTransformations(int[][] transformations, int[] weight);
    
}
