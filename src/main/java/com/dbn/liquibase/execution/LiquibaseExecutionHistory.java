/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 */

package com.dbn.liquibase.execution;

import com.dbn.common.state.PersistentStateElement;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.connectionIdAttribute;
import static com.dbn.common.options.setting.Settings.constantAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setConstantAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Strings.isEmpty;

/** Persistent Liquibase execution history associated with database connection schemas. */
public class LiquibaseExecutionHistory implements PersistentStateElement {
    private static final int MAX_TAGS = 10;

    private final Map<ConnectionId, Map<SchemaId, List<String>>> checkpointTags = new LinkedHashMap<>();

    @NotNull
    public List<String> getTags(
            @NotNull ConnectionId connectionId,
            @NotNull SchemaId schemaId) {
        Map<SchemaId, List<String>> schemaTags = checkpointTags.get(connectionId);
        List<String> tags = schemaTags == null ? null : schemaTags.get(schemaId);
        return tags == null ? List.of() : new ArrayList<>(tags);
    }

    public void rememberTag(
            @NotNull ConnectionId connectionId,
            @NotNull SchemaId schemaId,
            @NotNull String tag) {
        if (isEmpty(tag)) return;

        List<String> tags = checkpointTags
                .computeIfAbsent(connectionId, id -> new LinkedHashMap<>())
                .computeIfAbsent(schemaId, id -> new ArrayList<>());
        tags.remove(tag);
        tags.add(0, tag);
        if (tags.size() > MAX_TAGS) tags.remove(tags.size() - 1);
    }

    public void removeTag(
            @NotNull ConnectionId connectionId,
            @NotNull SchemaId schemaId,
            @NotNull String tag) {
        Map<SchemaId, List<String>> schemaTags = checkpointTags.get(connectionId);
        if (schemaTags == null) return;

        List<String> tags = schemaTags.get(schemaId);
        if (tags == null) return;

        tags.remove(tag);
        if (tags.isEmpty()) schemaTags.remove(schemaId);
        if (schemaTags.isEmpty()) checkpointTags.remove(connectionId);
    }

    public void removeConnection(@NotNull ConnectionId connectionId) {
        checkpointTags.remove(connectionId);
    }

    public void readState(@NotNull Element element) {
        checkpointTags.clear();
        for (Element tagElement : childrenOf(element, "checkpoint-tag")) {
            ConnectionId connectionId = connectionIdAttribute(tagElement, "connection-id");
            SchemaId schemaId = constantAttribute(tagElement, "schema-id", SchemaId.class);
            String tag = stringAttribute(tagElement, "value");
            if (connectionId != null && schemaId != null && !isEmpty(tag)) {
                rememberTag(connectionId, schemaId, tag);
            }
        }
    }

    public void writeState(@NotNull Element element) {
        checkpointTags.forEach((connectionId, schemaTags) ->
                schemaTags.forEach((schemaId, tags) -> tags.forEach(tag -> {
                    Element tagElement = newElement(element, "checkpoint-tag");
                    setConstantAttribute(tagElement, "connection-id", connectionId);
                    setConstantAttribute(tagElement, "schema-id", schemaId);
                    setStringAttribute(tagElement, "value", tag);
                })));
    }
}
