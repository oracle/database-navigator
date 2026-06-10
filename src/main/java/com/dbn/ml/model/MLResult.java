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
import com.dbn.ml.backend.dbms.DBMSAlgorithmType;
import com.dbn.ml.backend.dbms.DBMSEvaluationResult;
import com.dbn.ml.backend.dbms.DBMSModelHandle;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nls;

import java.util.List;

import static com.dbn.nls.NlsResources.txt;

/**
 * Result of ML training execution using Oracle DBMS_DATA_MINING.
 *
 * @author ayoub allali
 */
@Getter
@Setter
public class MLResult {

    private MLTaskType taskType;
    private DBMSModelHandle modelHandle;
    private DBMSEvaluationResult evaluationResult;

    private ConnectionHandler connection;
    private @Nls String algorithmName;
    private DBMSAlgorithmType algorithmType;

    private int trainingDataSize;
    private int testingDataSize;
    private int featureCount;
    private int classCount;
    private int outputDimensions;

    private long trainingTimeMs;

    // Column names for prediction UI
    private List<String> featureColumns;
    private String labelColumn;
    private List<String> labelColumns;

    // Source name for default model naming (table name or CSV file name)
    private String sourceName;

    // Model internals from Oracle Model Detail Views (DM$V*)
    private MLModelDetails modelDetails;

    /**
     * Returns the database model name.
     */
    public String getModelName() {
        return modelHandle != null ? modelHandle.getModelName() : null;
    }

    // ==================== Metrics (delegated to evaluation result) ====================

    public double getAccuracy() {
        return evaluationResult != null ? evaluationResult.getAccuracy() : 0.0;
    }

    public String getConfusionMatrix() {
        if (evaluationResult != null && taskType == MLTaskType.CLASSIFICATION) {
            return evaluationResult.getConfusionMatrix();
        }
        return txt("app.machineLearning.placeholder.NotApplicable");
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
        return evaluationResult != null ? evaluationResult.getSummaryText() : txt("app.machineLearning.text.NoEvaluationAvailable");
    }

    public boolean isClassification() {
        return taskType == MLTaskType.CLASSIFICATION;
    }

    public boolean isRegression() {
        return taskType == MLTaskType.REGRESSION;
    }
}
