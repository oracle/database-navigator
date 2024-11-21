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

import com.dbn.browser.model.BrowserTreeNode;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.StatefulDisposable;
import com.dbn.common.latent.Latent;
import com.dbn.common.ui.SpeedSearchBase;
import com.dbn.connection.ConnectionBundle;
import com.dbn.connection.ConnectionHandler;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBObjectBundle;
import lombok.Getter;
import lombok.Setter;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;

public class DatabaseBrowserTreeSpeedSearch extends SpeedSearchBase<DatabaseBrowserTree> implements StatefulDisposable, TreeModelListener {
    private static final Object[] EMPTY_ARRAY = new Object[0];
    private final Latent<Object[]> elements = Latent.basic(() -> {
        List<BrowserTreeNode> nodes = new ArrayList<>();
        BrowserTreeNode root = getComponent().getModel().getRoot();
        loadElements(nodes, root);
        return nodes.toArray();
    });

    DatabaseBrowserTreeSpeedSearch(DatabaseBrowserTree tree) {
        super(tree);
        getComponent().getModel().addTreeModelListener(this);

        Disposer.register(tree, this);
    }

    @Override
    protected int getSelectedIndex() {
        Object[] elements = getAllElements();
        BrowserTreeNode treeNode = getSelectedTreeElement();
        if (treeNode != null) {
            for (int i=0; i<elements.length; i++) {
                if (treeNode == elements[i]) {
                    return i;
                }
            }
        }
        return -1;
    }

    private BrowserTreeNode getSelectedTreeElement() {
        TreePath selectionPath = getComponent().getSelectionPath();
        if (selectionPath != null) {
            return (BrowserTreeNode) selectionPath.getLastPathComponent();
        }
        return null;
    }

    @Override
    protected Object[] getElements() {
        return elements.get();
    }

    private static void loadElements(List<BrowserTreeNode> nodes, BrowserTreeNode browserTreeNode) {
        if (!browserTreeNode.isTreeStructureLoaded()) return;

        if (browserTreeNode instanceof ConnectionBundle) {
            ConnectionBundle connectionBundle = (ConnectionBundle) browserTreeNode;
            for (ConnectionHandler connection : connectionBundle.getConnections()){
                DBObjectBundle objectBundle = connection.getObjectBundle();
                loadElements(nodes, objectBundle);
            }
        }
        else {
            for (BrowserTreeNode treeNode : browserTreeNode.getChildren()) {
                if (treeNode instanceof DBObject) {
                    nodes.add(treeNode);
                }
                loadElements(nodes, treeNode);
            }
        }
    }

    @Override
    protected String getElementText(Object o) {
        BrowserTreeNode treeNode = (BrowserTreeNode) o;
        return treeNode.getPresentableText();
    }

    @Override
    protected void selectElement(Object o, String s) {
        BrowserTreeNode treeNode = (BrowserTreeNode) o;
        getComponent().selectElement(treeNode, false);

/*
        TreePath treePath = DatabaseBrowserUtils.createTreePath(treeNode);
        tree.setSelectionPath(treePath);
        tree.scrollPathToVisible(treePath);
*/
    }

    @Override
    public void treeNodesChanged(TreeModelEvent e) { elements.reset(); }

    @Override
    public void treeNodesInserted(TreeModelEvent e) { elements.reset(); }

    @Override
    public void treeNodesRemoved(TreeModelEvent e) { elements.reset(); }

    @Override
    public void treeStructureChanged(TreeModelEvent e) { elements.reset(); }

    @Getter
    @Setter
    private boolean disposed;


    @Override
    public void disposeInner() {
        getComponent().getModel().removeTreeModelListener(this);
        elements.set(EMPTY_ARRAY);
        nullify();
    }
}
