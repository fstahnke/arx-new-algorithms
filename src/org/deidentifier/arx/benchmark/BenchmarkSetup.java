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

import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.AttributeType.Hierarchy;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.criteria.KAnonymity;
import org.deidentifier.arx.metric.Metric;
import org.deidentifier.arx.metric.Metric.AggregateFunction;

/**
 * This class encapsulates most of the parameters of a benchmark run
 * @author Fabian Prasser
 */
public class BenchmarkSetup {
    
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
        }, K20_ANONYMITY {
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
        }, TASSA {
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
     * @param dataset
     * @param suppression 
     * @param criteria
     * @return
     * @throws IOException
     */
    public static ARXConfiguration getConfiguration(BenchmarkDataset dataset, BenchmarkUtilityMeasure utility, BenchmarkPrivacyModel criterion, double suppression) throws IOException {
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
     * @param dataset
     * @param criteria
     * @return
     * @throws IOException
     */
    
    public static Data getData(BenchmarkDataset dataset, BenchmarkPrivacyModel criterion) throws IOException {
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
     * Returns all datasets
     * @return
     */
    public static BenchmarkDataset[] getDatasets() {
        return new BenchmarkDataset[] {
//        		BenchmarkDataset.ADULT_SUBSET,
                BenchmarkDataset.ADULT,
                BenchmarkDataset.CUP,
//                BenchmarkDataset.FARS,
//                BenchmarkDataset.ATUS,
//                BenchmarkDataset.IHIS,
        };
    }
    
    /**
     * Returns the generalization hierarchy for the dataset and attribute
     * @param dataset
     * @param attribute
     * @return
     * @throws IOException
     */
    public static Hierarchy getHierarchy(BenchmarkDataset dataset, String attribute) throws IOException {
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
     * @param dataset
     * @return
     */
    public static String[] getQuasiIdentifyingAttributes(BenchmarkDataset dataset) {
        switch (dataset) {
        case ADULT:
        case ADULT_SUBSET:
            return new String[] {   "age",
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
            return new String[] {   "Age",
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
            return new String[] {   "AGE",
                                    "GENDER",
                                    "INCOME",
                                    "MINRAMNT",
                                    "NGIFTALL",
                                    "STATE",
                                    "ZIP",
                                    "RAMNTALL" };
        case FARS:
        case FARS_SUBSET:
            return new String[] {   "iage",
                                    "ideathday",
                                    "ideathmon",
                                    "ihispanic",
                                    "iinjury",
                                    "irace",
                                    "isex",
                                    "istatenum" };
        case IHIS:
        case IHIS_SUBSET:
            return new String[] {   "AGE",
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
     * Returns a set of utility measures
     * @return
     */
    public static BenchmarkUtilityMeasure[] getUtilityMeasures() {
        return new BenchmarkUtilityMeasure[]{
                BenchmarkUtilityMeasure.DISCERNIBILITY,
                BenchmarkUtilityMeasure.LOSS};
    }

	public static BenchmarkPrivacyModel[] getPrivacyModels() {
		return new BenchmarkPrivacyModel[]{
				BenchmarkPrivacyModel.K5_ANONYMITY,
//				BenchmarkPrivacyModel.K20_ANONYMITY
		};
	}

	public static double[] getSuppressionLimits() {
		return new double[]{
		        0.02d,
		        0.05d,
		        0.1d,
		        1.0d
		};
	}
	
	public static BenchmarkAlgorithm[] getAlgorithms() {
		return new BenchmarkAlgorithm[]{
			BenchmarkAlgorithm.RECURSIVE_GLOBAL_RECODING,
			BenchmarkAlgorithm.TASSA
		};
		
	}
}
