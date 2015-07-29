package org.deidentifier.arx.recursive;

public interface BenchmarkAlgorithmListener {

	public void updated(long timestamp, String[][] output, int[] transformation);
}
