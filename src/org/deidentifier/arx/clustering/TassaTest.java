package org.deidentifier.arx.clustering;

import java.io.IOException;

import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.AttributeType.Hierarchy;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.criteria.KAnonymity;

public class TassaTest {
    
    private static final int K = 8;
    
    public static void main(String[] args) throws IOException {
        
    	// Init
        final Data data = Data.create("data/adult_subset.csv", ';');
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
        config.addCriterion(new KAnonymity(K));
        config.setMaxOutliers(0d);
        
        final TassaAlgorithm algorithm = new TassaAlgorithm(null, data, config);
        algorithm.setLogging(false);
        
        // Execute
        final int REPETITIONS = 1;
        long time = System.currentTimeMillis();
        for (int i=0; i<REPETITIONS; i++) {
            algorithm.execute();
        }
        System.out.println("Execution time: " + (System.currentTimeMillis() - time) / (double)REPETITIONS);
        
        // Sanity checks, for testing only
        int count = 0;
        for (TassaCluster c : algorithm.getClustering()) {
            count += c.getSize();
            if (c.getSize() < K) {
                throw new IllegalStateException("Privacy guarantees not fulfilled");
            }
        }
        if (count < data.getHandle().getNumRows()) {
            throw new IllegalStateException("Output dataset misses some records");
        }
        
        // Print
        System.out.println(algorithm.getStatistics());
    }
}
