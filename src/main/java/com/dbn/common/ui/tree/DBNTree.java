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
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.util.UserInterface;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.Tree;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import static com.dbn.common.dispose.ComponentDisposer.removeListeners;

public class DBNTree extends Tree implements DBNComponent {
    private final WeakRef<DBNComponent> parent;

    public DBNTree(@NotNull DBNComponent parent) {
        this.parent = WeakRef.of(parent);
        setTransferHandler(DBNTreeTransferHandler.INSTANCE);

        Disposer.register(parent, this);
    }

    public DBNTree(@NotNull DBNComponent parent, TreeModel treeModel) {
        super(treeModel);
        this.parent = WeakRef.of(parent);
        setTransferHandler(DBNTreeTransferHandler.INSTANCE);

        Disposer.register(parent, this);
        Disposer.register(this, treeModel);
    }

    public DBNTree(@NotNull DBNComponent parent, TreeNode root) {
        super(root);
        this.parent = WeakRef.of(parent);
        setTransferHandler(DBNTreeTransferHandler.INSTANCE);

        Disposer.register(parent, this);
        Disposer.register(this, root);
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

    protected void showContextMenu(TreePath path, int x, int y) {

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
