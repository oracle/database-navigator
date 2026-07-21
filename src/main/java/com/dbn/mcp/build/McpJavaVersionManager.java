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
import com.dbn.mcp.model.McpServerImplementation;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.execution.MavenRunner;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dbn.common.component.Components.optionalProjectService;

@Slf4j
public class McpJavaVersionManager extends ProjectComponentBase {
    // Standard Java only needs what the plain MCP Java SDK itself requires (mcp-core:1.1.1 is
    // built at Java 17). Micronaut Native/Container are Micronaut 5.x applications, which raised
    // its own baseline to Java 25 (https://micronaut.io/2026/04/27/micronaut-framework-5-0-with-java-25-baseline/) -
    // there is no released Micronaut MCP integration compatible with an older Micronaut/JDK pairing.
    public static final int MIN_JAVA_VERSION_STANDARD = 17;
    public static final int MIN_JAVA_VERSION_MICRONAUT = 25;

    private static final String COMPONENT_NAME = "DBNavigator.Project.McpJavaVersionManager";
    private static final Pattern JAVA_FEATURE = Pattern.compile("(?<!\\d)(?:1\\.)?(\\d{1,2})(?=\\D|$)");

    private McpJavaVersionManager(@NotNull Project project) {
        super(project, COMPONENT_NAME);
    }

    public static int minJavaVersion(@NotNull McpServerImplementation implementation) {
        return implementation.isNative() ? MIN_JAVA_VERSION_MICRONAUT : MIN_JAVA_VERSION_STANDARD;
    }

    @NotNull
    public static String resolveJavaVersion(@NotNull Project project, @NotNull McpServerImplementation implementation) {
        McpJavaVersionManager manager = getInstance(project);
        int minVersion = minJavaVersion(implementation);
        return manager == null ? String.valueOf(minVersion) : manager.getProjectJavaVersion(minVersion);
    }

    @Nullable
    public static McpJavaVersionManager getInstance(@NotNull Project project) {
        return optionalProjectService(project, McpJavaVersionManager.class);
    }

    @NotNull
    public String getProjectJavaVersion(int minVersion) {
        return normalizeJavaVersion(getConfiguredRunnerJavaVersion(), minVersion);
    }

    /**
     * Every implementation is actually compiled and run by the Maven runner JRE, not by the
     * IDE's Project Structure SDK - that JDK plays no part in this build pipeline. Resolving
     * the runner JRE the same way {@link McpGraalVmSupport} does keeps both checks consistent
     * with what Maven will really execute with.
     */
    @Nullable
    public String getConfiguredRunnerJavaVersion() {
        try {
            @NonNls String jreName = MavenRunner.getInstance(getProject()).getSettings().getJreName();
            Sdk sdk = ExternalSystemJdkUtil.getJdk(getProject(), jreName);
            if (sdk == null) return null;

            String javaVersion = extractJavaFeature(sdk.getVersionString());
            if (javaVersion == null) javaVersion = extractJavaFeature(sdk.getHomePath());
            if (javaVersion == null) javaVersion = extractJavaFeature(sdk.getName());
            return javaVersion;
        } catch (ProcessCanceledException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Could not resolve Maven runner Java version", e);
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
    private static String normalizeJavaVersion(@Nullable String javaVersion, int minVersion) {
        if (javaVersion == null) return String.valueOf(minVersion);

        int feature = Integer.parseInt(javaVersion);
        return String.valueOf(Math.max(feature, minVersion));
    }
}
