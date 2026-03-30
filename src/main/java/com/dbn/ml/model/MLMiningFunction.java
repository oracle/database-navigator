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
import org.jetbrains.annotations.Nullable;

/**
 * Oracle DBMS_DATA_MINING mining functions.
 * Represents the top-level operation type the user wants to perform.
 * Only CLASSIFICATION and REGRESSION are currently supported.
 */
@Getter
public enum MLMiningFunction {

    CLASSIFICATION("Classification", "Predict a category (e.g. Yes/No, species)", true, MLTaskType.CLASSIFICATION),
    REGRESSION("Regression", "Predict a number (e.g. price, score)", true, MLTaskType.REGRESSION),
    CLUSTERING("Clustering", "Group similar rows without a label", false, null),
    ANOMALY_DETECTION("Anomaly Detection", "Detect outliers and unusual patterns", false, null);

    private final String name;
    private final String tooltip;
    private final boolean supported;
    @Nullable private final MLTaskType taskType;

    MLMiningFunction(String name, String tooltip, boolean supported, @Nullable MLTaskType taskType) {
        this.name = name;
        this.tooltip = tooltip;
        this.supported = supported;
        this.taskType = taskType;
    }
}
