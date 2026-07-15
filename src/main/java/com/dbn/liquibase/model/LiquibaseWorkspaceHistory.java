/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 */

package com.dbn.liquibase.model;

import com.dbn.common.util.Cloneable;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import lombok.SneakyThrows;
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

/** Persistent execution history associated with a Liquibase workspace. */
public class LiquibaseWorkspaceHistory implements Cloneable<LiquibaseWorkspaceHistory> {
    private static final int MAX_CHECKPOINT_TAGS = 10;

    private Map<ConnectionId, Map<SchemaId, List<String>>> checkpointTags = new LinkedHashMap<>();

    @NotNull
    public List<String> getCheckpointTags(
            @NotNull ConnectionId connectionId,
            @NotNull SchemaId schemaId) {
        Map<SchemaId, List<String>> schemaTags = checkpointTags.get(connectionId);
        List<String> tags = schemaTags == null ? null : schemaTags.get(schemaId);
        return tags == null ? List.of() : new ArrayList<>(tags);
    }

    public void rememberCheckpointTag(
            @NotNull ConnectionId connectionId,
            @NotNull SchemaId schemaId,
            @NotNull String tag) {
        if (isEmpty(tag)) return;

        List<String> tags = checkpointTags
                .computeIfAbsent(connectionId, id -> new LinkedHashMap<>())
                .computeIfAbsent(schemaId, id -> new ArrayList<>());
        tags.remove(tag);
        tags.add(0, tag);
        if (tags.size() > MAX_CHECKPOINT_TAGS) tags.remove(tags.size() - 1);
    }

    public void readState(@NotNull Element element) {
        checkpointTags.clear();
        for (Element tagElement : childrenOf(element, "checkpoint-tag")) {
            ConnectionId connectionId = connectionIdAttribute(tagElement, "connection-id");
            SchemaId schemaId = constantAttribute(tagElement, "schema-id", SchemaId.class);
            String tag = stringAttribute(tagElement, "value");
            if (connectionId != null && schemaId != null && !isEmpty(tag)) {
                rememberCheckpointTag(connectionId, schemaId, tag);
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

    @Override
    @SneakyThrows
    public LiquibaseWorkspaceHistory clone() {
        LiquibaseWorkspaceHistory clone = (LiquibaseWorkspaceHistory) super.clone();
        clone.checkpointTags = new LinkedHashMap<>();
        checkpointTags.forEach((connectionId, schemaTags) -> {
            Map<SchemaId, List<String>> tags = new LinkedHashMap<>();
            schemaTags.forEach((schemaId, values) -> tags.put(schemaId, new ArrayList<>(values)));
            clone.checkpointTags.put(connectionId, tags);
        });
        return clone;
    }
}
