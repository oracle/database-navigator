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

package com.dbn.liquibase.model;

import com.dbn.common.project.ProjectRef;
import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.util.Cloneable;
import com.dbn.connection.ConnectionId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.util.Strings.isEmpty;

/**
 * Project-level container for Liquibase artifacts and their connection mappings.
 */
@Getter
public class LiquibaseWorkspace implements PersistentStateElement, Cloneable<LiquibaseWorkspace> {
    private final ProjectRef project;
    private Map<ConnectionId, LiquibaseArtifact> artifacts = new LinkedHashMap<>();

    public LiquibaseWorkspace(@NotNull Project project) {
        this.project = ProjectRef.of(project);
    }

    @NotNull
    public Project getProject() {
        return project.ensure();
    }

    @NotNull
    public LiquibaseArtifact ensureArtifact(@NotNull ConnectionId connectionId) {
        return artifacts.computeIfAbsent(connectionId, id -> {
            LiquibaseArtifact artifact = new LiquibaseArtifact();
            artifact.setConnectionId(id);
            return artifact;
        });
    }

    public boolean hasArtifact(@NotNull ConnectionId connectionId) {
        return artifacts.containsKey(connectionId);
    }

    public void removeArtifact(@NotNull ConnectionId connectionId) {
        artifacts.remove(connectionId);
    }

    public void replaceArtifact(@NotNull LiquibaseArtifact artifact) {
        artifacts.put(artifact.getConnectionId(), artifact);
    }

    public void replaceArtifacts(LiquibaseWorkspace workspace) {
        artifacts = new LinkedHashMap<>();
        workspace.artifacts.values().forEach(a -> artifacts.put(a.getConnectionId(), a));
    }

    @NotNull
    public VirtualFile[] getContentRoots() {
        return ProjectRootManager.getInstance(getProject()).getContentRoots();
    }

    @Nullable
    public LiquibaseArtifact findRootOwner(
            @NotNull String contentRootPath,
            @NotNull String rootPath,
            @NotNull LiquibaseArtifact currentArtifact) {
        Path resolvedRoot = resolveRootPath(contentRootPath, rootPath);
        if (resolvedRoot == null) return null;

        return artifacts.values().stream()
                .filter(artifact -> !Objects.equals(artifact.getConnectionId(), currentArtifact.getConnectionId()))
                .filter(artifact -> resolvedRoot.equals(resolveRootPath(artifact.getContentRootPath(), artifact.getRootPath())))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    private Path resolveRootPath(String contentRootPath, String rootPath) {
        if (isEmpty(contentRootPath) || isEmpty(rootPath)) return null;

        try {
            Path contentRoot = Paths.get(contentRootPath).toAbsolutePath().normalize();
            Path resolvedRoot = contentRoot.resolve(rootPath).normalize();
            return resolvedRoot.startsWith(contentRoot) ? resolvedRoot : null;
        } catch (InvalidPathException e) {
            return null;
        }
    }

    @Override
    public void readState(@NotNull Element element) {
        artifacts.clear();
        for (Element artifactElement : childrenOf(element, "artifact")) {
            LiquibaseArtifact artifact = new LiquibaseArtifact();
            artifact.readState(artifactElement);
            artifacts.put(artifact.getConnectionId(), artifact);
        }
    }

    @Override
    public void writeState(@NotNull Element element) {
        for (LiquibaseArtifact artifact : artifacts.values()) {
            Element artifactElement = newElement(element, "artifact");
            artifact.writeState(artifactElement);
        }
    }


    @Override
    @SneakyThrows
    public LiquibaseWorkspace clone() {
        LiquibaseWorkspace clone = (LiquibaseWorkspace) super.clone();
        clone.artifacts = new LinkedHashMap<>();
        artifacts.forEach((connectionId, artifact) -> clone.artifacts.put(connectionId, artifact.clone()));
        return clone;
    }
}
