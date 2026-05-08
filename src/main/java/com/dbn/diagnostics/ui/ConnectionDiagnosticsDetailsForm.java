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

package com.dbn.diagnostics.ui;

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.misc.DBNScrollPane;
import com.dbn.common.ui.table.DBNMutableTableModel;
import com.dbn.common.ui.table.DBNTable;
import com.dbn.common.ui.util.TabbedPanes;
import com.dbn.connection.ConnectionHandler;
import com.dbn.diagnostics.ui.model.AbstractDiagnosticsTableModel;
import com.dbn.diagnostics.ui.model.ConnectivityDiagnosticsTableModel;
import com.dbn.diagnostics.ui.model.MetadataDiagnosticsTableModel2;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;

public class ConnectionDiagnosticsDetailsForm extends DBNFormBase {

    private JPanel mainPanel;
    private JPanel headerPanel;
    private JBTabbedPane diagnosticsTabs;

    public ConnectionDiagnosticsDetailsForm(@NotNull ConnectionDiagnosticsForm parent, ConnectionHandler connection) {
        super(parent);

        DBNHeaderForm headerForm = new DBNHeaderForm(this, connection);
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);
        //diagnosticsTabs.enableFocusInheritance();


        AbstractDiagnosticsTableModel metadataTableModel = new MetadataDiagnosticsTableModel2(connection);
        DBNTable<AbstractDiagnosticsTableModel> metadataTable = new DiagnosticsTable<>(this, metadataTableModel);
        metadataTable.getRowSorter().toggleSortOrder(0);
        addTab(metadataTable, "Metadata Interface");

        AbstractDiagnosticsTableModel connectivityTableModel = new ConnectivityDiagnosticsTableModel(connection);
        DBNTable<AbstractDiagnosticsTableModel> connectivityTable = new DiagnosticsTable<>(this, connectivityTableModel);
        connectivityTable.getRowSorter().toggleSortOrder(0);
        addTab(connectivityTable, "Database Connectivity");


        TabbedPanes.onSelectionChange(diagnosticsTabs, i -> {
            ConnectionDiagnosticsForm parentForm = ensureParentComponent();
            parentForm.setTabSelectionIndex(i);
        });
   }

    private void addTab(DBNTable component, String title) {
        JScrollPane scrollPane = new DBNScrollPane(component);
        diagnosticsTabs.addTab(title, scrollPane);
    }

    protected void selectTab(int tabIndex) {
        diagnosticsTabs.setSelectedIndex(tabIndex);
        JBScrollPane scrollPane = (JBScrollPane) diagnosticsTabs.getComponentAt(tabIndex);
        DBNTable table = (DBNTable) scrollPane.getViewport().getView();
        DBNMutableTableModel model = (DBNMutableTableModel) table.getModel();
        model.notifyRowChanges();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
