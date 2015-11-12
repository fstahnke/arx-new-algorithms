/*
 * Benchmark of risk-based anonymization in ARX 3.0.0 Copyright 2015 - Fabian
 * Prasser
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.deidentifier.arx.benchmark;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.AttributeType.Hierarchy;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.DataSource;
import org.deidentifier.arx.criteria.KAnonymity;
import org.deidentifier.arx.metric.Metric;
import org.deidentifier.arx.metric.Metric.AggregateFunction;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class encapsulates most of the parameters of a benchmark run
 * 
 * @author Fabian Prasser
 */
public class BenchmarkSetup {

    // Setup of the benchmark
    private BenchmarkDataset[]        datasets;
    private BenchmarkAlgorithm[]      algorithms;
    private BenchmarkPrivacyModel[]   privacyModels;
    private BenchmarkUtilityMeasure[] utilityMeasures;
    private double[]                  suppressionLimits;
    private double[]                  gsFactors;
    private double[]                  gsStepSizes;
    private int                       numberOfRuns = 1;

    private String                    outputFile;

    private String                    plotFile;

    /**
     * Create new BenchmarkSetup instance with xml for configuration
     * 
     * @param configFile
     *            path to xml benchmark configuration. If null, default values
     *            will be used.
     */
    public BenchmarkSetup(String configFile) {
        if (configFile != null && !configFile.equals("")) {
            readXML(configFile);
        }
    }

    /**
     * Create new BenchmarkSetup instance with default configuration
     */
    public BenchmarkSetup() {
        // empty by design
    }

    public static enum BenchmarkDataset {

        ADULT("Adult", 9, 30162),
        ADULT_SUBSET("AdultSubset", 9, 3016),
        CUP("Cup", 8, 63441),
        CUP_SUBSET("CupSubset", 8, 6344),
        FARS("Fars", 8, 100937),
        FARS_SUBSET("FarsSubset", 8, 10093),
        ATUS("Atus", 9, 539253),
        ATUS_SUBSET("AtusSubset", 9, 53925),
        IHIS("Ihis", 9, 1193504),
        IHIS_SUBSET("IhisSubset", 9, 119350),
        ADULT1("Adult", 1, 30162),
        ADULT2("Adult", 2, 30162),
        ADULT3("Adult", 3, 30162),
        ADULT4("Adult", 4, 30162),
        ADULT5("Adult", 5, 30162),
        ADULT6("Adult", 6, 30162),
        ADULT7("Adult", 7, 30162),
        ADULT8("Adult", 8, 30162),
        ADULT9("Adult", 9, 30162),
        ADULT1000("Adult", 9, 1000),
        ADULT2000("Adult", 9, 2000),
        ADULT3000("Adult", 9, 3000),
        ADULT4000("Adult", 9, 4000),
        ADULT5000("Adult", 9, 5000),
        ADULT6000("Adult", 9, 6000),
        ADULT7000("Adult", 9, 7000),
        ADULT8000("Adult", 9, 8000),
        ADULT9000("Adult", 9, 9000),
        ADULT10000("Adult", 9, 10000),
        ADULT11000("Adult", 9, 11000),
        ADULT12000("Adult", 9, 12000),
        ADULT13000("Adult", 9, 13000),
        ADULT14000("Adult", 9, 14000),
        ADULT15000("Adult", 9, 15000),
        ADULT16000("Adult", 9, 16000),
        ADULT17000("Adult", 9, 17000),
        ADULT18000("Adult", 9, 18000),
        ADULT19000("Adult", 9, 19000),
        ADULT20000("Adult", 9, 20000),
        ADULT21000("Adult", 9, 21000),
        ADULT22000("Adult", 9, 22000),
        ADULT23000("Adult", 9, 23000),
        ADULT24000("Adult", 9, 24000),
        ADULT25000("Adult", 9, 25000),
        ADULT26000("Adult", 9, 26000),
        ADULT27000("Adult", 9, 27000),
        ADULT28000("Adult", 9, 28000),
        ADULT29000("Adult", 9, 29000),
        ADULT30000("Adult", 9, 30000),
        CUP1("Cup", 1, 63441),
        CUP2("Cup", 2, 63441),
        CUP3("Cup", 3, 63441),
        CUP4("Cup", 4, 63441),
        CUP5("Cup", 5, 63441),
        CUP6("Cup", 6, 63441),
        CUP7("Cup", 7, 63441),
        CUP8("Cup", 8, 63441);

        private final int    qis;
        private final String name;
        private final int    numberOfRecords;

        /**
         * Constructor for original datasets.
         * 
         * @param name
         */
        private BenchmarkDataset(String name) {
            this(name, -1, -1);
        }

        /**
         * Constructor for selecting only a certain number of QIs of a dataset.
         * 
         * @param name
         * @param qis
         */
        private BenchmarkDataset(String name, int qis) {
            this(name, qis, -1);
        }

        /**
         * Constructor for subsets of datasets.
         * 
         * @param name
         * @param val
         * @param numberOfRecords
         */
        private BenchmarkDataset(String name, int val, int numberOfRecords) {
            this.name = name;
            this.qis = val;
            this.numberOfRecords = numberOfRecords;
        }

        @Override
        public String toString() {
            return this.name;
        }

        public int getNumQIs() {
            return this.qis;
        }

        public int getNumRecords() {
            return this.numberOfRecords;
        }
    }

    public static enum BenchmarkPrivacyModel {
        K5_ANONYMITY("5-anonymity", 5),
        K10_ANONYMITY("10-anonymity", 10),
        K20_ANONYMITY("20-anonymity", 20),
        K25_ANONYMITY("25-anonymity", 25),
        K50_ANONYMITY("50-anonymity", 50);

        private final String name;
        private final int    strength;

        private BenchmarkPrivacyModel(String name, int strength) {
            this.name = name;
            this.strength = strength;
        }

        @Override
        public String toString() {
            return this.name;
        }

        public int getStrength() {
            return this.strength;
        }
    }

    public static enum BenchmarkAlgorithm {
        RECURSIVE_GLOBAL_RECODING("RGR"),
        TASSA("Clustering"),
        FLASH("Flash");

        private final String name;

        private BenchmarkAlgorithm(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    public static enum BenchmarkUtilityMeasure {
        DISCERNIBILITY("Discernibility"),
        LOSS("Loss");

        private final String name;

        private BenchmarkUtilityMeasure(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    /**
     * Returns a configuration for the ARX framework
     * 
     * @param dataset
     * @param utility
     * @param criterion
     * @param suppressionLimit
     *            Limits the ratio of suppressed tuples.
     * @return
     * @throws IOException
     */
    public static ARXConfiguration getConfiguration(BenchmarkDataset dataset,
                                                    BenchmarkUtilityMeasure utility,
                                                    BenchmarkPrivacyModel criterion,
                                                    double suppressionLimit) throws IOException {
        // Take the default generalization/suppression factor of 0.5
        return getConfiguration(dataset, utility, criterion, suppressionLimit, 0.5d);
    }

    /**
     * Returns a configuration for the ARX framework
     * 
     * @param dataset
     * @param utility
     * @param criterion
     * @param suppressionLimit
     *            Limits the ratio of suppressed tuples.
     * @param gsFactor
     *            The weight of generalization towards suppression. Smaller
     *            values mean more generalized tuples.
     * @return
     * @throws IOException
     */
    public static ARXConfiguration getConfiguration(BenchmarkDataset dataset,
                                                    BenchmarkUtilityMeasure utility,
                                                    BenchmarkPrivacyModel criterion,
                                                    double suppressionLimit,
                                                    double gsFactor) throws IOException {
        ARXConfiguration config = ARXConfiguration.create();
        switch (utility) {
            case DISCERNIBILITY:
                config.setMetric(Metric.createDiscernabilityMetric(false));
                break;
            case LOSS:
                config.setMetric(Metric.createLossMetric(gsFactor, AggregateFunction.GEOMETRIC_MEAN));
                break;
            default:
                throw new IllegalArgumentException("");
        }

        config.setMaxOutliers(suppressionLimit);

        switch (criterion) {
            case K5_ANONYMITY:
            case K10_ANONYMITY:
            case K20_ANONYMITY:
            case K25_ANONYMITY:
            case K50_ANONYMITY:
                config.addCriterion(new KAnonymity(criterion.getStrength()));
                break;
            default:
                throw new RuntimeException("Invalid criterion");
        }
        return config;
    }

    /**
     * Configures and returns the dataset
     * 
     * @param dataset
     * @param criteria
     * @return
     * @throws IOException
     */

    public static Data
            getData(BenchmarkDataset dataset, BenchmarkPrivacyModel criterion) throws IOException {
        Data data = null;
        DataSource source = null;
        switch (dataset) {
            case ADULT:
                data = Data.create("data/adult.csv", ';');
                break;
            case ADULT1:
            case ADULT2:
            case ADULT3:
            case ADULT4:
            case ADULT5:
            case ADULT6:
            case ADULT7:
            case ADULT8:
            case ADULT9:
                source = DataSource.createCSVSource("data/adult.csv", ';', true);
                for (String attribute : getQuasiIdentifyingAttributes(dataset)) {
                    source.addColumn(attribute);
                }
                data = Data.create(source);
                break;
            case ADULT_SUBSET:
                data = Data.create("data/adult_subset.csv", ';');
                break;
            case ATUS:
                data = Data.create("data/atus.csv", ';');
                break;
            case ATUS_SUBSET:
                data = Data.create("data/atus_subset.csv", ';');
                break;
            case CUP:
                data = Data.create("data/cup.csv", ';');
                break;
            case CUP1:
            case CUP2:
            case CUP3:
            case CUP4:
            case CUP5:
            case CUP6:
            case CUP7:
            case CUP8:
                source = DataSource.createCSVSource("data/cup.csv", ';', true);
                for (String attribute : getQuasiIdentifyingAttributes(dataset)) {
                    source.addColumn(attribute);
                }
                data = Data.create(source);
                break;
            case CUP_SUBSET:
                data = Data.create("data/cup_subset.csv", ';');
                break;
            case FARS:
                data = Data.create("data/fars.csv", ';');
                break;
            case FARS_SUBSET:
                data = Data.create("data/fars_subset.csv", ';');
                break;
            case IHIS:
                data = Data.create("data/ihis.csv", ';');
                break;
            case IHIS_SUBSET:
                data = Data.create("data/ihis_subset.csv", ';');
                break;
            case ADULT1000:
            case ADULT2000:
            case ADULT3000:
            case ADULT4000:
            case ADULT5000:
            case ADULT6000:
            case ADULT7000:
            case ADULT8000:
            case ADULT9000:
            case ADULT10000:
            case ADULT11000:
            case ADULT12000:
            case ADULT13000:
            case ADULT14000:
            case ADULT15000:
            case ADULT16000:
            case ADULT17000:
            case ADULT18000:
            case ADULT19000:
            case ADULT20000:
            case ADULT21000:
            case ADULT22000:
            case ADULT23000:
            case ADULT24000:
            case ADULT25000:
            case ADULT26000:
            case ADULT27000:
            case ADULT28000:
            case ADULT29000:
            case ADULT30000:
                data = getDataSubset(BenchmarkDataset.ADULT, criterion, dataset.getNumRecords());
                // immediately return data for subsets because QIs are already
                // set
                return data;
            default:
                throw new RuntimeException("Invalid dataset");
        }

        for (String qi : getQuasiIdentifyingAttributes(dataset)) {
            data.getDefinition().setAttributeType(qi, getHierarchy(dataset, qi));
        }

        return data;
    }

    /**
     * Configures and returns a subset of the dataset. Subset has to exist as
     * csv in folder "datasetname_subset". Subset has to be named
     * "datasetname_subsetCount".
     * 
     * @param dataset
     *            The BenchmarkDataset enum of the original dataset.
     * @param criterion
     * @param subsetCount
     *            The number of records that the subset contains.
     * @return A Data object containing the subset.
     * @throws IOException
     */
    public static Data getDataSubset(BenchmarkDataset dataset,
                                     BenchmarkPrivacyModel criterion,
                                     int subsetCount) throws IOException {
        String datasetName = dataset.toString().toLowerCase();
        // Trim possible "subset" from name
        if (datasetName.contains("subset")) {
            datasetName = datasetName.substring(0, datasetName.lastIndexOf("subset"));
        }
        // Create path to according subset csv file
        Data data = Data.create(String.format("data/%1$s_subset/%1$s_%2$d.csv",
                                              datasetName,
                                              subsetCount), ';');

        for (String qi : getQuasiIdentifyingAttributes(dataset)) {
            data.getDefinition().setAttributeType(qi, getHierarchy(dataset, qi));
        }

        return data;
    }

    /**
     * Returns the generalization hierarchy for the dataset and attribute
     * 
     * @param dataset
     * @param attribute
     * @return
     * @throws IOException
     */
    public static Hierarchy
            getHierarchy(BenchmarkDataset dataset, String attribute) throws IOException {
        switch (dataset) {
            case ADULT:
            case ADULT1:
            case ADULT2:
            case ADULT3:
            case ADULT4:
            case ADULT5:
            case ADULT6:
            case ADULT7:
            case ADULT8:
            case ADULT9:
            case ADULT1000:
            case ADULT2000:
            case ADULT3000:
            case ADULT4000:
            case ADULT5000:
            case ADULT6000:
            case ADULT7000:
            case ADULT8000:
            case ADULT9000:
            case ADULT10000:
            case ADULT11000:
            case ADULT12000:
            case ADULT13000:
            case ADULT14000:
            case ADULT15000:
            case ADULT16000:
            case ADULT17000:
            case ADULT18000:
            case ADULT19000:
            case ADULT20000:
            case ADULT21000:
            case ADULT22000:
            case ADULT23000:
            case ADULT24000:
            case ADULT25000:
            case ADULT26000:
            case ADULT27000:
            case ADULT28000:
            case ADULT29000:
            case ADULT30000:
            case ADULT_SUBSET:
                return Hierarchy.create("hierarchies/adult_hierarchy_" + attribute + ".csv", ';');
            case ATUS:
            case ATUS_SUBSET:
                return Hierarchy.create("hierarchies/atus_hierarchy_" + attribute + ".csv", ';');
            case CUP:
            case CUP1:
            case CUP2:
            case CUP3:
            case CUP4:
            case CUP5:
            case CUP6:
            case CUP7:
            case CUP8:
            case CUP_SUBSET:
                return Hierarchy.create("hierarchies/cup_hierarchy_" + attribute + ".csv", ';');
            case FARS:
            case FARS_SUBSET:
                return Hierarchy.create("hierarchies/fars_hierarchy_" + attribute + ".csv", ';');
            case IHIS:
            case IHIS_SUBSET:
                return Hierarchy.create("hierarchies/ihis_hierarchy_" + attribute + ".csv", ';');
            default:
                throw new RuntimeException("Invalid dataset");
        }
    }

    /**
     * Returns the quasi-identifiers for the dataset
     * 
     * @param dataset
     * @return
     */
    public static String[] getQuasiIdentifyingAttributes(BenchmarkDataset dataset) {
        final String[] adultAttributes = new String[] {
                "sex",
                "age",
                "race",
                "marital-status",
                "education",
                "native-country",
                "workclass",
                "occupation",
                "salary-class" };
        final String[] cupAttributes = new String[] {
                "AGE",
                "GENDER",
                "INCOME",
                "MINRAMNT",
                "NGIFTALL",
                "STATE",
                "ZIP",
                "RAMNTALL" };

        switch (dataset) {
            case ADULT:
            case ADULT_SUBSET:
            case ADULT9:
            case ADULT1000:
            case ADULT2000:
            case ADULT3000:
            case ADULT4000:
            case ADULT5000:
            case ADULT6000:
            case ADULT7000:
            case ADULT8000:
            case ADULT9000:
            case ADULT10000:
            case ADULT11000:
            case ADULT12000:
            case ADULT13000:
            case ADULT14000:
            case ADULT15000:
            case ADULT16000:
            case ADULT17000:
            case ADULT18000:
            case ADULT19000:
            case ADULT20000:
            case ADULT21000:
            case ADULT22000:
            case ADULT23000:
            case ADULT24000:
            case ADULT25000:
            case ADULT26000:
            case ADULT27000:
            case ADULT28000:
            case ADULT29000:
            case ADULT30000:
                return adultAttributes;
            case ADULT1:
            case ADULT2:
            case ADULT3:
            case ADULT4:
            case ADULT5:
            case ADULT6:
            case ADULT7:
            case ADULT8:
                return Arrays.copyOf(adultAttributes, dataset.getNumQIs());
            case ATUS:
            case ATUS_SUBSET:
                return new String[] {
                        "Age",
                        "Birthplace",
                        "Citizenship status",
                        "Labor force status",
                        "Marital status",
                        "Race",
                        "Region",
                        "Sex",
                        "Highest level of school completed" };
            case CUP:
            case CUP_SUBSET:
            case CUP8:
                return cupAttributes;
            case CUP1:
            case CUP2:
            case CUP3:
            case CUP4:
            case CUP5:
            case CUP6:
            case CUP7:
                return Arrays.copyOf(cupAttributes, dataset.getNumQIs());
            case FARS:
            case FARS_SUBSET:
                return new String[] {
                        "iage",
                        "ideathday",
                        "ideathmon",
                        "ihispanic",
                        "iinjury",
                        "irace",
                        "isex",
                        "istatenum" };
            case IHIS:
            case IHIS_SUBSET:
                return new String[] {
                        "AGE",
                        "MARSTAT",
                        "PERNUM",
                        "QUARTER",
                        "RACEA",
                        "REGION",
                        "SEX",
                        "YEAR",
                        "EDUC" };
            default:
                throw new RuntimeException("Invalid dataset");
        }
    }

    /**
     * Returns all datasets
     * 
     * @return
     */
    public BenchmarkDataset[] getDatasets() {
        if (datasets != null) {
            return datasets;
        } else {
            return new BenchmarkDataset[] {
                    BenchmarkDataset.ADULT,
//                    BenchmarkDataset.CUP,
//                    BenchmarkDataset.FARS,
//                    BenchmarkDataset.ATUS,
//                    BenchmarkDataset.IHIS,
                    };
        }
    }

    public BenchmarkAlgorithm[] getAlgorithms() {
        if (algorithms != null) {
            return algorithms;
        } else {
            return new BenchmarkAlgorithm[] {
                    BenchmarkAlgorithm.RECURSIVE_GLOBAL_RECODING,
                    BenchmarkAlgorithm.FLASH,
                    BenchmarkAlgorithm.TASSA
                    };
        }
    }

    public BenchmarkPrivacyModel[] getPrivacyModels() {
        if (privacyModels != null) {
            return privacyModels;
        } else {
            return new BenchmarkPrivacyModel[] {
                    BenchmarkPrivacyModel.K5_ANONYMITY,
//                    BenchmarkPrivacyModel.K10_ANONYMITY,
//                    BenchmarkPrivacyModel.K25_ANONYMITY,
//                    BenchmarkPrivacyModel.K50_ANONYMITY
                    };
        }
    }

    /**
     * Returns a set of utility measures
     * 
     * @return
     */
    public BenchmarkUtilityMeasure[] getUtilityMeasures() {
        if (utilityMeasures != null) {
            return utilityMeasures;
        } else {
            return new BenchmarkUtilityMeasure[] {
//                    BenchmarkUtilityMeasure.DISCERNIBILITY,
                    BenchmarkUtilityMeasure.LOSS };
        }
    }

    /**
     * Get suppression limits.
     * @return Suppression limits as defined in xml. Default is { 0.02, 0.05, 0.1, 1.0 }.
     */
    public double[] getSuppressionLimits() {
        if (suppressionLimits != null) {
            return suppressionLimits;
        } else {
            return new double[] { 0.05d, 0.1d, 0.95d };
        }
    }

    /**
     * Get generalization/suppression factors.
     * @return gsFactors as defined in xml. Default is 0.
     */
    public double[] getGsFactors() {
        if (gsFactors != null) {
            return gsFactors;
        } else {
            return new double[] { 0.0d };
        }
    }

    /**
     * Get step sizes for RGR with dynamic gsFactor.
     * @return Step sizes as defined in xml. Default is 0.
     */
    public double[] getGsStepSizes() {
        if (gsStepSizes != null) {
            return gsStepSizes;
        } else {
            return new double[] { 0.05d };
        }
    }

    public String getOutputFile() {
        if (outputFile != null) {
            return outputFile;
        } else {
            return "results/experiment.csv";
        }
    }

    public String getPlotFile() {
        if (plotFile != null) {
            return plotFile;
        } else {
            return "results/experiment";
        }
    }

    /**
     * @return The number of runs for this benchmark. Default is 1.
     */
    public int getNumberOfRuns() {
        return this.numberOfRuns;
    }

    /**
     * The value is in depencance of the number of runs (numberOfRuns) of this
     * benchmark. If numberOfRuns > 1, it will return 10% of numberOfRuns
     * (rounded up to the next integer). If numberOfRuns <= 1, it will return 0.
     * 
     * @return The number of warmup runs for this benchmark.
     */
    public int getNumberOfWarmups() {
        return (this.numberOfRuns > 1) ? (int) Math.ceil(numberOfRuns / 10.0) : 0;
    }

    /**
     * Reads an xml config file to create a customized BenchmarkSetup
     * 
     * @param configFile
     *            The path to the xml config file
     * @return The root node of the xml
     */
    public boolean readXML(String configFile) {

        // Init
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Element doc;
        try {
            // Parse document
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document dom = db.parse(configFile);

            // Get root element
            doc = dom.getDocumentElement();

        } catch (SAXException | IOException | ParserConfigurationException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            return false;
        }

        // Read datasets
        NodeList nList = doc.getElementsByTagName("dataset");
        if (nList.getLength() > 0) {
            if (nList.getLength() == 1 &&
                nList.item(0).getTextContent().toUpperCase().equals("ADULT_ALL_SUBSETS")) {
                datasets = new BenchmarkDataset[30];
                for (int records = 1; records <= 30; records += 1) {
                    datasets[records - 1] = BenchmarkDataset.valueOf("ADULT" + (records * 1000));
                }
            } else {
                datasets = new BenchmarkDataset[nList.getLength()];
                for (int i = 0; i < nList.getLength(); i++) {
                    datasets[i] = BenchmarkDataset.valueOf(nList.item(i)
                                                                .getTextContent()
                                                                .toUpperCase());
                }
            }
        }

        // Read algorithms
        nList = doc.getElementsByTagName("algorithm");
        if (nList.getLength() > 0) {
            algorithms = new BenchmarkAlgorithm[nList.getLength()];
            for (int i = 0; i < nList.getLength(); i++) {
                algorithms[i] = BenchmarkAlgorithm.valueOf(nList.item(i)
                                                                .getTextContent()
                                                                .toUpperCase());
            }
        }

        // Read privacyModels
        nList = doc.getElementsByTagName("privacyModel");
        if (nList.getLength() > 0) {
            privacyModels = new BenchmarkPrivacyModel[nList.getLength()];
            for (int i = 0; i < nList.getLength(); i++) {
                privacyModels[i] = BenchmarkPrivacyModel.valueOf(nList.item(i)
                                                                      .getTextContent()
                                                                      .toUpperCase());
            }
        }

        // Read utilityMeasures
        nList = doc.getElementsByTagName("utilityMeasure");
        if (nList.getLength() > 0) {
            utilityMeasures = new BenchmarkUtilityMeasure[nList.getLength()];
            for (int i = 0; i < nList.getLength(); i++) {
                utilityMeasures[i] = BenchmarkUtilityMeasure.valueOf(nList.item(i)
                                                                          .getTextContent()
                                                                          .toUpperCase());
            }
        }

        // Read suppressionLimits
        nList = doc.getElementsByTagName("suppressionLimit");
        suppressionLimits = parseDoubleParameters(nList);

        // Read generalization/suppression-factors
        nList = doc.getElementsByTagName("gsFactor");
        gsFactors = parseDoubleParameters(nList);
        
        // Read gsStepSize for dynamic gsFactor
        nList = doc.getElementsByTagName("gsStepSize");
        gsStepSizes = parseDoubleParameters(nList);

        // Read number of runs for runtime benchmarking
        nList = doc.getElementsByTagName("outputFile");
        if (nList.getLength() == 1) {
            outputFile = nList.item(0).getTextContent();
        } else if (nList.getLength() > 1) {
            throw new IllegalArgumentException("XML config: Too many parameters for outputFile!");
        }

        // Read number of runs for runtime benchmarking
        nList = doc.getElementsByTagName("plotFile");
        if (nList.getLength() == 1) {
            plotFile = nList.item(0).getTextContent();
            // trim ".pdf" at the end, since plotter adds it automatically
            if (plotFile.toLowerCase().endsWith(".pdf")) {
                plotFile = plotFile.substring(0, plotFile.lastIndexOf(".pdf"));
            }
        } else if (nList.getLength() > 1) {
            throw new IllegalArgumentException("XML config: Too many parameters for plotFile!");
        }

        // Read number of runs for runtime benchmarking
        nList = doc.getElementsByTagName("numberOfRuns");
        if (nList.getLength() == 1) {
            numberOfRuns = Integer.valueOf(nList.item(0).getTextContent());
        } else if (nList.getLength() > 1) {
            throw new IllegalArgumentException("XML config: Too many parameters for numberOfRuns!");
        }

        return true;
    }

    /**
     * Helper function to parse double values from config xml files. Supports
     * the interval attributes "from", "to" and "stepSize".
     * 
     * @param nList
     *            The nodes containing the values.
     * @return An array with all parsed values.
     */
    private static double[] parseDoubleParameters(NodeList nList) {

        double[] result = null;
        if (nList.getLength() > 0) {
            ArrayList<Double> valueList = new ArrayList<>();
            for (int i = 0; i < nList.getLength(); i++) {
                Node n = nList.item(i);
                if (n.getAttributes().getLength() != 0) {
                    double from = Double.valueOf(n.getAttributes()
                                                  .getNamedItem("from")
                                                  .getTextContent());
                    double to = Double.valueOf(n.getAttributes()
                                                .getNamedItem("to")
                                                .getTextContent());
                    double stepSize = Double.valueOf(n.getAttributes()
                                                      .getNamedItem("stepSize")
                                                      .getTextContent());

                    for (double value = from; round(value, 5) <= to; value += stepSize) {
                        valueList.add(round(value, 5));
                    }
                } else {
                    valueList.add(Double.valueOf(nList.item(i).getTextContent()));
                }
            }

            result = new double[valueList.size()];
            for (int i = 0; i < result.length; i++) {
                result[i] = valueList.get(i);
            }
        }

        return result;
    }

    /**
     * Helper method for rounding doubles to a specific number of decimals.
     * 
     * @param value
     *            Input value.
     * @param places
     *            Number of decimals.
     * @return Rounded value.
     */
    private static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
