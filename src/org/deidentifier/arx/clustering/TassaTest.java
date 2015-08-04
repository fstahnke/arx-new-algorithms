package org.deidentifier.arx.clustering;

import java.io.IOException;

import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.AttributeType.Hierarchy;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.benchmark.IBenchmarkObserver;
import org.deidentifier.arx.criteria.KAnonymity;
import org.deidentifier.arx.utility.AggregateFunction;
import org.deidentifier.arx.utility.DataConverter;
import org.deidentifier.arx.utility.UtilityMeasureLoss;

public class TassaTest {
    
    public static void main(String[] args) throws IOException {
        
        IBenchmarkObserver observer = new IBenchmarkObserver() {

            @Override
            public void notify(long timestamp, String[][] output,
                    int[] transformation) {
                // TODO Auto-generated method stub
                
            }
            
        };
        
    	
        final Data data = Data.create("data/adult_subset.csv", ';');
//        final Data data = Data.create("data/adult.csv", ';');
        data.getDefinition().setAttributeType("age", Hierarchy.create("hierarchies/adult_hierarchy_age.csv", ';'));
        data.getDefinition().setAttributeType("education", Hierarchy.create("hierarchies/adult_hierarchy_education.csv", ';'));
        data.getDefinition().setAttributeType("marital-status", Hierarchy.create("hierarchies/adult_hierarchy_marital-status.csv", ';'));
        data.getDefinition().setAttributeType("native-country", Hierarchy.create("hierarchies/adult_hierarchy_native-country.csv", ';'));
        data.getDefinition().setAttributeType("occupation", Hierarchy.create("hierarchies/adult_hierarchy_occupation.csv", ';'));
        data.getDefinition().setAttributeType("race", Hierarchy.create("hierarchies/adult_hierarchy_race.csv", ';'));
        data.getDefinition().setAttributeType("salary-class", Hierarchy.create("hierarchies/adult_hierarchy_salary-class.csv", ';'));
        data.getDefinition().setAttributeType("sex", Hierarchy.create("hierarchies/adult_hierarchy_sex.csv", ';'));
        data.getDefinition().setAttributeType("workclass", Hierarchy.create("hierarchies/adult_hierarchy_workclass.csv", ';'));
        
        // Configuration
        final ARXConfiguration config = ARXConfiguration.create();
        config.addCriterion(new KAnonymity(8));
        config.setMaxOutliers(0d);
        
        final TassaAlgorithm tassaAlgo = new TassaAlgorithm(observer, data, config);
        
        String[][] outputGeneralized = tassaAlgo.execute();
        
        double utility = new UtilityMeasureLoss<Double>(new DataConverter().getHeader(data.getHandle()), new DataConverter().toMap(data.getDefinition()), AggregateFunction.GEOMETRIC_MEAN).evaluate(outputGeneralized).getUtility();

        System.out.println("Information Loss by Tassa: " + tassaAlgo.getInformationLoss() + ", Information Loss by ARX: " + utility);
        
        //final TassaClusterSet clusterList = tassa.execute(0.5, 1.5);

//        double lastDeltaIL = -Double.MAX_VALUE;
//
//        
//        long initTime = System.nanoTime();
//        tassa.execute(0.5, 1.5, tassa.getTassaClustering());
//        long stopTime = System.nanoTime();
//        double initialInformationLoss = tassa.getInititalInformationLoss();
//        double finalInformationLoss = tassa.getFinalInformationLoss();
//        lastDeltaIL = finalInformationLoss - initialInformationLoss;
//        System.out.println("Total runtime: " + Math.round((stopTime-initTime) / 1000000000.0) + " s, Initial Information Loss: " + initialInformationLoss + ", Final Information Loss: " + finalInformationLoss);
        
        
        /*
        for (int i = 0; lastDeltaIL <= -0.000000001d || lastDeltaIL > 0; i++) {
            final long initTime = System.nanoTime();
            output = tassa2.execute(0.5, 1.5, output);
            final long stopTime = System.nanoTime();
            final double initialInformationLoss = tassa2.getInititalInformationLoss();
            final double finalInformationLoss = tassa2.getFinalInformationLoss();
            lastDeltaIL = finalInformationLoss - initialInformationLoss;
            System.out.println("#: " + i + ", Total runtime: " + Math.round((stopTime-initTime) / 1000000000.0) + " s, Initial Information Loss: " + initialInformationLoss + ", Final Information Loss: " + finalInformationLoss);
        }
        */

//        final int exp = 8;
//        for (int i = 0; i < exp; i++) {
//
//            // Configuration
//            final ARXConfiguration configTmp = ARXConfiguration.create();
//            configTmp.addCriterion(new KAnonymity((int)(Math.pow(2, exp-i))));
//            System.out.println("K-Anonymity: " + (Math.pow(2, exp-i)));
//            configTmp.setMaxOutliers(0d);
//            
//            final TassaAlgorithm tassaAlgoTmp = new TassaAlgorithm(data, config);
//            final TassaAlgorithmImpl tassaTmp = tassaAlgoTmp.getImpl();
//            
//            initTime = System.nanoTime();
//            tassaTmp.execute(0.5, 1.5, null);
//            stopTime = System.nanoTime();
//            initialInformationLoss = tassaTmp.getInititalInformationLoss();
//            finalInformationLoss = tassaTmp.getFinalInformationLoss();
//            System.out.println("#: " + i + ", Total runtime: " + ((stopTime-initTime) / 1000000000.0) + " s, Initial Information Loss: " + initialInformationLoss + ", Final Information Loss: " + finalInformationLoss);
//        }
        
        
    }
}
