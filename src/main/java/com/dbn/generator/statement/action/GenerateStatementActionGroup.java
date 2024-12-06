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

package com.dbn.generator.statement.action;

import com.dbn.browser.DatabaseBrowserManager;
import com.dbn.database.DatabaseFeature;
import com.dbn.object.DBColumn;
import com.dbn.object.DBDataset;
import com.dbn.object.DBProgram;
import com.dbn.object.DBTable;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObject;
import com.intellij.openapi.actionSystem.DefaultActionGroup;

import java.util.List;

public class GenerateStatementActionGroup extends DefaultActionGroup {

    public GenerateStatementActionGroup(DBObject object) {
        super("Extract SQL Statement", true);
        if (object instanceof DBColumn || object instanceof DBDataset) {
            List<DBObject> selectedObjects = DatabaseBrowserManager.getInstance(object.getProject()).getSelectedObjects();
            add(new GenerateSelectStatementAction(selectedObjects));
        }

        if (object instanceof DBTable) {
            DBTable table = (DBTable) object;
            add(new GenerateInsertStatementAction(table));
        }

        if (object instanceof DBSchemaObject &&
                !(object.getParentObject() instanceof DBProgram) &&
                DatabaseFeature.OBJECT_DDL_EXTRACTION.isSupported(object)) {
            if (getChildrenCount() > 1) {
                addSeparator();
            }
            add(new GenerateDDLStatementAction(object));
        }
    }
}