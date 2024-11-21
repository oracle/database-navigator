/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.navigation.object;

import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.load.ProgressMonitor;
import com.dbn.common.project.ProjectRef;
import com.dbn.common.sign.Signed;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionLoadListener;
import com.dbn.connection.ConnectionRef;
import com.dbn.navigation.options.ObjectsLookupSettings;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.dbn.options.ProjectSettings;
import com.intellij.ide.util.gotoByName.ChooseByNameModel;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static com.dbn.common.dispose.Failsafe.guarded;

public class DBObjectLookupModel extends StatefulDisposableBase implements ChooseByNameModel, Signed {
    private static final Object[] EMPTY_ARRAY = new Object[0];
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private final ProjectRef project;
    private final ConnectionRef selectedConnection;
    private final DBObjectRef<DBSchema> selectedSchema;
    private final ObjectsLookupSettings settings;
    private final DBObjectLookupData data = new DBObjectLookupData();

    @Getter
    private int signature;

    public DBObjectLookupModel(@NotNull Project project, @Nullable ConnectionHandler selectedConnection, DBSchema selectedSchema) {
        this.project = ProjectRef.of(project);
        this.selectedConnection = ConnectionRef.of(selectedConnection);
        this.selectedSchema = DBObjectRef.of(selectedSchema);
        this.settings = ProjectSettings.get(project).getNavigationSettings().getObjectsLookupSettings();

        ConnectionId connectionId = getSelectedConnectionId();
        ProjectEvents.subscribe(project, this,
                ConnectionLoadListener.TOPIC,
                ConnectionLoadListener.create(connectionId, () -> signature++));
        Disposer.register(this, data);
    }

    @Override
    public String getPromptText() {
        ConnectionHandler selectedConnection = getSelectedConnection();
        String connectionIdentifier = selectedConnection == null || selectedConnection.isVirtual() ?
                "All Connections" :
                selectedConnection.getName();
        return "Enter database object name (" + connectionIdentifier + (selectedSchema == null ? "" : " / " + selectedSchema.getObjectName()) + ")";
    }

    protected ConnectionHandler getSelectedConnection() {
        return selectedConnection.get();
    }

    protected ConnectionId getSelectedConnectionId() {
        return selectedConnection == null ? null : selectedConnection.getConnectionId();
    }

    @Nullable
    public DBSchema getSelectedSchema() {
        return DBObjectRef.get(selectedSchema);
    }

    @NotNull
    @Override
    public String getNotInMessage() {
        return "No database object matching criteria";
    }

    @NotNull
    @Override
    public String getNotFoundMessage() {
        return "Database object not found";
    }

    @Override
    public String getCheckBoxName() {
        return getSettings().getForceDatabaseLoad().value() ? "Load database objects" : null;
    }

    @Override
    public boolean loadInitialCheckBoxState() {
        return false;
    }

    @Override
    public void saveInitialCheckBoxState(boolean state) {
    }

    @NotNull
    @Override
    public ListCellRenderer getListCellRenderer() {
        return DBObjectListCellRenderer.INSTANCE;
    }

    @Override
    public boolean willOpenEditor() {
        return false;
    }

    @Override
    public boolean useMiddleMatching() {
        return true;
    }

    @Override
    @NotNull
    public String[] getNames(boolean checkBoxState) {
        return guarded(EMPTY_STRING_ARRAY, () -> {
            boolean databaseLoadActive = getSettings().getForceDatabaseLoad().value();
            boolean forceLoad = checkBoxState && databaseLoadActive;

            if (!forceLoad && selectedSchema != null) {
                // touch the schema for next load
                selectedSchema.ensure().getChildren();
            }
            checkCancelled();

            if (data.getSignature() != signature) {
                data.setSignature(signature);
                DBObjectLookupScanner.scan(this, forceLoad);
            }

            return data.names();
        });
    }


    public void accept(DBObject object) {
        data.accept(object);
    }

    @NotNull
    public Project getProject() {
        return project.ensure();
    }

    @Override
    @NotNull
    public Object[] getElementsByName(@NotNull String name, boolean checkBoxState, @NotNull String pattern) {
        return guarded(EMPTY_ARRAY, data, d -> d.elements(name));
    }

    protected boolean isListLookupEnabled(DBObjectType objectType) {
        boolean enabled = isObjectLookupEnabled(objectType);
        if (enabled) return true;

        for (DBObjectType childObjectType : objectType.getChildren()) {
            if (isListLookupEnabled(childObjectType)) {
                return true;
            }
        }
        return enabled;
    }

    protected boolean isObjectLookupEnabled(DBObjectType objectType) {
        return getSettings().isEnabled(objectType);
    }

    @NotNull
    public ObjectsLookupSettings getSettings() {
        return Failsafe.nn(settings);
    }

    @Override
    public String getElementName(@NotNull Object element) {
        if (element instanceof DBObject) {
            DBObject object = (DBObject) element;
            return object.getQualifiedName();
        }

        return element.toString();
    }

    @Override
    @NotNull
    public String[] getSeparators() {
        return new String[]{"."};
    }

    @Override
    public String getFullName(@NotNull Object element) {
        return getElementName(element);
    }

    @Override
    public String getHelpId() {
        return null;
    }

    @Override
    public void disposeInner() {
        nullify();
    }

    public void checkCancelled() {
        checkDisposed();
        ProgressMonitor.checkCancelled();
    }
}
