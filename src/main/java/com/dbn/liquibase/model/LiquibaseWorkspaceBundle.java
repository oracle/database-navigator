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
 * Project-level container for Liquibase workspaces and their connection mappings.
 */
@Getter
public class LiquibaseWorkspaceBundle implements PersistentStateElement, Cloneable<LiquibaseWorkspaceBundle> {
    private final ProjectRef project;
    private Map<String, LiquibaseWorkspace> entries = new LinkedHashMap<>();
    private Map<ConnectionId, String> connectionMappings = new LinkedHashMap<>();

    public LiquibaseWorkspaceBundle(@NotNull Project project) {
        this.project = ProjectRef.of(project);
    }

    @NotNull
    public Project getProject() {
        return project.ensure();
    }

    @NotNull
    public LiquibaseWorkspace createWorkspace() {
        LiquibaseWorkspace workspace = new LiquibaseWorkspace();
        entries.put(workspace.getId(), workspace);
        return workspace;
    }

    @NotNull
    public List<LiquibaseWorkspace> getWorkspaceList() {
        return new ArrayList<>(entries.values());
    }

    public void moveWorkspace(@NotNull LiquibaseWorkspace workspace, int offset) {
        List<String> workspaceIds = new ArrayList<>(entries.keySet());
        int currentIndex = workspaceIds.indexOf(workspace.getId());
        int targetIndex = currentIndex + offset;
        if (currentIndex < 0 || targetIndex < 0 || targetIndex >= workspaceIds.size()) return;

        String movedId = workspaceIds.remove(currentIndex);
        workspaceIds.add(targetIndex, movedId);
        Map<String, LiquibaseWorkspace> reordered = new LinkedHashMap<>();
        workspaceIds.forEach(id -> reordered.put(id, entries.get(id)));
        entries = reordered;
    }

    @Nullable
    public LiquibaseWorkspace getWorkspace(@NotNull ConnectionId connectionId) {
        String workspaceId = connectionMappings.get(connectionId);
        return workspaceId == null ? null : entries.get(workspaceId);
    }

    public boolean hasWorkspace(@NotNull ConnectionId connectionId) {
        return getWorkspace(connectionId) != null;
    }

    public void removeWorkspace(@NotNull ConnectionId connectionId) {
        connectionMappings.remove(connectionId);
    }

    public void removeWorkspace(@NotNull String workspaceId) {
        connectionMappings.values().removeIf(id -> Objects.equals(id, workspaceId));
        entries.remove(workspaceId);
    }

    public void attachWorkspace(
            @NotNull ConnectionId connectionId,
            @NotNull String workspaceId) {
        if (!entries.containsKey(workspaceId)) {
            throw new IllegalArgumentException("Unknown Liquibase workspace: " + workspaceId);
        }
        connectionMappings.put(connectionId, workspaceId);
    }

    public void replaceWorkspace(@NotNull LiquibaseWorkspace workspace) {
        entries.put(workspace.getId(), workspace);
    }

    public void replaceWorkspaces(LiquibaseWorkspaceBundle workspaces) {
        this.entries = new LinkedHashMap<>();
        workspaces.entries.values().forEach(a -> this.entries.put(a.getId(), a));
        connectionMappings = new LinkedHashMap<>(workspaces.connectionMappings);
    }

    @NotNull
    public VirtualFile[] getContentRoots() {
        return ProjectRootManager.getInstance(getProject()).getContentRoots();
    }

    @Nullable
    public LiquibaseWorkspace findRootOwner(
            @NotNull String contentRootPath,
            @NotNull String rootPath,
            @NotNull LiquibaseWorkspace currentWorkspace) {
        Path resolvedRoot = resolveRootPath(contentRootPath, rootPath);
        if (resolvedRoot == null) return null;

        return entries.values().stream()
                .filter(w -> !Objects.equals(w.getId(), currentWorkspace.getId()))
                .filter(w -> resolvedRoot.equals(resolveRootPath(w.getContentRootPath(), w.getRootPath())))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    public LiquibaseWorkspace findNameOwner(
            @NotNull String name,
            @NotNull LiquibaseWorkspace currentWorkspace) {
        if (isEmpty(name)) return null;

        return entries.values().stream()
                .filter(w -> !Objects.equals(w.getId(), currentWorkspace.getId()))
                .filter(w -> equalsIgnoreCase(w.getName(), name))
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
        entries.clear();
        connectionMappings.clear();
        for (Element workspaceElement : childrenOf(element, "workspace")) {
            LiquibaseWorkspace workspace = new LiquibaseWorkspace();
            workspace.readState(workspaceElement);
            entries.put(workspace.getId(), workspace);
        }
        for (Element mappingElement : childrenOf(element, "mapping")) {
            ConnectionId connectionId = connectionIdAttribute(mappingElement, "connection-id");
            String workspaceId = stringAttribute(mappingElement, "workspace-id");
            if (connectionId != null && entries.containsKey(workspaceId)) {
                connectionMappings.put(connectionId, workspaceId);
            }
        }
    }

    @Override
    public void writeState(@NotNull Element element) {
        for (LiquibaseWorkspace workspace : entries.values()) {
            Element workspaceElement = newElement(element, "workspace");
            workspace.writeState(workspaceElement);
        }
        connectionMappings.forEach((connectionId, workspace) -> {
            Element mappingElement = newElement(element, "mapping");
            setConstantAttribute(mappingElement, "connection-id", connectionId);
            setStringAttribute(mappingElement, "workspace-id", workspace);
        });
    }


    @Override
    @SneakyThrows
    public LiquibaseWorkspaceBundle clone() {
        LiquibaseWorkspaceBundle clone = (LiquibaseWorkspaceBundle) super.clone();
        clone.entries = new LinkedHashMap<>();
        clone.connectionMappings = new LinkedHashMap<>(connectionMappings);
        entries.forEach((workspaceId, workspace) -> clone.entries.put(workspaceId, workspace.clone()));
        return clone;
    }
}
