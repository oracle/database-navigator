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

import com.dbn.common.data.Data;
import com.dbn.execution.common.input.ExecutionValue;
import com.dbn.execution.common.input.ValueHolder;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaField;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBJavaParameter;
import com.dbn.object.lookup.DBObjectRef;
import lombok.Getter;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.Arrays;
import java.util.List;

import static com.dbn.object.lookup.DBJavaNameCache.getCanonicalName;
import static com.dbn.object.type.DBJavaScalarType.isScalar;

@Getter
public class ArgumentValuesTreeModel implements TreeModel {
    private final ArgumentValuesTreeNode root;

    ArgumentValuesTreeModel(DBJavaMethod method, List<ExecutionValue> inputValues, List<ExecutionValue> outputValues) {
        root = new ArgumentValuesTreeNode(null, null, method.ref(), null);
        ArgumentValuesTreeNode inputNode = new ArgumentValuesTreeNode(root, "Input", null, null);
        ArgumentValuesTreeNode outputNode = new ArgumentValuesTreeNode(root, "Output", null, null);

        createArgumentValueNodes(method, inputNode, inputValues);
        createOutputValuesNodes(method, outputNode, outputValues);
    }

    private static void createOutputValuesNodes(DBJavaMethod method, ArgumentValuesTreeNode parentNode, List<ExecutionValue> outputValues) {
        // void
        if (method.isReturningVoid()) return;

        DBObjectRef<DBJavaClass> returnClassRef = method.getReturnClassRef();

        if (outputValues.get(0).isArrayObject()) {
            Object[] elements = (Object[]) outputValues.get(0).getValue();
            Object[] firstThree = Arrays.copyOfRange(elements, 0, Math.min(3, elements.length));

            String arrayString = Data.listToArrayString(Arrays.asList(firstThree));
            if (elements.length > 3) arrayString += "...";

            ExecutionValue<String> executionValue = new ExecutionValue<>("return", ValueHolder.basic(arrayString));
            new ArgumentValuesTreeNode(parentNode, "return", returnClassRef, executionValue);
            return;
        }

        // scalar (single value)
        if(returnClassRef == null || isScalar(returnClassRef)){
            new ArgumentValuesTreeNode(parentNode, "return", returnClassRef, outputValues.get(0));
            return;
        }

        // complex java class
        ArgumentValuesTreeNode  argumentNode = new ArgumentValuesTreeNode(parentNode, "return", returnClassRef, null);
        DBJavaClass returnClass = returnClassRef.get();
        if (returnClass == null) return;

        for (ExecutionValue fieldValue : outputValues) {
            String[] tokens = fieldValue.getPath().split("\\.");
            ArgumentValuesTreeNode currentNode = argumentNode;
            DBJavaClass currentClass = returnClass;

            for (int i = 1; i < tokens.length; i++) {
                DBJavaField field = getField(currentClass, tokens[i]);
                if(field == null) continue;

                ArgumentValuesTreeNode childNode = currentNode.initChild(field);
                childNode.setValue(fieldValue);

                if (field.isScalar())  continue;
                DBJavaClass childReturnClass = field.getJavaClass();
                if (childReturnClass != null) { // it is complex field
                    childNode.setValue(null);
                    currentNode = childNode;
                    currentClass = childReturnClass;
                }
            }
        }
    }

    private static void createArgumentValueNodes(DBJavaMethod method, ArgumentValuesTreeNode parentNode, List<ExecutionValue> inputValues) {
        for (ExecutionValue fieldValue : inputValues) {
            String[] tokens = fieldValue.getPath().split("\\.");
            DBJavaParameter parameter = method.getParameter(tokens[0]);
            String dataType = getCanonicalName(parameter.getJavaClassRef());

            Class dataTypeClass = Data.asPrimitiveClass(dataType);
            if (parameter.isArray()) {
                List elementsString = Data.arrayStringToList((String) fieldValue.getValue(), dataTypeClass);
                Object[] elements = elementsString.toArray();
                Object[] firstThree = Arrays.copyOfRange(elements, 0, Math.min(3, elements.length));

                String arrayString = Data.listToArrayString(Arrays.asList(firstThree));
                if (elements.length > 3) arrayString += "...";

                ExecutionValue<String> executionValue = new ExecutionValue<>(parameter.getName(), ValueHolder.basic(arrayString));
                ArgumentValuesTreeNode argumentNode = parentNode.initChild(parameter);
                argumentNode.setValue(executionValue);
                return;
            }

            ArgumentValuesTreeNode argumentNode = parentNode.initChild(parameter);
            DBJavaClass argumentClass = parameter.getJavaClass();

            for (int i = 1; i < tokens.length; i++) {
                if (argumentClass == null) break;
                DBJavaField field = getField(argumentClass, tokens[i]);
                argumentNode = argumentNode.initChild(field);
                argumentClass = field.getJavaClass();
            }
            argumentNode.setValue(fieldValue);
        }
    }

    private static DBJavaField getField(DBJavaClass dbJavaClass, String fieldName){
        DBJavaField field = dbJavaClass.getField(fieldName);
        if(field == null){
            // TODO check why field is null
            List<DBJavaField> fields =  dbJavaClass.getFields();
            for(DBJavaField field1: fields){
                if(field1.getName().equals(fieldName))
                    field = field1;
            }
        }
        return field;
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
