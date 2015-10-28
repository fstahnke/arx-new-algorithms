package org.deidentifier.arx.utility.util;

import java.util.HashMap;
import java.util.Map;

public class DomainShare {
    
    /** Domain shares */
    private final Map<String, Map<String, Double>> shares;

    /** Domain sizes */
    public final double[]                          domainSize;

    /** Maxlevels */
    public final Map<String, Integer>              maxlevels;
    
    public DomainShare(Map<String, String[][]> hierarchies, String[] header) {
        
        this.shares = new HashMap<String, Map<String, Double>>();
        for (String attr : hierarchies.keySet()) {
            this.shares.put(attr, getLoss(hierarchies.get(attr)));
        }
        this.domainSize = getDomainSize(header, hierarchies);
        this.maxlevels = new HashMap<String, Integer>();
        for (String attr : header) {
            this.maxlevels.put(attr, hierarchies.get(attr)[0].length-1);
        }
    }
    
    /**
     * Returns the domain share
     * @param attribute
     * @param value
     * @param level
     * @return
     */
    public double getShare(String attribute, String value, int level) {
        
        for (;level<=maxlevels.get(attribute); level++) {
            Double loss = this.shares.get(attribute).get(value + level);
            if (loss != null) {
                return loss;
            }
        }
        return 1d;
    }
    
    /**
     * Returns the domain sizes
     * @param header
     * @param hierarchies
     * @return
     */
    private double[] getDomainSize(String[] header, Map<String, String[][]> hierarchies) {
        double[] result = new double[header.length];
        for (int i = 0; i < header.length; i++) {
            result[i] = hierarchies.get(header[i]).length;
        }
        return result;
    }
    
    /**
     * Build loss
     * @param hierarchy
     * @return
     */
    private Map<String, Double> getLoss(String[][] hierarchy) {
        
        Map<String, Double> loss = new HashMap<String, Double>();
        
        // Prepare map:
        // Level -> Value on level + 1 -> Count of values on level that are generalized to this value
        Map<Integer, Map<String, Integer>> map = new HashMap<Integer, Map<String, Integer>>();
        for (int level = 0; level < hierarchy[0].length - 1; level++) {
            for (int row = 0; row < hierarchy.length; row++) {
                
                // Obtain map per level
                Map<String, Integer> levelMap = map.get(level);
                if (levelMap == null) {
                    levelMap = new HashMap<String, Integer>();
                    map.put(level, levelMap);
                }
                
                // Count
                String value = hierarchy[row][level + 1];
                value += (level + 1);
                Integer count = levelMap.get(value);
                count = count == null ? 1 : count + 1;
                levelMap.put(value, count);
            }
        }
        
        // Level 0
        for (int row = 0; row < hierarchy.length; row++) {
            String value = hierarchy[row][0];
            value += 0;
            if (!loss.containsKey(value)) {
                loss.put(value, 1d / (double) hierarchy.length);
            }
        }
        
        // Level > 1
        for (int col = 1; col < hierarchy[0].length; col++) {
            for (int row = 0; row < hierarchy.length; row++) {
                String value = hierarchy[row][col];
                value += col;
                if (!loss.containsKey(value)) {
                    double count = map.get(col - 1).get(value);
                    loss.put(value, (double) count / (double) hierarchy.length);
                }
            }
        }
        
        return loss;
    }
    
}
