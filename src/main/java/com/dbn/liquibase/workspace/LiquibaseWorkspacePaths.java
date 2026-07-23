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

package com.dbn.liquibase.workspace;

import com.dbn.common.util.Strings;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Resolves and validates filesystem paths derived from a Liquibase workspace. */
public class LiquibaseWorkspacePaths {
    private final LiquibaseWorkspace workspace;

    public LiquibaseWorkspacePaths(@NotNull LiquibaseWorkspace workspace) {
        this.workspace = workspace;
    }

    @NotNull
    public Path getContentRootPath() {
        String contentRootPath = requirePath(workspace.getContentRootPath(), "Content root");
        try {
            Path contentRoot = Paths.get(contentRootPath).toAbsolutePath().normalize();
            if (!Files.isDirectory(contentRoot)) {
                throw new IllegalArgumentException("Content root is not a directory: " + contentRoot);
            }
            return contentRoot;
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("Invalid content root path: " + contentRootPath, e);
        }
    }

    @NotNull
    public Path getLiquibaseRootPath() {
        return resolvePath(getContentRootPath(), workspace.getRootPath(), "Root path");
    }

    @NotNull
    public Path getChangelogDirectoryPath() {
        return resolvePath(getLiquibaseRootPath(), workspace.getChangelogDirectory(), "Changelog directory");
    }

    @NotNull
    public Path getSqlDirectoryPath() {
        return resolvePath(getLiquibaseRootPath(), workspace.getSqlDirectory(), "SQL directory");
    }

    /** Returns the directory used for generated Liquibase database documentation. */
    @NotNull
    public Path getDocumentationDirectoryPath() {
        return resolvePath(getLiquibaseRootPath(), workspace.getDocumentationDirectory(), "Documentation directory");
    }

    @NotNull
    public Path getMasterChangelogPath() {
        return resolvePath(getChangelogDirectoryPath(), workspace.getMasterChangelog(), "Master changelog");
    }

    @NotNull
    public String getMasterChangelogRelativePath() {
        return getRelativePath(getMasterChangelogPath());
    }

    @NotNull
    public Path getPropertiesFilePath() {
        return resolvePath(getContentRootPath(), workspace.getPropertiesFile(), "Properties file");
    }

    @NotNull
    public String getRelativePath(@NotNull Path path) {
        Path contentRoot = getContentRootPath();
        Path normalizedPath = path.toAbsolutePath().normalize();
        if (!normalizedPath.startsWith(contentRoot)) {
            throw new IllegalArgumentException("Path must remain below the content root: " + path);
        }
        return contentRoot.relativize(normalizedPath).toString().replace(File.separatorChar, '/');
    }

    @NotNull
    private Path resolvePath(@NotNull Path parent, String path, String name) {
        String value = requirePath(path, name);
        Path resolved = parent.resolve(value).normalize();
        if (!resolved.startsWith(parent)) {
            throw new IllegalArgumentException("Path must remain below the content root: " + value);
        }
        return resolved;
    }

    @NotNull
    private String requirePath(String path, String name) {
        if (Strings.isEmpty(path) || Strings.isEmpty(path.trim())) {
            throw new IllegalArgumentException(name + " is required");
        }
        return path;
    }
}
