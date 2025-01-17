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
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.tree.DBNColoredTreeCellRenderer;
import com.dbn.common.ui.tree.DBNTree;
import com.dbn.common.util.TextAttributes;
import com.dbn.data.grid.color.DataGridTextAttributesKeys;
import com.dbn.execution.java.ArgumentValue;
import com.dbn.object.DBJavaField;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBJavaParameter;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import java.awt.Color;
import java.sql.ResultSet;
import java.util.List;

import static com.dbn.common.util.Strings.cachedLowerCase;

class ArgumentValuesTree extends DBNTree{

    ArgumentValuesTree(JavaExecutionResultForm parent, List<ArgumentValue> inputArgumentValues) {
        super(parent, createModel(parent, inputArgumentValues));
        setCellRenderer(new CellRenderer());
        Color bgColor = TextAttributes.getSimpleTextAttributes(DataGridTextAttributesKeys.PLAIN_DATA).getBgColor();
        setBackground(bgColor == null ? Colors.getTableBackground() : bgColor);

        addTreeSelectionListener(createTreeSelectionListener());
    }


    @NotNull
    public JavaExecutionResultForm getParentForm() {
        return this.getParentComponent();
    }

    @NotNull
    private static ArgumentValuesTreeModel createModel(JavaExecutionResultForm parentForm, List<ArgumentValue> inputArgumentValues) {
        return new ArgumentValuesTreeModel(parentForm.getMethod(), inputArgumentValues);
    }

    private TreeSelectionListener createTreeSelectionListener() {
        return e -> {
            TreePath path = e.getPath();
            ArgumentValuesTreeNode treeNode = (ArgumentValuesTreeNode) path.getLastPathComponent();
            if (treeNode == null) return;

            Object userValue = treeNode.getUserValue();
            if (userValue instanceof ArgumentValue) {
                ArgumentValue argumentValue = (ArgumentValue) userValue;
                DBJavaParameter argument = argumentValue.getArgument();
                if (argument == null) return;

                Object value = argumentValue.getValue();
                if (value instanceof ResultSet || argumentValue.isLargeObject() || argumentValue.isLargeValue()) {
                    getParentForm().selectArgumentOutputTab(argument);
                }
            }
        };
    }

    static class CellRenderer extends DBNColoredTreeCellRenderer {
        @Override
        public void customizeCellRenderer(@NotNull DBNTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            ArgumentValuesTreeNode treeNode = (ArgumentValuesTreeNode) value;
            Object userValue = treeNode.getUserValue();
            if (userValue instanceof DBJavaMethod) {
                DBJavaMethod method = (DBJavaMethod) userValue;
                setIcon(method.getIcon());
                append(method.getSignature(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
            }

            if (userValue instanceof String) {
                append((String) userValue, treeNode.isLeaf() ?
                        SimpleTextAttributes.REGULAR_ATTRIBUTES :
                        SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
            }

            if (userValue instanceof DBObjectRef) {
                DBObjectRef<DBJavaParameter> argumentRef = (DBObjectRef<DBJavaParameter>) userValue;
                DBJavaParameter argument = DBObjectRef.get(argumentRef);
                setIcon(argument == null ? Icons.DBO_ARGUMENT : argument.getIcon());
                append(argumentRef.getObjectName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
            }

            if (userValue instanceof ArgumentValue) {
                ArgumentValue argumentValue = (ArgumentValue) userValue;
                DBJavaParameter argument = argumentValue.getArgument();
                Object originalValue = argumentValue.getValue();
                String displayValue = originalValue instanceof ResultSet || argumentValue.isLargeObject() || argumentValue.isLargeValue() ? "" : String.valueOf(originalValue);
                String argumentName;
                String dataType;

                setIcon(DBObjectType.ARGUMENT.getIcon());

                if (argument == null) {
                    DBJavaField field = argumentValue.getField();

                    if(field == null) {
                        argumentName = "[unknown]";
                        dataType = null;
                    } else {
                        argumentName = field.getName();
                        dataType = field.getType();
                    }
                } else {
                    argumentName = argument.getName();
                    dataType = argument.getParameterType();
                }

                append(argumentName, SimpleTextAttributes.REGULAR_ATTRIBUTES);
                append(" = ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
                if (dataType != null) {
                    append("{" + cachedLowerCase(dataType) + "} " , SimpleTextAttributes.GRAY_ATTRIBUTES);
                }
                append(displayValue, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
            }
        }
    }
}
