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

import com.dbn.code.sql.color.SQLTextAttributesKeys;
import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.icon.Icons;
import com.dbn.common.project.ProjectRef;
import com.dbn.common.util.Commons;
import com.dbn.connection.ConnectionBundle;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.object.type.DBObjectType;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FileStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.List;

public final class ConnectionBundleBrowserTreeRoot extends StatefulDisposableBase implements BrowserTreeNode {
    private final List<ConnectionBundle> rootChildren = new ArrayList<>();
    private final ProjectRef project;

    ConnectionBundleBrowserTreeRoot(@NotNull Project project, @Nullable ConnectionBundle connectionBundle) {
        this.project = ProjectRef.of(project);
        if (connectionBundle != null) {
            this.rootChildren.add(connectionBundle);
        }
    }

    @Override
    @NotNull
    public Project getProject() {
        return project.ensure();
    }

    /**************************************************
     *              BrowserTreeNode            *
     **************************************************/
    @Override
    public boolean isTreeStructureLoaded() {
        return true;
    }

    @Override
    public void initTreeElement() {}

    @Override
    public boolean canExpand() {
        return true;
    }

    @Override
    public int getTreeDepth() {
        return 0;
    }

    @Override
    @Nullable
    public BrowserTreeNode getParent() {
        return null;
    }

    @Override
    public List<ConnectionBundle> getChildren() {
        return rootChildren;
    }

    @Override
    public void refreshTreeChildren(@NotNull DBObjectType... objectTypes) {
        for (ConnectionBundle connectionBundle : getChildren()) {
            connectionBundle.refreshTreeChildren(objectTypes);
        }
    }

    @Override
    public void rebuildTreeChildren() {
        for (ConnectionBundle connectionBundle : getChildren()) {
            connectionBundle.rebuildTreeChildren();
        }
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
    public boolean isLeaf() {
        return false;
    }

    @Override
    public int getIndex(BrowserTreeNode child) {
        return getChildren().indexOf(child);
    }

    @Override
    public Icon getIcon(int flags) {
        return Icons.CONNECTIONS;
    }

    @Override
    public String getPresentableText() {
        return "Connection Managers";
    }

    @Override
    public String getPresentableTextDetails() {
        return null;
    }

    @Override
    public String getPresentableTextConditionalDetails() {
        return null;
    }

    /**************************************************
     *              GenericDatabaseElement            *
     **************************************************/

    @NotNull
    @Override
    public ConnectionId getConnectionId() {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public ConnectionHandler getConnection() {
        throw new UnsupportedOperationException();
    }

   /*********************************************************
    *                    ToolTipProvider                    *
    *********************************************************/
    @Override
    public String getToolTip() {
        return null;
    }

    /*********************************************************
     *                  NavigationItem                       *
     *********************************************************/
    @NotNull
    @Override
    public String getName() {
        return Commons.nvl(getPresentableText(), "Database Objects");
    }
    
    @Override
    public void navigate(boolean b) {}

    @Override
    public boolean canNavigate() {
        return false;
    }

    @Override
    public boolean canNavigateToSource() {
        return false;
    }

    @Override
    public ItemPresentation getPresentation() {
        return this;
    }

    public FileStatus getFileStatus() {
        return FileStatus.NOT_CHANGED;
    }

    /*********************************************************
     *                 ItemPresentation                      *
     *********************************************************/

    @Override
    public Icon getIcon(boolean open) {
        return getIcon(0);
    }

    public TextAttributesKey getTextAttributesKey() {
        return SQLTextAttributesKeys.IDENTIFIER;
    }


    @Override
    public void disposeInner() {
        rootChildren.clear();
    }
}
