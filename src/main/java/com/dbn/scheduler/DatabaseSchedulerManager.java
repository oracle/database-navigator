/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.scheduler;

import com.dbn.common.Priority;
import com.dbn.common.component.Components;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.outcome.Outcome;
import com.dbn.common.outcome.OutcomeHandlers;
import com.dbn.common.thread.Progress;
import com.dbn.common.thread.Threads;
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.database.interfaces.DatabaseSchedulerInterface;
import com.dbn.diagnostics.Diagnostics;
import com.dbn.scheduler.model.SchedulerJob;
import com.dbn.scheduler.model.SchedulerJobCancellationPolicy;
import com.dbn.scheduler.model.SchedulerJobCompletion;
import com.dbn.scheduler.model.SchedulerJobCompletionPolicy;
import com.dbn.scheduler.model.SchedulerJobMonitor;
import com.dbn.scheduler.model.SchedulerJobRequest;
import com.dbn.scheduler.model.SchedulerJobSnapshot;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

import static com.dbn.database.DatabaseFeature.SCHEDULER_JOBS;
import static com.dbn.nls.NlsResources.txt;

public class DatabaseSchedulerManager extends ProjectComponentBase {
    public static final String COMPONENT_NAME = "DBNavigator.Project.DatabaseSchedulerManager";

    public DatabaseSchedulerManager(@NotNull Project project) {
        super(project, COMPONENT_NAME);
    }

    public static DatabaseSchedulerManager getInstance(@NotNull Project project) {
        return Components.projectService(project, DatabaseSchedulerManager.class);
    }

    @NotNull
    public SchedulerJob submitJob(@NotNull ConnectionHandler connection, @NotNull SchedulerJobRequest request) throws SQLException {
        if (SCHEDULER_JOBS.isNotSupported(connection))
            throw new SQLFeatureNotSupportedException("Scheduler jobs are not supported for " + connection.getDatabaseType().getName() + " databases");

        String jobName = SchedulerJobs.newJobName(request.getNamePrefix());
        DatabaseInterfaceInvoker.execute(Priority.HIGH,
                getProject(), connection.getConnectionId(),
                conn -> connection.getSchedulerInterface().createJob(conn, jobName, request.getAction()));
        return new SchedulerJob(connection, jobName);
    }

    public void submitAndMonitor(
            @NotNull ConnectionHandler connection,
            @NotNull SchedulerJobRequest request,
            @NotNull SchedulerJobMonitor monitor,
            @NotNull OutcomeHandlers outcomeHandlers) throws SQLException {
        monitorJob(submitJob(connection, request), monitor, outcomeHandlers);
    }

    public void monitorJob(
            @NotNull SchedulerJob job,
            @NotNull SchedulerJobMonitor monitor,
            @NotNull OutcomeHandlers outcomeHandlers) {
        ConnectionHandler connection = job.getConnection().ensure();
        Progress.background(getProject(), connection, true, monitor.getTitle(), monitor.getInitialText(), progress -> {
            SchedulerJobSnapshot snapshot = SchedulerJobSnapshot.notFound();
            try {
                long startTime = System.currentTimeMillis();
                while (true) {
                    progress.checkCanceled();
                    snapshot = loadJobSnapshot(job);
                    progress.setText2(monitor.getStatusTextProvider().apply(snapshot));

                    if (snapshot.getStatus().isTerminal()) {
                        if (monitor.getCompletionPolicy() == SchedulerJobCompletionPolicy.DROP) {
                            dropJobQuietly(job, false);
                        }
                        outcomeHandlers.handle(createCompletionOutcome(job, snapshot));
                        return;
                    }

                    if (System.currentTimeMillis() - startTime >= monitor.getTimeoutMillis()) {
                        // monitoring is being abandoned - apply the same job treatment as user cancellation
                        // (STOP_AND_DROP cleans up the job, DETACH leaves it running server-side)
                        cancelJob(job, monitor.getCancellationPolicy());
                        throw new IllegalStateException(txt("msg.scheduler.error.JobTimedOut", job.getName()));
                    }
                    Threads.sleep(monitor.getPollIntervalMillis());
                }
            } catch (ProcessCanceledException e) {
                cancelJob(job, monitor.getCancellationPolicy());
                outcomeHandlers.handle(Outcome.warning().withData(new SchedulerJobCompletion(job, snapshot)));
            } catch (Exception e) {
                outcomeHandlers.handle(Outcome.failure()
                        .withException(e)
                        .withData(new SchedulerJobCompletion(job, snapshot)));
            }
        });
    }

    @NotNull
    public SchedulerJobSnapshot loadJobSnapshot(@NotNull SchedulerJob job) throws SQLException {
        ConnectionHandler connection = job.getConnection().ensure();
        return DatabaseInterfaceInvoker.load(Priority.LOW,
                getProject(), connection.getConnectionId(),
                conn -> connection.getSchedulerInterface().loadJobSnapshot(conn, job.getName()));
    }

    public void stopAndDropJob(@NotNull SchedulerJob job) throws SQLException {
        ConnectionHandler connection = job.getConnection().ensure();
        DatabaseInterfaceInvoker.execute(Priority.HIGH,
                getProject(), connection.getConnectionId(),
                conn -> {
                    DatabaseSchedulerInterface schedulerInterface = connection.getSchedulerInterface();
                    try {
                        schedulerInterface.stopJob(conn, job.getName());
                    } catch (SQLException e) {
                        // the job may have already completed/stopped - drop it regardless so it is never orphaned
                        Diagnostics.conditionallyLog(e);
                    }
                    schedulerInterface.dropJob(conn, job.getName(), true);
                });
    }

    private void cancelJob(SchedulerJob job, SchedulerJobCancellationPolicy policy) {
        if (policy != SchedulerJobCancellationPolicy.STOP_AND_DROP) return;
        try {
            stopAndDropJob(job);
        } catch (SQLException e) {
            Diagnostics.conditionallyLog(e);
        }
    }

    private void dropJobQuietly(SchedulerJob job, boolean force) {
        try {
            ConnectionHandler connection = job.getConnection().ensure();
            DatabaseInterfaceInvoker.execute(Priority.LOW,
                    getProject(), connection.getConnectionId(),
                    conn -> connection.getSchedulerInterface().dropJob(conn, job.getName(), force));
        } catch (SQLException e) {
            Diagnostics.conditionallyLog(e);
        }
    }

    @NotNull
    private static Outcome createCompletionOutcome(SchedulerJob job, SchedulerJobSnapshot snapshot) {
        SchedulerJobCompletion completion = new SchedulerJobCompletion(job, snapshot);
        if (snapshot.getStatus().isSuccessful()) return Outcome.success().withData(completion);

        String message = snapshot.getErrorNumber() == null ?
                txt("msg.scheduler.error.JobFailed", job.getName(), snapshot.getStatus()) :
                txt("msg.scheduler.error.JobFailedWithError", job.getName(), snapshot.getStatus(), snapshot.getErrorNumber());
        return Outcome.failure()
                .withException(new IllegalStateException(message))
                .withData(completion);
    }
}
