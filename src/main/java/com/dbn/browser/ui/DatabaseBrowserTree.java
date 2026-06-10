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

package com.dbn.browser.ui;

import com.dbn.browser.DatabaseBrowserManager;
import com.dbn.browser.DatabaseBrowserUtils;
import com.dbn.browser.TreeNavigationHistory;
import com.dbn.browser.model.BrowserTreeEventListener;
import com.dbn.browser.model.BrowserTreeModel;
import com.dbn.browser.model.BrowserTreeNode;
import com.dbn.browser.model.ConnectionBrowserTreeModel;
import com.dbn.browser.model.ConnectionBundleBrowserTreeModel;
import com.dbn.common.dispose.Checks;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.filter.Filter;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Progress;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.tree.DBNTree;
import com.dbn.common.ui.tree.Trees;
import com.dbn.common.ui.util.Borderless;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.Mouse;
import com.dbn.connection.ConnectionBundle;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionManager;
import com.dbn.connection.action.ConnectionActionGroup;
import com.dbn.editor.DatabaseFileEditorManager;
import com.dbn.object.DBConsole;
import com.dbn.object.action.ObjectActionGroup;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBObjectBundle;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.common.list.action.ObjectListActionGroup;
import com.dbn.object.common.property.DBObjectProperty;
import com.dbn.object.navigation.DBObjectNavigationInfoProvider;
import com.dbn.object.navigation.DBObjectNavigationInfoProviderCache;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.tree.TreeUtil;
import lombok.Getter;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.nls.NlsResources.txt;

@Getter
public final class DatabaseBrowserTree extends DBNTree implements Borderless {
    private final TreeNavigationHistory navigationHistory = new TreeNavigationHistory();
    private transient BrowserTreeNode targetSelection;
    private transient boolean listenersEnabled = true;

    public DatabaseBrowserTree(@NotNull DBNComponent parent, @Nullable ConnectionHandler connection) {
        super(parent, createModel(parent.ensureProject(), connection));

        addKeyListener(createKeyListener());
        addMouseListener(createMouseListener());
        addTreeSelectionListener(createTreeSelectionListener());

        setToggleClickCount(0);
        setRootVisible(true);
        setShowsRootHandles(true);
        setAutoscrolls(true);
        setBorder(Borders.EMPTY_BORDER);
        DatabaseBrowserTreeCellRenderer browserTreeCellRenderer = new DatabaseBrowserTreeCellRenderer(parent.ensureProject());
        setCellRenderer(browserTreeCellRenderer);

        new DatabaseBrowserTreeSpeedSearch(this);

        Disposer.register(parent, this);
        Disposer.register(this, navigationHistory);
        Disposer.register(this, getModel());
    }

    private static BrowserTreeModel createModel(@NotNull Project project, @Nullable ConnectionHandler connection) {
        ConnectionManager connectionManager = ConnectionManager.getInstance(project);
        return connection == null ?
                new ConnectionBundleBrowserTreeModel(project, connectionManager.getConnectionBundle()) :
                new ConnectionBrowserTreeModel(connection);

    }

    @Override
    public BrowserTreeModel getModel() {
        return (BrowserTreeModel) super.getModel();
    }

    public void expandConnectionManagers() {
        ConnectionManager connectionManager = ConnectionManager.getInstance(ensureProject());
        ConnectionBundle connectionBundle = connectionManager.getConnectionBundle();
        TreePath treePath = DatabaseBrowserUtils.createTreePath(connectionBundle);

        Dispatch.run(() -> setExpandedState(treePath, true));
    }

    public void selectElement(BrowserTreeNode treeNode, boolean focus) {
        if (treeNode == null) return;

        ConnectionHandler connection = treeNode.getConnection();
        Filter<BrowserTreeNode> filter = connection.getObjectTypeFilter();
        if (!filter.accepts(treeNode)) return;

        targetSelection = treeNode;
        scrollToSelectedElement();
        if (focus) requestFocus();
    }

    public void scrollToSelectedElement() {
        Project project = ensureProject();
        if (!project.isOpen() || targetSelection == null) return;

        Background.run(() -> {
            BrowserTreeNode targetSelection = this.targetSelection;
            if (targetSelection == null) return;

            targetSelection = targetSelection.getUndisposedEntity();
            if (targetSelection == null) return;

            TreePath treePath = DatabaseBrowserUtils.createTreePath(targetSelection);
            if (treePath == null) return;

            for (Object object : treePath.getPath()) {
                if (!isValidTreeNode(object)) {
                    this.targetSelection = null;
                    return;
                }

                BrowserTreeNode treeNode = (BrowserTreeNode) object;
                if (treeNode.equals(targetSelection)) {
                    break;
                }

                if (!treeNode.isLeaf() && !treeNode.isTreeStructureLoaded()) {
                    selectPath(DatabaseBrowserUtils.createTreePath(treeNode));
                    treeNode.getChildren();
                    return;
                }
            }

            this.targetSelection = null;
            selectPath(treePath);
        });
    }

    @Nullable
    public BrowserTreeNode getSelectedNode() {
        TreePath selectionPath = getSelectionPath();
        if (selectionPath == null) return null;

        Object object = selectionPath.getLastPathComponent();
        if (!isValidTreeNode(object)) return null;

        return (BrowserTreeNode) object;
    }

    private void selectPath(TreePath treePath) {
        Dispatch.run(this, () -> TreeUtil.selectPath(this, treePath, true));
    }


    @Override
    public String getToolTipText(MouseEvent e) {
        Object object = getTreeNode(e);
        if (object instanceof ToolTipProvider toolTipProvider) {
            return toolTipProvider.getToolTip();
        }
        return null;
    }

    public void navigateBack() {
        BrowserTreeNode treeNode = navigationHistory.previous();
        if (treeNode == null) return;

        selectPathSilently(DatabaseBrowserUtils.createTreePath(treeNode));
    }

    public void navigateForward() {
        BrowserTreeNode treeNode = navigationHistory.next();
        if (treeNode == null) return;

        selectPathSilently(DatabaseBrowserUtils.createTreePath(treeNode));
    }


    private void selectPathSilently(TreePath treePath) {
        if (treePath == null) return;

        listenersEnabled = false;
        selectionModel.setSelectionPath(treePath);
        TreeUtil.selectPath(DatabaseBrowserTree.this, treePath, true);
        listenersEnabled = true;
    }

    public void expandAll() {
        BrowserTreeNode root = getModel().getRoot();
        expand(root);
    }

    public void expand(BrowserTreeNode treeNode) {
        if (!treeNode.canExpand()) return;

        expandPath(DatabaseBrowserUtils.createTreePath(treeNode));
        for (int i = 0; i < treeNode.getChildCount(); i++) {
            BrowserTreeNode childTreeNode = treeNode.getChildAt(i);
            expand(childTreeNode);
        }
    }

    public void collapseAll() {
        BrowserTreeNode root = getModel().getRoot();
        collapse(root);
    }

    private void collapse(BrowserTreeNode treeNode) {
        if (treeNode.isLeaf()) return;
        if (!treeNode.isTreeStructureLoaded()) return;

        for (int i = 0; i < treeNode.getChildCount(); i++) {
            BrowserTreeNode childTreeNode = treeNode.getChildAt(i);
            collapse(childTreeNode);
            collapsePath(DatabaseBrowserUtils.createTreePath(childTreeNode));
        }
    }

    private void processSelectEvent(InputEvent event, TreePath path, boolean deliberate) {
        if (path == null) return;

        Object lastPathEntity = path.getLastPathComponent();
        if (isNotValid(lastPathEntity)) return;

        DatabaseFileEditorManager editorManager = DatabaseFileEditorManager.getInstance(getProject());
        if (lastPathEntity instanceof DBObject object) {

            Project project = ensureProject();
            if (object instanceof DBConsole console) {
                editorManager.openDatabaseConsole(console, false, deliberate);
                event.consume();
            } else if (object.is(DBObjectProperty.EDITABLE)) {
                DBSchemaObject schemaObject = (DBSchemaObject) object;
                editorManager.connectAndOpenEditor(schemaObject, null, false, deliberate);
                event.consume();

            } else if (object.is(DBObjectProperty.NAVIGABLE)) {
                editorManager.connectAndOpenEditor(object, null, false, deliberate);
                event.consume();

            } else if (deliberate) {
                Progress.prompt(project, object, true,
                        txt("prc.databaseBrowser.title.LoadingObjectReferences"),
                        txt("prc.databaseBrowser.text.LoadingReferencesOf", object.getQualifiedNameWithType()),
                        progress -> navigateToObject(object, progress));
            }
        } else if (lastPathEntity instanceof DBObjectBundle objectBundle) {
            ConnectionHandler connection = objectBundle.getConnection();
            DBConsole defaultConsole = connection.getConsoleBundle().getDefaultConsole();
            editorManager.openDatabaseConsole(defaultConsole, false, deliberate);
        }
    }

    private void navigateToObject(DBObject object, ProgressIndicator progress) {
        DBObjectType objectType = object.getObjectType();
        DBObjectNavigationInfoProvider<DBObject> infoProvider = DBObjectNavigationInfoProviderCache.get(objectType);
        if  (infoProvider == null) return;

        DBObject navigationObject = infoProvider.getDefaultNavigationTarget(object);
        if (navigationObject == null) return;

        progress.checkCanceled();
        Dispatch.run(this, () -> navigationObject.navigate(true));
    }

    /********************************************************
     *                 TreeSelectionListener                *
     ********************************************************/
    private TreeSelectionListener createTreeSelectionListener() {
        return new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                if (isNotValid(this)) return;
                if (!listenersEnabled) return;

                Object object = e.getPath().getLastPathComponent();
                if (!isValidTreeNode(object)) return;

                BrowserTreeNode treeNode = (BrowserTreeNode) object;
                if (targetSelection == null || treeNode.equals(targetSelection)) {
                    navigationHistory.add(treeNode);
                }

                ProjectEvents.notify(ensureProject(),
                        BrowserTreeEventListener.TOPIC,
                        (listener) -> listener.selectionChanged());
            }
        };
    }

    /********************************************************
     *                      MouseListener                   *
     ********************************************************/
    private MouseListener createMouseListener() {
        return Mouse.listener().
                onClick(e -> {
                    if (e.getButton() != MouseEvent.BUTTON1) return;

                    DatabaseBrowserManager browserManager = DatabaseBrowserManager.getInstance(ensureProject());
                    if (browserManager.getAutoscrollToEditor().value() || e.getClickCount() > 1) {
                        TreePath path = Trees.getPathAtMousePosition(this, e);
                        processSelectEvent(e, path, e.getClickCount() > 1);
                    }
                });
    }

    @Nullable
    @Override
    protected ActionGroup createContextActions(TreePath path) {
        Object node = path.getLastPathComponent();
        if (!isValidTreeNode(node)) return null;

        BrowserTreeNode pathNode = (BrowserTreeNode) node;
        if (isNotValid(pathNode)) return null;

        if (pathNode instanceof DBObjectList<?> objectList) {
            return new ObjectListActionGroup(objectList);
        }

        if (pathNode instanceof DBObject object) {
            DBObject[] objects = getSelectedObjects(object);
            return new ObjectActionGroup(objects);
        }

        if (pathNode instanceof DBObjectBundle objectsBundle) {
            ConnectionHandler connection = objectsBundle.getConnection();
            return new ConnectionActionGroup(connection);
        }

        return null;
    }

    private DBObject[] getSelectedObjects(DBObject sourceObject) {
        List<DBObject> objects = new ArrayList<>();
        for (TreePath path : getSelectionModel().getSelectionPaths()) {
            Object node = path.getLastPathComponent();
            if (!(node instanceof DBObject selectedObject)) continue;
            if (isNotValid(selectedObject)) continue;
            if (selectedObject.getObjectType() != sourceObject.getObjectType()) continue;

            objects.add(selectedObject);
        }

        if (objects.isEmpty()) {
            objects.add(sourceObject);
        }

        return objects.toArray(new DBObject[0]);
    }

    @Override
    protected @Nls String getContextMenuNodeName(Object node) {
        if (node instanceof DBObjectList<?> objectList) {
            return txt("app.objects.token.ObjectList", objectList.getObjectType().getListDisplayName());
        } else if (node instanceof DBObject object) {
            return txt("app.object.token.QualifiedNameWithType", object.getTypeName(), object.getName());

        } else if (node instanceof DBObjectBundle objectsBundle) {
            ConnectionHandler connection = objectsBundle.getConnection();
            return txt("app.connection.token.Connection", connection.getName());
        }

        return super.getContextMenuNodeName(node);
    }

    /********************************************************
     *                      KeyListener                     *
     ********************************************************/
    private KeyListener createKeyListener() {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == 10) {  // ENTER
                    TreePath path = getSelectionPath();
                    processSelectEvent(e, path, true);
                }
            }
        };
    }

    private static boolean isValidTreeNode(Object object) {
        return object instanceof BrowserTreeNode && Checks.isValid(object);
    }
}
