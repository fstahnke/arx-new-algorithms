package org.deidentifier.arx.recursive;

public class BenchmarkAlgorithm {

	private final BenchmarkAlgorithmListener listener;
	private long start;

	public BenchmarkAlgorithm(BenchmarkAlgorithmListener listener) {
		this.listener = listener;
	}

	protected void start() {
		this.start = System.currentTimeMillis();
	}

	protected void updated(String[][] data) {
		listener.updated(System.currentTimeMillis() - start, data);
	}
}
