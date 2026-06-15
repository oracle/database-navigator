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

package com.dbn.ml;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.Components;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Progress;
import com.dbn.common.thread.Threads;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Messages;
import com.dbn.common.util.Naming;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.config.ConnectionConfigListener;
import com.dbn.execution.ExecutionManager;
import com.dbn.ml.execution.MLPipelineExecutor;
import com.dbn.ml.execution.MLTrainingJobSubmission;
import com.dbn.ml.model.MLRequest;
import com.dbn.ml.model.MLResult;
import com.dbn.ml.model.source.MLSourceNames;
import com.dbn.ml.result.MLExecutionResult;
import com.dbn.ml.ui.MLToolboxDialog;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.dbn.common.options.setting.Settings.*;

@Slf4j
@State(
        name = DatabaseMLManager.COMPONENT_NAME,
        storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class DatabaseMLManager extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.DatabaseMLManager";
    private static final long JOB_POLL_INTERVAL_MILLIS = 3000;
    private static final long JOB_POLL_TIMEOUT_MILLIS = TimeUnit.HOURS.toMillis(8);

    private final Map<ConnectionId, MLRequest> requestTemplates = new ConcurrentHashMap<>();

    public DatabaseMLManager(@NotNull Project project) {
        super(project, COMPONENT_NAME);
        ProjectEvents.subscribe(project, this, ConnectionConfigListener.TOPIC, connectionConfigListener());
    }

    @NotNull
    private ConnectionConfigListener connectionConfigListener() {
        return new ConnectionConfigListener() {
            @Override
            public void connectionRemoved(ConnectionId connectionId) {
                requestTemplates.remove(connectionId);
            }
        };
    }

    public static DatabaseMLManager getInstance(Project project) {
        return Components.projectService(project, DatabaseMLManager.class);
    }

    public void openToolbox(ConnectionHandler connection) {
        try {
            MLRequest request = getRequestTemplate(connection);
            Dialogs.show(() -> new MLToolboxDialog(connection, request));
        } catch (Exception e) {
            Messages.showErrorDialog(
                getProject(),
                "ML Toolbox Error",
                "Failed to open ML Toolbox: " + e.getMessage()
            );
            log.warn("Failed to open ML Toolbox", e);
        }
    }

    @NotNull
    public MLRequest getRequestTemplate(ConnectionHandler connection) {
        ConnectionId connectionId = connection.getConnectionId();
        return requestTemplates.computeIfAbsent(connectionId, id -> {
            MLRequest request = new MLRequest(id);
            request.setTemplate(true);
            request.initialize(connection.getUserSchemaId());
            return request;
        });
    }

    public void setRequestTemplate(ConnectionId connectionId, MLRequest request) {
        requestTemplates.put(connectionId, request);
    }

    /**
     * Submits model training as an Oracle Scheduler job.
     * Data prep runs briefly in background, then CREATE_MODEL is submitted to Oracle
     * and returns immediately — Oracle continues training server-side.
     */
    public void trainModelAsync(MLRequest request, ConnectionHandler connection) {
        MLRequest requestSnapshot = request.clone();
        requestSnapshot.setTemplate(false);

        String sourceName = getSourceDisplayName(requestSnapshot);
        String algorithmName = requestSnapshot.getTrainerConfig().getTrainerType().getName();

        Progress.prompt(
                getProject(),
                connection,
                true,
                "Submitting Training Job",
                "Preparing data for " + algorithmName + " on \"" + sourceName + "\"...",
                progress -> {
                    try {
                        MLPipelineExecutor executor = new MLPipelineExecutor();
                        MLTrainingJobSubmission submission = executor.submitAsync(requestSnapshot, connection);
                        String modelName = submission.getModelName();
                        log.info("Training job submitted for model: {}", modelName);
                        Dispatch.run(() -> Messages.showInfoDialog(
                                getProject(),
                                "Training Job Submitted",
                                "Model \"" + modelName + "\" has been submitted to Oracle Scheduler.\n" +
                                "Training continues on the database server."
                        ));
                        monitorTrainingJob(executor, submission, connection);
                    } catch (Exception e) {
                        log.warn("Failed to submit training job", e);
                        Dispatch.run(() -> Messages.showErrorDialog(
                                getProject(),
                                "Failed to Submit Training Job",
                                "An error occurred:\n" + e.getMessage()
                        ));
                    }
                });
    }

    private void monitorTrainingJob(
            MLPipelineExecutor executor,
            MLTrainingJobSubmission submission,
            ConnectionHandler connection) {

        String modelName = submission.getModelName();
        String jobName = submission.getJobName();

        Progress.background(
                getProject(),
                connection,
                true,
                "Waiting for Training Job",
                "Monitoring training job for model \"" + modelName + "\"...",
                progress -> {
                    boolean cancelled = false;
                    try {
                        waitForTrainingCompletion(executor, connection, jobName, progress);
                        MLResult result = executor.completeAsync(submission, connection);
                        Dispatch.run(() -> {
                            showResultInExecutionManager(result);
                            Messages.showInfoDialog(
                                    getProject(),
                                    "Training Completed",
                                    "Model \"" + modelName + "\" finished training.\nDashboard result is now available."
                            );
                        });
                    } catch (ProcessCanceledException e) {
                        cancelled = true;
                        log.info("Training monitor cancelled for model: {}", modelName);
                    } catch (Exception e) {
                        log.warn("Async training monitor failed for model {}", modelName, e);
                        Dispatch.run(() -> Messages.showErrorDialog(
                                getProject(),
                                "Training Monitoring Failed",
                                "Model \"" + modelName + "\" could not be finalized:\n" + e.getMessage()
                        ));
                    } finally {
                        if (!cancelled) {
                            dropSchedulerJobQuietly(executor, connection, jobName);
                        }
                    }
                });
    }

    private void waitForTrainingCompletion(
            MLPipelineExecutor executor,
            ConnectionHandler connection,
            String jobName,
            ProgressIndicator progress) throws Exception {

        if (jobName == null || jobName.isBlank()) {
            throw new IllegalStateException("Missing scheduler job name for async training");
        }

        long startTime = System.currentTimeMillis();
        while (true) {
            progress.checkCanceled();

            String state = normalizeStatus(executor.getSchedulerJobState(connection, jobName));
            String runStatus = normalizeStatus(executor.getSchedulerJobRunStatus(connection, jobName));
            String statusText = statusText(state, runStatus);

            progress.setText("Training status (" + jobName + "): " + statusText);

            if (isJobSucceeded(state, runStatus)) {
                return;
            }
            if (isJobFailed(state, runStatus)) {
                throw new IllegalStateException("Scheduler job failed with status: " + statusText);
            }

            if (System.currentTimeMillis() - startTime > JOB_POLL_TIMEOUT_MILLIS) {
                throw new IllegalStateException("Timed out while waiting for scheduler job completion");
            }

            Threads.sleep(JOB_POLL_INTERVAL_MILLIS);
        }
    }

    private static String normalizeStatus(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private static String statusText(String state, String runStatus) {
        if (runStatus != null && !runStatus.isEmpty()) return runStatus;
        if (state != null && !state.isEmpty()) return state;
        return "PENDING";
    }

    private static boolean isJobSucceeded(String state, String runStatus) {
        return "SUCCEEDED".equals(runStatus) || "SUCCEEDED".equals(state);
    }

    private static boolean isJobFailed(String state, String runStatus) {
        return "FAILED".equals(runStatus)
                || "FAILED".equals(state)
                || "BROKEN".equals(state)
                || "STOPPED".equals(state);
    }

    private void dropSchedulerJobQuietly(
            MLPipelineExecutor executor,
            ConnectionHandler connection,
            String jobName) {

        if (jobName == null || jobName.isBlank()) return;
        try {
            executor.dropSchedulerJob(connection, jobName);
        } catch (Exception e) {
            log.debug("Could not drop scheduler job {}", jobName, e);
        }
    }

    private String getSourceDisplayName(MLRequest request) {
        return MLSourceNames.getDisplayName(request.getSourceConfig());
    }

    /**
     * Shows the training result in the Execution Manager panel.
     */
    private void showResultInExecutionManager(MLResult result) {
        ExecutionManager executionManager = ExecutionManager.getInstance(getProject());
        Set<String> existingNames = executionManager.getExecutionResultNames(MLExecutionResult.class);
        String name = Naming.nextNumberedIdentifier("ML Training Result", true, () -> existingNames);
        MLExecutionResult executionResult = new MLExecutionResult(result, name);
        executionManager.addExecutionResult(executionResult);
    }

    @Nullable
    @Override
    public Element getComponentState() {
        Element element = new Element("state");
        Element templatesElement = newElement(element, "request-templates");
        for (Map.Entry<ConnectionId, MLRequest> entry : requestTemplates.entrySet()) {
            Element templateElement = newElement(templatesElement, "request-template");
            setConstantAttribute(templateElement, "connection-id", entry.getKey());
            entry.getValue().writeState(templateElement);
        }
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        Element templatesElement = element.getChild("request-templates");
        if (templatesElement == null) return;
        for (Element templateElement : childrenOf(templatesElement, "request-template")) {
            ConnectionId connectionId = constantAttribute(templateElement, "connection-id", ConnectionId.class);
            if (connectionId != null) {
                MLRequest request = new MLRequest(connectionId);
                request.setTemplate(true);
                request.readState(templateElement);
                requestTemplates.put(connectionId, request);
            }
        }
    }
}
