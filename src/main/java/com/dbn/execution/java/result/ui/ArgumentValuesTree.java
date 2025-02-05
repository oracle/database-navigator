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

import com.dbn.common.color.Colors;
import com.dbn.common.ui.tree.DBNTree;
import com.dbn.common.util.TextAttributes;
import com.dbn.data.grid.color.DataGridTextAttributesKeys;
import com.dbn.execution.common.input.ExecutionValue;
import com.dbn.object.DBJavaParameter;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import java.awt.Color;
import java.sql.ResultSet;
import java.util.List;

class ArgumentValuesTree extends DBNTree{

    ArgumentValuesTree(JavaExecutionResultForm parent, List<ExecutionValue> inputValues) {
        super(parent, createModel(parent, inputValues));
        setCellRenderer(new ArgumentValuesTreeRenderer());
        Color bgColor = TextAttributes.getSimpleTextAttributes(DataGridTextAttributesKeys.PLAIN_DATA).getBgColor();
        setBackground(bgColor == null ? Colors.getTableBackground() : bgColor);

        addTreeSelectionListener(createTreeSelectionListener());
    }


    @NotNull
    public JavaExecutionResultForm getParentForm() {
        return this.getParentComponent();
    }

    @NotNull
    private static ArgumentValuesTreeModel createModel(JavaExecutionResultForm parentForm, List<ExecutionValue> inputValues) {
        return new ArgumentValuesTreeModel(parentForm.getMethod(), inputValues);
    }

    private TreeSelectionListener createTreeSelectionListener() {
        return e -> {
            TreePath path = e.getPath();
            ArgumentValuesTreeNode treeNode = (ArgumentValuesTreeNode) path.getLastPathComponent();
            if (treeNode == null) return;

            Object userValue = treeNode.getUserValue();
            if (userValue instanceof ExecutionValue) {
                ExecutionValue fieldValue = (ExecutionValue) userValue;
                DBJavaParameter argument = null; // TODO inputValue.getArgument();
                if (argument == null) return;

                Object value = fieldValue.getValue();
                if (value instanceof ResultSet || fieldValue.isLargeObject() || fieldValue.isLargeValue()) {
                    getParentForm().selectArgumentOutputTab(argument);
                }
            }
        };
    }

}
