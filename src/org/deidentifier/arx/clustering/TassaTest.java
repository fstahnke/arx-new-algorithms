package org.deidentifier.arx.clustering;

import java.io.IOException;

import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.AttributeType.Hierarchy;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.criteria.KAnonymity;

public class TassaTest {
    
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
        config.addCriterion(new KAnonymity(8));
        config.setMaxOutliers(0d);
        
        final TassaAlgorithm algorithm = new TassaAlgorithm(null, data, config);
        algorithm.setLogging(true);
        
        // Execute
        algorithm.execute();
        
        // Print
        System.out.println(algorithm.getStatistics());
    }
}
