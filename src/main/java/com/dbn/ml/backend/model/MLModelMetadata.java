/*
 * Copyright 2024-2025 Oracle and/or its affiliates
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

package com.dbn.ml.backend.model;

import lombok.Builder;
import lombok.Getter;
import org.jetbrains.annotations.Nls;

import java.util.List;

/**
 * Metadata about a trained ML model.
 *
 * @author Oracle
 */
@Getter
@Builder
public class MLModelMetadata {

    /** Feature column names used for training */
    private final List<String> featureNames;

    /** Label/target column name(s) */
    private final List<String> labelNames;

    /** Number of classes (for classification) */
    private final Integer classCount;

    /** Class labels (for classification) */
    private final List<String> classLabels;

    /** Number of output dimensions (for regression) */
    private final Integer outputDimensions;

    /** Algorithm name */
    private final @Nls String algorithmName;

    /** Training data size */
    private final Integer trainingDataSize;

    /** Test data size */
    private final Integer testDataSize;
}
