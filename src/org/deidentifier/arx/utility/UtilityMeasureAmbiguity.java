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
 * Implementation of the Ambiguity measure, as described in:<br>
 * <br>
 * "Goldberger, Tassa: Efficient Anonymizations with Enhanced Utility
 * 
 * @author Fabian Prasser
 */
public class UtilityMeasureAmbiguity extends UtilityMeasure<Double> {
    
    /** Domain shares */
    private final DomainShare shares;
    /** Header */
    private final String[]    header;
    
    /**
     * Creates a new instance
     * @param hierarchies
     */
    public UtilityMeasureAmbiguity(String[] header, Map<String, String[][]> hierarchies) {
        this.header = header;
        this.shares = new DomainShare(hierarchies, header);
    }
    
    @Override
    public Utility<Double> evaluate(String[][] input, int[] transformation) {
        
        double result = 0d;
        for (String[] row : input) {
            double resultRow = 1d;
            for (int i = 0; i < row.length; i++) {
                resultRow *= shares.getShare(header[i], row[i], transformation[i]) * shares.domainSize[i];
            }
            result += resultRow;
        }
        return new UtilityDouble(result);
    }
    
}
