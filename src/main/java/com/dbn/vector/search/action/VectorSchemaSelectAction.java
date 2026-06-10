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
import com.dbn.common.util.Lists;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.type.DBObjectType;
import com.dbn.vector.search.VectorSearchConsole;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;

import java.util.List;

import static com.dbn.nls.NlsResources.txt;
import static java.util.Collections.emptyList;

@BackgroundUpdate
public class VectorSchemaSelectAction extends SelectDropdownAction<DBSchema> implements VectorActionSupport {

    public VectorSchemaSelectAction() {
        super(txt("app.vector.action.VectorSearchSchema"));
    }

    @Override
    protected List<DBSchema> getObjects(DataContext dataContext) {
        VectorSearchConsole console = getConsole(dataContext);
        if (console == null) return emptyList();

        List<DBSchema> schemas = console.getConnection().getObjectBundle().getSchemas();
        return Lists.filter(schemas, s -> !s.isSystemSchema());
    }

    @Override
    protected DBSchema getSelectedObject(AnActionEvent e) {
        VectorSearchConsole console = getConsole(e);
        if (console == null) return null;

        return console.getSelectedSchema();
    }

    @Override
    protected void setSelectedObject(AnActionEvent e, DBSchema schema) {
        VectorSearchConsole console = getConsole(e);
        if (console == null) return;

        console.setSelectedSchema(schema);
    }

    @Override
    protected boolean isLoading(AnActionEvent e) {
        VectorSearchConsole console = getConsole(e);
        if (console == null) return false;

        DBObjectList<DBObject> schemaList = console.getConnection().getObjectBundle().getObjectList(DBObjectType.SCHEMA);
        return schemaList != null && schemaList.isLoading();
    }

    @Override
    protected boolean isEnabled(AnActionEvent e) {
        VectorSearchConsole console = getConsole(e);
        if (console == null) return false;

        if (console.isSearching()) return false;

        return true;
    }

    @Override
    protected String getEmptySelectionText(AnActionEvent e) {
        return txt("app.vector.action.Schema");
    }

    @Override
    protected String getDescription(AnActionEvent e) {
        return txt("app.vector.tooltip.VectorTableSchema");
    }
}
