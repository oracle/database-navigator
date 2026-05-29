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
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dbn.common.component.Components.optionalProjectService;

@Slf4j
public class McpJavaVersionManager extends ProjectComponentBase {
    private static final String COMPONENT_NAME = "DBNavigator.Project.McpJavaVersionManager";
    private static final int MIN_JAVA_VERSION = 17;
    private static final String FALLBACK_JAVA_VERSION = String.valueOf(MIN_JAVA_VERSION);
    private static final Pattern JAVA_FEATURE = Pattern.compile("(?<!\\d)(?:1\\.)?(\\d{1,2})(?=\\D|$)");

    private McpJavaVersionManager(@NotNull Project project) {
        super(project, COMPONENT_NAME);
    }

    @NotNull
    public static String resolveJavaVersion(@NotNull Project project) {
        McpJavaVersionManager manager = getInstance(project);
        return manager == null ? FALLBACK_JAVA_VERSION : manager.getProjectJavaVersion();
    }

    public static void ensureSupportedJavaVersion(@NotNull Project project) {
        McpJavaVersionManager manager = getInstance(project);
        if (manager == null) return;

        String javaVersion = manager.getConfiguredProjectJavaVersion();
        if (javaVersion == null) return;

        int feature = Integer.parseInt(javaVersion);
        if (feature < MIN_JAVA_VERSION) {
            throw new IllegalStateException(
                    "MCP Server generation requires Java " + MIN_JAVA_VERSION + " or newer. " +
                    "The current project SDK resolves to Java " + javaVersion + ". " +
                    "Configure the project SDK to use JDK " + MIN_JAVA_VERSION + "+ and try again.");
        }
    }

    @Nullable
    public static McpJavaVersionManager getInstance(@NotNull Project project) {
        return optionalProjectService(project, McpJavaVersionManager.class);
    }

    @NotNull
    public String getProjectJavaVersion() {
        return normalizeJavaVersion(getConfiguredProjectJavaVersion());
    }

    @Nullable
    private String getConfiguredProjectJavaVersion() {
        try {
            Sdk sdk = Read.call(() -> ProjectRootManager.getInstance(getProject()).getProjectSdk());
            if (sdk == null) return null;

            String javaVersion = extractJavaFeature(sdk.getVersionString());
            if (javaVersion == null) javaVersion = extractJavaFeature(sdk.getHomePath());
            if (javaVersion == null) javaVersion = extractJavaFeature(sdk.getName());
            return javaVersion;
        } catch (ProcessCanceledException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Could not resolve project Java version", e);
            return null;
        }
    }

    @Nullable
    private static String extractJavaFeature(String value) {
        if (value == null) return null;

        Matcher matcher = JAVA_FEATURE.matcher(value);
        while (matcher.find()) {
            int feature = Integer.parseInt(matcher.group(1));
            if (feature >= 8) return String.valueOf(feature);
        }
        return null;
    }

    @NotNull
    private static String normalizeJavaVersion(@Nullable String javaVersion) {
        if (javaVersion == null) return FALLBACK_JAVA_VERSION;

        int feature = Integer.parseInt(javaVersion);
        return String.valueOf(Math.max(feature, MIN_JAVA_VERSION));
    }
}
