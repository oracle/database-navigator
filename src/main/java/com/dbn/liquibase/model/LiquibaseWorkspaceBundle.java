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
import com.dbn.connection.DatabaseType;
import com.dbn.connection.SchemaId;
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
import static com.dbn.common.options.setting.Settings.constantAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setConstantAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Lists.filter;
import static com.dbn.common.util.Strings.equalsIgnoreCase;
import static com.dbn.common.util.Strings.isEmpty;

/**
 * Project-level container for Liquibase workspaces and schema selection preferences.
 */
@Getter
public class LiquibaseWorkspaceBundle implements PersistentStateElement, Cloneable<LiquibaseWorkspaceBundle> {
    private final ProjectRef project;
    private Map<String, LiquibaseWorkspace> entries = new LinkedHashMap<>();
    private Map<ConnectionId, Map<SchemaId, String>> selections = new LinkedHashMap<>();

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
    public List<LiquibaseWorkspace> getWorkspaces() {
        return new ArrayList<>(entries.values());
    }

    public List<LiquibaseWorkspace> getWorkspaces(DatabaseType databaseType) {
        return filter(getWorkspaces(), w -> isCompatible(w, databaseType));
    };

    public boolean containsWorkspaces(@NotNull DatabaseType databaseType) {
        return !getWorkspaces(databaseType).isEmpty();
    }

    public static boolean isCompatible(
            @NotNull LiquibaseWorkspace workspace,
            @NotNull DatabaseType databaseType) {
        DatabaseType workspaceType = workspace.getDatabaseType();
        if (workspaceType == null || workspaceType == DatabaseType.GENERIC) return true;
        DatabaseType contextType = databaseType == DatabaseType.UNKNOWN ? DatabaseType.GENERIC : databaseType;
        return workspaceType == contextType;
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

    public void removeWorkspace(@NotNull String workspaceId) {
        selections.values().forEach(connectionSelections ->
                connectionSelections.values().removeIf(id -> Objects.equals(id, workspaceId)));
        entries.remove(workspaceId);
    }

    @Nullable
    public LiquibaseWorkspace getSelectedWorkspace(
            @NotNull ConnectionId connectionId,
            @NotNull SchemaId schemaId) {
        Map<SchemaId, String> connectionSelections = selections.get(connectionId);
        String workspaceId = connectionSelections == null ? null : connectionSelections.get(schemaId);
        return workspaceId == null ? null : entries.get(workspaceId);
    }

    public void rememberWorkspace(
            @NotNull ConnectionId connectionId,
            @NotNull SchemaId schemaId,
            @NotNull LiquibaseWorkspace workspace) {
        selections.computeIfAbsent(connectionId, id -> new LinkedHashMap<>())
                .put(schemaId, workspace.getId());
    }

    public void replaceWorkspace(@NotNull LiquibaseWorkspace workspace) {
        entries.put(workspace.getId(), workspace);
    }

    public void replaceWorkspaces(LiquibaseWorkspaceBundle that) {
        this.entries = new LinkedHashMap<>();
        that.entries.values().forEach(a -> this.entries.put(a.getId(), a));

        this.selections = new LinkedHashMap<>();
        that.selections.forEach((c, s) -> selections.put(c, new LinkedHashMap<>(s)));

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
        selections.clear();
        for (Element workspaceElement : childrenOf(element, "workspace")) {
            LiquibaseWorkspace workspace = new LiquibaseWorkspace();
            workspace.readState(workspaceElement);
            entries.put(workspace.getId(), workspace);
        }
        for (Element selectionElement : childrenOf(element, "selection")) {
            ConnectionId connectionId = connectionIdAttribute(selectionElement, "connection-id");
            SchemaId schemaId = constantAttribute(selectionElement, "schema-id", SchemaId.class);
            String workspaceId = stringAttribute(selectionElement, "workspace-id");
            if (connectionId != null && schemaId != null && entries.containsKey(workspaceId)) {
                selections.computeIfAbsent(connectionId, id -> new LinkedHashMap<>())
                        .put(schemaId, workspaceId);
            }
        }
    }

    @Override
    public void writeState(@NotNull Element element) {
        for (LiquibaseWorkspace workspace : entries.values()) {
            Element workspaceElement = newElement(element, "workspace");
            workspace.writeState(workspaceElement);
        }
        selections.forEach((connectionId, connectionSelections) -> connectionSelections.forEach((schemaId, workspaceId) -> {
            Element selectionElement = newElement(element, "selection");
            setConstantAttribute(selectionElement, "connection-id", connectionId);
            setConstantAttribute(selectionElement, "schema-id", schemaId);
            setStringAttribute(selectionElement, "workspace-id", workspaceId);
        }));
    }


    @Override
    @SneakyThrows
    public LiquibaseWorkspaceBundle clone() {
        LiquibaseWorkspaceBundle clone = (LiquibaseWorkspaceBundle) super.clone();
        clone.entries = new LinkedHashMap<>();
        clone.selections = new LinkedHashMap<>();
        selections.forEach((connectionId, connectionSelections) ->
                clone.selections.put(connectionId, new LinkedHashMap<>(connectionSelections)));
        entries.forEach((workspaceId, workspace) -> clone.entries.put(workspaceId, workspace.clone()));
        return clone;
    }

}
