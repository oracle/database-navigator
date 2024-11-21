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

package com.dbn.execution.method.result.ui;

import com.dbn.execution.method.ArgumentValue;
import com.dbn.object.DBArgument;
import com.dbn.object.DBMethod;
import com.dbn.object.DBTypeAttribute;
import com.dbn.object.lookup.DBObjectRef;
import lombok.Getter;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class ArgumentValuesTreeModel implements TreeModel {
    private final ArgumentValuesTreeNode root;

    ArgumentValuesTreeModel(DBMethod method, List<ArgumentValue> inputArgumentValues, List<ArgumentValue> outputArgumentValues) {
        root = new ArgumentValuesTreeNode(null, method);
        ArgumentValuesTreeNode inputNode = new ArgumentValuesTreeNode(root, "Input");
        ArgumentValuesTreeNode outputNode = new ArgumentValuesTreeNode(root, "Output");

        createArgumentValueNodes(inputNode, inputArgumentValues);
        createArgumentValueNodes(outputNode, outputArgumentValues);
    }

    private static void createArgumentValueNodes(ArgumentValuesTreeNode parentNode, List<ArgumentValue> inputArgumentValues) {
        Map<DBObjectRef<DBArgument>, ArgumentValuesTreeNode> nodeMap = new HashMap<>();
        for (ArgumentValue argumentValue : inputArgumentValues) {
            DBObjectRef<DBArgument> argumentRef = argumentValue.getArgumentRef();
            DBTypeAttribute attribute = argumentValue.getAttribute();

            if (attribute == null) {
                // single value
                new ArgumentValuesTreeNode(parentNode, argumentValue);
            } else {
                // multiple attribute values
                ArgumentValuesTreeNode treeNode = nodeMap.get(argumentRef);
                if (treeNode == null) {
                    treeNode = new ArgumentValuesTreeNode(parentNode, argumentRef);
                    nodeMap.put(argumentRef, treeNode);
                }
                new ArgumentValuesTreeNode(treeNode, argumentValue);
            }
        }
    }

    @Override
    public Object getChild(Object parent, int index) {
        TreeNode treeNode = (TreeNode) parent;
        return treeNode.getChildAt(index);
    }

    @Override
    public int getChildCount(Object parent) {
        TreeNode treeNode = (TreeNode) parent;
        return treeNode.getChildCount();
    }

    @Override
    public boolean isLeaf(Object node) {
        TreeNode treeNode = (TreeNode) node;
        return treeNode.isLeaf();
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        TreeNode treeNode = (TreeNode) parent;
        TreeNode childNode = (TreeNode) child;
        return treeNode.getIndex(childNode);
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
    }
}
