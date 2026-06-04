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

package com.dbn.vector.search.action;

import com.dbn.common.action.BackgroundUpdate;
import com.dbn.common.action.SelectDropdownAction;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.type.DBObjectType;
import com.dbn.vector.DatabaseVectorManager;
import com.dbn.vector.search.VectorSearchConsole;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;

import java.util.List;

import static com.dbn.nls.NlsResources.txt;
import static java.util.Collections.emptyList;

@BackgroundUpdate
public class VectorTableSelectAction extends SelectDropdownAction<DBTable> implements VectorActionSupport{

    public VectorTableSelectAction() {
        super(txt("app.vector.action.VectorSearchTable"));
    }

    @Override
    protected List<DBTable> getObjects(DataContext dataContext) {
        VectorSearchConsole console = getConsole(dataContext);
        if (console == null) return emptyList();

        DBSchema selectedSchema = console.getSelectedSchema();
        if (selectedSchema == null) return emptyList();

        DatabaseVectorManager vectorManager = DatabaseVectorManager.getInstance(console.getProject());
        ConnectionId connectionId = console.getConnectionId();
        SchemaId schemaId = selectedSchema.getSchemaId();

        return vectorManager.getVectorTables(connectionId, schemaId);
    }

    @Override
    protected DBTable getSelectedObject(AnActionEvent e) {
        VectorSearchConsole console = getConsole(e);
        if (console == null) return null;

        return console.getSelectedTable();
    }

    @Override
    protected void setSelectedObject(AnActionEvent e, DBTable object) {
        VectorSearchConsole console = getConsole(e);
        if (console == null) return;

        console.setSelectedTable(object);
    }

    @Override
    protected boolean isLoading(AnActionEvent e) {
        VectorSearchConsole console = getConsole(e);
        if (console == null) return false;

        DBSchema schema = console.getSelectedSchema();
        if (schema == null) return false;

        DBObjectList<DBObject> tableList = schema.getChildObjectList(DBObjectType.TABLE);
        return tableList != null && tableList.isLoading();
    }

    @Override
    protected boolean isEnabled(AnActionEvent e) {
        VectorSearchConsole console = getConsole(e);
        if (console == null) return false;

        if (console.isSearching()) return false;

        DBSchema schema = console.getSelectedSchema();
        if (schema == null) return false;

        return true;
    }

    @Override
    protected String getEmptySelectionText(AnActionEvent e) {
        return txt("app.vector.action.Table");
    }

    @Override
    protected String getDescription(AnActionEvent e) {
        return txt("app.vector.tooltip.VectorTable");
    }
}
