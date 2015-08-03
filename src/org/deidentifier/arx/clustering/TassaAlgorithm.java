package org.deidentifier.arx.clustering;

import java.io.IOException;

import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.ARXInterface;
import org.deidentifier.arx.Data;

public class TassaAlgorithm {

	private ARXInterface iface;
	private TassaAlgorithmImpl tassaImpl;

	public TassaAlgorithm(Data data, ARXConfiguration config)
			throws IOException {
		iface = new ARXInterface(data, config);
		tassaImpl = new TassaAlgorithmImpl(iface);
	}

	public String[][] execute() throws IOException {

		tassaImpl.execute(0.5, 1.5, null);

		return getOutputTable(tassaImpl.getOutputBuffer());
	}
	
	public TassaClusterSet getTassaClustering() {
		return tassaImpl.getTassaClustering();
	}

	private String[][] getOutputTable(int[][] outputBuffer) {

		String[][] result = new String[outputBuffer.length + 1][outputBuffer[0].length];

		result[0] = iface.getDataManager().getHeader();

		String[][] mapping = iface.getDataManager().getDataGeneralized()
				.getDictionary().getMapping();

		for (int dataEntry = 1; dataEntry < result.length; dataEntry++) {
			for (int attribute = 0; attribute < result[0].length; attribute++) {
				result[dataEntry][attribute] = mapping[attribute][outputBuffer[dataEntry - 1][attribute]];
			}
		}

		return result;
	}

	public TassaAlgorithmImpl getImpl() {
		return tassaImpl;
	}

}
