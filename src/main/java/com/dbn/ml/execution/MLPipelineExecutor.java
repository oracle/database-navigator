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

package com.dbn.ml.execution;

import com.dbn.connection.ConnectionHandler;
import com.dbn.ml.backend.MLBackend;
import com.dbn.ml.backend.MLBackendFactory;
import com.dbn.ml.backend.model.MLEvaluationResult;
import com.dbn.ml.backend.model.MLModelHandle;
import com.dbn.ml.backend.model.MLTrainingContext;
import com.dbn.ml.model.*;
import com.dbn.ml.model.source.MLSourceConfig;
import com.dbn.ml.model.source.MLSourceType;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

/**
 * Executes ML training pipeline.
 * Delegates to backend implementations (Tribuo, DBMS_DATA_MINING).
 * <p>
 *
 * @author ayoub allali
 */
@Slf4j
public class MLPipelineExecutor {

    /**
     * Execute ML training pipeline using the configured backend.
     *
     * @param request MLRequest containing all configuration
     * @param connectionHandler Connection handler for database operations
     * @return MLResult containing trained model and evaluation metrics
     * @throws Exception if training fails
     */
    public MLResult execute(MLRequest request, ConnectionHandler connectionHandler) throws Exception {
        // Build training context
        MLTrainingContext context = buildContext(request);

        // Get backend
        MLBackend backend = MLBackendFactory.createBackend(
                request.getBackendConfig().getBackendType(),
                connectionHandler
        );

        // Prepare result object
        MLResult result = new MLResult();
        result.setTaskType(context.getTaskType());
        result.setConnection(context.getConnection());
        result.setBackendType(backend.getBackendType());
        result.setAlgorithmName(context.getAlgorithmName());

        long startTime = System.currentTimeMillis();

        try {
            // Train model
            MLModelHandle modelHandle = backend.train(context);
            result.setModelHandle(modelHandle);

            // Evaluate model
            MLEvaluationResult evaluation = backend.evaluate(modelHandle, context);
            result.setEvaluationResult(evaluation);

            // Populate result metadata from context
            result.setTrainingDataSize(context.getTrainingDataSize());
            result.setTestingDataSize(context.getTestingDataSize());
            result.setFeatureCount(request.getFeatureConfig().getFeatureColumns().size());

            // Set class count or output dimensions based on task type
            if (context.getTaskType() == MLTaskType.CLASSIFICATION) {
                result.setClassCount(modelHandle.getMetadata().getClassCount());
            } else {
                result.setOutputDimensions(modelHandle.getMetadata().getOutputDimensions());
            }

            // Store original column names for ONNX metadata
            result.setFeatureColumns(new ArrayList<>(request.getFeatureConfig().getFeatureColumns()));

            if (context.getTaskType() == MLTaskType.CLASSIFICATION) {
                result.setLabelColumn(request.getFeatureConfig().getLabelColumn());
            } else {
                result.setLabelColumns(new ArrayList<>(request.getFeatureConfig().getLabelColumns()));
            }

            // Set source name for default model naming
            result.setSourceName(extractSourceName(request));

            // Record training time
            result.setTrainingTimeMs(System.currentTimeMillis() - startTime);

            return result;

        } finally {
            // Cleanup resources (staging tables, etc.)
            try {
                backend.cleanup(context);
            } catch (Exception e) {
                // Log cleanup failure but don't fail the entire operation
                // The training was successful, cleanup is a best-effort operation
                log.warn("Failed to cleanup backend resources", e);
            }
        }
    }

    /**
     * Build training context from request.
     */
    private MLTrainingContext buildContext(MLRequest request) {
        MLTrainingContext context = new MLTrainingContext();
        context.setRequest(request);
        context.setShouldCleanupStagingTable(request.getBackendConfig().isAutoCleanupStagingTables());
        return context;
    }

    /**
     * Extract source name from request for default model naming.
     * For database tables: returns table name
     * For CSV files: returns file name without extension
     */
    private String extractSourceName(MLRequest request) {
        MLSourceConfig sourceConfig = request.getSourceConfig();
        MLSourceType sourceType = sourceConfig.getSourceType();

        if (sourceType == MLSourceType.DATABASE_TABLE) {
            return sourceConfig.getTableSourceConfig().getTableName();
        } else if (sourceType == MLSourceType.FILE_SYSTEM) {
            String filePath = sourceConfig.getFileSourceConfig().getFilePath();
            if (filePath == null || filePath.isEmpty()) return "model";

            // Extract file name without path
            int lastSep = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
            String fileName = lastSep >= 0 ? filePath.substring(lastSep + 1) : filePath;

            // Remove extension
            int dotIndex = fileName.lastIndexOf('.');
            return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
        }

        return "model";
    }
}
