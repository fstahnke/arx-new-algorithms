/*
 * ARX: Powerful Data Anonymization
 * Copyright 2012 - 2015 Florian Kohlmayer, Fabian Prasser
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.deidentifier.arx.utility;

import java.util.Map;

import org.deidentifier.arx.utility.util.DomainShare;

/**
 * Implementation of the Loss measure, as proposed in:<br>
 * <br>
 * Iyengar, V.: Transforming data to satisfy privacy constraints. In: Proc Int Conf Knowl Disc Data Mining, p. 279288 (2002)
 * 
 * @author Fabian Prasser
 */
public class UtilityMeasureLoss<T> extends UtilityMeasureAggregatable<T> {
    
    /** Header */
    private final String[]    header;
    /** Domain shares */
    private final DomainShare shares;
    
    /**
     * Creates a new instance
     * @param hierarchies
     */
    @SuppressWarnings("unchecked")
    public UtilityMeasureLoss(String[] header, Map<String, String[][]> hierarchies) {
        this(header, hierarchies, (AggregateFunction<T>) AggregateFunction.ARITHMETIC_MEAN);
    }
    
    /**
     * Creates a new instance
     * @param hierarchies
     */
    public UtilityMeasureLoss(String[] header, Map<String, String[][]> hierarchies, AggregateFunction<T> function) {
        super(function);
        this.header = header;
        this.shares = new DomainShare(hierarchies, header);
    }
    
    /**
     * Evaluates the utility measure
     * @param output
     * @param transformation
     * @return
     */
    public double[] evaluateAggregatable(String[][] input, int[] transformation) {
        
        double[] result = new double[input[0].length];
        
        for (String[] row : input) {
            for (int i = 0; i < result.length; i++) {
                result[i] += shares.getShare(header[i], row[i], transformation[i]);
            }
        }
        
        for (int i = 0; i < result.length; i++) {
            double min = input.length / shares.domainSize[i];
            double max = input.length;
            result[i] = (result[i] - min) / (max - min);
        }
        return result;
    }
    
}
