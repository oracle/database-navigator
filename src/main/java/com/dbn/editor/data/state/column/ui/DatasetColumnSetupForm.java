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

package com.dbn.editor.data.state.column.ui;

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.list.CheckBoxList;
import com.dbn.common.util.Actions;
import com.dbn.editor.data.DatasetEditor;
import com.dbn.editor.data.state.column.DatasetColumnSetup;
import com.dbn.editor.data.state.column.DatasetColumnState;
import com.dbn.editor.data.state.column.action.MoveDownAction;
import com.dbn.editor.data.state.column.action.MoveUpAction;
import com.dbn.editor.data.state.column.action.OrderAlphabeticallyAction;
import com.dbn.editor.data.state.column.action.RevertColumnOrderAction;
import com.dbn.editor.data.state.column.action.SelectAllColumnsAction;
import com.dbn.object.DBDataset;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionToolbar;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import javax.swing.ListModel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DatasetColumnSetupForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel actionPanel;
    private JPanel headerPanel;
    private CheckBoxList<ColumnStateSelectable> columnList;
    private final DatasetColumnSetup columnSetup;

    public DatasetColumnSetupForm(@NotNull Disposable parent, @NotNull DatasetEditor datasetEditor) {
        super(parent);
        DBDataset dataset = datasetEditor.getDataset();
        columnSetup = datasetEditor.getColumnSetup();
        List<DatasetColumnState> columnStates = columnSetup.getColumnStates();
        List<ColumnStateSelectable> columnStateSel = new ArrayList<>();
        for (DatasetColumnState columnState : columnStates) {
            columnStateSel.add(new ColumnStateSelectable(dataset, columnState));
        }

        columnList.setElements(columnStateSel);
        columnList.setMutable(true);

        ActionToolbar actionToolbar = Actions.createActionToolbar(actionPanel, false,
                new SelectAllColumnsAction(columnList),
                Actions.SEPARATOR,
                new MoveUpAction(columnList),
                new MoveDownAction(columnList),
                Actions.SEPARATOR,
                new OrderAlphabeticallyAction(columnList),
                new RevertColumnOrderAction(columnList));
        actionPanel.add(actionToolbar.getComponent(), BorderLayout.WEST);

        createHeaderForm(dataset);
    }

    private void createHeaderForm(DBDataset dataset) {
        DBNHeaderForm headerForm = new DBNHeaderForm(this, dataset);
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public boolean applyChanges(){
        boolean changed = columnList.applyChanges();
        ListModel model = columnList.getModel();
        for(int i=0; i<model.getSize(); i++ ) {
            ColumnStateSelectable columnState = columnList.getElementAt(i);
            changed = changed || columnState.getPosition() != i;
            columnState.setPosition((short) i);
        }
        Collections.sort(columnSetup.getColumnStates());
        return changed;
    }

}
