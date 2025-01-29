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

package com.dbn.common.ui.dialog;

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.JBList;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.List;

public class SelectionListForm<T> extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private @Getter JBList<T> selectionList;

    public SelectionListForm(SelectionListDialog<T> dialog, @Nullable Object contextObject) {
        super(dialog);

        if (contextObject == null) {
            headerPanel.setVisible(false);
        } else {
            DBNHeaderForm headerForm = new DBNHeaderForm(this, contextObject);
            headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);
        }


        List<T> elements = dialog.getElements();
        T selection = dialog.getInitialSelection();

        DefaultListModel<T> model = new DefaultListModel<>();
        elements.forEach(e -> model.addElement(e));

        selectionList.setModel(model);
        selectionList.setSelectedValue(selection == null ? elements.get(0) : selection, true);
        selectionList.setCellRenderer(SimpleListCellRenderer.create("", o -> o.toString()));
        selectionList.setBorder(null);
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return selectionList;
    }
}
