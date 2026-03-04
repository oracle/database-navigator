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
import com.dbn.ml.backend.dbms.DBMSBackend;
import com.dbn.ml.backend.dbms.DBMSEvaluationResult;
import com.dbn.ml.backend.dbms.DBMSModelHandle;
import com.dbn.ml.backend.model.MLTrainingContext;
import com.dbn.ml.model.*;
import com.dbn.ml.model.source.MLSourceConfig;
import com.dbn.ml.model.source.MLSourceType;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

/**
 * Executes ML training pipeline using Oracle DBMS_DATA_MINING.
 *
 * @author ayoub allali
 */
@Slf4j
public class MLPipelineExecutor {

    /**
     * Execute ML training pipeline.
     *
     * @param request MLRequest containing all configuration
     * @param connectionHandler Connection handler for database operations
     * @return MLResult containing trained model and evaluation metrics
     * @throws Exception if training fails
     */
    public MLResult execute(MLRequest request, ConnectionHandler connectionHandler) throws Exception {
        MLTrainingContext context = buildContext(request);
        DBMSBackend backend = new DBMSBackend(connectionHandler);

        MLResult result = new MLResult();
        result.setTaskType(context.getTaskType());
        result.setConnection(context.getConnection());
        result.setAlgorithmName(context.getAlgorithmName());

        long startTime = System.currentTimeMillis();

        try {
            DBMSModelHandle modelHandle = backend.train(context);
            result.setModelHandle(modelHandle);

            DBMSEvaluationResult evaluation = backend.evaluate(modelHandle, context);
            result.setEvaluationResult(evaluation);

            result.setTrainingDataSize(context.getTrainingDataSize());
            result.setTestingDataSize(context.getTestingDataSize());
            result.setFeatureCount(request.getFeatureConfig().getFeatureColumns().size());

            if (context.getTaskType() == MLTaskType.CLASSIFICATION) {
                result.setClassCount(modelHandle.getMetadata().getClassCount());
            } else {
                result.setOutputDimensions(modelHandle.getMetadata().getOutputDimensions());
            }

            result.setFeatureColumns(new ArrayList<>(request.getFeatureConfig().getFeatureColumns()));

            if (context.getTaskType() == MLTaskType.CLASSIFICATION) {
                result.setLabelColumn(request.getFeatureConfig().getLabelColumn());
            } else {
                result.setLabelColumns(new ArrayList<>(request.getFeatureConfig().getLabelColumns()));
            }

            result.setSourceName(extractSourceName(request));
            result.setTrainingTimeMs(System.currentTimeMillis() - startTime);

            return result;

        } finally {
            try {
                backend.cleanup(context);
            } catch (Exception e) {
                log.warn("Failed to cleanup backend resources", e);
            }
        }
    }

    private MLTrainingContext buildContext(MLRequest request) {
        MLTrainingContext context = new MLTrainingContext();
        context.setRequest(request);
        context.setShouldCleanupStagingTable(true);
        return context;
    }

    private String extractSourceName(MLRequest request) {
        MLSourceConfig sourceConfig = request.getSourceConfig();
        MLSourceType sourceType = sourceConfig.getSourceType();

        if (sourceType == MLSourceType.DATABASE_TABLE) {
            return sourceConfig.getTableSourceConfig().getTableName();
        } else if (sourceType == MLSourceType.FILE_SYSTEM) {
            String filePath = sourceConfig.getFileSourceConfig().getFilePath();
            if (filePath == null || filePath.isEmpty()) return "model";

            int lastSep = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
            String fileName = lastSep >= 0 ? filePath.substring(lastSep + 1) : filePath;

            int dotIndex = fileName.lastIndexOf('.');
            return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
        }

        return "model";
    }
}
