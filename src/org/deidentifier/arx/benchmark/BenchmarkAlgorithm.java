package org.deidentifier.arx.benchmark;

import java.io.IOException;

public abstract class BenchmarkAlgorithm {

	private final IBenchmarkObserver observer;
	private long start;

	public BenchmarkAlgorithm(IBenchmarkObserver observer) {
		this.observer = observer;
	}
	
	public abstract String[][] execute() throws IOException;

	protected void start() {
		this.start = System.currentTimeMillis();
	}

	protected void updated(String[][] data, int[] transformation) {
		observer.notify(System.currentTimeMillis() - start, data, transformation);
	}
}
