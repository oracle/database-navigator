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
import com.dbn.object.DBSchema;
import com.dbn.object.DBView;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.type.DBObjectType;
import com.dbn.ml.backend.dbms.DBMSBackend;
import com.dbn.ml.backend.dbms.DBMSEvaluationResult;
import com.dbn.ml.backend.dbms.DBMSModelHandle;
import com.dbn.ml.backend.model.MLTrainingContext;
import com.dbn.ml.model.*;
import com.dbn.ml.model.source.MLSourceNames;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Objects;

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
        long startTime = System.currentTimeMillis();

        try {
            DBMSModelHandle modelHandle = backend.train(context);
            return buildResult(request, connectionHandler, context, backend, modelHandle, startTime);

        } finally {
            try {
                backend.cleanup(context);
            } catch (Exception e) {
                log.warn("Failed to cleanup backend resources", e);
            }
        }
    }

    /**
     * Prepares training data and submits CREATE_MODEL as an Oracle Scheduler job.
     * Returns immediately with a pending job descriptor — training continues server-side.
     */
    /**
     * Prepares training data and submits CREATE_MODEL as an Oracle Scheduler job.
     * Returns the model name. Training continues on the DB server — client can disconnect.
     */
    public MLTrainingJobSubmission submitAsync(MLRequest request, ConnectionHandler connectionHandler) throws Exception {
        MLTrainingContext context = buildContext(request);
        DBMSBackend backend = new DBMSBackend(connectionHandler);
        String modelName = backend.submitAsync(context);
        String jobName = Objects.requireNonNullElse(context.getSchedulerJobName(), "");
        return new MLTrainingJobSubmission(modelName, jobName, context);
    }

    public MLResult completeAsync(MLTrainingJobSubmission submission, ConnectionHandler connectionHandler) throws Exception {
        MLTrainingContext context = submission.getContext();
        DBMSBackend backend = new DBMSBackend(connectionHandler);

        long startTime = context.getTrainingStartTime() > 0
                ? context.getTrainingStartTime()
                : System.currentTimeMillis();

        try {
            DBMSModelHandle modelHandle = backend.loadModelHandle(context, submission.getModelName());
            return buildResult(context.getRequest(), connectionHandler, context, backend, modelHandle, startTime);
        } finally {
            try {
                backend.cleanup(context);
            } catch (Exception e) {
                log.warn("Failed to cleanup backend resources", e);
            }
        }
    }

    public String getSchedulerJobState(ConnectionHandler connectionHandler, String jobName) throws Exception {
        DBMSBackend backend = new DBMSBackend(connectionHandler);
        return backend.getSchedulerJobState(jobName);
    }

    public String getSchedulerJobRunStatus(ConnectionHandler connectionHandler, String jobName) throws Exception {
        DBMSBackend backend = new DBMSBackend(connectionHandler);
        return backend.getSchedulerJobRunStatus(jobName);
    }

    public void dropSchedulerJob(ConnectionHandler connectionHandler, String jobName) throws Exception {
        DBMSBackend backend = new DBMSBackend(connectionHandler);
        backend.dropSchedulerJob(jobName);
    }

    private MLResult buildResult(
            MLRequest request,
            ConnectionHandler connectionHandler,
            MLTrainingContext context,
            DBMSBackend backend,
            DBMSModelHandle modelHandle,
            long startTime) throws Exception {

        MLResult result = new MLResult();
        result.setTaskType(context.getTaskType());
        result.setConnection(context.getConnection());
        result.setAlgorithmName(context.getAlgorithmName());
        result.setModelHandle(modelHandle);

        // Oracle creates DM$V* views when a model is trained - reload schema views so they appear in the browser.
        DBSchema schema = connectionHandler.getUserSchema();
        if (schema != null) {
            DBObjectList<DBView> viewList = schema.getChildObjectList(DBObjectType.VIEW);
            if (viewList != null) viewList.reloadInBackground();
        }

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
