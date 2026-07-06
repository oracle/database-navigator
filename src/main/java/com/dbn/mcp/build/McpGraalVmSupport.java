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

import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Read;
import com.dbn.common.util.Messages;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.util.SystemProperties;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.execution.MavenRunner;
import org.jetbrains.idea.maven.execution.MavenRunnerSettings;
import org.jetbrains.idea.maven.server.MavenDistributionsCache;
import org.jetbrains.idea.maven.utils.MavenUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Messages.options;
import static com.dbn.common.util.Messages.showErrorDialog;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

/**
 * Native MCP server builds require the Maven runner to execute on a GraalVM JDK
 * that includes the native-image tool. This support verifies the configured Maven
 * runner JRE and, if it does not qualify, tries to locate a GraalVM installation
 * on the machine and (with user consent) configures the Maven runner to use it.
 */
@Slf4j
public final class McpGraalVmSupport {
    private McpGraalVmSupport() {}

    public static void verifyGraalVmAvailability(@NotNull Project project) {
        if (isRunnerGraalVmReady(project)) return;

        Path graalVmHome = findGraalVmHome(project);
        if (graalVmHome == null) {
            showErrorDialog(project,
                    txt("msg.mcp.title.GraalVmRequired"),
                    txt("msg.mcp.error.GraalVmNotFound"));
            throw new ProcessCanceledException();
        }

        int option = Messages.showConfirmationDialog(project,
                txt("msg.mcp.title.GraalVmRequired"),
                txt("msg.mcp.question.UseDetectedGraalVm", graalVmHome),
                options(txt("msg.mcp.button.UseGraalVm"), txt("msg.shared.button.Cancel")), 0);
        if (option != 0) {
            throw new ProcessCanceledException();
        }

        String sdkName = resolveSdkName(project, graalVmHome);
        MavenRunner.getInstance(project).getSettings().setJreName(sdkName);
        log.info("Configured Maven runner JRE for native MCP builds: {} ({})", sdkName, graalVmHome);
    }

    /**
     * The Maven runner is ready when the JRE it resolves to is a GraalVM home AND the
     * Maven integration accepts the SDK entry for launching. The second condition matters:
     * the Maven runner rejects SDK entries that are not full Java SDKs (and JDKs below 1.7)
     * with "Maven 3.3.1+ requires JDK 1.7+", regardless of how valid the underlying JDK is.
     */
    private static boolean isRunnerGraalVmReady(@NotNull Project project) {
        try {
            @NonNls String jreName = MavenRunner.getInstance(project).getSettings().getJreName();
            Sdk sdk;
            switch (jreName) {
                case MavenRunnerSettings.USE_PROJECT_JDK:
                    sdk = Read.call(() -> ProjectRootManager.getInstance(project).getProjectSdk());
                    break;
                case MavenRunnerSettings.USE_JAVA_HOME:
                case MavenRunnerSettings.USE_INTERNAL_JAVA:
                    // environment-dependent JREs cannot be pinned or vetted;
                    // native builds require an explicit SDK entry (offered below)
                    return false;
                default:
                    sdk = Read.call(() -> ProjectJdkTable.getInstance().findJdk(jreName));
                    break;
            }
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

    @Nullable
    private static Path findGraalVmHome(@NotNull Project project) {
        // prefer installations matching the host CPU architecture, then the newest version
        return collectCandidateHomes(project).stream()
                .filter(McpGraalVmSupport::isGraalVm)
                .max(Comparator
                        .comparing(McpGraalVmSupport::matchesHostArchitecture)
                        .thenComparing(McpGraalVmSupport::readJavaVersion, McpGraalVmSupport::compareVersions))
                .orElse(null);
    }

    private static boolean matchesHostArchitecture(Path jdkHome) {
        @NonNls String hostArch = System.getProperty("os.arch");
        @NonNls String jdkArch = readReleaseProperty(jdkHome, "OS_ARCH");
        if (jdkArch == null) return true; // unknown: do not penalize
        if (jdkArch.equals(hostArch)) return true;
        // normalize the common macOS/Linux aliases
        return (jdkArch.equals("aarch64") && hostArch.equals("arm64")) ||
                (jdkArch.equals("arm64") && hostArch.equals("aarch64")) ||
                (jdkArch.equals("amd64") && hostArch.equals("x86_64")) ||
                (jdkArch.equals("x86_64") && hostArch.equals("amd64"));
    }

    private static String readJavaVersion(Path jdkHome) {
        return nvl(readReleaseProperty(jdkHome, "JAVA_VERSION"), "0");
    }

    @Nullable
    private static @NonNls String readReleaseProperty(Path jdkHome, @NonNls String key) {
        Path releaseFile = jdkHome.resolve("release");
        if (!Files.isRegularFile(releaseFile)) return null;
        try {
            for (String line : Files.readAllLines(releaseFile)) {
                if (line.startsWith(key + "=")) {
                    return line.substring(key.length() + 1).replace("\"", "").trim();
                }
            }
        } catch (IOException e) {
            conditionallyLog(e);
        }
        return null;
    }

    private static int compareVersions(String left, String right) {
        String[] leftParts = left.split("[.+_-]");
        String[] rightParts = right.split("[.+_-]");
        int length = Math.max(leftParts.length, rightParts.length);
        for (int i = 0; i < length; i++) {
            int leftValue = i < leftParts.length ? parseVersionPart(leftParts[i]) : 0;
            int rightValue = i < rightParts.length ? parseVersionPart(rightParts[i]) : 0;
            if (leftValue != rightValue) return Integer.compare(leftValue, rightValue);
        }
        return 0;
    }

    private static int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static Set<Path> collectCandidateHomes(@NotNull Project project) {
        Set<Path> candidates = new LinkedHashSet<>();

        Sdk[] sdks = Read.call(() -> ProjectJdkTable.getInstance().getAllJdks());
        for (Sdk sdk : sdks) {
            String homePath = sdk.getHomePath();
            if (homePath != null) candidates.add(Paths.get(homePath));
        }

        String graalVmHome = System.getenv("GRAALVM_HOME");
        if (graalVmHome != null) candidates.add(Paths.get(graalVmHome));

        String userHome = SystemProperties.getUserHome();
        candidates.addAll(listJdkHomes(Paths.get(userHome, "Library/Java/JavaVirtualMachines"), "Contents/Home"));
        candidates.addAll(listJdkHomes(Paths.get("/Library/Java/JavaVirtualMachines"), "Contents/Home"));
        candidates.addAll(listJdkHomes(Paths.get(userHome, ".sdkman/candidates/java"), null));
        candidates.addAll(listJdkHomes(Paths.get(userHome, ".jdks"), null));
        candidates.addAll(listJdkHomes(Paths.get("/usr/lib/jvm"), null));

        String programFiles = System.getenv("ProgramFiles");
        if (programFiles != null) {
            candidates.addAll(listJdkHomes(Paths.get(programFiles, "Java"), null));
            candidates.addAll(listJdkHomes(Paths.get(programFiles, "GraalVM"), null));
        }

        return candidates;
    }

    private static List<Path> listJdkHomes(Path installDirectory, @Nullable @NonNls String homeSuffix) {
        if (!Files.isDirectory(installDirectory)) return List.of();

        List<Path> homes = new ArrayList<>();
        try (Stream<Path> entries = Files.list(installDirectory)) {
            for (Path entry : entries.toList()) {
                homes.add(homeSuffix == null ? entry : entry.resolve(homeSuffix));
            }
        } catch (IOException e) {
            conditionallyLog(e);
        }
        return homes;
    }

    private static boolean isGraalVm(@Nullable Path jdkHome) {
        if (jdkHome == null || !Files.isDirectory(jdkHome)) return false;
        return Files.isRegularFile(jdkHome.resolve("bin/native-image")) ||
                Files.isRegularFile(jdkHome.resolve("bin/native-image.cmd"));
    }

    private static String resolveSdkName(@NotNull Project project, @NotNull Path graalVmHome) {
        Sdk[] sdks = Read.call(() -> ProjectJdkTable.getInstance().getAllJdks());
        for (Sdk sdk : sdks) {
            String homePath = sdk.getHomePath();
            if (homePath != null && Paths.get(homePath).equals(graalVmHome) && isAcceptedByMavenRunner(project, sdk)) {
                return sdk.getName();
            }
        }
        return registerSdk(project, graalVmHome);
    }

    private static String registerSdk(@NotNull Project project, @NotNull Path graalVmHome) {
        // must be a full JavaSdk entry: the Maven runner rejects other SDK types outright
        Sdk sdk = Dispatch.call(() ->
                SdkConfigurationUtil.createAndAddSDK(graalVmHome.toString(), JavaSdk.getInstance()));

        if (sdk == null) {
            showErrorDialog(project,
                    txt("msg.mcp.title.GraalVmRequired"),
                    txt("msg.mcp.error.GraalVmSdkSetupFailed", graalVmHome));
            throw new ProcessCanceledException();
        }
        return sdk.getName();
    }
}
