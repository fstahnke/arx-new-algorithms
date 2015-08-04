package org.deidentifier.arx.clustering;

import java.io.IOException;

import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.ARXInterface;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.benchmark.BenchmarkAlgorithm;
import org.deidentifier.arx.benchmark.IBenchmarkObserver;

public class TassaAlgorithm extends BenchmarkAlgorithm {
    
    // configuration of the arx algorithm instance
    private ARXInterface iface;
    private TassaAlgorithmImpl tassaImpl;
	private double alpha = 0.5;
	
	public double getAlpha() {
        return alpha;
    }

    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    private double omega = 1.5;
	
	public double getOmega() {
        return omega;
    }

    public void setOmega(double omega) {
        this.omega = omega;
    }

    // TODO: This should actually be in a result object
	private double initialInformationLoss = 0d;
    private double informationLoss = 0d;
    private TassaClusterSet lastResult = null;
	

	public TassaAlgorithm(IBenchmarkObserver observer, Data data, ARXConfiguration config)
			throws IOException {
	    super(observer);
		iface = new ARXInterface(data, config);
		tassaImpl = new TassaAlgorithmImpl(iface);
	}

	@Override
	public String[][] execute() throws IOException {

		tassaImpl.execute(alpha, omega, lastResult);
		if (lastResult == null) {
		    initialInformationLoss = tassaImpl.getInititalInformationLoss();
		}
		lastResult = tassaImpl.getTassaClustering();
		informationLoss = tassaImpl.getFinalInformationLoss();

		return getOutputTable(tassaImpl.getOutputBuffer());
	}
	
	public double getInitialInformationLoss() {
	    return initialInformationLoss;
	}

	public double getInformationLoss() {
        return informationLoss;
    }
	
	public void resetResults() {
	    initialInformationLoss = 0d;
	    informationLoss = 0d;
	    lastResult = null;
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

}
