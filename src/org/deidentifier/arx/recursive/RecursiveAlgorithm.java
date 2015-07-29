package org.deidentifier.arx.recursive;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.deidentifier.arx.ARXAnonymizer;
import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.ARXResult;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.Data.DefaultData;
import org.deidentifier.arx.DataHandle;
import org.deidentifier.arx.criteria.DPresence;
import org.deidentifier.arx.criteria.KAnonymity;
import org.deidentifier.arx.criteria.LDiversity;
import org.deidentifier.arx.criteria.TCloseness;
import org.deidentifier.arx.utility.AggregateFunction;
import org.deidentifier.arx.utility.DataConverter;
import org.deidentifier.arx.utility.UtilityMeasureLoss;

public class RecursiveAlgorithm {
	
	

    public String[][] execute(final Data data, final ARXConfiguration config, final ARXAnonymizer anonymizer) throws IOException
    {
        // Execute the first anonymization
        ARXResult result = anonymizer.anonymize(data, config);
        
        // Get handle for input data and result
        DataHandle inHandle = data.getHandle();
        DataHandle outHandle = result.getOutput(false);

        // Convert input and output to array of string arrays
        DataConverter converter = new DataConverter();
        String[][] output = converter.toArray(outHandle);
        Map<String, String[][]> hierarchies = converter.toMap(data.getDefinition());
        String[] header = converter.getHeader(inHandle);
        
        // Prepare input for next step
        DefaultData outliers = Data.create();
        outliers.getDefinition().read(data.getDefinition());
        outliers.add(header); // add header
        
        // Collect input and row indices
        List<Integer> indexes = new ArrayList<Integer>();
        Iterator<String[]> rowIter = inHandle.iterator();
        rowIter.next(); // Skip header
        for (int j = 0; j < outHandle.getNumRows(); j++) {
        	String[] row = rowIter.next();
            if (outHandle.isOutlier(j)) {
                outliers.add(row);
                indexes.add(j);
            }
        }
        int numOutliers = indexes.size();
        outHandle.release();
        inHandle.release();

        // Calculate and print the current loss of the output and the number of suppressed entries
        double outputLoss = new UtilityMeasureLoss<Double>(header, hierarchies, AggregateFunction.GEOMETRIC_MEAN).evaluate(output).getUtility();
        System.out.println("Inital anonymization: Suppressed entries: " + numOutliers + ", Information Loss: " + outputLoss);
        
        // TODO: This works for k-anonymity and l-diversity, only. Implement for t-closeness and d-presence
        if (config.containsCriterion(TCloseness.class)) {
        	throw new IllegalArgumentException("T-Closeness is not supported");
        } else if (config.containsCriterion(DPresence.class)) {
        	throw new IllegalArgumentException("D-Presence is not supported");
        }
        
        // Prepare initial class size
        int minimalClassSize = getMinimalClassSize(config);         

        // Repeat while possible
        while (numOutliers >= minimalClassSize) {

			// Anonymize the outliers and get the handle for the result
			result = anonymizer.anonymize(outliers, config);
			inHandle = outliers.getHandle();
			outHandle = result.getOutput(false);

			// Iterate over result and write all non-outliers to the output
			rowIter = outHandle.iterator();
			rowIter.next(); // skip header
			Iterator<Integer> indexIterator = indexes.iterator();
			for (int j = 0; j < outHandle.getNumRows(); j++) {
				int sourceIndex = indexIterator.next();
				String[] row = rowIter.next();
				if (!outHandle.isOutlier(j)) {
					output[sourceIndex] = row;
				}
			}

            // Prepare input for next step
            outliers = Data.create();
            outliers.getDefinition().read(data.getDefinition());
            outliers.add(header); // add header
            
            // Collect input and row indices
            indexIterator = indexes.iterator();
            rowIter = inHandle.iterator();
            rowIter.next(); // Skip header
            for (int j = 0; j < outHandle.getNumRows(); j++) {
            	String[] row = rowIter.next();
            	indexIterator.next();
                if (outHandle.isOutlier(j)) {
                    outliers.add(row);
                } else {
                	indexIterator.remove();
                }
            }
            numOutliers = indexes.size();
            outHandle.release();
            inHandle.release();
            
            // Calculate and print the current loss of the output and the number of suppressed entries
            outputLoss = new UtilityMeasureLoss<Double>(header, hierarchies, AggregateFunction.GEOMETRIC_MEAN).evaluate(output).getUtility();
            System.out.println("Next iteration: Suppressed entries: " + numOutliers + ", Information Loss: " + outputLoss);
            
        }
        

        
        return output;
    }

	private int getMinimalClassSize(ARXConfiguration config) {
		int result = Integer.MIN_VALUE;
		if (config.containsCriterion(KAnonymity.class)) {
			result = Math.max(result, config.getCriterion(KAnonymity.class).getK());
		}
		for (LDiversity c : config.getCriteria(LDiversity.class)) {
			result = Math.max(result, (int)Math.ceil(c.getL()));
		}
		if (result == Integer.MIN_VALUE) {
			throw new IllegalStateException("Invalid minimal class size");
		}
		return result;
	}
}
