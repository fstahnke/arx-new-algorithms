package org.deidentifier.arx.recursive;

import java.io.IOException;

import org.deidentifier.arx.ARXAnonymizer;
import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.AttributeType.Hierarchy;
import org.deidentifier.arx.benchmark.BenchmarkSetup;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkDataset;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkPrivacyModel;
import org.deidentifier.arx.criteria.KAnonymity;
import org.deidentifier.arx.metric.Metric;
import org.deidentifier.arx.metric.Metric.AggregateFunction;

public class RecursiveTest {
    
    public static void main(String[] args) throws IOException {
        
        RecursiveAlgorithm recursiveInstance = new RecursiveAlgorithm();
        
      //final Data data2 = Data.create("data/adult_subset.csv", ';');
//        Data data = Data.create("data/adult.csv", ';');
//        data.getDefinition().setAttributeType("age", Hierarchy.create("hierarchies/adult_hierarchy_age.csv", ';'));
//        data.getDefinition().setAttributeType("education", Hierarchy.create("hierarchies/adult_hierarchy_education.csv", ';'));
//        data.getDefinition().setAttributeType("marital-status", Hierarchy.create("hierarchies/adult_hierarchy_marital-status.csv", ';'));
//        data.getDefinition().setAttributeType("native-country", Hierarchy.create("hierarchies/adult_hierarchy_native-country.csv", ';'));
//        data.getDefinition().setAttributeType("race", Hierarchy.create("hierarchies/adult_hierarchy_race.csv", ';'));
//        data.getDefinition().setAttributeType("salary-class", Hierarchy.create("hierarchies/adult_hierarchy_salary-class.csv", ';'));
//        data.getDefinition().setAttributeType("sex", Hierarchy.create("hierarchies/adult_hierarchy_sex.csv", ';'));
//        data.getDefinition().setAttributeType("workclass", Hierarchy.create("hierarchies/adult_hierarchy_workclass.csv", ';'));
//        data.getDefinition().setAttributeType("occupation", Hierarchy.create("hierarchies/adult_hierarchy_occupation.csv", ';'));
//        
//        
        Data data = BenchmarkSetup.getData(BenchmarkDataset.ADULT, BenchmarkPrivacyModel.K_ANONYMITY);
        
        
        final ARXAnonymizer anonymizer = new ARXAnonymizer();
        
        final ARXConfiguration config = ARXConfiguration.create();
        

        config.addCriterion(new KAnonymity(5));
        config.setMaxOutliers(1d);
        config.setMetric(Metric.createLossMetric(AggregateFunction.GEOMETRIC_MEAN));
        
        recursiveInstance.execute(data, config, anonymizer);
        
    }
    
}
