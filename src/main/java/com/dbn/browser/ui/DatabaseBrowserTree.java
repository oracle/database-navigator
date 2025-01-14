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
import com.dbn.common.color.Colors;
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
import com.dbn.common.util.Actions;
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
import com.intellij.ide.IdeTooltip;
import com.intellij.ide.IdeTooltipManager;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.util.ui.tree.TreeUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import static com.dbn.common.dispose.Checks.isNotValid;

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
                BrowserTreeNode treeNode = (BrowserTreeNode) object;
                if (isNotValid(treeNode)) {
                    this.targetSelection = null;
                    return;
                }


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



    public BrowserTreeNode getSelectedNode() {
        TreePath selectionPath = getSelectionPath();
        return selectionPath == null ? null : (BrowserTreeNode) selectionPath.getLastPathComponent();
    }

    private void selectPath(TreePath treePath) {
        Dispatch.run(() -> {
            DatabaseBrowserTree tree = DatabaseBrowserTree.this;
            ActionCallback callback = TreeUtil.selectPath(tree, treePath, true);
            if (callback == ActionCallback.REJECTED) {
                Object target = treePath.getLastPathComponent();
                if (target instanceof DBObject) {
                    DBObject object = (DBObject) target;
                    IdeTooltip tooltip = new IdeTooltip(tree, tree.getMousePosition(), new JLabel("Cannot navigate to " + object.getQualifiedNameWithType() + ". "));
                    tooltip.setTextBackground(Colors.getWarningHintColor());
                    IdeTooltipManager.getInstance().show(tooltip, true);
                }
            }
        });
    }


    @Override
    public String getToolTipText(MouseEvent e) {
        Object object = getTreeNode(e);
        if (object instanceof ToolTipProvider) {
            ToolTipProvider toolTipProvider = (ToolTipProvider) object;
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
        if (lastPathEntity instanceof DBObject) {
            DBObject object = (DBObject) lastPathEntity;

            Project project = ensureProject();
            if (object instanceof DBConsole) {
                DBConsole console = (DBConsole) object;
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
                        "Loading object references",
                        "Loading references of " + object.getQualifiedNameWithType(),
                        progress -> {
                            DBObject navigationObject = object.getDefaultNavigationObject();
                            if (navigationObject != null) {
                                progress.checkCanceled();
                                Dispatch.run(() -> navigationObject.navigate(true));
                            }
                        });
            }
        } else if (lastPathEntity instanceof DBObjectBundle) {
            DBObjectBundle objectBundle = (DBObjectBundle) lastPathEntity;
            ConnectionHandler connection = objectBundle.getConnection();
            DBConsole defaultConsole = connection.getConsoleBundle().getDefaultConsole();
            editorManager.openDatabaseConsole(defaultConsole, false, deliberate);
        }
    }

/*    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        boolean navigable = false;
        if (e.isControlDown() && e.getID() != MouseEvent.MOUSE_DRAGGED && !e.isConsumed()) {
            TreePath path = getPathForLocation(e.getX(), e.getY());
            Object lastPathEntity = path == null ? null : path.getLastPathComponent();
            if (lastPathEntity instanceof DBObject) {
                DBObject object = (DBObject) lastPathEntity;
                DBObject navigationObject = object.getDefaultNavigationObject();
                navigable = navigationObject != null;
            }
            
        }

        if (navigable) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            super.processMouseMotionEvent(e);
            setCursor(Cursor.getDefaultCursor());
        }
    }  */

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
                if (isNotValid(object)) return;

                if (object instanceof BrowserTreeNode) {
                    BrowserTreeNode treeNode = (BrowserTreeNode) object;
                    if (targetSelection == null || treeNode.equals(targetSelection)) {
                        navigationHistory.add(treeNode);
                    }
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
                }).
                onRelease(e -> {
                    if (e.getButton() != MouseEvent.BUTTON3) return;

                    TreePath path = Trees.getPathAtMousePosition(this, e);
                    showContextMenu(path, e.getX(), e.getY());
                });
    }

    @Override
    protected void showContextMenu(TreePath path, int x, int y) {
        if (isNotValid(path)) return;

        BrowserTreeNode lastPathEntity = (BrowserTreeNode) path.getLastPathComponent();
        if (isNotValid(lastPathEntity)) return;

        ActionGroup actionGroup = null;
        if (lastPathEntity instanceof DBObjectList) {
            DBObjectList<?> objectList = (DBObjectList<?>) lastPathEntity;
            actionGroup = new ObjectListActionGroup(objectList);
        } else if (lastPathEntity instanceof DBObject) {
            DBObject object = (DBObject) lastPathEntity;
            Progress.prompt(
                    getProject(),
                    object,
                    true,
                    "Loading object properties",
                    "Loading properties of " + object.getQualifiedNameWithType(),
                    progress -> showPopupMenu(new ObjectActionGroup(object), x, y));
        } else if (lastPathEntity instanceof DBObjectBundle) {
            DBObjectBundle objectsBundle = (DBObjectBundle) lastPathEntity;
            ConnectionHandler connection = objectsBundle.getConnection();
            actionGroup = new ConnectionActionGroup(connection);
        }

        showPopupMenu(actionGroup, x, y);
    }

    private void showPopupMenu(ActionGroup actionGroup, int x, int y) {
        if (actionGroup == null) return;
        ActionPopupMenu actionPopupMenu = Actions.createActionPopupMenu(this, actionGroup);
        JPopupMenu popupMenu = actionPopupMenu.getComponent();
        Dispatch.run(() -> {
            if (!isShowing()) return;
            popupMenu.show(this, x, y);
        });
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
}
