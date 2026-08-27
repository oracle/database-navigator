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
import com.dbn.common.outcome.OutcomeHandler;
import com.dbn.common.outcome.OutcomeHandlers;
import com.dbn.common.outcome.OutcomeHandlersImpl;
import com.dbn.common.outcome.OutcomeType;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Progress;
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
import com.dbn.scheduler.DatabaseSchedulerManager;
import com.dbn.scheduler.model.SchedulerJob;
import com.dbn.scheduler.model.SchedulerJobCancellationPolicy;
import com.dbn.scheduler.model.SchedulerJobCompletionPolicy;
import com.dbn.scheduler.model.SchedulerJobMonitor;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.constantAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setConstantAttribute;
import static com.dbn.nls.NlsResources.txt;

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
        MLRequest request = getRequestTemplate(connection).clone();
        request.setTemplate(true);
        openToolbox(connection, request);
    }

    public void openToolbox(ConnectionHandler connection, MLRequest request) {
        try {
            Dialogs.show(() -> new MLToolboxDialog(connection, request));
        } catch (Exception e) {
            Messages.showErrorDialog(
                getProject(),
                txt("msg.machineLearning.title.MLToolboxError"),
                txt("msg.machineLearning.error.MLToolboxOpenFailed", e.getMessage())
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
                txt("prc.machineLearning.title.SubmittingTrainingJob"),
                txt("prc.machineLearning.text.PreparingTrainingData", algorithmName, sourceName),
                progress -> {
                    try {
                        MLPipelineExecutor executor = new MLPipelineExecutor();
                        MLTrainingJobSubmission submission = executor.prepareTrainingJob(requestSnapshot, connection);
                        String modelName = getResultModelName(submission);

                        DatabaseSchedulerManager schedulerManager = DatabaseSchedulerManager.getInstance(getProject());
                        SchedulerJob job = schedulerManager.submitJob(connection, submission.getJobRequest());
                        log.info("Training job {} submitted for model: {}", job.getName(), submission.getModelName());

                        Dispatch.run(() -> Messages.showInfoDialog(
                                getProject(),
                                txt("msg.machineLearning.title.TrainingJobSubmitted"),
                                txt("msg.machineLearning.info.TrainingJobSubmitted", modelName)
                        ));

                        schedulerManager.monitorJob(job,
                                createTrainingJobMonitor(modelName),
                                createTrainingOutcomeHandlers(executor, submission, connection));
                    } catch (Exception e) {
                        log.warn("Failed to submit training job", e);
                        Dispatch.run(() -> Messages.showErrorDialog(
                                getProject(),
                                txt("msg.machineLearning.title.TrainingJobSubmitFailed"),
                                txt("msg.machineLearning.error.ModelTrainingFailed", e.getMessage())
                        ));
                    }
                });
    }

    @NotNull
    private SchedulerJobMonitor createTrainingJobMonitor(String modelName) {
        return new SchedulerJobMonitor(
                txt("prc.machineLearning.title.MonitoringTrainingJob"),
                txt("prc.machineLearning.text.MonitoringTrainingJob", modelName),
                snapshot -> txt("prc.machineLearning.text.TrainingJobStatus", modelName, snapshot.getStatus().getName()),
                JOB_POLL_INTERVAL_MILLIS,
                JOB_POLL_TIMEOUT_MILLIS,
                SchedulerJobCompletionPolicy.DROP,
                // cancelling the monitor must not abort training - it continues on the database server
                SchedulerJobCancellationPolicy.DETACH);
    }

    @NotNull
    private OutcomeHandlers createTrainingOutcomeHandlers(
            MLPipelineExecutor executor,
            MLTrainingJobSubmission submission,
            ConnectionHandler connection) {

        String modelName = getResultModelName(submission);
        OutcomeHandlers outcomeHandlers = new OutcomeHandlersImpl();
        outcomeHandlers.addHandler(OutcomeType.SUCCESS,
                (OutcomeHandler.HighPriority) outcome -> completeTrainingJob(executor, submission, connection));
        outcomeHandlers.addHandler(OutcomeType.FAILURE, (OutcomeHandler.HighPriority) outcome -> {
            log.warn("Async training monitor failed for model {}", modelName, outcome.getException());
            Dispatch.run(() -> Messages.showErrorDialog(
                    getProject(),
                    txt("msg.machineLearning.title.TrainingMonitoringFailed"),
                    txt("msg.machineLearning.error.TrainingMonitoringFailed", modelName, outcome.getMessage())
            ));
        });
        return outcomeHandlers;
    }

    private void completeTrainingJob(
            MLPipelineExecutor executor,
            MLTrainingJobSubmission submission,
            ConnectionHandler connection) {

        String modelName = getResultModelName(submission);
        try {
            MLResult result = executor.completeAsync(submission, connection);
            Dispatch.run(() -> {
                showResultInExecutionManager(result);
                Messages.showInfoDialog(
                        getProject(),
                        txt("msg.machineLearning.title.TrainingCompleted"),
                        txt("msg.machineLearning.info.TrainingCompleted", modelName)
                );
            });
        } catch (Exception e) {
            log.warn("Failed to finalize training result for model {}", modelName, e);
            Dispatch.run(() -> Messages.showErrorDialog(
                    getProject(),
                    txt("msg.machineLearning.title.TrainingMonitoringFailed"),
                    txt("msg.machineLearning.error.TrainingMonitoringFailed", modelName, e.getMessage())
            ));
        }
    }

    private String getResultModelName(MLTrainingJobSubmission submission) {
        MLRequest request = submission.getContext().getRequest();
        return request.isModelReplacement() ? request.getModelToReplace() : submission.getModelName();
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
        String name = Naming.nextNumberedIdentifier(txt("app.machineLearning.title.MLTrainingResult"), true, () -> existingNames);
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
