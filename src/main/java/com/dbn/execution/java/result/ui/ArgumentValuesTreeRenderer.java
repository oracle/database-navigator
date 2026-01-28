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

import com.dbn.common.ui.tree.DBNColoredTreeCellRenderer;
import com.dbn.common.ui.tree.DBNTree;
import com.dbn.common.util.Strings;
import com.dbn.execution.common.input.ExecutionValue;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaField;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBJavaParameter;
import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static com.dbn.object.lookup.DBJavaNameCache.getCanonicalName;
import static com.intellij.ui.SimpleTextAttributes.GRAY_ATTRIBUTES;
import static com.intellij.ui.SimpleTextAttributes.REGULAR_ATTRIBUTES;
import static com.intellij.ui.SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES;

class ArgumentValuesTreeRenderer extends DBNColoredTreeCellRenderer {
    @Override
    public void customizeCellRenderer(@NotNull DBNTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        ArgumentValuesTreeNode treeNode = (ArgumentValuesTreeNode) value;
        DBObject object = DBObjectRef.get(treeNode.getObject());

        if (object instanceof DBJavaMethod method) {
            setIcon(method.getIcon());
            append(method.getSignature(), REGULAR_ATTRIBUTES);
            return;
        }

        renderName(treeNode);
        renderKey(object);
        renderValue(treeNode);

        // data type qualification
        renderDataType(object);

    }

    private void renderName(ArgumentValuesTreeNode treeNode) {
        String name = treeNode.getName();
        if (Strings.isNotEmpty(name)) {
            append(name, REGULAR_ATTRIBUTES);
        }
    }

    private void renderKey(DBObject object) {
        if (object instanceof DBJavaField) {
            append(object.getName(), REGULAR_ATTRIBUTES);

        } else if (object instanceof DBJavaParameter) {
            append(object.getName(), REGULAR_ATTRIBUTES);
        }
    }

    private void renderValue(ArgumentValuesTreeNode treeNode) {
        Object userValue = treeNode.getValue();
        if (userValue instanceof ExecutionValue fieldValue) {
            String stringValue = Objects.toString(fieldValue.getValue());
            append(" = ", REGULAR_ATTRIBUTES);
            append(stringValue, REGULAR_BOLD_ATTRIBUTES);
        }
    }

    private void renderDataType(DBObject object) {
        if (object instanceof DBJavaParameter parameter) {
            String dataType = getCanonicalName(parameter.getJavaClassRef());

            append(" (" + dataType + ")", GRAY_ATTRIBUTES);
            setIcon(object.getIcon());
        } else if (object instanceof DBJavaField field) {
            String dataType = getCanonicalName(field.getJavaClassRef());

            append(" (" + dataType + ")", GRAY_ATTRIBUTES);
            setIcon(object.getIcon());
        } else if (object instanceof DBJavaClass javaClass) {
            append(" (" + javaClass.getCanonicalName() + ")", GRAY_ATTRIBUTES);
        }
    }
}
