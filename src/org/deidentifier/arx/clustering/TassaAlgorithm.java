package org.deidentifier.arx.clustering;

import java.io.IOException;

import org.deidentifier.arx.ARXAnonymizer;
import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.Data;

public class TassaAlgorithm {
	
	public String[][] execute(final Data data, final ARXConfiguration config, final ARXAnonymizer anonymizer) throws IOException {
		
		TassaAlgorithmImpl tassa = new TassaAlgorithmImpl(data, config);
		
		TassaClusterSet output = tassa.execute(0.5, 1.5, null);
		
		return getOutputTable(output);
	}
	
	private String[][] getOutputTable(TassaClusterSet output) {
		
		String[][] result = new String[output.size() + 1][];
		
		
		return result;
	}
    
}
