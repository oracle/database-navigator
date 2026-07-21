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

import com.dbn.common.util.Messages;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.execution.MavenRunner;
import org.jetbrains.idea.maven.server.MavenDistributionsCache;
import org.jetbrains.idea.maven.utils.MavenUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Messages.options;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

/**
 * Native MCP server builds require Maven to execute on a GraalVM JDK that
 * includes the native-image tool. This support only checks whether the
 * currently configured Maven runner JRE already qualifies; it does not scan
 * the machine for GraalVM installations or register SDKs on the user's
 * behalf. When the runner does not qualify, the user is routed to Maven
 * Settings to configure one explicitly, or can use the Container Image
 * build target instead, which needs no local GraalVM at all.
 */
public final class McpGraalVmSupport {
    private McpGraalVmSupport() {}

    public static void verifyGraalVmAvailability(@NotNull Project project) {
        if (isRunnerGraalVmReady(project)) return;

        int option = Messages.showConfirmationDialog(project,
                txt("msg.mcp.title.GraalVmRequired"),
                txt("msg.mcp.question.GraalVmNotFound"),
                options(txt("msg.mcp.button.OpenMavenSettings"), txt("msg.shared.button.Cancel")), 0);
        if (option == 0) {
            McpMavenPluginSupport.openMavenPluginSettings(project);
        }
        throw new ProcessCanceledException();
    }

    /**
     * The Maven runner is ready when the JRE it resolves to is a GraalVM home AND the
     * Maven integration accepts the SDK entry for launching. The second condition matters:
     * the Maven runner rejects SDK entries that are not full Java SDKs (and JDKs below 1.7)
     * with "Maven 3.3.1+ requires JDK 1.7+", regardless of how valid the underlying JDK is.
     * <p>
     * {@link ExternalSystemJdkUtil#getJdk} resolves the runner's JRE selection the same way
     * the Runner settings panel does, including the "Use JAVA_HOME" and "Use Internal JRE"
     * macros (matching values, see {@link org.jetbrains.idea.maven.execution.MavenRunnerSettings})
     * - not just named SDK table entries.
     */
    public static boolean isRunnerGraalVmReady(@NotNull Project project) {
        try {
            @NonNls String jreName = MavenRunner.getInstance(project).getSettings().getJreName();
            Sdk sdk = ExternalSystemJdkUtil.getJdk(project, jreName);
            return isGraalVm(homeOf(sdk)) && isAcceptedByMavenRunner(project, sdk);
        } catch (ProcessCanceledException e) {
            throw e;
        } catch (Exception e) {
            conditionallyLog(e);
            return false;
        }
    }

    private static boolean isAcceptedByMavenRunner(@NotNull Project project, @NotNull Sdk sdk) {
        try {
            String mavenVersion = MavenDistributionsCache.getInstance(project).getSettingsDistribution().getVersion();
            return MavenUtil.verifyMavenSdkRequirements(sdk, nvl(mavenVersion, "3.3.1"));
        } catch (Exception e) {
            conditionallyLog(e);
            return true; // do not block the build on validation glitches; Maven will re-verify
        }
    }

    @Nullable
    private static Path homeOf(@Nullable Sdk sdk) {
        return sdk == null || sdk.getHomePath() == null ? null : Paths.get(sdk.getHomePath());
    }

    private static boolean isGraalVm(@Nullable Path jdkHome) {
        if (jdkHome == null || !Files.isDirectory(jdkHome)) return false;
        return Files.isRegularFile(jdkHome.resolve("bin/native-image")) ||
                Files.isRegularFile(jdkHome.resolve("bin/native-image.cmd"));
    }
}
