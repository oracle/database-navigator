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

package com.dbn.object.common.ui;

import com.dbn.common.filter.Filter;
import com.dbn.common.text.TextContent;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

@Getter
public class DBObjectSelectionInput<T extends DBObject> {
    private final ConnectionRef connection;
    private final DBObjectType objectType;
    private TextContent hint;
    private Filter<DBSchema> schemaFilter;
    private Predicate<DBSchema> schemaPreselector;

    private Filter<T> objectFilter;
    private Predicate<T> objectPreselector;

    public DBObjectSelectionInput(@NotNull ConnectionHandler connection, @NotNull DBObjectType objectType) {
        this.connection = connection.ref();
        this.objectType = objectType;
    }

    @NotNull
    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    public DBObjectSelectionInput<T> withHint(TextContent hint) {
        this.hint = hint;
        return this;
    }

    public DBObjectSelectionInput<T> withSchemaFilter(Filter<DBSchema> schemaFilter) {
        this.schemaFilter = schemaFilter;
        return this;
    }

    public DBObjectSelectionInput<T> withObjectFilter(Filter<T> objectFilter) {
        this.objectFilter = objectFilter;
        return this;
    }

    public DBObjectSelectionInput<T> withObjectPreselector(Predicate<T> objectPreselector) {
        this.objectPreselector = objectPreselector;
        return this;
    }

    public DBObjectSelectionInput<T> withSchemaPreselector(Predicate<DBSchema> schemaPreselector) {
        this.schemaPreselector = schemaPreselector;
        return this;
    }
}
