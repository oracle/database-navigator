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

package com.dbn.execution.method.browser.ui;

import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Progress;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.tree.DBNTree;
import com.dbn.common.ui.tree.DBNTreeNode;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Actions;
import com.dbn.connection.ConnectionHandler;
import com.dbn.execution.method.MethodExecutionManager;
import com.dbn.execution.method.browser.MethodBrowserSettings;
import com.dbn.execution.method.browser.action.ConnectionSelectDropdownAction;
import com.dbn.execution.method.browser.action.ObjectTypeToggleAction;
import com.dbn.execution.method.browser.action.SchemaSelectDropdownAction;
import com.dbn.object.DBMethod;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.ui.ObjectTree;
import com.dbn.object.common.ui.ObjectTreeModel;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.actionSystem.ActionToolbar;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;

import static com.dbn.common.dispose.Failsafe.nd;

public class MethodExecutionBrowserForm extends DBNFormBase {

    private JPanel actionsPanel;
    private JPanel mainPanel;
    private DBNTree methodsTree;

    MethodExecutionBrowserForm(MethodExecutionBrowserDialog parent, ObjectTreeModel model, boolean debug) {
        super(parent);
        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, true,
                new ConnectionSelectDropdownAction(this, debug),
                new SchemaSelectDropdownAction(this),
                Actions.SEPARATOR,
                new ObjectTypeToggleAction(this, DBObjectType.PROCEDURE),
                new ObjectTypeToggleAction(this, DBObjectType.FUNCTION));
        actionsPanel.add(actionToolbar.getComponent(), BorderLayout.CENTER);
        methodsTree.setModel(model);
        TreePath selectionPath = model.getInitialSelection();
        if (selectionPath != null) {
            methodsTree.setSelectionPath(selectionPath);
            methodsTree.scrollPathToVisible(selectionPath.getParentPath());
        }
    }

    public MethodBrowserSettings getSettings() {
        MethodExecutionManager methodExecutionManager = MethodExecutionManager.getInstance(ensureProject());
        return methodExecutionManager.getBrowserSettings();
    }

    public void setObjectsVisible(DBObjectType objectType, boolean state) {
        if (getSettings().setObjectVisibility(objectType, state)) {
            updateTree();
        }
    }

    public void setConnectionHandler(ConnectionHandler connection) {
        MethodBrowserSettings settings = getSettings();
        if (settings.getConnection() != connection) {
            settings.setSelectedConnection(connection);
            if (settings.getSelectedSchema() != null) {
                DBSchema schema  = connection.getObjectBundle().getSchema(settings.getSelectedSchema().getName());
                setSchema(schema);
            }
            updateTree();
        }
    }

    public void setSchema(final DBSchema schema) {
        MethodBrowserSettings settings = getSettings();
        if (settings.getSelectedSchema() != schema) {
            settings.setSelectedSchema(schema);
            DBNTreeNode root = (DBNTreeNode) methodsTree.getModel().getRoot();
            root.setUserObject("Loading...");
            updateTree();
        }
    }

    void addTreeSelectionListener(TreeSelectionListener selectionListener) {
        methodsTree.addTreeSelectionListener(selectionListener);
    }

    DBMethod getSelectedMethod() {
        TreePath selectionPath = methodsTree.getSelectionPath();
        if (selectionPath == null) return null;

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
        Object userObject = node.getUserObject();
        if (userObject instanceof DBObjectRef) {
            DBObjectRef<?> objectRef = (DBObjectRef<?>) userObject;
            DBObject object = DBObjectRef.get(objectRef);
            if (object instanceof DBMethod) {
                return (DBMethod) object;
            }
        }
        return null;
    }

    private void updateTree() {
        Progress.prompt(getProject(), null, false,
                "Loading data dictionary",
                "Loading executable elements",
                progress -> {
                    MethodBrowserSettings settings = getSettings();
                    ObjectTreeModel model = new ObjectTreeModel(settings.getSelectedSchema(), settings.getVisibleObjectTypes(), null);
                    DBNTree methodsTree = nd(this.methodsTree);

                    Dispatch.run(() -> methodsTree.setModel(model));
                    UserInterface.repaint(methodsTree);
                });
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    private void createUIComponents() {
        methodsTree = new ObjectTree(this);
    }
}
