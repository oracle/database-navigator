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

package com.dbn.mcp.deploy;

import com.dbn.common.thread.Progress;
import com.dbn.connection.ConnectionAction;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.mcp.build.McpBuilderResult;
import com.dbn.mcp.model.McpServerDefinition;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.dbn.common.util.Messages.showErrorDialog;
import static com.dbn.common.util.Messages.showInfoDialog;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

/**
 * Orchestrates the two deployment operations, keeping Docker and JDBC work off the EDT and out
 * of Swing listeners. Each operation first re-checks that the build is actually deployable,
 * because the build result may have been produced before the deployment dialog was opened.
 */
@RequiredArgsConstructor
public class McpGraalDeployTask {
    // provisioning usually completes within a couple of minutes; poll generously before giving up
    private static final long ACTIVATION_POLL_INTERVAL_MILLIS = 5_000;
    private static final long ACTIVATION_TIMEOUT_MILLIS = 10 * 60 * 1_000L;
    private static final long POLL_SLEEP_STEP_MILLIS = 200;
    private static final @NonNls String STATE_ACTIVE = "ACTIVE";
    private static final @NonNls String STATE_FAILED = "FAILED";
    private static final @NonNls String STATE_PROVISIONING = "PROVISIONING";

    private final Project project;
    private final ConnectionRef connection;
    private final McpServerDefinition definition;
    private final McpBuilderResult result;

    /** Returns a localized problem description, or null when the build can be deployed. */
    private final Supplier<String> deployabilityValidator;

    /** Receives the image OCID resolved from the registry, so the dialog can fill it in. */
    private final Consumer<String> resolvedOcidHandler;

    public void buildAndPushImage(@NotNull McpGraalDeploymentInput input) {
        if (!verifyDeployable()) return;

        Path sourceProjectDir = result.getSourceDirectory();
        // modal, not background: launched from the modal deployment dialog, a background
        // indicator would sit in the status bar behind it and never be seen
        Progress.modal(project, null, true,
                txt("prc.mcp.title.DeployingToGraal"),
                txt("prc.mcp.text.BuildingGraalImage"),
                indicator -> {
            try {
                new McpGraalImagePublisher().publish(sourceProjectDir, definition, input, indicator, null);
                resolveImageOcid(input, indicator);
            } catch (ProcessCanceledException e) {
                throw e;
            } catch (Throwable e) {
                conditionallyLog(e);
                showErrorDialog(project, txt("msg.mcp.title.GraalDeployment"),
                        txt("msg.mcp.error.GraalImageBuildFailed"), e);
            }
        });
    }

    /**
     * Looks the pushed image up in the registry to obtain its OCID, which is what Graal requires.
     * A lookup failure is not fatal - the image is already published, so the user can still paste
     * the OCID from the OCI console and continue.
     */
    private void resolveImageOcid(McpGraalDeploymentInput input, ProgressIndicator indicator) {
        indicator.setText2(txt("prc.mcp.text.ResolvingImageOcid"));
        String imageName = input.getFullImageName();
        try {
            String ocid = new McpOciImageResolver().resolveImageOcid(input);
            if (ocid == null) {
                showInfoDialog(project, txt("msg.mcp.title.GraalDeployment"),
                        txt("msg.mcp.text.GraalImagePushed", imageName));
                return;
            }

            resolvedOcidHandler.accept(ocid);
            showInfoDialog(project, txt("msg.mcp.title.GraalDeployment"),
                    txt("msg.mcp.text.GraalImagePushedWithOcid", imageName, ocid));
        } catch (ProcessCanceledException e) {
            throw e;
        } catch (Throwable e) {
            conditionallyLog(e);
            // fall back to the manual path rather than failing an otherwise successful push
            showInfoDialog(project, txt("msg.mcp.title.GraalDeployment"),
                    txt("msg.mcp.text.GraalImagePushedOcidUnresolved", imageName));
        }
    }

    public void createApplication(@NotNull McpGraalDeploymentInput input) {
        if (!verifyDeployable()) return;

        ConnectionHandler connectionHandler = ConnectionRef.ensure(connection);
        ConnectionAction.invoke(txt("msg.mcp.title.GraalDeployment"), true, connectionHandler,
                action -> Progress.modal(project, connectionHandler, true,
                        txt("prc.mcp.title.DeployingToGraal"),
                        txt("prc.mcp.text.CreatingGraalApplication"),
                        indicator -> {
            try {
                McpGraalApplicationManager manager = new McpGraalApplicationManager(connection);
                manager.createApplication(input);
                reportActivation(manager, input, indicator);
            } catch (ProcessCanceledException e) {
                throw e;
            } catch (Throwable e) {
                conditionallyLog(e);
                showErrorDialog(project, txt("msg.mcp.title.GraalDeployment"),
                        txt("msg.mcp.error.GraalApplicationCreationFailed"), e);
            }
        }));
    }

    /**
     * Polls the newly created application until it becomes ACTIVE (reporting its endpoint), fails,
     * or the timeout elapses - keeping the progress indicator updated and honouring cancellation.
     * Cancelling only stops watching; the application keeps provisioning on the service.
     */
    private void reportActivation(
            McpGraalApplicationManager manager, McpGraalDeploymentInput input, ProgressIndicator indicator)
            throws SQLException {

        indicator.setText2(txt("prc.mcp.text.WaitingForGraalApplication"));
        String name = input.getApplicationName();
        long deadline = System.currentTimeMillis() + ACTIVATION_TIMEOUT_MILLIS;
        String lastState = null;

        while (System.currentTimeMillis() < deadline) {
            indicator.checkCanceled();
            McpGraalApplicationManager.ApplicationStatus status = manager.getApplicationStatus(name);
            if (status != null && status.lifecycleState() != null) {
                lastState = status.lifecycleState();
                indicator.setText2(txt("prc.mcp.text.GraalApplicationState", lastState));

                if (STATE_ACTIVE.equalsIgnoreCase(lastState)) {
                    String endpoint = status.endpoint() == null ? "-" : status.endpoint();
                    showInfoDialog(project, txt("msg.mcp.title.GraalDeployment"),
                            txt("msg.mcp.text.GraalApplicationActive", name, endpoint));
                    return;
                }
                if (STATE_FAILED.equalsIgnoreCase(lastState)) {
                    showErrorDialog(project, txt("msg.mcp.title.GraalDeployment"),
                            txt("msg.mcp.error.GraalApplicationFailedState", name));
                    return;
                }
            }
            sleep(ACTIVATION_POLL_INTERVAL_MILLIS, indicator);
        }

        // timed out while still provisioning: the application exists, it is just not ACTIVE yet
        showInfoDialog(project, txt("msg.mcp.title.GraalDeployment"),
                txt("msg.mcp.text.GraalApplicationPending", name,
                        lastState == null ? STATE_PROVISIONING : lastState));
    }

    private static void sleep(long millis, ProgressIndicator indicator) {
        long until = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < until) {
            indicator.checkCanceled();
            try {
                Thread.sleep(POLL_SLEEP_STEP_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ProcessCanceledException(e);
            }
        }
    }

    private boolean verifyDeployable() {
        @Nullable String problem = deployabilityValidator.get();
        if (problem == null) return true;

        showErrorDialog(project, txt("msg.mcp.title.GraalDeployment"), problem);
        return false;
    }
}
