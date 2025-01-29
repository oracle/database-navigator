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

package com.dbn.common.ui.tree;

import com.dbn.common.dispose.Disposer;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Progress;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Actions;
import com.dbn.connection.context.DatabaseContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import static com.dbn.common.dispose.ComponentDisposer.removeListeners;
import static com.dbn.common.ui.util.Keyboard.onKeyPress;
import static com.dbn.common.ui.util.Mouse.onButtonRelease;

public class DBNTree extends Tree implements DBNComponent {
    private WeakRef<DBNComponent> parent;

    public DBNTree(@NotNull DBNComponent parent) {
        init(parent, null);
    }

    public DBNTree(@NotNull DBNComponent parent, TreeModel model) {
        super(model);
        init(parent, model);
    }

    public DBNTree(@NotNull DBNComponent parent, TreeNode root) {
        super(root);
        init(parent, root);
    }

    private void init(DBNComponent parent, Object disposable) {
        this.parent = WeakRef.of(parent);
        setTransferHandler(DBNTreeTransferHandler.INSTANCE);
        initListeners();

        Disposer.register(parent, this);
        Disposer.register(this, disposable);
    }

    private void initListeners() {
        onButtonRelease(this, MouseEvent.BUTTON3, e -> showContextMenu(e));
        onKeyPress(this, KeyEvent.VK_CONTEXT_MENU, e -> showContextMenu(e));
        onKeyPress(this, KeyEvent.VK_SPACE, e -> showContextMenu(e));
    }

    @Override
    public void setModel(TreeModel treeModel) {
        treeModel = Disposer.replace(getModel(), treeModel);
        super.setModel(treeModel);

        Disposer.register(this, treeModel);
    }

    @Nullable
    public final Project getProject() {
        return parent.ensure().getProject();
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return this;
    }

    @NotNull
    @Override
    public <T extends Disposable> T getParentComponent() {
        return (T) parent.ensure();
    }

    @Nullable
    protected ActionGroup createContextActions(TreePath path) {
        return null;
    }

    private void showContextMenu(InputEvent event) {
        TreePath path = null;
        if (event instanceof MouseEvent) {
            MouseEvent mouseEvent = (MouseEvent) event;
            path = Trees.getPathAtMousePosition(this, mouseEvent);
        } else if (event instanceof KeyEvent) {
            path = getSelectionPath();
        }

        if (path == null) return;
        showContextMenu(path, event);
    }


    /**
     * Retrieves the name of the node to be displayed in a context menu.
     *
     * @param node the object representing the node from which the name is derived
     * @return the string representation of the node's name, as retrieved by the {@code toString()} method of the node
     */
    protected String getContextMenuNodeName(Object node) {
        return node.toString();
    }

    protected void showContextMenu(TreePath path, InputEvent event) {
        if (path == null) return;

        Object pathNode = path.getLastPathComponent();
        DatabaseContext context = pathNode instanceof DatabaseContext ? (DatabaseContext) pathNode : null;

        Progress.prompt(
                getProject(),
                context,
                true,
                "Preparing context menu",
                "Creating context menu for " + getContextMenuNodeName(pathNode) + "...",
                progress -> {
                    ActionGroup contextActions = createContextActions(path);
                    if (contextActions == null) return;
                    if (progress.isCanceled()) return;

                    showContextMenu(path, contextActions, event);
                });
    }

    protected void showContextMenu(TreePath treePath, ActionGroup contextActions, InputEvent event) {
        if (contextActions == null) return;

        ActionPopupMenu actionPopupMenu = Actions.createActionPopupMenu(this, contextActions);
        JPopupMenu popupMenu = actionPopupMenu.getComponent();
        Dispatch.run(this, () -> {
            if (!isShowing()) return;
            DBNTree source = event.getSource() instanceof DBNTree ? (DBNTree) event.getSource() : this;

            if (event instanceof MouseEvent) {
                MouseEvent mouseEvent = (MouseEvent) event;
                int x = mouseEvent.getX();
                int y = mouseEvent.getY();

                popupMenu.show(source, x, y);
            } else if (event instanceof KeyEvent) {
                Rectangle bounds = source.getPathBounds(treePath);
                if (bounds == null) return;

                int x = bounds.x + bounds.width / 2;
                int y = bounds.y + getRowHeight() + JBUI.scale(8);
                popupMenu.show(source, x, y);
            }
        });
    }

    /********************************************************
     *                    Disposable                        *
     ********************************************************/
    @Getter
    @Setter
    private boolean disposed;

    @Override
    public void disposeInner() {
        getUI().uninstallUI(this);
        setSelectionModel(null);
        clearToggledPaths();
        removeListeners(this);
        nullify();
    }

    @Nullable
    protected Object getTreeNode(MouseEvent event) {
        TreePath path = Trees.getPathAtMousePosition(this, event);
        if (path == null) return null;

        Rectangle pathBounds = getPathBounds(path);
        if (pathBounds == null) return null;

        Point mouseLocation = UserInterface.getRelativeMouseLocation(event.getComponent());
        if (mouseLocation == null) return null;
        if (!pathBounds.contains(mouseLocation)) return null;

        return path.getLastPathComponent();
    }
}
