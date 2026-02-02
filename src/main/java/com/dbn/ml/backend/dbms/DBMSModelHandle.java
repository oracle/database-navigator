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

package com.dbn.ml.backend.dbms;

import com.dbn.connection.ConnectionHandler;
import com.dbn.ml.backend.MLBackendType;
import com.dbn.ml.backend.model.MLModelHandle;
import com.dbn.ml.backend.model.MLModelMetadata;
import com.dbn.ml.model.MLTaskType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * DBMS_DATA_MINING implementation of MLModelHandle.
 * Stores reference to a model in the Oracle database along with
 * references to training/test tables for evaluation.
 *
 * @author Oracle
 */
@Getter
public class DBMSModelHandle implements MLModelHandle {

    private final String modelName;
    private final ConnectionHandler connection;
    private final MLTaskType taskType;
    private final MLModelMetadata metadata;

    // Table references
    private final String dataTableName;      // Training table (for backward compatibility)
    private final String settingsTableName;

    @Setter
    private String testTableName;            // Test table for evaluation

    @Setter
    private String sourceTableName;          // Original source table (before split)

    @Setter
    private List<String> classValues;        // Distinct class values (for classification)

    public DBMSModelHandle(
            String modelName,
            ConnectionHandler connection,
            MLTaskType taskType,
            MLModelMetadata metadata,
            String dataTableName,
            String settingsTableName) {

        this.modelName = modelName;
        this.connection = connection;
        this.taskType = taskType;
        this.metadata = metadata;
        this.dataTableName = dataTableName;
        this.settingsTableName = settingsTableName;
    }

    @Override
    public MLBackendType getBackendType() {
        return MLBackendType.DBMS_DATA_MINING;
    }

    @Override
    public Object getNativeModel() {
        // For DBMS backend, the "native model" is just the model name (String reference)
        return modelName;
    }

    /**
     * Gets the training table name (alias for dataTableName).
     */
    public String getTrainTableName() {
        return dataTableName;
    }

    /**
     * Checks if this is a binary classification model.
     */
    public boolean isBinaryClassification() {
        return taskType == MLTaskType.CLASSIFICATION
                && classValues != null
                && classValues.size() == 2;
    }
}
