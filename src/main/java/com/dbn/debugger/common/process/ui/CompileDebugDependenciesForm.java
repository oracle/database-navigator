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

package com.dbn.debugger.common.process.ui;

import com.dbn.common.text.TextContent;
import com.dbn.common.ui.Presentable;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Lists;
import com.dbn.connection.context.DatabaseContext;
import com.dbn.debugger.common.config.DBRunConfig;
import com.dbn.debugger.common.config.ui.CompileDebugDependenciesDialog;
import com.dbn.debugger.common.config.ui.ObjectListCellRenderer;
import com.dbn.object.DBMethod;
import com.dbn.object.DBProgram;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.lookup.DBObjectRef;
import org.jetbrains.annotations.NotNull;

import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.dbn.common.text.TextContent.plain;

public class CompileDebugDependenciesForm extends DBNFormBase {
    private JList<DBSchemaObject> objectList;
    private JPanel mainPanel;
    private JCheckBox rememberSelectionCheckBox;
    private JPanel headerPanel;
    private JPanel hintPanel;

    public CompileDebugDependenciesForm(CompileDebugDependenciesDialog parent, DBRunConfig<?> runConfiguration, List<DBSchemaObject> compileList) {
        super(parent);
        TextContent hintText = plain("The program you are trying to debug or some of its dependencies are not compiled with debug information." +
                "This may result in breakpoints being ignored during the debug execution, as well as missing information about execution stacks and variables.\n" +
                "In order to achieve full debugging support you are advised to compile the respective programs in debug mode.");

        DBNHintForm hintForm = new DBNHintForm(this, hintText, null, true);
        hintPanel.add(hintForm.getComponent());

        objectList.setCellRenderer(new ObjectListCellRenderer());
        DefaultListModel<DBSchemaObject> model = new DefaultListModel<>();

        Collections.sort(compileList);
        for (DBSchemaObject schemaObject : compileList) {
            model.addElement(schemaObject);
        }
        objectList.setModel(model);

        List<DBMethod> methods = runConfiguration.getMethods();

        List<DBSchemaObject> selectedObjects = new ArrayList<>();
        for (DBMethod method : methods) {
            DBProgram<?, ?, ?> program = method.getProgram();
            DBSchemaObject selectedObject = program == null ? method : program;
            if (!selectedObjects.contains(selectedObject)) {
                selectedObjects.add(selectedObject);
            }
        }

        int[] selectedIndicesArray = computeSelection(compileList, selectedObjects);

        objectList.setSelectedIndices(selectedIndicesArray);
        if (selectedIndicesArray.length > 0) {
            objectList.ensureIndexIsVisible(selectedIndicesArray.length - 1);
        }

        DatabaseContext databaseContext = runConfiguration.getDatabaseContext();
        DBNHeaderForm headerForm = databaseContext instanceof DBObject ?
                new DBNHeaderForm(this, (DBObject) databaseContext) :
                new DBNHeaderForm(this, Commons.nvl(databaseContext, Presentable.UNKNOWN));
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);
        parent.registerRememberSelectionCheckBox(rememberSelectionCheckBox);
    }

    private int[] computeSelection(List<DBSchemaObject> compileList, List<DBSchemaObject> selectedObjects) {
        List<Integer> selectedIndices = new ArrayList<>();
        for (DBSchemaObject selectedObject : selectedObjects) {
            int index = compileList.indexOf(selectedObject);
            if (index > -1) {
                selectedIndices.add(index);
            }
        }


        int[] selectedIndicesArray = new int[selectedIndices.size()];
        for (int i = 0; i < selectedIndices.size(); i++) {
            Integer selectedIndex = selectedIndices.get(i);
            selectedIndicesArray[i] = selectedIndex;
        }
        return selectedIndicesArray;
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public List<DBObjectRef<?>> getSelection() {
        List<DBSchemaObject> selectedValuesList = objectList.getSelectedValuesList();
        return Lists.convert(selectedValuesList, o -> o.ref());
    }

    public void selectAll() {
        objectList.setSelectionInterval(0, objectList.getModel().getSize() -1);
    }

    public void selectNone() {
        objectList.clearSelection();
    }

}
