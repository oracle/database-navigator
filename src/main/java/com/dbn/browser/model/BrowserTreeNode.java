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

import com.dbn.browser.ui.ToolTipProvider;
import com.dbn.connection.DatabaseEntity;
import com.dbn.object.type.DBObjectType;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.tree.TreeNode;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public interface BrowserTreeNode extends TreeNode, NavigationItem, ItemPresentation, ToolTipProvider, DatabaseEntity {
    void initTreeElement();

    boolean canExpand();

    int getTreeDepth();

    boolean isTreeStructureLoaded();

    List<? extends BrowserTreeNode> getChildren();

    void refreshTreeChildren(@NotNull DBObjectType... objectTypes);

    void rebuildTreeChildren();

    Icon getIcon(int flags);

    String getPresentableTextDetails();

    String getPresentableTextConditionalDetails();

    @Override
    BrowserTreeNode getChildAt(int index);

    @Override
    @Nullable
    BrowserTreeNode getParent();

    int getIndex(BrowserTreeNode child);

    default String getLocationString() {
        return null;
    }

    @Override
    default Enumeration<? extends BrowserTreeNode> children() {
        return Collections.enumeration(getChildren());
    }

    @Override
    default int getIndex(TreeNode child) {
        return getIndex((BrowserTreeNode) child);
    }

    @Override
    default boolean getAllowsChildren() {
        return !isLeaf();
    }
}
