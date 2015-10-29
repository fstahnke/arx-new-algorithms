package org.deidentifier.arx.benchmark;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BenchmarkHelper {

    private static final int DECIMAL_SCALE = 10;

    public static double getVariance(String[][] output,
                                     String[] header,
                                     Map<String, String[][]> hierarchies,
                                     boolean ignoreSuppressed) {

        final int numberOfRecords = output.length;
        final int numberOfAttributes = output[0].length;
        final int[] maxGeneralizationLevels = new int[numberOfAttributes];

        // Create maps with the generalization level for each string
        ArrayList<Map<String, Integer>> stringToLevelMaps = new ArrayList<Map<String, Integer>>();
        for (int columnIndex = 0; columnIndex < numberOfAttributes; columnIndex++) {
            String attribute = header[columnIndex];
            Map<String, Integer> map = new HashMap<String, Integer>();
            stringToLevelMaps.add(map);
            for (String[] row : hierarchies.get(attribute)) {
                maxGeneralizationLevels[columnIndex] = row.length;
                for (int level = row.length - 1; level >= 0; level--) {
                    if (map.containsKey(row[level])) {
                        int lvl = Math.max(map.get(row[level]), level);
                        map.put(row[level], lvl);
                    } else {
                        map.put(row[level], level);
                    }
                }
            }
        }

        // Compute average generalization degree per attribute
        double[] averageDegrees = new double[numberOfAttributes];
        Arrays.fill(averageDegrees, 0.0);
        int numberOfTuplesConsidered = 0;
        for (int rowIndex = 0; rowIndex < numberOfRecords; rowIndex++) {
            String[] row = output[rowIndex];
            if (!ignoreSuppressed || !isSuppressed(row)) {
                for (int columnIndex = 0; columnIndex < numberOfAttributes; columnIndex++) {
                    averageDegrees[columnIndex] += (double) stringToLevelMaps.get(columnIndex)
                                                                             .get(row[columnIndex]) /
                                                   maxGeneralizationLevels[columnIndex];
                }
                numberOfTuplesConsidered++;
            }
        }

        if (numberOfTuplesConsidered > 0) {
            for (int i = 0; i < averageDegrees.length; i++) {
                averageDegrees[i] /= numberOfTuplesConsidered;
            }

            // Compute variances
            double[] variances = new double[numberOfAttributes];
            Arrays.fill(variances, 0.0);
            for (int rowIndex = 0; rowIndex < numberOfRecords; rowIndex++) {
                String[] row = output[rowIndex];
                if (!ignoreSuppressed || !isSuppressed(row)) {
                    for (int columnIndex = 0; columnIndex < numberOfAttributes; columnIndex++) {
                        double degree = (double) stringToLevelMaps.get(columnIndex)
                                                                  .get(row[columnIndex]) /
                                        maxGeneralizationLevels[columnIndex];
                        variances[columnIndex] += Math.pow(degree - averageDegrees[columnIndex], 2);
                    }
                }
            }
            // Normalize
            for (int i = 0; i < variances.length; i++) {
                variances[i] /= numberOfTuplesConsidered;
            }
            return calculateArithmeticMean(variances);
        } else {
            return 0;
        }

    }

    public static double getVarianceBigDecimal(String[][] output,
                                               String[] header,
                                               Map<String, String[][]> hierarchies,
                                               boolean ignoreSuppressed) {

        final int numberOfRecords = output.length;
        final int numberOfAttributes = output[0].length;
        final int[] maxGeneralizationLevels = new int[numberOfAttributes];

        // Create maps with the generalization level for each string
        ArrayList<Map<String, Integer>> stringToLevelMaps = new ArrayList<Map<String, Integer>>();
        for (int columnIndex = 0; columnIndex < numberOfAttributes; columnIndex++) {
            String attribute = header[columnIndex];
            Map<String, Integer> map = new HashMap<String, Integer>();
            stringToLevelMaps.add(map);
            for (String[] row : hierarchies.get(attribute)) {
                maxGeneralizationLevels[columnIndex] = row.length;
                for (int level = row.length - 1; level >= 0; level--) {
                    if (map.containsKey(row[level])) {
                        int lvl = Math.max(map.get(row[level]), level);
                        map.put(row[level], lvl);
                    } else {
                        map.put(row[level], level);
                    }
                }
            }
        }

        // Compute average generalization degree per attribute
        BigDecimal[] averageDegrees = new BigDecimal[numberOfAttributes];
        Arrays.fill(averageDegrees, new BigDecimal(0d));
        int numberOfTuplesConsidered = 0;
        for (int rowIndex = 0; rowIndex < numberOfRecords; rowIndex++) {
            String[] row = output[rowIndex];
            if (!ignoreSuppressed || !isSuppressed(row)) {
                for (int columnIndex = 0; columnIndex < numberOfAttributes; columnIndex++) {
                    BigDecimal degree = new BigDecimal(divideInts(stringToLevelMaps.get(columnIndex)
                                                                                    .get(row[columnIndex]),
                                                                   maxGeneralizationLevels[columnIndex]));
                    averageDegrees[columnIndex] = averageDegrees[columnIndex].add(degree);
                }
                numberOfTuplesConsidered++;
            }
        }

        if (numberOfTuplesConsidered > 0) {
            for (int i = 0; i < averageDegrees.length; i++) {
                averageDegrees[i] = averageDegrees[i].divide(new BigDecimal(numberOfTuplesConsidered),
                                                             DECIMAL_SCALE,
                                                             BigDecimal.ROUND_HALF_UP);
            }

            // Compute variances
            BigDecimal[] variances = new BigDecimal[numberOfAttributes];
            Arrays.fill(variances, new BigDecimal(0d));
            for (int rowIndex = 0; rowIndex < numberOfRecords; rowIndex++) {
                String[] row = output[rowIndex];
                if (!ignoreSuppressed || !isSuppressed(row)) {
                    for (int columnIndex = 0; columnIndex < numberOfAttributes; columnIndex++) {
                        BigDecimal degree = new BigDecimal(divideInts(stringToLevelMaps.get(columnIndex)
                                                                                        .get(row[columnIndex]),
                                                                       maxGeneralizationLevels[columnIndex]));
                        BigDecimal variance = degree.subtract(averageDegrees[columnIndex]).pow(2);
                        variances[columnIndex] = variances[columnIndex].add(variance);
                    }
                }
            }
            // Normalize
            for (int i = 0; i < variances.length; i++) {
                variances[i] = variances[i].divide(new BigDecimal(numberOfTuplesConsidered),
                                                   DECIMAL_SCALE,
                                                   BigDecimal.ROUND_HALF_UP);
            }
            return calculateArithmeticMeanBigDecimal(variances).doubleValue();
        } else {
            return 0;
        }

    }

    /**
     * Get the arithmetic mean for a set of values.
     * 
     * @param values
     * @return The arithmetic mean.
     */
    public static BigDecimal calculateArithmeticMeanBigDecimal(BigDecimal[] values) {
        if (values.length == 1) { return values[0]; }
        BigDecimal arithmeticMean = new BigDecimal(0d);
        for (BigDecimal value : values) {
            arithmeticMean = arithmeticMean.add(value);
        }
        arithmeticMean = arithmeticMean.divide(new BigDecimal(values.length),
                                               DECIMAL_SCALE,
                                               BigDecimal.ROUND_HALF_UP);
        return arithmeticMean;
    }

    /**
     * Get the arithmetic mean for a set of values.
     * 
     * @param values
     * @return The arithmetic mean.
     */
    public static double calculateArithmeticMean(double[] values) {
        if (values.length == 1) { return values[0]; }
        double arithmeticMean = 0d;
        for (double value : values) {
            arithmeticMean += value;
        }
        arithmeticMean /= values.length;
        return arithmeticMean;
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
        BigDecimal result = new BigDecimal(numerator).divide(new BigDecimal(denominator),
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
