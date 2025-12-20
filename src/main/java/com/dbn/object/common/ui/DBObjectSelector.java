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

package com.dbn.object.common.ui;

import com.dbn.common.routine.Consumer;
import com.dbn.common.ui.ValueFactory;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.event.ObjectChangeEvent;
import com.dbn.object.factory.DatabaseObjectFactory;
import com.dbn.object.factory.model.DBObjectSpec;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

import static com.dbn.common.ui.ValueSelectorOption.HIDE_DESCRIPTION;
import static com.dbn.common.util.Unsafe.cast;

@Getter
public class DBObjectSelector<T extends DBObject> extends DBNComboBox<T> {
    private DBObjectType objectType;
    private Supplier<DBObjectSpec> valueFactoryInput;

    private Supplier<ConnectionHandler> connectionContext;
    private Supplier<DBSchema> schemaContext;

    public DBObjectSelector() {
        set(HIDE_DESCRIPTION, true);
    }

    public DBObjectSelector<T> initialize(
            @NotNull DBNForm parentForm,
            @NotNull DBObjectType objectType){

        this.objectType = objectType;

        if (objectType.isSchemaObject()) {
            ObjectChangeEvent.subscribe(
                    parentForm.getProject(),
                    parentForm,
                    () -> getConnectionId(),
                    () -> getSchemaId(),
                    () -> getObjectType(),
                    () -> reloadValues());
        }

        return this;
    }

    public DBObjectSelector<T> withConnectionContext(Supplier<ConnectionHandler> connectionContext) {
        this.connectionContext = connectionContext;
        return this;
    }

    public DBObjectSelector<T> withSchemaContext(Supplier<DBSchema> schemaContext) {
        this.schemaContext = schemaContext;
        return this;
    }

    public DBObjectSelector<T> withValueLoader(Supplier<List<T>> valueLoader) {
        return cast(super.withValueLoader(valueLoader));
    }

    public DBObjectSelector<T> withValuePreselector(Supplier<String> preselectName) {
        return cast(super.withValuePreselector(o -> o.getName().equalsIgnoreCase(preselectName.get())));
    }

    public DBObjectSelector<T> withObjectValueFactory(String actionName) {
        return cast(super.withValueFactory(ValueFactory.create(actionName, () -> openObjectFactory(getSchema()))));
    }

    public DBObjectSelector<T> withValueFactoryInput(Supplier<DBObjectSpec> initialFactoryInput) {
        this.valueFactoryInput = initialFactoryInput;
        return this;
    }

    @Nullable
    public ConnectionHandler getConnection() {
        return connectionContext == null ? null : connectionContext.get();
    }

    @Nullable
    public DBSchema getSchema() {
        return schemaContext == null ? null : schemaContext.get();
    }

    private ConnectionId getConnectionId() {
        ConnectionHandler connection = getConnection();
        return connection == null ? null : connection.getConnectionId();
    }

    public SchemaId getSchemaId() {
        DBSchema schema = getSchema();
        return schema == null ? null : schema.getSchemaId();
    }

    @Nullable
    public Project getProject() {
        ConnectionHandler connection = getConnection();
        return connection == null ? null : connection.getProject();
    }

    private T openObjectFactory(DBSchema schema) {
        Project project = getProject();
        if (project == null) throw new IllegalStateException("Database context not initialized");

        DBObjectSpec initialInput = valueFactoryInput == null ? null : valueFactoryInput.get();

        DatabaseObjectFactory factoryManager = DatabaseObjectFactory.getInstance(project);
        factoryManager.openFactoryInputDialog(
                schema,
                objectType,
                initialInput,
                reloadConsumer());

        return null; // async handling
    }

    private @NotNull Consumer<String> reloadConsumer() {
        return n -> reloadValues(m -> m.getName().equalsIgnoreCase(n));
    }
}
