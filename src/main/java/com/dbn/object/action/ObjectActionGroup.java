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

package com.dbn.object.action;

import com.dbn.connection.ConnectionHandler;
import com.dbn.editor.DBContentType;
import com.dbn.execution.compiler.action.CompileActionGroup;
import com.dbn.execution.method.action.MethodDebugAction;
import com.dbn.execution.method.action.MethodRunAction;
import com.dbn.execution.method.action.ProgramMethodDebugAction;
import com.dbn.execution.method.action.ProgramMethodRunAction;
import com.dbn.generator.statement.action.GenerateStatementActionGroup;
import com.dbn.object.DBColumn;
import com.dbn.object.DBConsole;
import com.dbn.object.DBMethod;
import com.dbn.object.DBProgram;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.common.list.DBObjectNavigationList;
import com.dbn.object.common.list.action.HideAuditColumnsToggleAction;
import com.dbn.object.common.list.action.HideEmptySchemasToggleAction;
import com.dbn.object.common.list.action.HidePseudoColumnsToggleAction;
import com.dbn.object.dependency.action.ObjectDependencyTreeAction;
import com.dbn.object.type.DBObjectType;
import com.dbn.vfs.DBConsoleType;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAware;

import java.util.List;

import static com.dbn.database.DatabaseFeature.CONSTRAINT_MANIPULATION;
import static com.dbn.database.DatabaseFeature.DEBUGGING;
import static com.dbn.database.DatabaseFeature.OBJECT_DEPENDENCIES;
import static com.dbn.database.DatabaseFeature.OBJECT_DISABLING;
import static com.dbn.database.DatabaseFeature.OBJECT_INVALIDATION;
import static com.dbn.database.DatabaseFeature.OBJECT_SOURCE_EDITING;
import static com.dbn.editor.DBContentType.CODE;
import static com.dbn.editor.DBContentType.CODE_AND_DATA;
import static com.dbn.editor.DBContentType.CODE_SPEC_AND_BODY;
import static com.dbn.editor.DBContentType.DATA;
import static com.dbn.object.common.property.DBObjectProperty.COMPILABLE;
import static com.dbn.object.common.property.DBObjectProperty.DISABLEABLE;
import static com.dbn.object.common.property.DBObjectProperty.EDITABLE;
import static com.dbn.object.common.property.DBObjectProperty.REFERENCEABLE;
import static com.dbn.object.common.property.DBObjectProperty.SCHEMA_OBJECT;
import static com.dbn.object.common.property.DBObjectProperty.COMPILABLE;
import static com.dbn.object.common.property.DBObjectProperty.DISABLEABLE;
import static com.dbn.object.common.property.DBObjectProperty.EDITABLE;
import static com.dbn.object.common.property.DBObjectProperty.REFERENCEABLE;
import static com.dbn.object.common.property.DBObjectProperty.SCHEMA_OBJECT;

public class ObjectActionGroup extends DefaultActionGroup implements DumbAware {

    public ObjectActionGroup(DBObject object) {
        addObjectManagementActions(object);
        addMethodActions(object);
        addProgramActions(object);
        addDependencyActions(object);
        addNavigationActions(object);
        addConsoleActions(object);
        addCodeGeneratorActions(object);
        addObjectListActions(object);
        addObjectPropertiesActions(object);
    }

    private void addObjectManagementActions(DBObject object) {
        if (object instanceof DBSchemaObject) {
            DBSchemaObject schemaObject = (DBSchemaObject) object;

            if (object.is(EDITABLE)) {
                DBContentType contentType = schemaObject.getContentType();
                if (contentType.isOneOf(DATA, CODE_AND_DATA)) {
                    add(new ObjectEditDataAction(schemaObject));
                }

                if (contentType.isOneOf(CODE, CODE_AND_DATA, CODE_SPEC_AND_BODY)) {
                    if (OBJECT_SOURCE_EDITING.isSupported(object)) {
                        add(new ObjectEditCodeAction(schemaObject));
                    }
                }
            }

            if (object.is(COMPILABLE) && OBJECT_INVALIDATION.isSupported(object)) {
                add(new CompileActionGroup(schemaObject));
            }

            if (object.is(DISABLEABLE) && OBJECT_DISABLING.isSupported(object)) {
                add(new ObjectEnableDisableAction(schemaObject));
            }

            if (object.is(SCHEMA_OBJECT) &&
                    !object.getSchema().isSystemSchema() &&
                    !object.getSchema().isPublicSchema()) {
                if (object.getObjectType() != DBObjectType.CONSTRAINT || CONSTRAINT_MANIPULATION.isSupported(object)) {
                    add(new ObjectDropAction((DBSchemaObject) object));
                }

                //add(new TestAction(object));
            }
        }
    }

    private void addMethodActions(DBObject object) {
        if (object instanceof DBMethod) {
            addSeparator();
            DBMethod method = (DBMethod) object;
            add(new MethodRunAction(method, false));
            if (DEBUGGING.isSupported(object)) {
                add(new MethodDebugAction(method, false));
            }
        }
    }

    private void addProgramActions(DBObject object) {
        if (object instanceof DBProgram && object.is(SCHEMA_OBJECT)) {
            addSeparator();
            add(new ProgramMethodRunAction((DBProgram) object));
            if (DEBUGGING.isSupported(object)) {
                add(new ProgramMethodDebugAction((DBProgram) object));
            }
        }
    }

    private void addDependencyActions(DBObject object) {
        if (object instanceof DBSchemaObject) {
            if (object.is(REFERENCEABLE) && OBJECT_DEPENDENCIES.isSupported(object)) {
                addSeparator();
                add(new ObjectDependencyTreeAction((DBSchemaObject) object));
            }
        }
    }

    private void addCodeGeneratorActions(DBObject object) {
        addSeparator();
        addActionGroup(new GenerateStatementActionGroup(object));

        ActionManager actionManager = ActionManager.getInstance();
        ActionGroup actionGroup = (ActionGroup) actionManager.getAction("DBNavigator.ActionGroup.ExtractJavaCode");
        // TODO...
    }

    private void addNavigationActions(DBObject object) {
        List<DBObjectNavigationList> navigationLists = object.getNavigationLists();
        if (navigationLists != null && !navigationLists.isEmpty()) {
            if (object.isNot(REFERENCEABLE)) addSeparator();
            //add(new DbsGoToActionGroup(linkLists));
            for (DBObjectNavigationList<?> navigationList : navigationLists) {
                DBObject parentObject = object.getParentObject();
                if (navigationList.isLazy()) {
                    add(new ObjectLazyNavigationListAction(parentObject, navigationList));
                } else {
                    add(new ObjectNavigationListActionGroup(parentObject, navigationList, false));
                }
            }
        }
    }

    private void addConsoleActions(DBObject object) {
        ConnectionHandler connection = object.getConnection();
        if (object instanceof DBConsole) {
            DBConsole console = (DBConsole) object;
            add(new ConsoleRenameAction(console));
            add(new ConsoleDeleteAction(console));
            addSeparator();
            add(new ConsoleCreateAction(connection, DBConsoleType.STANDARD));
            if (DEBUGGING.isSupported(connection)) {
                add(new ConsoleCreateAction(connection, DBConsoleType.DEBUG));
            }
        }
    }

    private void addObjectListActions(DBObject object) {
        ConnectionHandler connection = object.getConnection();
        addSeparator();
        if (object instanceof DBColumn) {
            add(new HideAuditColumnsToggleAction(connection));
            add(new HidePseudoColumnsToggleAction(connection));
        } else if (object instanceof DBSchema) {
            add(new HideEmptySchemasToggleAction(connection));
        }
        add(new RefreshActionGroup(object));
    }

    private void addObjectPropertiesActions(DBObject object) {
        // TODO...
        //add(new ObjectPropertiesAction(object));
        //add(new TestAction(object));
    }

    private void addActionGroup(DefaultActionGroup actionGroup) {
        if (actionGroup.getChildrenCount() > 0) {
            add(actionGroup);
        }
    }


}
