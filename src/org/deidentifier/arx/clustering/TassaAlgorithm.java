package org.deidentifier.arx.clustering;

import java.io.IOException;
import java.util.Set;

import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.ARXInterface;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.benchmark.BenchmarkAlgorithm;
import org.deidentifier.arx.benchmark.IBenchmarkObserver;

public class TassaAlgorithm extends BenchmarkAlgorithm {

    /** Interface */
    private ARXInterface    arxInterface;
    /** TODO */
    private double          alpha                  = 0.5;
    /** TODO */
    private double          omega                  = 1.5;
    /** TODO */
    private double          initialInformationLoss = -Double.MAX_VALUE;
    /** TODO */
    private double          informationLoss        = 0d;
    /** Threshold for recursive executions */
    private double          threshold;
    /** TODO */
    private boolean         logging                = false;
    /** TODO */
    private TassaStatistics statistics             = null;
    /** TODO */
    private Set<TassaCluster> clustering             = null;

    /**
     * Create a new instance
     * @param observer
     * @param data
     * @param config
     * @throws IOException
     */
    public TassaAlgorithm(IBenchmarkObserver observer, 
                          Data data, 
                          ARXConfiguration config) throws IOException {
        this(observer, data, config, 0d);
    }

    
	/**
	 * Create a new recursive instance
	 * @param observer
	 * @param data
	 * @param config
	 * @param threshold Set to 0 to perform a single pass
	 * @throws IOException
	 */
    public TassaAlgorithm(IBenchmarkObserver observer, 
                          Data data, 
                          ARXConfiguration config,
                          double threshold) throws IOException {
	    super(observer);
		this.arxInterface = new ARXInterface(data, config);
		this.threshold = threshold;
	}

    @Override
	public String[][] execute() throws IOException {
        
        this.statistics = null;
        this.clustering = null;
        
        if (threshold == 0) {
            TassaAlgorithmImpl algorithm = new TassaAlgorithmImpl(arxInterface);
            algorithm.setLogging(this.logging);
            algorithm.execute(alpha, omega, null);
            this.statistics = algorithm.getStatistics();
            this.initialInformationLoss = algorithm.getInititalInformationLoss();
            this.informationLoss = algorithm.getFinalInformationLoss();
            this.clustering = algorithm.getClustering();
            return getOutputTable(algorithm.getOutputBuffer());
        } else {
            
            TassaAlgorithmImpl algorithm = new TassaAlgorithmImpl(arxInterface);
            algorithm.setLogging(this.logging);
            double delta = Double.MAX_VALUE;
            while (delta > threshold) {
                algorithm.execute(alpha, omega, this.clustering);
                if (this.statistics == null) {
                    this.statistics = algorithm.getStatistics();
                } else {
                    this.statistics.merge(algorithm.getStatistics());
                }
                this.clustering = algorithm.getClustering();
                final double base = algorithm.getInititalInformationLoss();
                if (this.initialInformationLoss < 0d) {
                    this.initialInformationLoss = base;
                }
                this.informationLoss = algorithm.getFinalInformationLoss();
                delta = Math.abs(informationLoss - base);
            }
            return getOutputTable(algorithm.getOutputBuffer());
        }
	}
    
    /**
     * Returns alpha
     * @return
     */
    public double getAlpha() {
        return alpha;
    }
    
    /**
     * Returns the clustering
     * @return
     */
    public Set<TassaCluster> getClustering() {
        return this.clustering;
    }

	/**
	 * Returns omega
	 * @return
	 */
	public double getOmega() {
        return omega;
    }
	
	/**
     * Returns the resulting info loss
     * @return
     */
    public TassaStatistics getStatistics() {
        return this.statistics;
    }
	
	/**
	 * Sets alpha
	 * @param alpha
	 */
	public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

	/**
     * Enables/disables logging
     * @param logging
     */
    public void setLogging(boolean logging) {
        this.logging = logging;
    }
    
    /**
	 * Sets omage
	 * @param omega
	 */
	public void setOmega(double omega) {
        this.omega = omega;
    }
    
    /**
	 * Helper
	 * @param buffer
	 * @return
	 */
    private String[][] getOutputTable(int[][] buffer) {

		String[][] result = new String[buffer.length + 1][buffer[0].length];
		result[0] = arxInterface.getDataManager().getHeader();
		String[][] mapping = arxInterface.getDataManager().getDataGeneralized().getDictionary().getMapping();

		for (int dataEntry = 1; dataEntry < result.length; dataEntry++) {
			for (int attribute = 0; attribute < result[0].length; attribute++) {
				result[dataEntry][attribute] = mapping[attribute][buffer[dataEntry - 1][attribute]];
			}
		}

		return result;
	}
}
