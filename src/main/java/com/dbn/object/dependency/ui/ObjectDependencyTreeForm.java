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
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.tree.Trees;
import com.dbn.common.util.Actions;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.dependency.ObjectDependencyManager;
import com.dbn.object.dependency.ObjectDependencyType;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.tree.TreeModel;
import java.awt.BorderLayout;

import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;

public class ObjectDependencyTreeForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel actionsPanel;
    private JPanel contentPanel;
    private JPanel headerPanel;
    private JComboBox<ObjectDependencyType> dependencyTypeComboBox;
    private JBScrollPane treeScrollPane;

    private final DBNHeaderForm headerForm;
    private final ObjectDependencyTree dependencyTree;

    private final DBObjectRef<DBSchemaObject> object;

    public ObjectDependencyTreeForm(ObjectDependencyTreeDialog parentComponent, final DBSchemaObject schemaObject) {
        super(parentComponent);
        Project project = ensureProject();
        dependencyTree = new ObjectDependencyTree(this, schemaObject) {
            @Override
            public void setModel(TreeModel model) {
                super.setModel(model);
                if (headerForm != null && model instanceof ObjectDependencyTreeModel) {
                    ObjectDependencyTreeModel dependencyTreeModel = (ObjectDependencyTreeModel) model;
                    ObjectDependencyTreeNode rootNode = dependencyTreeModel.getRoot();
                    DBObject object = rootNode.getObject();
                    if (object != null) {
                        headerForm.update(object);
                    }
                }
            }
        };
        treeScrollPane.setViewportView(dependencyTree);
        ObjectDependencyManager dependencyManager = ObjectDependencyManager.getInstance(project);
        ObjectDependencyType dependencyType = dependencyManager.getLastUserDependencyType();


        initComboBox(dependencyTypeComboBox, ObjectDependencyType.values());
        setSelection(dependencyTypeComboBox, dependencyType);
        dependencyTypeComboBox.addActionListener(e -> {
            ObjectDependencyType selection = getSelection(dependencyTypeComboBox);
            dependencyTree.setDependencyType(selection);
        });

        this.object = DBObjectRef.of(schemaObject);
        headerForm = new DBNHeaderForm(this, schemaObject);
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);

        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, true,
                new PreviousSelectionAction(),
                new NextSelectionAction(),
                Actions.SEPARATOR,
                new ExpandTreeAction(),
                new CollapseTreeAction());
        actionsPanel.add(actionToolbar.getComponent(), BorderLayout.CENTER);
    }



    private DBSchemaObject getObject() {
        return DBObjectRef.get(object);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public class ExpandTreeAction extends BasicAction {

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            Trees.expandAll(dependencyTree);
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            Presentation presentation = e.getPresentation();
            presentation.setText(txt("app.shared.action.ExpandAll"));
            presentation.setIcon(Icons.ACTION_EXPAND_ALL);
        }
    }

    public class PreviousSelectionAction extends BasicAction {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            DBObject previous = dependencyTree.getSelectionHistory().previous();
            dependencyTree.setRootObject((DBSchemaObject) previous, false);
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            Presentation presentation = e.getPresentation();
            boolean enabled = dependencyTree.getSelectionHistory().hasPrevious();

            presentation.setEnabled(enabled);
            presentation.setText(txt("app.navigation.action.PreviousSelection"));
            presentation.setIcon(Icons.BROWSER_BACK);
        }
    }

    public class NextSelectionAction extends BasicAction {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            DBObject next = dependencyTree.getSelectionHistory().next();
            dependencyTree.setRootObject((DBSchemaObject) next, false);
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            Presentation presentation = e.getPresentation();
            boolean enabled = dependencyTree.getSelectionHistory().hasNext();
            presentation.setEnabled(enabled);
            presentation.setText(txt("app.navigation.action.NextSelection"));
            presentation.setIcon(Icons.BROWSER_NEXT);
        }
    }

    public class CollapseTreeAction extends BasicAction {

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            Trees.collapseAll(dependencyTree);
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            Presentation presentation = e.getPresentation();
            presentation.setText(txt("app.shared.action.CollapseAll"));
            presentation.setIcon(Icons.ACTION_COLLAPSE_ALL);
        }
    }
}
