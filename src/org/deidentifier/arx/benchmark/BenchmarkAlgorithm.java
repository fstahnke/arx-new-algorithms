package org.deidentifier.arx.benchmark;

import java.io.IOException;

import org.deidentifier.arx.exceptions.RollbackRequiredException;

public abstract class BenchmarkAlgorithm {

    private final IBenchmarkListener observer;
    private long                     start;
    private long                     overhead = 0;

    public BenchmarkAlgorithm(IBenchmarkListener observer) {
        this.observer = observer;
    }

    public abstract void execute() throws IOException, RollbackRequiredException;

    protected void start() {
        this.start = System.currentTimeMillis();
    }

    protected void updated(String[][] data, int[] transformation) {
        long startOverhead = System.currentTimeMillis();
        observer.notify(System.currentTimeMillis() - start - overhead, data, transformation);
        overhead += System.currentTimeMillis() - startOverhead;
    }

    protected void finished(String[][] data) {
        observer.notifyFinished(System.currentTimeMillis() - start - overhead, data);
    }
}
