package org.deidentifier.arx.recursive;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.benchmark.BenchmarkSetup;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkDataset;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkPrivacyModel;
import org.deidentifier.arx.benchmark.IBenchmarkObserver;
import org.deidentifier.arx.criteria.KAnonymity;
import org.deidentifier.arx.metric.Metric;
import org.deidentifier.arx.metric.Metric.AggregateFunction;

public class RecursiveTest {
    
    public static void main(String[] args) throws IOException {
    	
    	IBenchmarkObserver listener = new IBenchmarkObserver() {

			@Override
			public void notify(long timestamp, String[][] output, int[] transformation) {
				System.out.println("Iteration");
			}

            @Override
            public void notifyFinished(long timestamp, String[][] output, int[][] transformations, int[] weights) {
                System.out.println("Iteration");
                
            }

            @Override
            public void setWarmup(boolean isWarmup) {
                
            }
    		
    	};
        
        final Data data = BenchmarkSetup.getData(BenchmarkDataset.IHIS_SUBSET, BenchmarkPrivacyModel.K5_ANONYMITY);
        
        final ARXConfiguration config = ARXConfiguration.create();

        config.addCriterion(new KAnonymity(5));
        config.setMaxOutliers(1d);
        config.setMetric(Metric.createLossMetric(AggregateFunction.GEOMETRIC_MEAN));
        
        BenchmarkAlgorithmRGR recursiveInstance = new BenchmarkAlgorithmRGR(listener, data, config);
        

        
        long time = System.nanoTime();
        System.out.println("Maximum heap size: " + (Runtime.getRuntime().maxMemory() >> 20) + " MB");
        recursiveInstance.execute();
        
        time = System.nanoTime() - time;
        
        String timeString = String.format("%d minutes, %d seconds",
        		TimeUnit.NANOSECONDS.toMinutes(time),
        		TimeUnit.NANOSECONDS.toSeconds(time) -
        		TimeUnit.MINUTES.toSeconds(TimeUnit.NANOSECONDS.toMinutes(time)));
        
        System.out.println("RGR total runtime: " + timeString);
        
        
        
    }
    
}
