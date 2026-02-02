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

package com.dbn.ml.backend;

import com.dbn.common.ui.Presentable;
import lombok.Getter;

/**
 * Enum representing the available ML training backends.
 * Each backend has different capabilities and execution models.
 *
 * @author Oracle
 */
@Getter
public enum MLBackendType implements Presentable {

    TRIBUO(
        "Tribuo (Client-Side)",
        "Train models locally using the Tribuo ML library. Supports ONNX export.",
        true  // supports ONNX export
    ),

    DBMS_DATA_MINING(
        "Oracle DBMS_DATA_MINING (In-Database)",
        "Train models directly in Oracle Database using DBMS_DATA_MINING. Models stored in database.",
        false // models stored in DB, no ONNX export
    );

    private final String name;
    private final String description;
    private final boolean supportsOnnxExport;

    MLBackendType(String name, String description, boolean supportsOnnxExport) {
        this.name = name;
        this.description = description;
        this.supportsOnnxExport = supportsOnnxExport;
    }

    @Override
    public String getName() {
        return name;
    }
}
