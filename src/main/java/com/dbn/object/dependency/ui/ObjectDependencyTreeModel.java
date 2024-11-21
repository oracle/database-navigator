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

import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.exception.OutdatedContentException;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.ui.tree.TreeEventType;
import com.dbn.common.ui.tree.Trees;
import com.dbn.common.ui.util.Listeners;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.dependency.ObjectDependencyType;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.List;

public class ObjectDependencyTreeModel extends StatefulDisposableBase implements TreeModel {
    private final Listeners<TreeModelListener> listeners = Listeners.create(this);

    private final ObjectDependencyTreeNode root;
    private final ObjectDependencyType dependencyType;
    private final DBObjectRef<DBSchemaObject> object;

    private WeakRef<ObjectDependencyTree> tree;

    ObjectDependencyTreeModel(DBSchemaObject object, ObjectDependencyType dependencyType) {
        this.object = DBObjectRef.of(object);
        this.root = new ObjectDependencyTreeNode(this, object);
        this.dependencyType = dependencyType;
    }

    public DBSchemaObject getObject() {
        return DBObjectRef.get(object);
    }

    public void setTree(ObjectDependencyTree tree) {
        this.tree = WeakRef.of(tree);
    }

    @NotNull
    public ObjectDependencyTree getTree() {
        return tree.ensure();
    }

    @Nullable
    public Project getProject() {
        return getTree().getProject();
    }

    ObjectDependencyType getDependencyType() {
        return dependencyType;
    }

    @Override
    @NotNull
    public ObjectDependencyTreeNode getRoot() {
        return root;
    }

    @Override
    public Object getChild(Object parent, int index) {
        List<ObjectDependencyTreeNode> children = getChildren(parent);
        if (children.size() <= index) throw new OutdatedContentException(parent);
        return children.get(index);
    }

    @Override
    public int getChildCount(Object parent) {
        return getChildren(parent).size();
    }

    @Override
    public boolean isLeaf(Object node) {
        return getChildCount(node) == 0;
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        return getChildren(parent).indexOf(child);
    }

    private List<ObjectDependencyTreeNode> getChildren(Object parent) {
        ObjectDependencyTreeNode parentNode = (ObjectDependencyTreeNode) parent;
        return parentNode.getChildren(true);
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {

    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        listeners.remove(l);
    }

    void refreshLoadInProgressNode(ObjectDependencyTreeNode node) {
        TreePath treePath = new TreePath(node.getTreePath());
        Trees.notifyTreeModelListeners(node, listeners, treePath, TreeEventType.STRUCTURE_CHANGED);
    }

    void notifyNodeLoaded(ObjectDependencyTreeNode node) {
        TreePath treePath = new TreePath(node.getTreePath());
        Trees.notifyTreeModelListeners(node, listeners, treePath, TreeEventType.STRUCTURE_CHANGED);
    }

    @Override
    public void disposeInner() {
        Disposer.dispose(root);
        listeners.clear();
        nullify();
    }
}
