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

package com.dbn.object.dependency.ui;

import com.dbn.common.action.BasicAction;
import com.dbn.common.action.ProjectAction;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.icon.Icons;
import com.dbn.common.load.LoadInProgressRegistry;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.tree.DBNTree;
import com.dbn.common.ui.tree.Trees;
import com.dbn.common.ui.util.Cursors;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Commons;
import com.dbn.common.util.TimeUtil;
import com.dbn.editor.DatabaseFileEditorManager;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBObjectSelectionHistory;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.common.property.DBObjectProperty;
import com.dbn.object.dependency.ObjectDependencyManager;
import com.dbn.object.dependency.ObjectDependencyType;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.event.MouseEvent;

import static com.dbn.common.util.Naming.doubleQuoted;
import static com.dbn.nls.NlsResources.txt;

public class ObjectDependencyTree extends DBNTree{
    private final DBObjectSelectionHistory selectionHistory =  new DBObjectSelectionHistory();
    private final ObjectDependencyTreeSpeedSearch speedSearch;

    private final LoadInProgressRegistry<ObjectDependencyTreeNode> loadInProgressRegistry =
            LoadInProgressRegistry.create(this,
                    node -> getModel().refreshLoadInProgressNode(node));

    ObjectDependencyTree(@NotNull DBNComponent parent, @NotNull DBSchemaObject schemaObject) {
        super(parent);
        Project project = getProject();
        ObjectDependencyTreeModel model = createModel(project, schemaObject);

        setModel(model);
        selectionHistory.add(schemaObject);
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        setCellRenderer(new ObjectDependencyTreeCellRenderer());
        addTreeSelectionListener((TreeSelectionEvent e) -> UserInterface.repaint(ObjectDependencyTree.this));

        speedSearch = new ObjectDependencyTreeSpeedSearch(this);
    }

    @Nullable
    @Override
    protected  ActionGroup createContextActions(TreePath path) {
        ObjectDependencyTreeNode node = (ObjectDependencyTreeNode) path.getLastPathComponent();
        ObjectDependencyTreeNode rootNode = node.getModel().getRoot();

        DBObject object = node.getObject();
        if (object instanceof DBSchemaObject && !Commons.match(rootNode.getObject(), object)) {
            DefaultActionGroup actionGroup = new DefaultActionGroup();
            actionGroup.add(new SelectObjectAction((DBSchemaObject) object));
            DBSchemaObject schObject = (DBSchemaObject) object;
            if (schObject.is(DBObjectProperty.EDITABLE)) {
                actionGroup.add(new EditObjectAction((DBSchemaObject) object));
            }
            return actionGroup;
        }

        return null;
    }

    @Override
    protected String getContextMenuNodeName(Object node) {
        ObjectDependencyTreeNode objectNode = (ObjectDependencyTreeNode) node;
        DBObject object = objectNode.getObject();
        if (object == null) return super.getContextMenuNodeName(node);

        return object.getTypeName() + " " + doubleQuoted(object.getName());
    }

    @Override
    public void setModel(TreeModel model) {
        if (model instanceof ObjectDependencyTreeModel) {
            ObjectDependencyTreeModel treeModel = (ObjectDependencyTreeModel) model;
            treeModel.setTree(this);
            super.setModel(treeModel);
        }
    }

    @NotNull
    private static ObjectDependencyTreeModel createModel(Project project, DBSchemaObject schemaObject) {
        ObjectDependencyManager dependencyManager = ObjectDependencyManager.getInstance(project);
        ObjectDependencyType dependencyType = dependencyManager.getLastUserDependencyType();
        return new ObjectDependencyTreeModel(schemaObject, dependencyType);
    }

    private long selectionTimestamp = System.currentTimeMillis();
    @Override
    protected void processMouseEvent(MouseEvent e) {
        int button = e.getButton();
        int clickCount = e.getClickCount();
        if (button == MouseEvent.BUTTON1) {
            if (e.isControlDown()) {
                if (clickCount == 1) {
                    DBObject object = getMouseEventObject(e);
                    if (object != null) {
                        object.navigate(true);
                        e.consume();
                    }
                }
            } else if (clickCount == 2) {
                DBObject object = getMouseEventObject(e);
                if (object != null && TimeUtil.isOlderThan(selectionTimestamp, TimeUtil.Millis.ONE_SECOND)) {
                    selectionTimestamp = System.currentTimeMillis();

                    if (object instanceof DBSchemaObject) {
                        DBSchemaObject schemaObject = (DBSchemaObject) object;
                        if (schemaObject.is(DBObjectProperty.EDITABLE)) {
                            Project project = object.getProject();
                            DatabaseFileEditorManager editorManager = DatabaseFileEditorManager.getInstance(project);
                            editorManager.connectAndOpenEditor(schemaObject, null, true, true);
                        }
                    }

                    //setRootObject((DBSchemaObject) object, true);
                    e.consume();
                }
            }
        }

        if (!e.isConsumed()) {
            super.processMouseEvent(e);
        }
    }

    private DBObject getMouseEventObject(MouseEvent e) {
        TreePath path = Trees.getPathAtMousePosition(this, e);
        Object lastPathComponent = path == null ? null : path.getLastPathComponent();
        if (lastPathComponent instanceof ObjectDependencyTreeNode) {
            ObjectDependencyTreeNode dependencyTreeNode = (ObjectDependencyTreeNode) lastPathComponent;
            return dependencyTreeNode.getObject();
        }
        return null;
    }

    DBObjectSelectionHistory getSelectionHistory() {
        return selectionHistory;
    }

    public void selectElement(ObjectDependencyTreeNode treeNode) {
        TreePath treePath = new TreePath(treeNode.getTreePath());
        TreeUtil.selectPath(this, treePath);
    }

    void registerLoadInProgressNode(ObjectDependencyTreeNode loadInProgressNode) {
        loadInProgressRegistry.register(loadInProgressNode);
    }

    public class SelectObjectAction extends BasicAction {
        private final DBObjectRef<DBSchemaObject> objectRef;
        SelectObjectAction(DBSchemaObject object) {
            super(txt("app.shared.action.Select"));
            objectRef = DBObjectRef.of(object);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            DBSchemaObject schemaObject = DBObjectRef.get(objectRef);
            if (schemaObject != null) {
                setRootObject(schemaObject, true);
            }
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            Presentation presentation = e.getPresentation();
            presentation.setText("Select");
        }
    }

    public static class EditObjectAction extends ProjectAction {
        private final DBObjectRef<DBSchemaObject> object;

        EditObjectAction(DBSchemaObject object) {
            this.object = DBObjectRef.of(object);
        }


        @Override
        protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
            DBSchemaObject schemaObject = DBObjectRef.get(object);
            if (schemaObject == null) return;

            DatabaseFileEditorManager editorManager = DatabaseFileEditorManager.getInstance(project);
            editorManager.connectAndOpenEditor(schemaObject, null, true, true);
        }

        @Override
        protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
            Presentation presentation = e.getPresentation();
            presentation.setText("Edit");
            presentation.setIcon(Icons.ACTION_EDIT);
        }
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        boolean navigable = false;
        if (e.isControlDown() && e.getID() != MouseEvent.MOUSE_DRAGGED && !e.isConsumed()) {
            DBObject object = getMouseEventObject(e);
            if (object != null && !object.equals(getModel().getObject())) {
                navigable = true;
            }
        }

        if (navigable) {
            setCursor(Cursors.handCursor());
        } else {
            super.processMouseMotionEvent(e);
            setCursor(Cursors.defaultCursor());
        }
    }

    @Override
    public ObjectDependencyTreeModel getModel() {
        TreeModel model = super.getModel();
        return model instanceof ObjectDependencyTreeModel ? (ObjectDependencyTreeModel) model : null;
    }

    void setDependencyType(ObjectDependencyType dependencyType) {
        ObjectDependencyTreeModel oldModel = getModel();
        Project project = Failsafe.nn(oldModel.getProject());
        ObjectDependencyManager dependencyManager = ObjectDependencyManager.getInstance(project);
        dependencyManager.setLastUserDependencyType(dependencyType);

        DBSchemaObject object = oldModel.getObject();
        if (object != null) {
            setModel(new ObjectDependencyTreeModel(object, dependencyType));
            Disposer.dispose(oldModel);
        }
    }

    void setRootObject(DBSchemaObject object, boolean addHistory) {
        ObjectDependencyTreeModel oldModel = getModel();
        if (addHistory) {
            selectionHistory.add(object);
        }

        ObjectDependencyType dependencyType = oldModel.getDependencyType();
        setModel(new ObjectDependencyTreeModel(object, dependencyType));
        Disposer.dispose(oldModel);
    }

    @Override
    public void disposeInner() {
        Disposer.dispose(selectionHistory);
        Disposer.dispose(speedSearch);
        Disposer.dispose(getModel());
        super.disposeInner();
    }
}
