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

package com.dbn.migration.liquibase.workspace;

import com.dbn.common.component.ProjectUnit;
import com.dbn.common.util.Strings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.dbn.migration.shared.workspace.DatabaseMigrationWorkspacePaths;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/** Resolves and validates filesystem paths derived from a Liquibase workspace. */
public class LiquibaseWorkspacePaths extends ProjectUnit implements DatabaseMigrationWorkspacePaths<LiquibaseWorkspace> {
    private final LiquibaseWorkspace workspace;

    public LiquibaseWorkspacePaths(@NotNull Project project, @NotNull LiquibaseWorkspace workspace) {
        super(project);
        this.workspace = workspace;
    }

    @NotNull
    @Override
    public LiquibaseWorkspace getWorkspace() {
        return workspace;
    }

    @NotNull
    @Override
    public Path getContentRootPath() {
        String contentRootPath = requirePath(workspace.getContentRootPath(), "Content root");
        try {
            Path configuredRoot = Paths.get(contentRootPath).toAbsolutePath().normalize();
            if (!isProjectContentRoot(configuredRoot)) {
                throw new IllegalArgumentException("Content root is not a project content root: " + configuredRoot);
            }
            Path contentRoot = configuredRoot.toRealPath();
            if (!Files.isDirectory(contentRoot, LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalArgumentException("Content root is not a directory: " + contentRoot);
            }
            return contentRoot;
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("Invalid content root path: " + contentRootPath, e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to resolve content root: " + contentRootPath, e);
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
        String masterChangelog = workspace.getMasterChangelog();
        if (!Strings.isEmpty(masterChangelog)) {
            masterChangelog = LiquibaseChangelogFiles.normalize(
                    masterChangelog,
                    workspace.getChangelogFormat(),
                    workspace.getDatabaseType());
        }
        return resolvePath(getChangelogDirectoryPath(), masterChangelog, "Master changelog");
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
    @Override
    public String getRelativePath(@NotNull Path path) {
        Path contentRoot = getContentRootPath();
        Path normalizedPath = validatePath(path, contentRoot);
        return contentRoot.relativize(normalizedPath).toString().replace(File.separatorChar, '/');
    }

    /** Validates a filesystem destination immediately before it is created, deleted, or written. */
    @NotNull
    public Path validateOutputPath(@NotNull Path path) {
        return validatePath(path, getContentRootPath());
    }

    /** Rejects linked entries already present in a generated-output tree. */
    @NotNull
    public Path validateOutputDirectory(@NotNull Path path) throws IOException {
        Path directory = validateOutputPath(path);
        BasicFileAttributes attributes = readAttributes(directory);
        if (attributes == null) return directory;
        if (!attributes.isDirectory()) {
            throw new IllegalArgumentException("Output path is not a directory: " + directory);
        }

        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes) {
                if (attributes.isSymbolicLink()) {
                    throw new IllegalArgumentException("Output directory must not contain symbolic links: " + dir);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                if (attributes.isSymbolicLink()) {
                    throw new IllegalArgumentException("Output directory must not contain symbolic links: " + file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return directory;
    }

    @NotNull
    private Path resolvePath(@NotNull Path parent, String path, String name) {
        String value = requirePath(path, name);
        Path resolved = parent.resolve(value).normalize();
        Path contentRoot = getContentRootPath();
        if (!parent.startsWith(contentRoot) || !resolved.startsWith(parent)) {
            throw new IllegalArgumentException("Path must remain below the content root: " + value);
        }
        return validatePath(resolved, contentRoot);
    }

    @NotNull
    private Path validatePath(@NotNull Path path, @NotNull Path contentRoot) {
        Path normalizedPath = path.toAbsolutePath().normalize();
        if (!normalizedPath.startsWith(contentRoot)) {
            throw new IllegalArgumentException("Path must remain below the content root: " + path);
        }
        if (normalizedPath.equals(contentRoot)) return normalizedPath;

        try {
            Path current = contentRoot;
            Path relativePath = contentRoot.relativize(normalizedPath);
            for (int i = 0; i < relativePath.getNameCount(); i++) {
                current = current.resolve(relativePath.getName(i));
                BasicFileAttributes attributes = readAttributes(current);
                if (attributes == null) continue;
                if (attributes.isSymbolicLink()) {
                    throw new IllegalArgumentException("Path must not contain symbolic links: " + current);
                }

                boolean lastComponent = i == relativePath.getNameCount() - 1;
                if (!lastComponent && !attributes.isDirectory()) {
                    throw new IllegalArgumentException("Path component is not a directory: " + current);
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to validate workspace path: " + path, e);
        }
        return normalizedPath;
    }

    private BasicFileAttributes readAttributes(@NotNull Path path) throws IOException {
        try {
            return Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        } catch (NoSuchFileException e) {
            return null;
        }
    }

    private boolean isProjectContentRoot(@NotNull Path path) {
        ProjectRootManager rootManager = ProjectRootManager.getInstance(getProject());
        for (VirtualFile contentRoot : rootManager.getContentRoots()) {
            Path rootPath = Paths.get(contentRoot.getPath()).toAbsolutePath().normalize();
            if (path.equals(rootPath)) return true;
        }
        return false;
    }

    @NotNull
    private String requirePath(String path, String name) {
        if (Strings.isEmpty(path) || Strings.isEmpty(path.trim())) {
            throw new IllegalArgumentException(name + " is required");
        }
        return path;
    }
}
