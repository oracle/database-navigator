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

package com.dbn.browser.model;

import com.dbn.browser.DatabaseBrowserUtils;
import com.dbn.common.Pair;
import com.dbn.common.dispose.StatefulDisposable;
import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.load.LoadInProgressRegistry;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.ui.tree.TreeEventType;
import com.dbn.common.ui.tree.Trees;
import com.dbn.common.ui.util.Listeners;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.dispose.Failsafe.guarded;

public abstract class BrowserTreeModel extends StatefulDisposableBase implements TreeModel, StatefulDisposable {

    private final Listeners<TreeModelListener> listeners = Listeners.create(this);
    private final WeakRef<BrowserTreeNode> root;

    private final LoadInProgressRegistry<LoadInProgressTreeNode> loadInProgressRegistry =
            LoadInProgressRegistry.create(this,
                    node -> BrowserTreeModel.this.notifyListeners(node, TreeEventType.NODES_CHANGED));

    BrowserTreeModel(BrowserTreeNode root) {
        this.root = WeakRef.of(root);
        ProjectEvents.subscribe(getProject(), this, BrowserTreeEventListener.TOPIC, browserTreeEventListener());
    }

    @NotNull
    private BrowserTreeEventListener browserTreeEventListener() {
        return new BrowserTreeEventListener() {
            @Override
            public void nodeChanged(BrowserTreeNode node, TreeEventType eventType) {
                if (contains(node)) {
                    notifyListeners(node, eventType);
                }
            }
        };
    }

    @Override
    public void addTreeModelListener(TreeModelListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener listener) {
        listeners.remove(listener);
    }

    public void notifyListeners(BrowserTreeNode treeNode, final TreeEventType eventType) {
        if (isNotValid(this)) return;
        if (isNotValid(treeNode)) return;

        TreePath treePath = DatabaseBrowserUtils.createTreePath(treeNode);
        Trees.notifyTreeModelListeners(this, listeners, treePath, eventType);
    }

    @NotNull
    public Project getProject() {
        return getRoot().getProject();
    }

    public abstract boolean contains(BrowserTreeNode node);


    /***************************************
     *              TreeModel              *
     ***************************************/
    @Override
    public BrowserTreeNode getRoot() {
        return root.ensure();
    }

    @Override
    public Object getChild(Object parent, int index) {
        if (parent instanceof BrowserTreeNode) {
            BrowserTreeNode treeChild = ((BrowserTreeNode) parent).getChildAt(index);
            if (treeChild instanceof LoadInProgressTreeNode) {
                loadInProgressRegistry.register((LoadInProgressTreeNode) treeChild);
            }
            return treeChild;
        }
        return null;
    }

    @Override
    public int getChildCount(Object parent) {
        if (parent instanceof BrowserTreeNode) {
            BrowserTreeNode parentNode = (BrowserTreeNode) parent;
            if (parentNode.isLeaf()) return 0;

            return guarded(0, parentNode, p -> p.getChildCount());
        }
        return 0;
    }

    @Override
    public boolean isLeaf(Object node) {
        if (node instanceof BrowserTreeNode) {
            BrowserTreeNode treeNode = (BrowserTreeNode) node;
            return treeNode.isLeaf();
        }
        return true;
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if (parent instanceof BrowserTreeNode &&  child instanceof BrowserTreeNode) {
            BrowserTreeNode parentNode = (BrowserTreeNode) parent;
            BrowserTreeNode childNode = (BrowserTreeNode) child;
            return guarded(-1, Pair.of(parentNode, childNode), p -> p.first().getIndex(p.second()));
        }
        return -1;
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {

    }

    @Override
    public void disposeInner() {
        nullify();
    }


}
