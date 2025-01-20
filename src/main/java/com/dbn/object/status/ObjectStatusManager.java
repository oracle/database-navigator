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

package com.dbn.object.status;

import com.dbn.DatabaseNavigator;
import com.dbn.browser.model.BrowserTreeEventListener;
import com.dbn.browser.model.BrowserTreeNode;
import com.dbn.common.component.Components;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.load.ProgressMonitor;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Progress;
import com.dbn.common.ui.tree.TreeEventType;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.DatabaseEntity;
import com.dbn.connection.Resources;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.database.interfaces.DatabaseMetadataInterface;
import com.dbn.editor.DBContentType;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.common.property.DBObjectProperty;
import com.dbn.object.common.status.DBObjectStatus;
import com.dbn.object.common.status.DBObjectStatusHolder;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import static com.dbn.common.Priority.LOW;
import static com.dbn.common.notification.NotificationGroup.BROWSER;
import static com.dbn.database.DatabaseFeature.OBJECT_INVALIDATION;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

@State(
    name = ObjectStatusManager.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class ObjectStatusManager extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.ObjectStatusManager";

    private ObjectStatusManager(final Project project) {
        super(project, COMPONENT_NAME);
    }

    public static ObjectStatusManager getInstance(@NotNull Project project) {
        return Components.projectService(project, ObjectStatusManager.class);
    }

    public void refreshObjectsStatus(DBSchema schema) throws SQLException {
        DatabaseInterfaceInvoker.schedule(LOW,
                txt("prc.objects.title.RefreshingObjectsStatus"),
                txt("prc.objects.text.RefreshingObjectsStatus", schema.getQualifiedNameWithType()),
                getProject(),
                schema.getConnectionId(),
                conn -> refreshObjectsStatus(schema, conn));
    }

    public void refreshObjectsStatus(ConnectionHandler connection, @Nullable DBSchemaObject requester) {
        if (!OBJECT_INVALIDATION.isSupported(connection)) return;

        Background.run(() -> {
            try {
                List<DBSchema> schemas = requester == null ?
                        connection.getObjectBundle().getSchemas() :
                        requester.getReferencingSchemas();

                DatabaseInterfaceInvoker.schedule(LOW,
                        txt("prc.objects.title.RefreshingObjectsStatus"),
                        txt("prc.objects.text.RefreshingObjectsStatus", connection.getName()),
                        getProject(),
                        connection.getConnectionId(),
                        conn -> refreshObjectStatus(conn, schemas));
            } catch (SQLException e) {
                conditionallyLog(e);
                sendErrorNotification(BROWSER, txt("ntf.browser.error.FailedToRefreshObjectStatus", e));
            }
        });
    }

    private void refreshObjectStatus(DBNConnection conn, List<DBSchema> schemas) throws SQLException {
        int size = schemas.size();
        for (int i = 0; i < size; i++) {
            DBSchema schema = schemas.get(i);
            ProgressMonitor.setProgressText(txt("prc.objects.text.RefreshingSchemaObjectsStatus", schema.getQualifiedNameWithType()));
            ProgressMonitor.setProgressFraction(Progress.progressOf(i, size));
            refreshObjectsStatus(schema, conn);
        }
    }


    private void refreshObjectsStatus(DBSchema schema, DBNConnection conn) throws SQLException {
        Set<DatabaseEntity> entities = schema.resetObjectsStatus();
        refreshValidStatus(schema, entities, conn);
        refreshDebugStatus(schema, entities, conn);
        refreshBrowserNodes(entities);
    }

    private void refreshBrowserNodes(Set<DatabaseEntity> entities) {
        Project project = getProject();
        Background.run(() ->
                entities.forEach(n -> {
                    if (n instanceof BrowserTreeNode) {
                        BrowserTreeNode node = (BrowserTreeNode) n;
                        ProjectEvents.notify(project, BrowserTreeEventListener.TOPIC,
                                listener -> listener.nodeChanged(node, TreeEventType.NODES_CHANGED));
                    }
                }));
    }

    private void refreshValidStatus(DBSchema schema, Set<DatabaseEntity> entities, DBNConnection conn) throws SQLException {
        ResultSet resultSet = null;
        try {
            DatabaseMetadataInterface metadata = schema.getMetadataInterface();
            resultSet = metadata.loadInvalidObjects(schema.getName(), conn);
            while (resultSet != null && resultSet.next()) {
                String objectName = resultSet.getString("OBJECT_NAME");
                DBSchemaObject schemaObject = schema.getChildObjectNoLoad(objectName);
                if (schemaObject != null && schemaObject.is(DBObjectProperty.INVALIDABLE)) {
                    DBObjectStatusHolder objectStatus = schemaObject.getStatus();
                    boolean statusChanged;

                    if (schemaObject.getContentType().isBundle()) {
                        String objectType = resultSet.getString("OBJECT_TYPE");
                        statusChanged = objectType.contains("BODY") ?
                                objectStatus.set(DBContentType.CODE_BODY, DBObjectStatus.VALID, false) :
                                objectStatus.set(DBContentType.CODE_SPEC, DBObjectStatus.VALID, false);
                    } else {
                        statusChanged = objectStatus.set(DBObjectStatus.VALID, false);
                    }
                    if (statusChanged) {
                        entities.add(schemaObject.getParent());
                    }
                }
            }
        } finally {
            Resources.close(resultSet);
        }
    }

    private void refreshDebugStatus(DBSchema schema, Set<DatabaseEntity> entities, DBNConnection conn) throws SQLException {
        ResultSet resultSet = null;
        try {
            DatabaseMetadataInterface metadata = schema.getMetadataInterface();
            resultSet = metadata.loadDebugObjects(schema.getName(), conn);
            while (resultSet != null && resultSet.next()) {
                String objectName = resultSet.getString("OBJECT_NAME");
                DBSchemaObject schemaObject = schema.getChildObjectNoLoad(objectName);
                if (schemaObject != null && schemaObject.is(DBObjectProperty.DEBUGABLE)) {
                    DBObjectStatusHolder objectStatus = schemaObject.getStatus();
                    boolean statusChanged;

                    if (schemaObject.getContentType().isBundle()) {
                        String objectType = resultSet.getString("OBJECT_TYPE");
                        statusChanged = objectType.contains("BODY") ?
                                objectStatus.set(DBContentType.CODE_BODY, DBObjectStatus.DEBUG, true) :
                                objectStatus.set(DBContentType.CODE_SPEC, DBObjectStatus.DEBUG, true);
                    } else {
                        statusChanged = objectStatus.set(DBObjectStatus.DEBUG, true);
                    }
                    if (statusChanged) {
                        entities.add(schemaObject.getParent());
                    }
                }
            }
        } finally {
            Resources.close(resultSet);
        }
    }

    @Override
    public Element getComponentState() {
        return null;
    }

    @Override
    public void loadComponentState(@NotNull final Element element) {
    }

}
