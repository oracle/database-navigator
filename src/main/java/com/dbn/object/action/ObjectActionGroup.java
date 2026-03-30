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

import com.dbn.common.action.DefaultActionGroup;
import com.dbn.connection.ConnectionHandler;
import com.dbn.editor.DBContentType;
import com.dbn.event.action.ChangeNotificationsToggleAction;
import com.dbn.execution.compiler.action.CompileActionGroup;
import com.dbn.execution.java.action.JavaClassDebugAction;
import com.dbn.execution.java.action.JavaClassExecuteAction;
import com.dbn.execution.java.action.JavaClassWrapperAction;
import com.dbn.execution.java.action.JavaMethodDebugAction;
import com.dbn.execution.java.action.JavaMethodExecuteAction;
import com.dbn.execution.java.action.JavaMethodWrapperAction;
import com.dbn.execution.method.action.MethodDebugAction;
import com.dbn.execution.method.action.MethodExecuteAction;
import com.dbn.execution.method.action.ProgramMethodDebugAction;
import com.dbn.execution.method.action.ProgramMethodExecuteAction;
import com.dbn.generator.statement.action.GenerateStatementActionGroup;
import com.dbn.ml.action.AIModelPredictAction;
import com.dbn.object.DBAIModel;
import com.dbn.object.DBColumn;
import com.dbn.object.DBConsole;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBJavaResource;
import com.dbn.object.DBMethod;
import com.dbn.object.DBProgram;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.common.list.DBObjectNavigationList;
import com.dbn.object.common.list.action.HideAuditColumnsToggleAction;
import com.dbn.object.common.list.action.HideEmptySchemasToggleAction;
import com.dbn.object.common.list.action.HidePseudoColumnsToggleAction;
import com.dbn.object.dependency.action.ObjectDependencyTreeAction;
import com.dbn.object.navigation.DBObjectNavigationInfoProvider;
import com.dbn.object.navigation.DBObjectNavigationInfoProviderCache;
import com.dbn.object.type.DBObjectType;
import com.dbn.sync.java.action.JavaObjectDownloadAction;
import com.dbn.sync.java.action.JavaResourceDownloadAction;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.project.DumbAware;

import java.util.List;

import static com.dbn.database.DatabaseFeature.CONSTRAINT_MANIPULATION;
import static com.dbn.database.DatabaseFeature.DATA_CHANGE_NOTIFICATION;
import static com.dbn.database.DatabaseFeature.DEBUGGING;
import static com.dbn.database.DatabaseFeature.OBJECT_DEPENDENCIES;
import static com.dbn.database.DatabaseFeature.OBJECT_DISABLING;
import static com.dbn.database.DatabaseFeature.OBJECT_INVALIDATION;
import static com.dbn.database.DatabaseFeature.OBJECT_SOURCE_EDITING;
import static com.dbn.database.DatabaseFeature.VECTOR_SEARCH;
import static com.dbn.editor.DBContentType.CODE;
import static com.dbn.editor.DBContentType.CODE_AND_DATA;
import static com.dbn.editor.DBContentType.CODE_SPEC_AND_BODY;
import static com.dbn.editor.DBContentType.DATA;
import static com.dbn.object.common.property.DBObjectProperty.COMPILABLE;
import static com.dbn.object.common.property.DBObjectProperty.DISABLEABLE;
import static com.dbn.object.common.property.DBObjectProperty.EDITABLE;
import static com.dbn.object.common.property.DBObjectProperty.REFERENCEABLE;
import static com.dbn.object.common.property.DBObjectProperty.SCHEMA_OBJECT;
import static com.dbn.vfs.DBConsoleType.DEBUG;
import static com.dbn.vfs.DBConsoleType.SEARCH;
import static com.dbn.vfs.DBConsoleType.STANDARD;

public class ObjectActionGroup extends DefaultActionGroup implements DumbAware {

    public ObjectActionGroup(DBObject object) {
        addObjectManagementActions(object);
        addMethodActions(object);
        addProgramActions(object);
        addTableActions(object);
        addJavaActions(object);
        addAIModelActions(object);
        addDependencyActions(object);
        addNavigationActions(object);
        addConsoleActions(object);
        addCodeGeneratorActions(object);
        addObjectListActions(object);
        addObjectPropertiesActions(object);
    }

    private void addTableActions(DBObject object) {
        if (object instanceof DBTable table) {
            if (DATA_CHANGE_NOTIFICATION.isSupported(object)) {
                addSeparator();
                add(new ChangeNotificationsToggleAction(table));
            }
        }
    }

    private void addObjectManagementActions(DBObject object) {
        if (object instanceof DBSchemaObject schemaObject) {

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
                    add(new ObjectDropAction(schemaObject));
                }

                //add(new TestAction(object));
            }
        }
    }

    private void addMethodActions(DBObject object) {
        if (object instanceof DBMethod method) {
            addSeparator();
            add(new MethodExecuteAction(method, false));
            if (DEBUGGING.isSupported(object)) {
                add(new MethodDebugAction(method, false));
            }
        }
    }

    private void addProgramActions(DBObject object) {
        if (object instanceof DBProgram && object.is(SCHEMA_OBJECT)) {
            addSeparator();
            add(new ProgramMethodExecuteAction((DBProgram) object));
            if (DEBUGGING.isSupported(object)) {
                add(new ProgramMethodDebugAction((DBProgram) object));
            }
        }
    }

    private void addJavaActions(DBObject object) {
        if(object instanceof DBJavaMethod method){
            if (method.isExecutable()) {
                add(new JavaMethodExecuteAction(method, false));
                add(new JavaMethodDebugAction(method, false));
                add(new JavaMethodWrapperAction(method));
            }
        }

        if (object instanceof DBJavaClass) {
            add(new JavaObjectDownloadAction(object));
            addSeparator();
            add(new JavaClassExecuteAction((DBJavaClass) object));
            add(new JavaClassDebugAction((DBJavaClass) object));
            add(new JavaClassWrapperAction((DBJavaClass) object));
        }

        if (object instanceof DBJavaResource) {
            add(new JavaResourceDownloadAction(object));
        }
    }

    private void addAIModelActions(DBObject object) {
        if (object instanceof DBAIModel aiModel) {
            addSeparator();
            add(new AIModelPredictAction(aiModel));
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
        DBObjectType objectType = object.getObjectType();
        DBObjectNavigationInfoProvider<DBObject> infoProvider = DBObjectNavigationInfoProviderCache.get(objectType);
        if (infoProvider == null) return;

        List<DBObjectNavigationList<?>> navigationLists = infoProvider.createNavigationTargets(object);
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
        if (object instanceof DBConsole console) {
            add(new ConsoleRenameAction(console));
            add(new ConsoleDeleteAction(console));
            addSeparator();
            add(new ConsoleCreateAction(connection, STANDARD));
            if (DEBUGGING.isSupported(connection)) {
                add(new ConsoleCreateAction(connection, DEBUG));
            }
            if (VECTOR_SEARCH.isSupported(connection)) {
                add(new ConsoleCreateAction(connection, SEARCH));
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
