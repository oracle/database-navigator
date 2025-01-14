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

package com.dbn.assistant.credential.local.ui;

import com.dbn.assistant.credential.local.LocalCredentialBundle;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.table.DBNEditableTable;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.DefaultCellEditor;
import javax.swing.JPasswordField;
import javax.swing.ListSelectionModel;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.common.ui.util.Borders.TEXT_FIELD_INSETS;

/**
 * Table model for provider credentials information
 * The template credential stored locally.
 */
public class LocalCredentialsEditorTable extends DBNEditableTable<LocalCredentialsTableModel> {

  LocalCredentialsEditorTable(DBNComponent parent, LocalCredentialBundle credentials) {
    super(parent, createModel(credentials), true);
    setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
    setSelectionBackground(UIUtil.getTableBackground());
    setSelectionForeground(UIUtil.getTableForeground());
    setCellSelectionEnabled(true);
    setDefaultRenderer(String.class, new LocalCredentialsTableCellRenderer());

    JPasswordField passwordField = new JPasswordField();
    passwordField.setBorder(TEXT_FIELD_INSETS);
    DefaultCellEditor editor = new DefaultCellEditor(passwordField);
    getColumnModel().getColumn(LocalCredentialsTableCellRenderer.SECRET_COLUMN).setCellEditor(editor);

    setAccessibleName(this, "Credentials");
    setProportionalColumnWidths(20, 30, 50);
  }

  @NotNull
  private static LocalCredentialsTableModel createModel(LocalCredentialBundle credentials) {
    return new LocalCredentialsTableModel(credentials);
  }

  void setCredentials(LocalCredentialBundle credentials) {
    super.setModel(createModel(credentials));
  }

  @Override
  public boolean isCellEditable(int row, int column) {
    return column < 4;
  }
}
