/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * This software is dual-licensed to you under the Universal Permissive License
 * (UPL) 1.0 as shown at https://oss.oracle.com/licenses/upl or Apache License
 * 2.0 as shown at http://www.apache.org/licenses/LICENSE-2.0. You may choose
 * either license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.dbn.assistant.credential.local.ui;

import com.dbn.assistant.credential.local.LocalCredentialBundle;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.table.DBNEditableTable;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

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

    JPasswordField pwf = new JPasswordField();
    DefaultCellEditor editor = new DefaultCellEditor(pwf);
    getColumnModel().getColumn(LocalCredentialsTableCellRenderer.SECRET_COLUMN).setCellEditor(editor);

    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        adjustColumnSizes();
      }
    });
  }

  @NotNull
  private static LocalCredentialsTableModel createModel(LocalCredentialBundle credentials) {
    return new LocalCredentialsTableModel(credentials);
  }

  private void adjustColumnSizes() {
    int tableWidth = getWidth();

    // Ensure that the table has columns before adjusting sizes
    if (getColumnModel().getColumnCount() > 2) {
      TableColumn column1 = getColumnModel().getColumn(0);
      TableColumn column2 = getColumnModel().getColumn(1);
      TableColumn column3 = getColumnModel().getColumn(2);

      // Set first two columns to 1/5 of the table each, and third column to 2/5 of the table
      int column1and2Width = tableWidth / 5;
      int column3Width = 2 * column1and2Width;

      column1.setPreferredWidth(column1and2Width);
      column2.setPreferredWidth(column1and2Width);
      column3.setPreferredWidth(column3Width);
    }
  }

  void setCredentials(LocalCredentialBundle credentials) {
    super.setModel(createModel(credentials));
    adjustColumnSizes();
  }

  @Override
  public boolean isCellEditable(int row, int column) {
    return column < 4;
  }
}
