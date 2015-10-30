package org.deidentifier.arx.benchmark;

import java.io.IOException;

import org.deidentifier.arx.exceptions.RollbackRequiredException;

public abstract class BenchmarkAlgorithm {

    private final IBenchmarkListener observer;
    private long                     start;

    public BenchmarkAlgorithm(IBenchmarkListener observer) {
        this.observer = observer;
    }

    public abstract void execute() throws IOException, RollbackRequiredException;

    protected void start() {
        this.start = System.currentTimeMillis();
    }

    protected void updated(String[][] data, int[] transformation) {
        observer.notify(System.currentTimeMillis() - start, data, transformation);
    }

    protected void finished(String[][] data) {
        observer.notifyFinished(System.currentTimeMillis() - start, data);
    }
}
