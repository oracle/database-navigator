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
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;

import static com.dbn.nls.NlsResources.txt;

/**
 * Machine Learning task types supported by the ML Toolbox.
 */
@Getter
public enum MLTaskType implements Presentable {
    
    CLASSIFICATION(txt("app.machineLearning.const.MLTaskType_CLASSIFICATION"), txt("app.machineLearning.text.MLTaskType_CLASSIFICATION")),
    REGRESSION(txt("app.machineLearning.const.MLTaskType_REGRESSION"), txt("app.machineLearning.text.MLTaskType_REGRESSION"));
    
    private final @Nls String name;
    private final @Nls String description;
    
    MLTaskType(@Nls String name, @Nls String description) {
        this.name = name;
        this.description = description;
    }
    
    /**
     * Returns the Oracle metadata function name for this task type.
     */
    public @NonNls String getOracleFunction() {
        return switch (this) {
            case CLASSIFICATION -> "classification";
            case REGRESSION -> "regression";
        };
    }
}
