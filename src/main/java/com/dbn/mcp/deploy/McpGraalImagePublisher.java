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

import com.dbn.mcp.build.McpServerConfigBuilder;
import com.dbn.mcp.model.McpServerDefinition;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.Key;
import com.intellij.util.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.dbn.nls.NlsResources.txt;

/**
 * Builds the credential-free linux/amd64 deployment image and pushes it to OCIR. The native
 * compilation runs inside the image build itself, in the GraalOS Java SDK stage of
 * Dockerfile.graal, so nothing needs a local GraalVM or JDK.
 * <p>
 * Authentication to the registry is never handled here: the user establishes it out of band with
 * "docker login" / "podman login", so no token or password ever passes through the plugin.
 */
@Slf4j
final class McpGraalImagePublisher {
    /** The documented Graal environment accepts this platform only. */
    private static final @NonNls String TARGET_PLATFORM = "linux/amd64";
    private static final @NonNls String DOCKERFILE_GRAAL = "Dockerfile.graal";
    private static final @NonNls String GRAAL_CONFIG_FILE = "mcp-config-graal.yaml";

    private static final long PROCESS_POLL_INTERVAL_MILLIS = 100;
    private static final int OUTPUT_TAIL_LIMIT = 4000;

    /**
     * The native compilation happens inside the image build itself (Dockerfile.graal is a
     * multistage build whose first stage is the GraalOS Java SDK), so publishing is just build
     * and push - no separate compile step, no volume mounts, and nothing to verify on the host.
     */
    void publish(
            @NotNull Path sourceProjectDir,
            @NotNull McpServerDefinition definition,
            @NotNull McpGraalDeploymentInput input,
            @NotNull ProgressIndicator indicator,
            @Nullable Consumer<String> outputHandler) throws IOException {

        @NonNls String runtime = McpContainerRuntimeSupport.locateContainerRuntime();
        String imageName = input.getFullImageName();

        writeGraalConfig(sourceProjectDir, definition);

        indicator.setText2(txt("prc.mcp.text.BuildingGraalImage"));
        runImageBuild(runtime, sourceProjectDir, imageName, indicator, outputHandler);

        indicator.setText2(txt("prc.mcp.text.PushingImageToOcir"));
        runImagePush(runtime, sourceProjectDir, imageName, indicator, outputHandler);
    }

    /**
     * Writes the deployment configuration next to the Dockerfile. It carries tool definitions
     * only - the deployed image must contain no connection string, wallet or credentials.
     */
    private void writeGraalConfig(Path sourceProjectDir, McpServerDefinition definition) throws IOException {
        String yaml = McpServerConfigBuilder.buildGraalDeploymentConfig(definition);
        Files.writeString(sourceProjectDir.resolve(GRAAL_CONFIG_FILE), yaml, StandardCharsets.UTF_8);
    }

    private void runImageBuild(
            @NonNls String runtime, Path projectDir, String imageName, ProgressIndicator indicator, Consumer<String> out) throws IOException {

        GeneralCommandLine commandLine = new GeneralCommandLine(
                runtime, "build",
                "--platform", TARGET_PLATFORM,
                "-t", imageName,
                "-f", DOCKERFILE_GRAAL,
                ".").withWorkDirectory(projectDir.toFile());

        runProcess(commandLine, indicator, out, "msg.mcp.exception.GraalImageBuildFailed");
    }

    private void runImagePush(
            @NonNls String runtime, Path projectDir, String imageName, ProgressIndicator indicator, Consumer<String> out) throws IOException {

        GeneralCommandLine commandLine = new GeneralCommandLine(runtime, "push", imageName)
                .withWorkDirectory(projectDir.toFile());

        runProcess(commandLine, indicator, out, "msg.mcp.exception.OcirPushFailed");
    }

    /**
     * Runs an external command, streaming its output and honouring cancellation. Modelled on the
     * Maven build manager's process handling, but built directly on a process handler because no
     * Maven runner is involved here.
     */
    private void runProcess(
            GeneralCommandLine commandLine,
            ProgressIndicator indicator,
            @Nullable Consumer<String> outputHandler,
            @NonNls String failureMessageKey) throws IOException {

        log.info("Running deployment command: {}", commandLine.getCommandLineString());

        OSProcessHandler processHandler;
        try {
            processHandler = new OSProcessHandler(commandLine);
        } catch (ExecutionException e) {
            throw new IOException(txt("msg.mcp.exception.DeploymentProcessStartFailed", e.getMessage()), e);
        }

        StringBuilder output = new StringBuilder();
        CountDownLatch finished = new CountDownLatch(1);
        AtomicInteger exitCode = new AtomicInteger(Integer.MIN_VALUE);

        processHandler.addProcessListener(new ProcessListener() {
            @Override
            public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
                String text = event.getText();
                if (text == null || text.isEmpty()) return;

                synchronized (output) {
                    output.append(text);
                }
                if (outputHandler != null && !text.isBlank()) {
                    String source =
                            outputType == ProcessOutputTypes.STDERR ? "STDERR" :
                            outputType == ProcessOutputTypes.STDOUT ? "STDOUT" : "SYSTEM";
                    for (String line : text.split("\\R")) {
                        if (!line.isBlank()) outputHandler.consume("[" + source + "] " + line);
                    }
                }
            }

            @Override
            public void processTerminated(@NotNull ProcessEvent event) {
                exitCode.set(event.getExitCode());
                finished.countDown();
            }
        });

        processHandler.startNotify();
        waitForCompletion(processHandler, indicator, finished);

        if (exitCode.get() != 0) {
            throw new IOException(txt(failureMessageKey, tail(output)));
        }
    }

    private void waitForCompletion(
            OSProcessHandler processHandler, ProgressIndicator indicator, CountDownLatch finished) throws IOException {

        while (finished.getCount() > 0) {
            if (indicator.isCanceled()) {
                if (!processHandler.isProcessTerminated()) processHandler.destroyProcess();
                throw new IOException(txt("msg.mcp.exception.DeploymentCancelled"));
            }
            try {
                if (finished.await(PROCESS_POLL_INTERVAL_MILLIS, TimeUnit.MILLISECONDS)) return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException(txt("msg.mcp.exception.DeploymentInterrupted"), e);
            }
        }
    }

    private static String tail(StringBuilder builder) {
        synchronized (builder) {
            if (builder.isEmpty()) return txt("msg.mcp.placeholder.NoOutput");
            int from = Math.max(0, builder.length() - OUTPUT_TAIL_LIMIT);
            return builder.substring(from);
        }
    }
}
