/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.object.factory.model;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.language.common.QuotePair;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
public abstract class DBObjectSpec {
    private DBObjectSpec parent;
    private ConnectionId connectionId;
    private DBObjectType objectType;

    @NonNls
    private String objectName;
    private int index;
    private boolean readonly;

    protected DBObjectSpec(@NotNull ConnectionId connectionId, DBObjectType objectType) {
        this.connectionId = connectionId;
        this.objectType = objectType;
    }

    protected DBObjectSpec(@NotNull DBObjectSpec parent, DBObjectType objectType) {
        this(parent.getConnectionId(), objectType);
        this.parent = parent;
    }

    public ConnectionHandler getConnection() {
        return ConnectionHandler.ensure(connectionId);
    }

    public Project getProject() {
        return getConnection().getProject();
    }

    public String getObjectPath() {
        return objectName;
    }

    public String getObjectTypeName() {
        return objectType.getName();
    }

    public String getObjectDescription() {
        return getObjectTypeName() + " \"" + getObjectPath() + "\"";
    }

    @Override
    public String toString() {
        return objectType.getName() + " " + objectName;
    }

    public String getObjectName(boolean quoted) {
        if (!quoted) return objectName;

        QuotePair quotes = getConnection().getCompatibilityInterface().getDefaultIdentifierQuotes();
        return quotes.quote(objectName);
    }
}
