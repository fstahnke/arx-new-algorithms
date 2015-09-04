/*
 * Benchmark of risk-based anonymization in ARX 3.0.0
 * Copyright 2015 - Fabian Prasser
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

package org.deidentifier.arx.benchmark;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.AttributeType.Hierarchy;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.criteria.KAnonymity;
import org.deidentifier.arx.metric.Metric;
import org.deidentifier.arx.metric.Metric.AggregateFunction;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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

    private String                    outputFile;

    private String                    plotFile;

    /**
     * Create new BenchmarkSetup instance with xml for configuration
     * 
     * @param configFile
     *            path to xml benchmark configuration
     */
    public BenchmarkSetup(String configFile) {
        readXML(configFile);
    }

    /**
     * Create new BenchmarkSetup instance with default configuration
     */
    public BenchmarkSetup() {
        // empty by design
    }

    public static enum BenchmarkDataset {
        ADULT {
            @Override
            public String toString() {
                return "Adult";
            }
        },
        ADULT_SUBSET {
            @Override
            public String toString() {
                return "AdultSubset";
            }
        },
        CUP {
            @Override
            public String toString() {
                return "Cup";
            }
        },
        CUP_SUBSET {
            @Override
            public String toString() {
                return "CupSubset";
            }
        },
        FARS {
            @Override
            public String toString() {
                return "Fars";
            }
        },
        FARS_SUBSET {
            @Override
            public String toString() {
                return "FarsSubset";
            }
        },
        ATUS {
            @Override
            public String toString() {
                return "Atus";
            }
        },
        ATUS_SUBSET {
            @Override
            public String toString() {
                return "AtusSubset";
            }
        },
        IHIS {
            @Override
            public String toString() {
                return "Ihis";
            }
        },
        IHIS_SUBSET {
            @Override
            public String toString() {
                return "IhisSubset";
            }
        }
    }

    public static enum BenchmarkPrivacyModel {
        K5_ANONYMITY {
            @Override
            public String toString() {
                return "5-anonymity";
            }
        },
        K20_ANONYMITY {
            @Override
            public String toString() {
                return "20-anonymity";
            }
        }
    }

    public static enum BenchmarkAlgorithm {
        RECURSIVE_GLOBAL_RECODING {
            @Override
            public String toString() {
                return "RGR";
            }
        },
        TASSA {
            @Override
            public String toString() {
                return "Tassa & Goldberger";
            }
        }
    }

    public static enum BenchmarkUtilityMeasure {
        DISCERNIBILITY {
            @Override
            public String toString() {
                return "Discernibility";
            }
        },
        LOSS {
            @Override
            public String toString() {
                return "Loss";
            }
        },
    }

    /**
     * Returns a configuration for the ARX framework
     * 
     * @param dataset
     * @param suppression
     * @param criteria
     * @return
     * @throws IOException
     */
    public static ARXConfiguration getConfiguration(BenchmarkDataset dataset,
                                                    BenchmarkUtilityMeasure utility,
                                                    BenchmarkPrivacyModel criterion,
                                                    double suppression) throws IOException {
        ARXConfiguration config = ARXConfiguration.create();
        switch (utility) {
        case DISCERNIBILITY:
            config.setMetric(Metric.createDiscernabilityMetric(false));
            break;
        case LOSS:
            config.setMetric(Metric.createLossMetric(AggregateFunction.GEOMETRIC_MEAN));
            break;
        default:
            throw new IllegalArgumentException("");
        }

        config.setMaxOutliers(suppression);

        switch (criterion) {
        case K5_ANONYMITY:
            config.addCriterion(new KAnonymity(5));
            break;
        case K20_ANONYMITY:
            config.addCriterion(new KAnonymity(20));
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
        switch (dataset) {
        case ADULT:
            data = Data.create("data/adult.csv", ';');
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
        default:
            throw new RuntimeException("Invalid dataset");
        }

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
        case ADULT_SUBSET:
            return Hierarchy.create("hierarchies/adult_hierarchy_" + attribute + ".csv", ';');
        case ATUS:
        case ATUS_SUBSET:
            return Hierarchy.create("hierarchies/atus_hierarchy_" + attribute + ".csv", ';');
        case CUP:
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
        switch (dataset) {
        case ADULT:
        case ADULT_SUBSET:
            return new String[] {
                    "age",
                    "education",
                    "marital-status",
                    "native-country",
                    "race",
                    "salary-class",
                    "sex",
                    "workclass",
                    "occupation" };
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
            return new String[] {
                    "AGE",
                    "GENDER",
                    "INCOME",
                    "MINRAMNT",
                    "NGIFTALL",
                    "STATE",
                    "ZIP",
                    "RAMNTALL" };
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
                    BenchmarkDataset.CUP,
                    BenchmarkDataset.FARS,
                    BenchmarkDataset.ATUS,
                    BenchmarkDataset.IHIS, };
        }
    }

    public BenchmarkAlgorithm[] getAlgorithms() {
        if (algorithms != null) {
            return algorithms;
        } else {
            return new BenchmarkAlgorithm[] {
                    BenchmarkAlgorithm.RECURSIVE_GLOBAL_RECODING,
                    BenchmarkAlgorithm.TASSA };
        }
    }

    public BenchmarkPrivacyModel[] getPrivacyModels() {
        if (privacyModels != null) {
            return privacyModels;
        } else {
            return new BenchmarkPrivacyModel[] {
                    BenchmarkPrivacyModel.K5_ANONYMITY,
                    BenchmarkPrivacyModel.K20_ANONYMITY };
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
                    BenchmarkUtilityMeasure.DISCERNIBILITY,
                    BenchmarkUtilityMeasure.LOSS };
        }
    }

    public double[] getSuppressionLimits() {
        if (suppressionLimits != null) {
            return suppressionLimits;
        } else {
            return new double[] { 0.02d, 0.05d, 0.1d, 1.0d };
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
            return "results/experiment.pdf";
        }
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
            datasets = new BenchmarkDataset[nList.getLength()];
            for (int i = 0; i < nList.getLength(); i++) {
                datasets[i] = BenchmarkDataset.valueOf(nList.item(i).getTextContent());
            }
        }

        // Read algorithms
        nList = doc.getElementsByTagName("algorithm");
        if (nList.getLength() > 0) {
            algorithms = new BenchmarkAlgorithm[nList.getLength()];
            for (int i = 0; i < nList.getLength(); i++) {
                algorithms[i] = BenchmarkAlgorithm.valueOf(nList.item(i).getTextContent());
            }
        }

        // Read privacyModels
        nList = doc.getElementsByTagName("privacyModel");
        if (nList.getLength() > 0) {
            privacyModels = new BenchmarkPrivacyModel[nList.getLength()];
            for (int i = 0; i < nList.getLength(); i++) {
                privacyModels[i] = BenchmarkPrivacyModel.valueOf(nList.item(i).getTextContent());
            }
        }

        // Read utilityMeasures
        nList = doc.getElementsByTagName("utilityMeasure");
        if (nList.getLength() > 0) {
            utilityMeasures = new BenchmarkUtilityMeasure[nList.getLength()];
            for (int i = 0; i < nList.getLength(); i++) {
                utilityMeasures[i] = BenchmarkUtilityMeasure.valueOf(nList.item(i).getTextContent());
            }
        }

        // Read suppressionLimits
        nList = doc.getElementsByTagName("suppressionLimit");
        if (nList.getLength() > 0) {
            suppressionLimits = new double[nList.getLength()];
            for (int i = 0; i < nList.getLength(); i++) {
                suppressionLimits[i] = Double.valueOf(nList.item(i).getTextContent());
            }
        }

        // Read suppressionLimits
        nList = doc.getElementsByTagName("suppressionLimit");
        if (nList.getLength() > 0) {
            suppressionLimits = new double[nList.getLength()];
            for (int i = 0; i < nList.getLength(); i++) {
                suppressionLimits[i] = Double.valueOf(nList.item(i).getTextContent());
            }
        }

        // Read file names for output files
        nList = doc.getChildNodes();
        if (nList.getLength() > 0) {
            for (int i = 0; i < nList.getLength(); i++) {
                String nodeName = nList.item(i).getNodeName();
                if (nodeName.equals("outputFile")) {
                    outputFile = nList.item(i).getTextContent();
                } else if (nodeName.equals("plotFile")) {
                    plotFile = nList.item(i).getTextContent();
                }
            }
        }

        return true;
    }
}
