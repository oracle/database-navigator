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

import com.dbn.common.ui.Presentable;
import lombok.Getter;

/**
 * Machine Learning task types supported by the ML Toolbox.
 */
@Getter
public enum MLTaskType implements Presentable {
    
    CLASSIFICATION("Classification", "Predict categorical labels (e.g., Win/Lose/Draw)"),
    REGRESSION("Regression", "Predict numeric values (e.g., goals scored)");
    
    private final String name;
    private final String description;
    
    MLTaskType(String name, String description) {
        this.name = name;
        this.description = description;
    }
    
    /**
     * Returns the Oracle metadata function name for this task type.
     */
    public String getOracleFunction() {
        return switch (this) {
            case CLASSIFICATION -> "classification";
            case REGRESSION -> "regression";
        };
    }
}
