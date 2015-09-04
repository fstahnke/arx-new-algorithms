package org.deidentifier.arx.benchmark;

public interface IBenchmarkObserver {
    
    public boolean isWarmup();
    public void setWarmup(boolean isWarmup);
    
    public void notify(long timestamp, String[][] output, int[] transformation);
    
    public void notifyFinished(long timestamp, String[][] output, int[] transformation);
    
}
