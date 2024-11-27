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

import com.dbn.common.ui.tree.DBNTreeModel;
import com.dbn.common.ui.tree.DBNTreeNode;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Getter
public class ObjectTreeModel extends DBNTreeModel {
    private TreePath initialSelection;
    private Object[] elements;

    public ObjectTreeModel(DBSchema schema, Set<DBObjectType> objectTypes, DBObject selectedObject) {
        super(new DBNTreeNode(schema == null ? "No schema selected" : schema.ref()));
        if (schema == null) return;


        DBNTreeNode rootNode = getRoot();

        for (DBObjectType objectType : objectTypes) {
            for (DBObject schemaObject :schema.collectChildObjects(objectType)) {
                DefaultMutableTreeNode objectNode = new DBNTreeNode(schemaObject.ref());
                rootNode.add(objectNode);
                if (selectedObject != null && selectedObject.equals(schemaObject)) {
                    initialSelection = new TreePath(objectNode.getPath());
                }
            }
        }

        for (DBObjectType schemaObjectType : schema.getObjectType().getChildren()) {
            if (!hasChild(schemaObjectType, objectTypes)) continue;

            for (DBObject schemaObject : schema.collectChildObjects(schemaObjectType)) {
                DefaultMutableTreeNode bundleNode = new DBNTreeNode(schemaObject.ref());

                List<DBObject> objects = new ArrayList<>();
                for (DBObjectType objectType : objectTypes) {
                    objects.addAll(schemaObject.collectChildObjects(objectType));
                }
                if (objects.isEmpty()) continue;

                rootNode.add(bundleNode);
                for (DBObject object : objects) {
                    DefaultMutableTreeNode objectNode = new DBNTreeNode(object.ref());
                    bundleNode.add(objectNode);
                    if (selectedObject != null && selectedObject.equals(object)) {
                        initialSelection = new TreePath(objectNode.getPath());
                    }
                }
            }
        }
    }

    private boolean hasChild(DBObjectType parentObjectType, Set<DBObjectType> objectTypes) {
        for (DBObjectType objectType : objectTypes) {
            if (parentObjectType.hasChild(objectType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public DBNTreeNode getRoot() {
        return (DBNTreeNode) super.getRoot();
    }

    public Object[] getAllElements() {
        if (elements == null) {
            List elementList = new ArrayList();
            collect(getRoot(), elementList);
            elements = elementList.toArray();
        }
        return elements;
    }

    private static void collect(TreeNode node, List<TreeNode> bucket) {
        bucket.add(node);
        for (int i=0; i<node.getChildCount(); i++) {
            TreeNode childNode = node.getChildAt(i);
            collect(childNode, bucket);
        }
    }

    @Override
    public void disposeInner() {
        super.disposeInner();
        elements = new Object[0];
    }
}
