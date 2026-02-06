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

import com.dbn.connection.ConnectionHandler;
import com.dbn.ml.backend.MLBackendType;
import com.dbn.ml.backend.model.MLEvaluationResult;
import com.dbn.ml.backend.model.MLModelHandle;
import lombok.Getter;
import lombok.Setter;
import org.tribuo.Model;

import java.util.List;

/**
 * Result of ML training execution.
 * Backend-agnostic, supporting both Tribuo and DBMS_DATA_MINING backends.
 *
 * @author ayoub allali
 */
@Getter
@Setter
public class MLResult {

    private MLTaskType taskType;
    private MLBackendType backendType;
    private MLModelHandle modelHandle;
    private MLEvaluationResult evaluationResult;

    private ConnectionHandler connection;
    private String algorithmName;

    private int trainingDataSize;
    private int testingDataSize;
    private int featureCount;
    private int classCount;
    private int outputDimensions;

    private long trainingTimeMs;

    // Column names for ONNX metadata
    private List<String> featureColumns;
    private String labelColumn;
    private List<String> labelColumns;

    // Source name for default model naming (table name or CSV file name)
    private String sourceName;

    /**
     * Returns true if this is a multi-output regression (2+ labels)
     */
    public boolean isMultiOutput() {
        return labelColumns != null && labelColumns.size() > 1;
    }

    /**
     * Returns the native model object.
     * For Tribuo: Returns Model<?>
     * For DBMS: Returns model name (String)
     */
    public Object getModel() {
        return modelHandle != null ? modelHandle.getNativeModel() : null;
    }

    /**
     * Returns the Tribuo model.
     * Only valid for Tribuo backend.
     */
    @SuppressWarnings("unchecked")
    public Model<?> getTribuoModel() {
        if (backendType != MLBackendType.TRIBUO) {
            throw new IllegalStateException("getTribuoModel() can only be called for Tribuo backend");
        }
        return (Model<?>) getModel();
    }

    // ==================== Metrics (delegated to evaluation result) ====================

    public double getAccuracy() {
        return evaluationResult != null ? evaluationResult.getAccuracy() : 0.0;
    }

    public String getConfusionMatrix() {
        if (evaluationResult != null && taskType == MLTaskType.CLASSIFICATION) {
            return evaluationResult.getConfusionMatrix();
        }
        return "N/A";
    }

    public double getR2Score() {
        return evaluationResult != null ? evaluationResult.getR2Score() : 0.0;
    }

    public double getRMSE() {
        return evaluationResult != null ? evaluationResult.getRMSE() : 0.0;
    }

    public double getMAE() {
        return evaluationResult != null ? evaluationResult.getMAE() : 0.0;
    }

    public String getEvaluationSummary() {
        return evaluationResult != null ? evaluationResult.getSummaryText() : "No evaluation available";
    }

    public boolean isClassification() {
        return taskType == MLTaskType.CLASSIFICATION;
    }

    public boolean isRegression() {
        return taskType == MLTaskType.REGRESSION;
    }
}
