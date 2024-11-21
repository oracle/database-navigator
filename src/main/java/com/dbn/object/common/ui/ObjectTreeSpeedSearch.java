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

package com.dbn.object.common.ui;

import com.dbn.common.ui.SpeedSearchBase;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.util.ui.tree.TreeUtil;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

public class ObjectTreeSpeedSearch extends SpeedSearchBase<ObjectTree> {

    public ObjectTreeSpeedSearch(ObjectTree objectTree) {
        super(objectTree);
    }

    @Override
    protected int getSelectedIndex() {
        TreePath selectionPath = getComponent().getSelectionPath();
        if (selectionPath == null) return -1;

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
        for (int i=0; i<getAllElements().length; i++) {
            if (getAllElements()[i] == node) {
                return i;
            }
        }
        return 0;
    }

    @Override
    protected Object[] getElements() {
        return getComponent().getModel().getAllElements();
    }

    @Override
    protected String getElementText(Object obj) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) obj;
        Object userObject = node.getUserObject();
        if (userObject instanceof DBObjectRef) {
            DBObjectRef objectRef = (DBObjectRef) userObject;
            return objectRef.getObjectName();
        }
        return userObject.toString();
    }

    @Override
    protected void selectElement(Object obj, String s) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) obj;
        TreeUtil.selectPath(getComponent(), new TreePath(node.getPath()), true);
    }
}
