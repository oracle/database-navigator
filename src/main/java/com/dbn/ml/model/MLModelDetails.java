/*
 * Copyright 2025 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.ml.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Model internals loaded from Oracle Model Detail Views (DM$V*).
 * Populated after training from DM$VG, DM$VA, DM$VS, DM$VW, and
 * algorithm-specific views (DM$VD, DM$VL, DM$VP, DM$VV).
 */
@Getter
@Setter
public class MLModelDetails {

    /** Build alerts from DM$VW. Non-empty means Oracle flagged warnings during training. */
    private List<String> buildAlerts = new ArrayList<>();

    /** Variable importance from DM$VA (Random Forest, MDL, EM). */
    private List<VariableImportance> variableImportance = new ArrayList<>();

    /** GLM coefficients from DM$VD (Logistic Regression, Linear Regression). */
    private List<GLMCoefficient> glmCoefficients = new ArrayList<>();

    /** SVM linear coefficients from DM$VL (SVM Classification, SVM Regression). */
    private List<SVMCoefficient> svmCoefficients = new ArrayList<>();

    /** Decision Tree splits from DM$VP (Decision Tree). */
    private List<TreeSplit> treeSplits = new ArrayList<>();

    /** Naive Bayes class priors from DM$VP (Naive Bayes). */
    private List<NaiveBayesPrior> nbPriors = new ArrayList<>();

    /** Naive Bayes top conditional probabilities from DM$VV (Naive Bayes). */
    private List<NaiveBayesConditional> nbConditionals = new ArrayList<>();

    /** Global statistics from DM$VG: stat name -> display value. */
    private Map<String, String> globalStats = new LinkedHashMap<>();

    /** Computed settings from DM$VS: setting name -> value. */
    private Map<String, String> computedSettings = new LinkedHashMap<>();

    public boolean hasBuildAlerts() { return !buildAlerts.isEmpty(); }
    public boolean hasVariableImportance() { return !variableImportance.isEmpty(); }
    public boolean hasGLMCoefficients() { return !glmCoefficients.isEmpty(); }
    public boolean hasSVMCoefficients() { return !svmCoefficients.isEmpty(); }
    public boolean hasTreeSplits() { return !treeSplits.isEmpty(); }
    public boolean hasNaiveBayes() { return !nbPriors.isEmpty(); }
    public boolean hasGlobalStats() { return !globalStats.isEmpty(); }
    public boolean hasComputedSettings() { return !computedSettings.isEmpty(); }

    @Getter
    public static class VariableImportance {
        private final String attributeName;
        private final double importance;

        public VariableImportance(String attributeName, double importance) {
            this.attributeName = attributeName;
            this.importance = importance;
        }
    }

    @Getter
    public static class GLMCoefficient {
        private final String attributeName;
        private final String attributeValue;
        private final double coefficient;
        private final double stdError;
        private final double pValue;

        public GLMCoefficient(String attributeName, String attributeValue,
                              double coefficient, double stdError, double pValue) {
            this.attributeName = attributeName;
            this.attributeValue = attributeValue;
            this.coefficient = coefficient;
            this.stdError = stdError;
            this.pValue = pValue;
        }
    }

    @Getter
    public static class SVMCoefficient {
        private final String attributeName;
        private final String attributeValue;
        private final String className;
        private final double coefficient;

        public SVMCoefficient(String attributeName, String attributeValue, String className, double coefficient) {
            this.attributeName = attributeName;
            this.attributeValue = attributeValue;
            this.className = className;
            this.coefficient = coefficient;
        }
    }

    @Getter
    public static class TreeSplit {
        private final int node;
        private final int parent;
        private final String attributeName;
        private final String operator;
        private final String value;

        public TreeSplit(int node, int parent, String attributeName, String operator, String value) {
            this.node = node;
            this.parent = parent;
            this.attributeName = attributeName;
            this.operator = operator;
            this.value = value;
        }
    }

    @Getter
    public static class NaiveBayesPrior {
        private final String targetValue;
        private final double probability;
        private final int count;

        public NaiveBayesPrior(String targetValue, double probability, int count) {
            this.targetValue = targetValue;
            this.probability = probability;
            this.count = count;
        }
    }

    @Getter
    public static class NaiveBayesConditional {
        private final String targetValue;
        private final String attributeName;
        private final String attributeValue;
        private final double conditionalProbability;

        public NaiveBayesConditional(String targetValue, String attributeName,
                                     String attributeValue, double conditionalProbability) {
            this.targetValue = targetValue;
            this.attributeName = attributeName;
            this.attributeValue = attributeValue;
            this.conditionalProbability = conditionalProbability;
        }
    }
}
