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

package com.dbn.object.common;

import com.dbn.browser.DatabaseBrowserManager;
import com.dbn.browser.model.BrowserTreeEventListener;
import com.dbn.browser.model.BrowserTreeNode;
import com.dbn.browser.model.LoadInProgressTreeNode;
import com.dbn.browser.ui.ToolTipProvider;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.filter.Filter;
import com.dbn.common.ref.WeakRefCache;
import com.dbn.common.thread.Background;
import com.dbn.common.ui.tree.TreeEventType;
import com.dbn.common.util.Commons;
import com.dbn.connection.ConnectionHandler;
import com.dbn.object.common.property.DBObjectProperty;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.dbn.browser.DatabaseBrowserUtils.treeVisibilityChanged;
import static com.dbn.common.dispose.Failsafe.guarded;
import static com.dbn.common.util.Compactables.compact;
import static com.dbn.common.util.Lists.filter;

public abstract class DBObjectTreeNodeBase implements DBObject, ToolTipProvider {
    protected static final List<BrowserTreeNode> EMPTY_TREE_NODE_LIST = Collections.unmodifiableList(new ArrayList<>(0));

    private static final WeakRefCache<DBObjectTreeNodeBase, List<BrowserTreeNode>> possibleTreeChildren = WeakRefCache.weakKey();
    private static final WeakRefCache<DBObjectTreeNodeBase, List<BrowserTreeNode>> visibleTreeChildren = WeakRefCache.weakKey();

    @Override
    public int getTreeDepth() {
        BrowserTreeNode treeParent = getParent();
        return treeParent == null ? 1 : treeParent.getTreeDepth() + 1;
    }

    @NotNull
    public List<BrowserTreeNode> getPossibleTreeChildren() {
        return possibleTreeChildren.get(this, o -> {
            List<BrowserTreeNode> children = o.buildPossibleTreeChildren();
            return compact(children);
        });
    }

    @Override
    public List<? extends BrowserTreeNode> getChildren() {
        return visibleTreeChildren.get(this, o -> {
            Background.run(o.getProject(), () -> o.buildTreeChildren());
            return Collections.singletonList(new LoadInProgressTreeNode(o));
        });
    }

    public void buildTreeChildren() {
        checkDisposed();
        ConnectionHandler connection = this.getConnection();
        Filter<BrowserTreeNode> objectTypeFilter = connection.getObjectTypeFilter();

        List<BrowserTreeNode> treeNodes = filter(getPossibleTreeChildren(), objectTypeFilter);
        treeNodes = Commons.nvl(treeNodes, Collections.emptyList());

        for (BrowserTreeNode objectList : treeNodes) {
            Background.run(getProject(), () -> objectList.initTreeElement());
            checkDisposed();
        }

        visibleTreeChildren.set(this, compact(treeNodes));
        set(DBObjectProperty.TREE_LOADED, true);

        Project project = Failsafe.nn(getProject());
        ProjectEvents.notify(project,
                BrowserTreeEventListener.TOPIC,
                (listener) -> listener.nodeChanged(this, TreeEventType.STRUCTURE_CHANGED));
        DatabaseBrowserManager.scrollToSelectedElement(this.getConnection());
    }

    @Override
    public void refreshTreeChildren(@NotNull DBObjectType... objectTypes) {
        List<BrowserTreeNode> treeNodes = visibleTreeChildren.get(this);
        if (treeNodes == null) return;

        for (BrowserTreeNode treeNode : treeNodes) {
            treeNode.refreshTreeChildren(objectTypes);
        }

    }

    @Override
    public void rebuildTreeChildren() {
        List<BrowserTreeNode> treeNodes = visibleTreeChildren.get(this);
        if (treeNodes == null) return;

        ConnectionHandler connection = this.getConnection();
        Filter<BrowserTreeNode> filter = connection.getObjectTypeFilter();

        if (treeVisibilityChanged(getPossibleTreeChildren(), treeNodes, filter)) {
            buildTreeChildren();
        }
        for (BrowserTreeNode treeNode : treeNodes) {
            treeNode.rebuildTreeChildren();
        }
    }

    @NotNull
    public List<BrowserTreeNode> buildPossibleTreeChildren() {
        return EMPTY_TREE_NODE_LIST;
    }

    public boolean hasVisibleTreeChildren() {
        return false;
    }

    @Override
    public boolean isLeaf() {
        return guarded(true, this, n -> {
            List<BrowserTreeNode> treeNodes = visibleTreeChildren.get(n);
            if (treeNodes == null) return !n.hasVisibleTreeChildren();
            return treeNodes.isEmpty();
        });
    }

    @Override
    public BrowserTreeNode getChildAt(int index) {
        return getChildren().get(index);
    }

    @Override
    public int getChildCount() {
        return getChildren().size();
    }

    @Override
    public int getIndex(BrowserTreeNode child) {
        return child instanceof LoadInProgressTreeNode ? 0 : getChildren().indexOf(child);
    }

    /*********************************************************
     *                  BrowserTreeNode                   *
     *********************************************************/
    @Override
    public void initTreeElement() {}

    @Override
    public boolean isTreeStructureLoaded() {
        return is(DBObjectProperty.TREE_LOADED);
    }

    @Override
    public boolean canExpand() {
        return !isLeaf() && isTreeStructureLoaded() && getChildAt(0).isTreeStructureLoaded();
    }

    @Override
    public void disposeInner() {
        possibleTreeChildren.remove(this);
        visibleTreeChildren.remove(this);

    }
}
