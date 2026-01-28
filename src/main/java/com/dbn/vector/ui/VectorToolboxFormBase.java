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

package com.dbn.vector.ui;

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionRef;
import com.dbn.connection.SchemaId;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBObjectBundle;
import com.dbn.vector.model.VectorEmbeddingRequest;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public abstract class VectorToolboxFormBase extends DBNFormBase {
    private final ConnectionRef connection;

    public VectorToolboxFormBase(@Nullable Disposable parent, ConnectionHandler connection) {
        super(parent);
        this.connection = connection.ref();
    }

    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    public ConnectionId getConnectionId() {
        return connection.getId();
    }

    protected VectorEmbeddingRequest getEmbeddingRequest() {
        VectorToolboxForm rootForm = getToolboxForm();
        return rootForm.getEmbeddingRequest();
    }

    protected VectorToolboxForm getToolboxForm() {
        return ensureParentFrom(VectorToolboxForm.class);
    }

    protected static String getSelectedObjectName(DBNComboBox<? extends DBObject> comboBox, String defaultName) {
        DBObject selection = ComboBoxes.getSelection(comboBox);
        String objectName = getObjectName(selection);
        return objectName == null ? defaultName : objectName;
    }

    protected static @Nullable String getObjectName(@Nullable DBObject object) {
        return object == null ? null : object.getName();
    }

    protected List<DBSchema> loadSchemas() {
        DBObjectBundle objectBundle = getConnection().getObjectBundle();
        return objectBundle.getSchemas();
    }

    protected List<DBTable> loadTables() {
        DBSchema schema = getSelectedSchema();
        return schema == null ?
                Collections.emptyList() :
                schema.getTables();
    }

    public DBSchema getSelectedSchema() {
        return null;
    }

    public SchemaId getSelectedSchemaId() {
        DBSchema schema = getSelectedSchema();
        return schema == null ? null : schema.getSchemaId();
    }

/*
    protected static boolean matchesObjectName(@Nullable DBObject object, String name) {
        return object != null && Strings.equalsIgnoreCase(object.getName(), name);
    }*/

}
