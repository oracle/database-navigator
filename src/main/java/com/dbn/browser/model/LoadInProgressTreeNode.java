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
import com.dbn.common.load.LoadInProgressIcon;
import com.dbn.common.ref.WeakRef;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.type.DBObjectType;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FileStatus;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.util.ArrayList;

import static com.dbn.common.dispose.Failsafe.nn;

public class LoadInProgressTreeNode extends StatefulDisposableBase implements BrowserTreeNode {
    private final WeakRef<BrowserTreeNode> parent;

    public LoadInProgressTreeNode(@NotNull BrowserTreeNode parent) {
        this.parent = WeakRef.of(parent);
    }

    @Override
    public boolean isTreeStructureLoaded() {
        return true;
    }

    @Override
    public void initTreeElement() {}

    @Override
    public boolean canExpand() {
        return false;
    }

    @Override
    public int getTreeDepth() {
        return getParent().getTreeDepth() + 1;
    }

    @Override
    public BrowserTreeNode getChildAt(int index) {
        return null;
    }

    @Override
    @NotNull
    public BrowserTreeNode getParent() {
        return parent.ensure();
    }

    @Override
    public List getChildren() {
        return null;
    }

    @Override
    public void refreshTreeChildren(@NotNull DBObjectType... objectTypes) {}

    @Override
    public void rebuildTreeChildren() {}

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public int getIndex(BrowserTreeNode child) {
        return -1;
    }

    @Override
    public Icon getIcon(int flags) {
        return LoadInProgressIcon.INSTANCE;
    }
    @Override
    public String getPresentableText() {
        return "Loading...";
    }

    @Override
    public String getPresentableTextDetails() {
        return null;
    }

    @Override
    public String getPresentableTextConditionalDetails() {
        return null;
    }

    @NotNull
    @Override
    public ConnectionId getConnectionId() {
        return getParent().getConnectionId();
    }

    @Override
    @NotNull
    public ConnectionHandler getConnection() {
        return nn(getParent().getConnection());
    }

    @Override
    @NotNull
    public Project getProject() {
        return getParent().getProject();
    }

    /*********************************************************
    *                    ItemPresentation                    *
    *********************************************************/

    @Override
    public Icon getIcon(boolean open) {
        return null;
    }

    public TextAttributesKey getTextAttributesKey() {
        return SQLTextAttributesKeys.IDENTIFIER;
    }

    /*********************************************************
    *                    NavigationItem                      *
    *********************************************************/
    @Override
    public void navigate(boolean requestFocus) {}
    @Override
    public boolean canNavigate() { return false;}
    @Override
    public boolean canNavigateToSource() {return false;}

    @NotNull
    @Override
    public String getName() {
        return "";
    }

    @Override
    public ItemPresentation getPresentation() {
        return this;
    }

    public FileStatus getFileStatus() {
        return FileStatus.NOT_CHANGED;
    }

    /*********************************************************
    *                    ToolTipProvider                    *
    *********************************************************/
    @Override
    public String getToolTip() {
        return null;
    }

    public static class List extends ArrayList<BrowserTreeNode> implements Disposable {
        @Override
        public void dispose() {
            if (size() > 0) {
                BrowserTreeNode browserTreeNode = get(0);
                browserTreeNode.dispose();
                clear();
            }
        }
    }

    @Override
    public boolean isDisposed() {
        if (super.isDisposed()) return true;

        BrowserTreeNode parent = WeakRef.get(this.parent);
        if (parent == null || parent.isDisposed()) {
            setDisposed(true);
        }

        if (parent instanceof DBObject) {
            DBObject object = (DBObject) parent;
            if (object.isTreeStructureLoaded()) {
                setDisposed(true);
            }
        } else if (parent instanceof DBObjectList) {
            DBObjectList objectList = (DBObjectList) parent;
            if (objectList.isLoaded()) {
                setDisposed(true);
            }
        }

        return super.isDisposed();
    }

    @Override
    public void disposeInner() {

    }
}
