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

import com.dbn.connection.ConnectionHandler;
import com.dbn.ml.model.MLRequest;
import com.dbn.ml.model.MLTaskType;
import com.dbn.ml.model.feature.MLFeatureConfig;
import com.dbn.ml.model.source.MLSourceConfig;
import com.dbn.ml.model.trainer.MLTrainerConfig;
import com.dbn.ml.model.trainer.MLTrainerType;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nls;

/**
 * Runtime context for ML training execution.
 * Holds a reference to the user's request plus execution-specific state.
 *
 * @author ayoub allali
 */
@Getter
@Setter
public class MLTrainingContext {

    // ==================== Request Reference ====================

    /** The user's training request (contains all configs) */
    private MLRequest request;

    // ==================== Execution State ====================

    /** Number of samples in training dataset (set during execution) */
    private int trainingDataSize;

    /** Number of samples in test dataset (set during execution) */
    private int testingDataSize;

    /** Training start timestamp */
    private long trainingStartTime;

    /** Scheduler job name for async training submissions. */
    private String schedulerJobName;

    /** Target model name generated for async training submissions. */
    private String modelName;

    // ==================== DBMS-Specific State ====================

    /** Schema name for staging table (DBMS backend only) */
    private String stagingTableSchema;

    /** Staging table name (DBMS backend only) */
    private String stagingTableName;

    /** Whether to cleanup staging table after training (DBMS backend only) */
    private boolean shouldCleanupStagingTable;

    /** Schema name for settings table (DBMS backend only) */
    private String settingsTableSchema;

    /** Settings table name (DBMS backend only) */
    private String settingsTableName;

    /** Training table name (DBMS backend only - created from SAMPLE SEED) */
    private String trainTableName;

    /** Test table name (DBMS backend only - created from source MINUS training) */
    private String testTableName;

    /** Apply results table name (DBMS backend only - stores predictions on test data) */
    private String applyResultTableName;

    /** Confusion matrix table name (DBMS backend only - from COMPUTE_CONFUSION_MATRIX) */
    private String confusionMatrixTableName;

    /** ROC table name (DBMS backend only - from COMPUTE_ROC for binary classification) */
    private String rocTableName;

    /** Lift table name (DBMS backend only - from COMPUTE_LIFT for binary classification) */
    private String liftTableName;

    // ==================== Convenience Accessors ====================

    public MLSourceConfig getSourceConfig() {
        return request.getSourceConfig();
    }

    public MLFeatureConfig getFeatureConfig() {
        return request.getFeatureConfig();
    }

    public MLTrainerConfig getTrainerConfig() {
        return request.getTrainerConfig();
    }

    public ConnectionHandler getConnection() {
        return request.getConnection();
    }

    public MLTaskType getTaskType() {
        return request.getTrainerConfig().getTrainerType().getTaskType();
    }

    public MLTrainerType getTrainerType() {
        return request.getTrainerConfig().getTrainerType();
    }

    public @Nls String getAlgorithmName() {
        return getTrainerType().getName();
    }
}
