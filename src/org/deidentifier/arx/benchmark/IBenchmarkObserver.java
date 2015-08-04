package org.deidentifier.arx.benchmark;

public interface IBenchmarkObserver {

    public void notify(long timestamp, String[][] output, int[] transformation);
}
