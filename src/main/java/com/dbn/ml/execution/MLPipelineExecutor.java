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
import com.dbn.ml.backend.dbms.DBMSFeatureAnalyzer;
import com.dbn.ml.backend.dbms.DBMSModelHandle;
import com.dbn.ml.backend.model.MLTrainingContext;
import com.dbn.ml.model.MLRequest;
import com.dbn.ml.model.MLResult;
import com.dbn.ml.model.MLTaskType;
import com.dbn.ml.model.source.MLSourceNames;
import com.dbn.object.common.DBObjectUtil;
import com.dbn.object.type.DBObjectType;
import com.dbn.scheduler.model.SchedulerJobRequest;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Executes ML training pipeline using Oracle DBMS_DATA_MINING.
 *
 * @author ayoub allali
 */
@Slf4j
public class MLPipelineExecutor {

    /**
     * Prepares training data and renders the CREATE_MODEL action to be scheduled.
     * Training itself is submitted and monitored through the scheduler framework.
     */
    public MLTrainingJobSubmission prepareTrainingJob(MLRequest request, ConnectionHandler connectionHandler) throws Exception {
        MLTrainingContext context = buildContext(request);
        DBMSBackend backend = new DBMSBackend(connectionHandler);
        SchedulerJobRequest jobRequest = backend.prepareTrainingJob(context);
        return new MLTrainingJobSubmission(context.getModelName(), jobRequest, context);
    }

    public MLResult completeAsync(MLTrainingJobSubmission submission, ConnectionHandler connectionHandler) throws Exception {
        MLTrainingContext context = submission.getContext();
        DBMSBackend backend = new DBMSBackend(connectionHandler);
        MLRequest request = context.getRequest();

        long startTime = context.getTrainingStartTime() > 0
                ? context.getTrainingStartTime()
                : System.currentTimeMillis();
        boolean replacementReady = false;

        try {
            DBMSModelHandle modelHandle = backend.loadModelHandle(context, submission.getModelName());
            MLResult result = buildResult(request, connectionHandler, context, backend, modelHandle, startTime);
            replacementReady = true;
            if (request.isModelReplacement()) {
                replaceModel(result, backend);
            }

            loadModelDetailViews(result, backend);
            refreshModelObjects(connectionHandler);
            return result;
        } catch (Exception e) {
            if (request.isModelReplacement() && !replacementReady) {
                try {
                    backend.dropModel(submission.getModelName());
                } catch (Exception cleanupError) {
                    e.addSuppressed(cleanupError);
                }
            }
            throw e;
        } finally {
            try {
                backend.cleanup(context);
            } catch (Exception e) {
                log.warn("Failed to cleanup backend resources", e);
            }
        }
    }

    private void replaceModel(
            MLResult result,
            DBMSBackend backend) throws SQLException {

        MLRequest request = result.getRequest();
        DBMSModelHandle modelHandle = result.getModelHandle();
        String modelToReplace = request.getModelToReplace();

        backend.dropModel(modelToReplace);
        backend.renameModel(modelHandle.getModelName(), modelToReplace);

        modelHandle.setModelName(modelToReplace);
        request.setModelToReplace(null);
        request.getTrainerConfig().setModelName(modelToReplace);
    }

    private void refreshModelObjects(ConnectionHandler connectionHandler) {
        DBObjectUtil.refreshUserObjects(connectionHandler.getConnectionId(), DBObjectType.AI_MODEL);
        DBObjectUtil.refreshUserObjects(connectionHandler.getConnectionId(), DBObjectType.VIEW);
    }

    private void loadModelDetailViews(MLResult result, DBMSBackend backend) {
        try {
            result.setModelDetailViews(backend.loadModelDetailViews(result.getModelName()));
        } catch (SQLException e) {
            log.warn("Failed to load model detail views", e);
        }
    }

    private MLResult buildResult(
            MLRequest request,
            ConnectionHandler connectionHandler,
            MLTrainingContext context,
            DBMSBackend backend,
            DBMSModelHandle modelHandle,
            long startTime) throws Exception {

        MLResult result = new MLResult(request.clone());
        result.setTaskType(context.getTaskType());
        result.setConnection(context.getConnection());
        result.setAlgorithmName(context.getAlgorithmName());
        result.setModelHandle(modelHandle);

        DBMSEvaluationResult evaluation = backend.evaluate(modelHandle, context);
        result.setEvaluationResult(evaluation);

        analyzeFeatures(connectionHandler, context, modelHandle, result);

        result.setTrainingDataSize(context.getTrainingDataSize());
        result.setTestingDataSize(context.getTestingDataSize());
        result.setFeatureCount(request.getFeatureConfig().getFeatureColumns().size());

        if (context.getTaskType() == MLTaskType.CLASSIFICATION) {
            result.setClassCount(modelHandle.getMetadata().getClassCount());
        } else {
            result.setOutputDimensions(modelHandle.getMetadata().getOutputDimensions());
        }

        if (context.getTaskType() == MLTaskType.CLASSIFICATION) {
            result.setLabelColumn(request.getFeatureConfig().getLabelColumn());
        } else {
            result.setLabelColumns(new ArrayList<>(request.getFeatureConfig().getLabelColumns()));
        }

        result.setSourceName(extractSourceName(request));
        result.setTrainingTimeMs(System.currentTimeMillis() - startTime);
        return result;
    }

    /**
     * Column analysis is supplementary - a database that cannot produce it (missing privileges,
     * an algorithm without prediction details) must not fail an otherwise successful training.
     */
    private void analyzeFeatures(
            ConnectionHandler connectionHandler,
            MLTrainingContext context,
            DBMSModelHandle modelHandle,
            MLResult result) {

        DBMSFeatureAnalyzer analyzer = new DBMSFeatureAnalyzer(connectionHandler);
        try {
            result.setFeatureImportance(analyzer.computeFeatureImportance(context));
        } catch (Exception e) {
            log.warn("Failed to compute feature importance - result will omit the table", e);
        }

        try {
            result.setAttributeContributions(analyzer.computeAttributeContribution(modelHandle));
        } catch (Exception e) {
            log.warn("Failed to compute attribute contribution - result will omit the table", e);
        }
    }

    private MLTrainingContext buildContext(MLRequest request) {
        MLTrainingContext context = new MLTrainingContext();
        context.setRequest(request);
        context.setShouldCleanupStagingTable(true);
        return context;
    }

    private String extractSourceName(MLRequest request) {
        String name = MLSourceNames.extractBaseName(request.getSourceConfig());
        return name != null ? name : "model";
    }
}
