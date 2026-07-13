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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.connectionIdAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setConstantAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Strings.equalsIgnoreCase;
import static com.dbn.common.util.Strings.isEmpty;

/**
 * Project-level container for Liquibase artifacts and their connection mappings.
 */
@Getter
public class LiquibaseWorkspaceBundle implements PersistentStateElement, Cloneable<LiquibaseWorkspaceBundle> {
    private final ProjectRef project;
    private Map<String, LiquibaseArtifact> artifacts = new LinkedHashMap<>();
    private Map<ConnectionId, String> artifactMappings = new LinkedHashMap<>();

    public LiquibaseWorkspaceBundle(@NotNull Project project) {
        this.project = ProjectRef.of(project);
    }

    @NotNull
    public Project getProject() {
        return project.ensure();
    }

    @NotNull
    public LiquibaseArtifact createArtifact() {
        LiquibaseArtifact artifact = new LiquibaseArtifact();
        artifacts.put(artifact.getId(), artifact);
        return artifact;
    }

    @NotNull
    public List<LiquibaseArtifact> getArtifactList() {
        return new ArrayList<>(artifacts.values());
    }

    public void moveArtifact(@NotNull LiquibaseArtifact artifact, int offset) {
        List<String> artifactIds = new ArrayList<>(artifacts.keySet());
        int currentIndex = artifactIds.indexOf(artifact.getId());
        int targetIndex = currentIndex + offset;
        if (currentIndex < 0 || targetIndex < 0 || targetIndex >= artifactIds.size()) return;

        String movedId = artifactIds.remove(currentIndex);
        artifactIds.add(targetIndex, movedId);
        Map<String, LiquibaseArtifact> reordered = new LinkedHashMap<>();
        artifactIds.forEach(id -> reordered.put(id, artifacts.get(id)));
        artifacts = reordered;
    }

    @Nullable
    public LiquibaseArtifact getArtifact(@NotNull ConnectionId connectionId) {
        String artifactId = artifactMappings.get(connectionId);
        return artifactId == null ? null : artifacts.get(artifactId);
    }

    public boolean hasArtifact(@NotNull ConnectionId connectionId) {
        return getArtifact(connectionId) != null;
    }

    public void removeArtifact(@NotNull ConnectionId connectionId) {
        artifactMappings.remove(connectionId);
    }

    public void removeArtifact(@NotNull String artifactId) {
        artifactMappings.values().removeIf(id -> Objects.equals(id, artifactId));
        artifacts.remove(artifactId);
    }

    public void attachArtifact(
            @NotNull ConnectionId connectionId,
            @NotNull String artifactId) {
        if (!artifacts.containsKey(artifactId)) {
            throw new IllegalArgumentException("Unknown Liquibase artifact: " + artifactId);
        }
        artifactMappings.put(connectionId, artifactId);
    }

    public void replaceArtifact(@NotNull LiquibaseArtifact artifact) {
        artifacts.put(artifact.getId(), artifact);
    }

    public void replaceArtifacts(LiquibaseWorkspaceBundle workspace) {
        artifacts = new LinkedHashMap<>();
        workspace.artifacts.values().forEach(a -> artifacts.put(a.getId(), a));
        artifactMappings = new LinkedHashMap<>(workspace.artifactMappings);
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
                .filter(artifact -> !Objects.equals(artifact.getId(), currentArtifact.getId()))
                .filter(artifact -> resolvedRoot.equals(resolveRootPath(artifact.getContentRootPath(), artifact.getRootPath())))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    public LiquibaseArtifact findNameOwner(
            @NotNull String name,
            @NotNull LiquibaseArtifact currentArtifact) {
        if (isEmpty(name)) return null;

        return artifacts.values().stream()
                .filter(artifact -> !Objects.equals(artifact.getId(), currentArtifact.getId()))
                .filter(artifact -> equalsIgnoreCase(artifact.getName(), name))
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
        artifactMappings.clear();
        for (Element artifactElement : childrenOf(element, "artifact")) {
            LiquibaseArtifact artifact = new LiquibaseArtifact();
            artifact.readState(artifactElement);
            artifacts.put(artifact.getId(), artifact);
        }
        for (Element mappingElement : childrenOf(element, "mapping")) {
            ConnectionId connectionId = connectionIdAttribute(mappingElement, "connection-id");
            String artifactId = stringAttribute(mappingElement, "artifact-id");
            if (connectionId != null && artifacts.containsKey(artifactId)) {
                artifactMappings.put(connectionId, artifactId);
            }
        }
    }

    @Override
    public void writeState(@NotNull Element element) {
        for (LiquibaseArtifact artifact : artifacts.values()) {
            Element artifactElement = newElement(element, "artifact");
            artifact.writeState(artifactElement);
        }
        artifactMappings.forEach((connectionId, artifactId) -> {
            Element mappingElement = newElement(element, "mapping");
            setConstantAttribute(mappingElement, "connection-id", connectionId);
            setStringAttribute(mappingElement, "artifact-id", artifactId);
        });
    }


    @Override
    @SneakyThrows
    public LiquibaseWorkspaceBundle clone() {
        LiquibaseWorkspaceBundle clone = (LiquibaseWorkspaceBundle) super.clone();
        clone.artifacts = new LinkedHashMap<>();
        clone.artifactMappings = new LinkedHashMap<>(artifactMappings);
        artifacts.forEach((artifactId, artifact) -> clone.artifacts.put(artifactId, artifact.clone()));
        return clone;
    }
}
