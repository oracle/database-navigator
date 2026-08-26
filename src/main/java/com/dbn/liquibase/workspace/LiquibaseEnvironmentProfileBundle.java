/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 */

package com.dbn.liquibase.workspace;

import com.dbn.common.environment.EnvironmentTypeBundle;
import com.dbn.common.environment.EnvironmentTypeId;
import com.dbn.common.project.ProjectRef;
import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.util.Cloneable;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.connectionIdAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.schemaIdAttribute;
import static com.dbn.common.options.setting.Settings.setConstantAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Strings.equalsIgnoreCase;
import static com.dbn.common.util.Strings.isEmpty;

/** Project-level container for named Liquibase environment profiles. */
@Getter
public class LiquibaseEnvironmentProfileBundle implements PersistentStateElement, Cloneable<LiquibaseEnvironmentProfileBundle> {
    private final ProjectRef project;
    private Map<String, LiquibaseEnvironmentProfile> entries = new LinkedHashMap<>();
    private Map<ConnectionId, Map<SchemaId, String>> selections = new LinkedHashMap<>();

    public LiquibaseEnvironmentProfileBundle(@NotNull Project project) {
        this.project = ProjectRef.of(project);
    }

    @NotNull
    public Project getProject() {
        return project.ensure();
    }

    @NotNull
    public LiquibaseEnvironmentProfile getProfile(@NotNull EnvironmentTypeId environmentTypeId) {
        return getProfiles(environmentTypeId).stream()
                .findFirst()
                .orElseGet(() -> new LiquibaseEnvironmentProfile(environmentTypeId.id(), environmentTypeId));
    }

    @Nullable
    public LiquibaseEnvironmentProfile getProfile(@NotNull String profileId) {
        return entries.get(profileId);
    }

    @NotNull
    public List<LiquibaseEnvironmentProfile> getProfiles() {
        return List.copyOf(entries.values());
    }

    @NotNull
    public List<LiquibaseEnvironmentProfile> getProfiles(@NotNull EnvironmentTypeId environmentTypeId) {
        return entries.values().stream()
                .filter(profile -> environmentTypeId.equals(profile.getEnvironmentTypeId()))
                .toList();
    }

    @Nullable
    public LiquibaseEnvironmentProfile findNameOwner(
            @NotNull String name,
            @NotNull LiquibaseEnvironmentProfile currentProfile) {
        if (isEmpty(name)) return null;

        return entries.values().stream()
                .filter(p -> !Objects.equals(p.getId(), currentProfile.getId()))
                .filter(p -> equalsIgnoreCase(p.getName(), name))
                .findFirst()
                .orElse(null);
    }

    @NotNull
    public LiquibaseEnvironmentProfile createProfile(
            @NotNull String name,
            @NotNull EnvironmentTypeId environmentTypeId) {
        LiquibaseEnvironmentProfile profile = new LiquibaseEnvironmentProfile(name, environmentTypeId);
        entries.put(profile.getId(), profile);
        return profile;
    }

    public void removeProfile(@NotNull String profileId) {
        selections.values().forEach(selection ->
                selection.values().removeIf(id -> Objects.equals(id, profileId)));
        entries.remove(profileId);
    }

    @Nullable
    public LiquibaseEnvironmentProfile getSelectedProfile(
            @NotNull ConnectionId connectionId,
            @NotNull SchemaId schemaId) {
        Map<SchemaId, String> connectionSelections = selections.get(connectionId);
        String profileId = connectionSelections == null ? null : connectionSelections.get(schemaId);
        return profileId == null ? null : entries.get(profileId);
    }

    public void rememberProfile(
            @NotNull ConnectionId connectionId,
            @NotNull SchemaId schemaId,
            @Nullable LiquibaseEnvironmentProfile profile) {
        if (profile == null) return;
        selections.computeIfAbsent(connectionId, id -> new LinkedHashMap<>()).put(schemaId, profile.getId());
    }

    public void removeOrphanedProfiles(@NotNull EnvironmentTypeBundle environmentTypes) {
        Set<EnvironmentTypeId> validTypeIds = new HashSet<>();
        validTypeIds.add(EnvironmentTypeId.DEFAULT);
        environmentTypes.getEnvironmentTypes().forEach(environmentType -> validTypeIds.add(environmentType.getId()));
        entries.values().removeIf(profile -> !validTypeIds.contains(profile.getEnvironmentTypeId()));
    }

    public void replaceProfile(@NotNull LiquibaseEnvironmentProfile profile) {
        entries.put(profile.getId(), profile);
    }

    public void replaceProfiles(@NotNull LiquibaseEnvironmentProfileBundle bundle) {
        entries = new LinkedHashMap<>();
        bundle.entries.forEach((id, profile) -> entries.put(id, profile.clone()));
        selections = new LinkedHashMap<>(bundle.selections);
    }

    @Override
    public void readState(@NotNull Element element) {
        entries.clear();
        selections.clear();
        for (Element profileElement : childrenOf(element, "profile")) {
            String environmentType = profileElement.getAttributeValue("environment-type");
            if (isEmpty(environmentType)) continue;
            EnvironmentTypeId environmentTypeId = EnvironmentTypeId.get(environmentType);
            LiquibaseEnvironmentProfile profile = new LiquibaseEnvironmentProfile(environmentType, environmentTypeId);
            profile.readState(profileElement);
            entries.put(profile.getId(), profile);
        }
        for (Element selectionElement : childrenOf(element, "selection")) {
            ConnectionId connectionId = connectionIdAttribute(selectionElement, "connection-id");
            SchemaId schemaId = schemaIdAttribute(selectionElement, "schema-id");
            String profileId = stringAttribute(selectionElement, "profile-id");
            if (connectionId != null && schemaId != null && entries.containsKey(profileId)) {
                selections.computeIfAbsent(connectionId, id -> new LinkedHashMap<>()).put(schemaId, profileId);
            }
        }
    }

    @Override
    public void writeState(@NotNull Element element) {
        entries.values().forEach(profile -> {
            Element profileElement = newElement(element, "profile");
            profile.writeState(profileElement);
        });
        selections.forEach((connectionId, connectionSelections) ->
                connectionSelections.forEach((schemaId, profileId) -> {
                    Element selectionElement = newElement(element, "selection");
                    setConstantAttribute(selectionElement, "connection-id", connectionId);
                    setConstantAttribute(selectionElement, "schema-id", schemaId);
                    selectionElement.setAttribute("profile-id", profileId);
                }));
    }

    @Override
    @SneakyThrows
    public LiquibaseEnvironmentProfileBundle clone() {
        LiquibaseEnvironmentProfileBundle clone = (LiquibaseEnvironmentProfileBundle) super.clone();
        clone.entries = new LinkedHashMap<>();
        entries.forEach((id, profile) -> clone.entries.put(id, profile.clone()));
        clone.selections = new LinkedHashMap<>();
        selections.forEach((connectionId, connectionSelections) ->
                clone.selections.put(connectionId, new LinkedHashMap<>(connectionSelections)));
        return clone;
    }
}
