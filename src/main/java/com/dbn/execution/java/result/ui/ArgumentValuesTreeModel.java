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

package com.dbn.execution.java.result.ui;

import com.dbn.execution.common.input.ExecutionValue;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaField;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBJavaParameter;
import lombok.Getter;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.List;

@Getter
public class ArgumentValuesTreeModel implements TreeModel {
    private final ArgumentValuesTreeNode root;

    ArgumentValuesTreeModel(DBJavaMethod method, List<ExecutionValue> inputValues) {
        root = new ArgumentValuesTreeNode(null, method.ref(), null);
        ArgumentValuesTreeNode inputNode = new ArgumentValuesTreeNode(root, null, "Input");
        ArgumentValuesTreeNode outputNode = new ArgumentValuesTreeNode(root, null, "Output");
        createArgumentValueNodes(method, inputNode, inputValues);
    }

    private static void createArgumentValueNodes(DBJavaMethod method, ArgumentValuesTreeNode parentNode, List<ExecutionValue> inputValues) {
        for (ExecutionValue fieldValue : inputValues) {
            String[] tokens = fieldValue.getPath().split("\\.");
            DBJavaParameter parameter = method.getParameter(tokens[0]);

            ArgumentValuesTreeNode argumentNode = parentNode.initChild(parameter);
            DBJavaClass argumentClass = parameter.getJavaClass();

            for (int i = 1; i < tokens.length; i++) {
                if (argumentClass == null) break;
                DBJavaField field = argumentClass.getField(tokens[i]);
                argumentNode = argumentNode.initChild(field);
                argumentClass = field.getJavaClass();
            }
            argumentNode.setUserValue(fieldValue);
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
