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

import com.dbn.mcp.model.McpServerImplementation;
import com.intellij.openapi.project.Project;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;

@UtilityClass
public final class McpDistPaths {
    public static final @NonNls String CONFIG = "mcp-config.yaml";
    public static final @NonNls String DIST = "mcp-dist";
    public static final @NonNls String SOURCE_PROJECT = "source-project";
    public static final @NonNls String CONTAINER_MOUNT_DIR = "config";
    public static final @NonNls String README = "README.md";
    public static final @NonNls String BUILD_LOG = "build.log";
    public static final @NonNls String WALLET = "wallet";

    public static @NotNull Path distRoot(@NotNull Project project) {
        String basePath = project.getBasePath();
        Path projectPath = basePath == null ?
                Paths.get(System.getProperty("user.home")) :
                Paths.get(basePath);
        return projectPath.resolve(DIST).toAbsolutePath().normalize();
    }

    public static @NotNull Path payloadDirectory(
            @NotNull Path outputDirectory,
            @NotNull McpServerImplementation implementation) {
        return implementation.isContainer() ?
                outputDirectory.resolve(CONTAINER_MOUNT_DIR) :
                outputDirectory;
    }
}
