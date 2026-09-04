/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.object.type;

import com.dbn.common.constant.PseudoConstant;
import org.jetbrains.annotations.NonNls;

@SuppressWarnings("unused")
public class DBMiningModelAlgorithm extends PseudoConstant<DBMiningModelAlgorithm> {
    public static final DBMiningModelAlgorithm CLASSIFICATION = get("CLASSIFICATION");
    public static final DBMiningModelAlgorithm CUR_DECOMPOSITION = get("CUR_DECOMPOSITION");
    public static final DBMiningModelAlgorithm NAIVE_BAYES = get("NAIVE_BAYES");
    public static final DBMiningModelAlgorithm DECISION_TREE = get("DECISION_TREE");
    public static final DBMiningModelAlgorithm EXPLICIT_SEMANTIC_ANALYSIS = get("EXPLICIT_SEMANTIC_ANALYSIS");
    public static final DBMiningModelAlgorithm EXPONENTIAL_SMOOTHING = get("EXPONENTIAL_SMOOTHING");
    public static final DBMiningModelAlgorithm SUPPORT_VECTOR_MACHINES = get("SUPPORT_VECTOR_MACHINES");
    public static final DBMiningModelAlgorithm KMEANS = get("KMEANS");
    public static final DBMiningModelAlgorithm ONNX = get("ONNX");
    public static final DBMiningModelAlgorithm O_CLUSTER = get("O_CLUSTER");
    public static final DBMiningModelAlgorithm NONNEGATIVE_MATRIX_FACTOR = get("NONNEGATIVE_MATRIX_FACTOR");
    public static final DBMiningModelAlgorithm NEURAL_NETWORK = get("NEURAL_NETWORK");
    public static final DBMiningModelAlgorithm GENERALIZED_LINEAR_MODEL = get("GENERALIZED_LINEAR_MODEL");
    public static final DBMiningModelAlgorithm APRIORI_ASSOCIATION_RULES = get("APRIORI_ASSOCIATION_RULES");
    public static final DBMiningModelAlgorithm MINIMUM_DESCRIPTION_LENGTH = get("MINIMUM_DESCRIPTION_LENGTH");
    public static final DBMiningModelAlgorithm EXPECTATION_MAXIMIZATION = get("EXPECTATION_MAXIMIZATION");
    public static final DBMiningModelAlgorithm RANDOM_FOREST = get("RANDOM_FOREST");
    public static final DBMiningModelAlgorithm SINGULAR_VALUE_DECOMP = get("SINGULAR_VALUE_DECOMP");
    public static final DBMiningModelAlgorithm R_EXTENSIBLE = get("R_EXTENSIBLE");

    protected DBMiningModelAlgorithm(@NonNls String id) {
        super(id);
    }

    public static DBMiningModelAlgorithm get(@NonNls String id) {
        return PseudoConstant.get(DBMiningModelAlgorithm.class, id);
    }
}
