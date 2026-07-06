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

package com.dbn.mcp.build;

import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.thread.Read;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.Key;
import com.intellij.util.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.execution.MavenRunner;
import org.jetbrains.idea.maven.execution.MavenRunnerParameters;
import org.jetbrains.idea.maven.execution.MavenRunnerSettings;
import org.jetbrains.idea.maven.project.MavenGeneralSettings;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.server.MavenDistribution;
import org.jetbrains.idea.maven.server.MavenDistributionsCache;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.dbn.common.component.Components.optionalProjectService;
import static com.dbn.nls.NlsResources.txt;

@Slf4j
public class McpMavenBuildManager extends ProjectComponentBase {
    private static final String COMPONENT_NAME = "DBNavigator.Project.McpMavenBuildManager";
    private static final long PROCESS_ATTACH_TIMEOUT_MILLIS = 30_000;

    private McpMavenBuildManager(@NotNull Project project) {
        super(project, COMPONENT_NAME);
    }

    @Nullable
    public static McpMavenBuildManager getInstance(@NotNull Project project) {
        return optionalProjectService(project, McpMavenBuildManager.class);
    }

    public boolean isRuntimeAvailable() {
        try {
            MavenDistribution distribution = resolveSettingsDistribution();
            boolean valid = distribution.isValid();
            if (!valid) {
                log.warn("Maven distribution is not valid: {}", describeDistribution(distribution));
            }
            return valid;
        } catch (Throwable e) {
            log.warn("Could not resolve Maven runtime from Maven plugin API", e);
            return false;
        }
    }

    public void runBuild(@NotNull Path projectDir, @NotNull List<String> goals, @NotNull ProgressIndicator indicator, Consumer<String> outputHandler) throws IOException {
        indicator.setText2(txt("prc.mcp.text.RunningMavenBuild"));

        Project project = getProject();
        MavenDistribution distribution = resolveSettingsDistribution();
        if (!distribution.isValid()) {
            throw new IOException(txt("msg.mcp.exception.MavenRuntimeInvalid", describeDistribution(distribution)));
        }

        log.info("POM: {}", projectDir.resolve("pom.xml"));
        log.info("Working dir: {}", projectDir);
        log.info("Maven distribution: {}", describeDistribution(distribution));

        MavenRunnerParameters parameters = new MavenRunnerParameters(
                true,
                projectDir.toAbsolutePath().toString(),
                "pom.xml",
                goals,
                List.of());

        StringBuilder output = new StringBuilder();

        AtomicReference<ProcessHandler> processRef = new AtomicReference<>();
        AtomicInteger exitCode = new AtomicInteger(Integer.MIN_VALUE);
        CountDownLatch finished = new CountDownLatch(1);

        Consumer<? super ProcessHandler> processConsumer = processHandler -> {
            processRef.set(processHandler);
            processHandler.addProcessListener(new ProcessListener() {
                @Override
                public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
                    String text = event.getText();
                    if (text == null || text.isEmpty()) return;

                    // append verbatim: the console emits sub-line fragments, raw concatenation
                    // reconstructs the true line structure of the Maven output
                    synchronized (output) {
                        output.append(text);
                    }

                    if (outputHandler != null && !text.isBlank()) {
                        String source =
                                outputType == ProcessOutputTypes.STDERR ? "STDERR" :
                                outputType == ProcessOutputTypes.STDOUT ? "STDOUT" : "SYSTEM";
                        for (String line : text.split("\\R")) {
                            if (!line.isBlank()) {
                                outputHandler.consume("[" + source + "] " + line);
                            }
                        }
                    }
                }

                @Override
                public void processTerminated(@NotNull ProcessEvent event) {
                    exitCode.set(event.getExitCode());
                    finished.countDown();
                }
            });

            // If the process already terminated before listener attachment completed, finalize immediately.
            if (processHandler.isProcessTerminated()) {
                try {
                    exitCode.set(processHandler.getExitCode());
                } catch (Throwable ignored) {
                }
                finished.countDown();
            }
        };

        MavenProjectsManager projectsManager = MavenProjectsManager.getInstance(project);
        MavenGeneralSettings generalSettings = projectsManager.getGeneralSettings().clone();
        MavenRunnerSettings runnerSettings = MavenRunner.getInstance(project).getSettings().clone();
        alignToolchainEnvironment(runnerSettings);
        boolean success = MavenRunner.getInstance(project).runBatch(
                List.of(parameters),
                generalSettings,
                runnerSettings,
                "Build MCP Server",
                indicator,
                processConsumer,
                true);

        if (!success) {
            throw new IllegalStateException(txt("msg.mcp.exception.MavenBuildStartFailed"));
        }

        waitForCompletion(indicator, processRef, finished, exitCode, distribution, output);
        if (exitCode.get() != 0) {
            String errors = output.toString().lines()
                    .filter(line -> line.contains("[ERROR]") || line.contains("error:"))
                    .collect(Collectors.joining("\n"));
            log.error("Maven build output:\n{}", output);
            throw new IllegalStateException(txt("msg.mcp.exception.MavenBuildFailed", errors.isBlank() ? output.toString() : errors));
        }
    }

    /**
     * Build plugins like native-maven-plugin resolve their toolchain from GRAALVM_HOME/JAVA_HOME
     * rather than from the JVM running Maven. Align those variables with the configured runner
     * JRE so the build does not silently pick up whatever the IDE process inherited from the shell.
     */
    private static void alignToolchainEnvironment(MavenRunnerSettings runnerSettings) {
        String jreName = runnerSettings.getJreName();
        if (jreName.startsWith("#")) return; // environment-based JRE macros: keep inherited environment

        Sdk sdk = Read.call(() -> ProjectJdkTable.getInstance().findJdk(jreName));
        String jdkHome = sdk == null ? null : sdk.getHomePath();
        if (jdkHome == null) return;

        Map<String, String> environment = new HashMap<>(runnerSettings.getEnvironmentProperties());
        environment.put("JAVA_HOME", jdkHome);
        environment.put("GRAALVM_HOME", jdkHome);
        runnerSettings.setEnvironmentProperties(environment);
    }

    private static void waitForCompletion(
            @NotNull ProgressIndicator indicator,
            AtomicReference<ProcessHandler> processRef,
            CountDownLatch finished,
            AtomicInteger exitCode,
            MavenDistribution distribution,
            StringBuilder output) throws IOException {
        long startedAt = System.currentTimeMillis();
        while (finished.getCount() > 0) {
            indicator.setText2(txt("prc.mcp.text.RunningMavenBuild"));

            if (indicator.isCanceled()) {
                ProcessHandler processHandler = processRef.get();
                if (processHandler != null && !processHandler.isProcessTerminated()) {
                    processHandler.destroyProcess();
                }
                throw new IOException(txt("msg.mcp.exception.MavenBuildCancelled"));
            }

            ProcessHandler processHandler = processRef.get();
            if (processHandler != null && processHandler.isProcessTerminated()) {
                try {
                    exitCode.set(processHandler.getExitCode());
                } catch (Throwable ignored) {
                }
                finished.countDown();
                return;
            }

            if (processRef.get() == null && System.currentTimeMillis() - startedAt > PROCESS_ATTACH_TIMEOUT_MILLIS) {
                throw new IOException(txt("msg.mcp.exception.MavenProcessStartTimedOut",
                        PROCESS_ATTACH_TIMEOUT_MILLIS / 1000,
                        describeDistribution(distribution),
                        tail(output, 400)));
            }

            try {
                if (finished.await(100, TimeUnit.MILLISECONDS)) {
                    return;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException(txt("msg.mcp.exception.MavenBuildInterrupted"), e);
            }
        }
    }

    private MavenDistribution resolveSettingsDistribution() {
        return MavenDistributionsCache.getInstance(getProject()).getSettingsDistribution();
    }

    private static String describeDistribution(MavenDistribution distribution) {
        String version = distribution.getVersion();
        return distribution.getName() + " @ " + distribution.getMavenHome() + " (version: " + (version == null ? "unknown" : version) + ")";
    }

    private static String tail(StringBuilder sb, int maxChars) {
        if (sb == null || sb.isEmpty()) return txt("msg.mcp.placeholder.NoOutput");
        int len = sb.length();
        int from = Math.max(0, len - maxChars);
        String text = sb.substring(from, len).replaceAll("\\s+", " ").trim();
        return text.isEmpty() ? txt("msg.mcp.placeholder.NoOutput") : text;
    }
}
