package org.deidentifier.arx.benchmark;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class BenchmarkHelper {

    /**
     * The scale for calculations with BigDecimal.
     */
    public static final int DECIMAL_SCALE = 10;

    /** The separator*/
    private static final char   SEPERATOR = ';';
    /** The newline*/
    private static final String NEWLINE   = "\n";

    /**
     * Calculates the variance using BigDecimalals for better precision. The
     * scale of the result is {@value #DECIMAL_SCALE}.
     * 
     * @param output
     * @param header
     * @param hierarchies
     * @param ignoreSuppressed
     * @return
     */
    public static double calculateVariance(String[][] output,
                                           String[] header,
                                           Map<String, String[][]> hierarchies,
                                           boolean ignoreSuppressed) {

        final int numberOfRecords = output.length;
        final int numberOfAttributes = output[0].length;
        final int[] maxGeneralizationLevels = new int[numberOfAttributes];
        for (int columnIndex = 0; columnIndex < numberOfAttributes; columnIndex++) {
            maxGeneralizationLevels[columnIndex] = hierarchies.get(header[columnIndex]).length;
        }

        ArrayList<Map<String, Integer>> stringToLevelMaps = getStringToLevelMaps(header,
                                                                                 hierarchies);

        // Compute average generalization degree per attribute
        BigDecimal[] averageDegrees = new BigDecimal[numberOfAttributes];
        Arrays.fill(averageDegrees, BigDecimal.valueOf(0d));
        int numberOfTuplesConsidered = 0;
        for (int rowIndex = 0; rowIndex < numberOfRecords; rowIndex++) {
            String[] row = output[rowIndex];
            if (!ignoreSuppressed || !isSuppressed(row)) {
                for (int columnIndex = 0; columnIndex < numberOfAttributes; columnIndex++) {
                    BigDecimal degree = BigDecimal.valueOf(divideInts(stringToLevelMaps.get(columnIndex)
                                                                                       .get(row[columnIndex]),
                                                                      maxGeneralizationLevels[columnIndex]));
                    averageDegrees[columnIndex] = averageDegrees[columnIndex].add(degree);
                }
                numberOfTuplesConsidered++;
            }
        }

        if (numberOfTuplesConsidered > 0) {
            for (int i = 0; i < averageDegrees.length; i++) {
                averageDegrees[i] = averageDegrees[i].divide(BigDecimal.valueOf(numberOfTuplesConsidered),
                                                             DECIMAL_SCALE,
                                                             BigDecimal.ROUND_HALF_UP);
            }

            // Compute variances
            BigDecimal[] variances = new BigDecimal[numberOfAttributes];
            Arrays.fill(variances, BigDecimal.valueOf(0d));
            for (int rowIndex = 0; rowIndex < numberOfRecords; rowIndex++) {
                String[] row = output[rowIndex];
                if (!ignoreSuppressed || !isSuppressed(row)) {
                    for (int columnIndex = 0; columnIndex < numberOfAttributes; columnIndex++) {
                        BigDecimal degree = BigDecimal.valueOf(divideInts(stringToLevelMaps.get(columnIndex)
                                                                                           .get(row[columnIndex]),
                                                                          maxGeneralizationLevels[columnIndex]));
                        BigDecimal variance = degree.subtract(averageDegrees[columnIndex]).pow(2);
                        variances[columnIndex] = variances[columnIndex].add(variance);
                    }
                }
            }
            // Normalize
            for (int i = 0; i < variances.length; i++) {
                variances[i] = variances[i].divide(BigDecimal.valueOf(numberOfTuplesConsidered),
                                                   DECIMAL_SCALE,
                                                   BigDecimal.ROUND_HALF_UP);
            }
            return calculateArithmeticMean(variances).doubleValue();
        } else {
            return 0;
        }

    }

    /**
     * Calculates the number of distinct transformations in a generalized
     * data set.
     * 
     * @param output
     * @param header
     * @param hierarchies
     * @param ignoreSuppressed
     * @return
     */
    public static int calculateNumberOfTransformations(String[][] output,
                                                       String[] header,
                                                       Map<String, String[][]> hierarchies) {
        final int numberOfAttributes = output[0].length;

        ArrayList<Map<String, Integer>> stringToLevelMaps = getStringToLevelMaps(header,
                                                                                 hierarchies);

        // add transformation for each row to HashMap
        HashSet<ArrayList<Integer>> transformationSet = new HashSet<>();
        for (String[] row : output) {
            ArrayList<Integer> transformation = new ArrayList<>(numberOfAttributes);
            for (int columnIndex = 0; columnIndex < numberOfAttributes; columnIndex++) {
                transformation.add(stringToLevelMaps.get(columnIndex).get(row[columnIndex]));
            }
            transformationSet.add(transformation);
        }

        return transformationSet.size();
    }

    /**
     * Returns a list of maps for each attribute, which map each string to its
     * level in the according generalization hierarchy.
     * 
     * @param header
     * @param hierarchies
     * @return
     */
    private static ArrayList<Map<String, Integer>>
            getStringToLevelMaps(String[] header, Map<String, String[][]> hierarchies) {

        // Create maps with the generalization level for each string
        ArrayList<Map<String, Integer>> stringToLevelMaps = new ArrayList<Map<String, Integer>>();
        for (int columnIndex = 0; columnIndex < header.length; columnIndex++) {
            String attribute = header[columnIndex];
            Map<String, Integer> map = new HashMap<String, Integer>();
            stringToLevelMaps.add(map);
            for (String[] row : hierarchies.get(attribute)) {
                for (int level = row.length - 1; level >= 0; level--) {
                    if (map.containsKey(row[level])) {
                        int lvl = Math.max(map.get(row[level]), level);
                        if (map.get(row[level]) != level) {
                            System.out.println("That happened! (with " + attribute + ")");                            
                        }
                        map.put(row[level], lvl);
                    } else {
                        map.put(row[level], level);
                    }
                }
            }
        }

        return stringToLevelMaps;

    }

    /**
     * Get the arithmetic mean for a set of values. Using BigDecimal for exact
     * results.
     * 
     * @param values
     * @return The arithmetic mean.
     */
    public static BigDecimal calculateArithmeticMean(BigDecimal[] values) {
        if (values.length == 1) { return values[0].setScale(DECIMAL_SCALE, BigDecimal.ROUND_HALF_UP); }
        BigDecimal arithmeticMean = BigDecimal.valueOf(0d);
        for (BigDecimal value : values) {
            arithmeticMean = arithmeticMean.add(value);
        }
        arithmeticMean = arithmeticMean.divide(BigDecimal.valueOf(values.length),
                                               DECIMAL_SCALE,
                                               BigDecimal.ROUND_HALF_UP);
        return arithmeticMean;
    }

    /**
     * Get the arithmetic mean for a set of values. Using BigDecimal for exact
     * results.
     * 
     * @param values
     * @return The arithmetic mean.
     */
    public static double calculateArithmeticMean(double[] values) {
        BigDecimal[] valuesBigD = new BigDecimal[values.length];
        for (int i = 0; i < values.length; i++) {
            valuesBigD[i] = BigDecimal.valueOf(values[i]);
        }
        return calculateArithmeticMean(valuesBigD).doubleValue();
    }

    /**
     * @param output
     *            An array of String tuples
     * @return The number of suppressed tuples in the given array.
     */
    public static int getNumSuppressed(String[][] output) {
        int suppressedTuples = 0;
        for (int i = 0; i < output.length; i++) {
            suppressedTuples += isSuppressed(output[i]) ? 1 : 0;
        }
        return suppressedTuples;
    }

    /**
     * Provides exact division of two integer values by using BigDecimal
     * division.
     * 
     * @param numerator
     * @param denominator
     * @return
     */
    public static double divideInts(int numerator, int denominator) {
        BigDecimal result = BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator),
                                                                 DECIMAL_SCALE,
                                                                 BigDecimal.ROUND_HALF_UP);
        return result.doubleValue();
    }

    /**
     * Is this row suppressed?
     * 
     * @param row
     * @return
     */
    private static boolean isSuppressed(String[] row) {
        for (String s : row) {
            if (!s.equals("*")) { return false; }
        }
        return true;
    }

}
