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
import com.dbn.connection.ConnectionId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.newElement;

/**
 * Project-level container for Liquibase artifacts and their connection mappings.
 */
@Getter
public class LiquibaseWorkspace implements PersistentStateElement {
    private final ProjectRef project;
    private final List<LiquibaseArtifact> artifacts = new ArrayList<>();

    public LiquibaseWorkspace(@NotNull Project project) {
        this.project = ProjectRef.of(project);
    }

    @NotNull
    public Project getProject() {
        return project.ensure();
    }

    @NotNull
    public LiquibaseArtifact ensureArtifact(@NotNull ConnectionId connectionId) {
        return artifacts.stream()
                .filter(artifact -> connectionId.equals(artifact.getConnectionId()))
                .findFirst()
                .orElseGet(() -> {
                    LiquibaseArtifact artifact = new LiquibaseArtifact();
                    artifact.setConnectionId(connectionId);
                    artifacts.add(artifact);
                    return artifact;
                });
    }

    public boolean hasArtifact(@NotNull ConnectionId connectionId) {
        return artifacts.stream().anyMatch(artifact -> connectionId.equals(artifact.getConnectionId()));
    }

    public void removeArtifact(@NotNull ConnectionId connectionId) {
        artifacts.removeIf(artifact -> connectionId.equals(artifact.getConnectionId()));
    }

    @NotNull
    public VirtualFile[] getContentRoots() {
        return ProjectRootManager.getInstance(getProject()).getContentRoots();
    }

    public boolean hasDuplicateArtifactData(@NotNull LiquibaseArtifact candidate) {
        return artifacts.stream()
                .filter(artifact -> artifact != candidate)
                .anyMatch(artifact -> artifact.usesSameContentRoot(candidate));
    }

    public boolean hasContentRootConflict(@NotNull String contentRootPath, @NotNull LiquibaseArtifact currentArtifact) {
        return findContentRootOwner(contentRootPath, currentArtifact) != null;
    }

    @Nullable
    public LiquibaseArtifact findContentRootOwner(@NotNull String contentRootPath, @NotNull LiquibaseArtifact currentArtifact) {
        return artifacts.stream()
                .filter(artifact -> artifact != currentArtifact)
                .filter(artifact -> contentRootPath.equals(artifact.getContentRootPath()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void readState(@NotNull Element element) {
        artifacts.clear();
        for (Element artifactElement : childrenOf(element, "artifact")) {
            LiquibaseArtifact artifact = new LiquibaseArtifact();
            artifact.readState(artifactElement);
            artifacts.add(artifact);
        }
    }

    @Override
    public void writeState(@NotNull Element element) {
        for (LiquibaseArtifact artifact : artifacts) {
            Element artifactElement = newElement(element, "artifact");
            artifact.writeState(artifactElement);
        }
    }
}
