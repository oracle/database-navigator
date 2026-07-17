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

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.util.ExecUtil;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.SystemProperties;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

import static com.dbn.common.util.Messages.showErrorDialog;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

/**
 * Container image builds require a Docker-compatible container runtime on the
 * developer machine (Docker, or Podman exposing its Docker-compatible socket).
 * This support locates a container CLI and verifies the daemon is reachable
 * before the long Maven build starts. No local GraalVM is required for this
 * build target: the native compilation runs inside a Linux builder container.
 */
@Slf4j
public final class McpContainerRuntimeSupport {
    private static final int DAEMON_PROBE_TIMEOUT_MILLIS = 10_000;

    // IDE processes often lack the login-shell PATH; probe well-known install locations too
    private static final @NonNls List<String> CLI_NAMES = List.of("docker", "podman");
    private static final @NonNls List<String> CLI_LOCATIONS = List.of(
            "/usr/local/bin",
            "/opt/homebrew/bin",
            "/opt/podman/bin",
            "/usr/bin");

    private McpContainerRuntimeSupport() {}

    public static void verifyContainerRuntimeAvailability(@NotNull Project project) {
        if (findReachableRuntime() != null) return;

        showErrorDialog(project,
                txt("msg.mcp.title.ContainerRuntimeRequired"),
                txt("msg.mcp.error.ContainerRuntimeNotFound"));
        throw new ProcessCanceledException();
    }

    /**
     * Returns the Docker-compatible command that DBN can use for this machine.
     * The result is suitable for the run command shown after a successful build.
     */
    @NotNull
    public static String getContainerRuntimeCommand() {
        String runtime = findReachableRuntime();
        return runtime == null ? "docker" : runtime;
    }

    /**
     * The host architecture the built image will target, in container-platform
     * notation (e.g. "arm64", "amd64").
     */
    public static @NonNls String normalizedHostArch() {
        @NonNls String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        switch (arch) {
            case "aarch64":
            case "arm64": return "arm64";
            case "x86_64":
            case "amd64": return "amd64";
            default: return arch;
        }
    }

    @Nullable
    private static String findReachableRuntime() {
        for (String cli : CLI_NAMES) {
            String executable = locateCli(cli);
            if (executable != null && isDaemonReachable(executable)) return cli;
        }
        return null;
    }

    @Nullable
    private static String locateCli(@NonNls String cli) {
        @NonNls String fileName = SystemInfo.isWindows ? cli + ".exe" : cli;

        for (String location : CLI_LOCATIONS) {
            Path candidate = Paths.get(location, fileName);
            if (Files.isExecutable(candidate)) return candidate.toString();
        }
        Path userLocal = Paths.get(SystemProperties.getUserHome(), ".docker/bin", fileName);
        if (Files.isExecutable(userLocal)) return userLocal.toString();

        return cli; // fall back to PATH resolution by the command line itself
    }

    private static boolean isDaemonReachable(String executable) {
        try {
            GeneralCommandLine commandLine = new GeneralCommandLine(executable, "info");
            var output = ExecUtil.execAndGetOutput(commandLine, DAEMON_PROBE_TIMEOUT_MILLIS);
            boolean reachable = output.getExitCode() == 0 && !output.isTimeout();
            if (!reachable) log.info("Container runtime probe failed for {}: exit={}, timeout={}",
                    executable, output.getExitCode(), output.isTimeout());
            return reachable;
        } catch (Exception e) {
            conditionallyLog(e);
            return false;
        }
    }
}
