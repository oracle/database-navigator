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

import com.dbn.common.latent.Loader;
import com.dbn.common.routine.Consumer;
import com.dbn.common.ui.ValueFactory;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.event.ObjectChangeEvent;
import com.dbn.object.factory.DatabaseObjectFactory;
import com.dbn.object.factory.ObjectFactoryInput;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.function.Supplier;

import static com.dbn.common.ui.ValueSelectorOption.HIDE_DESCRIPTION;

public class DBObjectSelector<T extends DBObject> extends DBNComboBox<T> {
    private ConnectionRef connection;
    private DBObjectType objectType;
    private Supplier<ObjectFactoryInput> initialFactoryInput;

    public DBObjectSelector() {
        set(HIDE_DESCRIPTION, true);
    }

    public void initialize(
            Disposable parentDisposable,
            ConnectionHandler connection,
            DBObjectType objectType,
            Loader<List<T>> valueLoader,
            Supplier<String> preselectName){

        this.connection = ConnectionRef.of(connection);
        this.objectType = objectType;
        super.initialize(valueLoader, o -> o.getName().equalsIgnoreCase(preselectName.get()));

        ObjectChangeEvent.subscribe(parentDisposable,
                getConnection(),
                objectType,
                () -> reloadValues());
    }

    public ConnectionHandler getConnection() {
        return ConnectionRef.ensure(connection);
    }

    public Project getProject() {
        return getConnection().getProject();
    }

    public void initValueFactory(String actionName, Supplier<DBSchema> schema) {
        initValueFactory(actionName, schema, () -> null);
    }

    public void initValueFactory(String actionName, Supplier<DBSchema> schema, Supplier<ObjectFactoryInput> initialInput) {
        this.initialFactoryInput = initialInput;
        var valueFactory = ValueFactory.create(actionName, () -> openObjectFactory(schema.get()));
        setValueFactory(valueFactory);
    }

    private T openObjectFactory(DBSchema schema) {
        Project project = getProject();
        ObjectFactoryInput initialInput = initialFactoryInput.get();

        DatabaseObjectFactory factoryManager = DatabaseObjectFactory.getInstance(project);
        factoryManager.openFactoryInputDialog(
                schema,
                objectType,
                initialInput,
                reloadConsumer());

        return null; // async handling
    }

    private @NonNull Consumer<String> reloadConsumer() {
        return n -> reloadValues(m -> m.getName().equalsIgnoreCase(n));
    }
}
