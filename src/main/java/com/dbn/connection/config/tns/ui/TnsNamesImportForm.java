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

package com.dbn.connection.config.tns.ui;

import com.dbn.common.icon.Icons;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.util.Strings;
import com.dbn.connection.config.tns.TnsNames;
import com.dbn.connection.config.tns.TnsNamesParser;
import com.dbn.connection.config.tns.TnsProfile;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBScrollPane;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.io.File;
import java.util.List;

import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class TnsNamesImportForm extends DBNFormBase {
    private TextFieldWithBrowseButton tnsNamesFileTextField;
    private JTextField filterTextField;
    private JBScrollPane tnsNamesScrollPanel;
    private JPanel mainPanel;
    private JLabel errorLabel;

    private final TnsNamesTable tnsNamesTable;

    @Getter
    private TnsNames tnsNames;

    TnsNamesImportForm(@NotNull TnsNamesImportDialog parent, @Nullable File file) {
        super(parent);
        tnsNamesTable = new TnsNamesTable(this, new TnsNames());
        tnsNamesScrollPanel.setViewportView(tnsNamesTable);
        errorLabel.setIcon(Icons.COMMON_ERROR);
        errorLabel.setVisible(false);

        if (file != null) {
            tnsNamesFileTextField.setText(file.getPath());
            updateTnsNamesTable();
        }
        updateSelections();

        tnsNamesTable.getSelectionModel().addListSelectionListener(e -> updateSelections());

        tnsNamesFileTextField.addBrowseFolderListener(
                null,
                null,
                getProject(),
                TnsNamesParser.FILE_CHOOSER_DESCRIPTOR);

        onTextChange(tnsNamesFileTextField, e -> updateTnsNamesTable());
        onTextChange(filterTextField, e -> filterTnsNamesTable());
    }

    private void filterTnsNamesTable() {
        TnsNamesTableModel model = tnsNamesTable.getModel();
        model.filter(filterTextField.getText());
    }

    private void updateSelections() {
        int rowCount = tnsNamesTable.getRowCount();
        int selectedRowCount = tnsNamesTable.getSelectedRowCount();

        TnsNamesImportDialog parentComponent = ensureParentComponent();
        parentComponent.getImportSelectedAction().setEnabled(selectedRowCount > 0);
        parentComponent.getImportAllAction().setEnabled(rowCount > 0);

        List<TnsProfile> profiles = tnsNamesTable.getModel().getProfiles();
        for (int i = 0; i < rowCount; i++) {
            boolean selected = tnsNamesTable.isRowSelected(i);
            TnsProfile profile = profiles.get(tnsNamesTable.convertRowIndexToModel(i));
            profile.setSelected(selected);
        }
    }

    private void updateTnsNamesTable() {
        try {
            String fileName = tnsNamesFileTextField.getTextField().getText();
            if (Strings.isNotEmpty(fileName)) {
                tnsNames = TnsNamesParser.get(new File(fileName));
                tnsNamesTable.setModel(new TnsNamesTableModel(tnsNames));
                tnsNamesTable.accommodateColumnsSize();
                filterTextField.setText(tnsNames.getFilter().getText());
            }
            errorLabel.setVisible(false);
        } catch (Exception e) {
            conditionallyLog(e);
            tnsNamesTable.setModel(new TnsNamesTableModel(new TnsNames()));
            tnsNamesTable.accommodateColumnsSize();

            errorLabel.setVisible(true);
            String message = e.getMessage();
            message = Strings.isEmpty(message) ? "File may be corrupt or not a valid tnsnames.ora file." : message;
            errorLabel.setText("Error reading file: " + message);
        }
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }


}
