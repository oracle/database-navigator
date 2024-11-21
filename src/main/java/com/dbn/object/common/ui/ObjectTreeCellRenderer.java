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

import com.dbn.common.ui.tree.DBNColoredTreeCellRenderer;
import com.dbn.common.ui.tree.DBNTree;
import com.dbn.common.ui.tree.Trees;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBMethod;
import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultMutableTreeNode;

public class ObjectTreeCellRenderer extends DBNColoredTreeCellRenderer {
    @Override
    public void customizeCellRenderer(@NotNull DBNTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) value;
        Object userObject = treeNode.getUserObject();
        if (userObject instanceof DBObjectRef) {
            DBObjectRef<?> objectRef = (DBObjectRef) userObject;
            append(objectRef.getObjectName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);

            DBObject object = DBObjectRef.get(objectRef);
            setIcon(object == null ? objectRef.getObjectType().getIcon() : object.getOriginalIcon());

            if (object instanceof DBMethod || object instanceof DBJavaMethod) {
                if (object.getOverload() > 0) {
                    append(" #" + object.getOverload(), SimpleTextAttributes.GRAY_ATTRIBUTES);
                }
            }

        } else {
            append(userObject.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }
        Trees.applySpeedSearchHighlighting(tree, this, true, selected);
    }
}
