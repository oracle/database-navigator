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

package com.dbn.assistant.credential.remote.ui;

import com.dbn.assistant.credential.local.LocalCredential;
import com.dbn.assistant.credential.local.LocalCredentialBundle;
import com.dbn.assistant.credential.local.LocalCredentialSettings;
import com.dbn.assistant.settings.AssistantSettings;
import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.table.DBNReadonlyTableModel;
import com.dbn.common.ui.table.DBNTable;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.Mouse;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ColoredTableCellRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static com.dbn.common.util.Conditional.when;
import static java.awt.event.MouseEvent.BUTTON1;

/**
 * This class is for a window that lists the available ai provider keys,
 * and allows us to select one and hydrate the createCredentialWindow with it.
 */
public class CredentialPickerForm extends DBNFormBase {

  private JPanel mainPanel;
  private JScrollPane credentialsScrollPane;
  private DBNTable<?> credentialsTable;


  protected CredentialPickerForm(CredentialPickerDialog dialog) {
    super(dialog, dialog.getProject());
    initCredentialTable(getProject());
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }

  private CredentialPickerDialog getDialog() {
    return getParentComponent();
  }

  private void initCredentialTable(Project project) {
    LocalCredentialSettings settings = AssistantSettings.getInstance(project).getCredentialSettings();
    LocalCredentialBundle credentials = settings.getCredentials();

    CredentialPickerTableModel credentialTableModel = new CredentialPickerTableModel(credentials);
    credentialsTable = new DBNTable<>(this, credentialTableModel, true);
    credentialsTable.setCellSelectionEnabled(false);
    credentialsTable.setRowSelectionAllowed(true);
    credentialsTable.setRowHeight(24);

    credentialsTable.setDefaultRenderer(LocalCredential.class, createCellRenderer());
    credentialsScrollPane.setViewportView(credentialsTable);

    credentialsTable.getSelectionModel().addListSelectionListener(e -> selectionChanged());
    credentialsTable.addMouseListener(Mouse.listener().onClick(e -> when(
            e.getButton() == BUTTON1 &&
                    e.getClickCount() == 2 &&
                    getSelectedCredential() != null,
            () -> getDialog().doOKAction())));
  }

  private void selectionChanged() {
    getDialog().selectionChanged();
  }

  private static @NotNull ColoredTableCellRenderer createCellRenderer() {
    return new ColoredTableCellRenderer() {
      @Override
      protected void customizeCellRenderer(@NotNull JTable table, @Nullable Object value, boolean selected, boolean hasFocus, int row, int column) {
        LocalCredential credential = (LocalCredential) value;
        if (credential == null) return;
        switch (column) {
          case 0: append(credential.getName()); break;
          case 1: append(credential.getUser()); break;
          case 2: append("************"); break;
        }
        setBorder(Borders.EMPTY_BORDER);
      }
    };
  }

  private static class CredentialPickerTableModel extends StatefulDisposableBase implements DBNReadonlyTableModel<LocalCredential> {
    private final String NAME = "Credential Name";
    private final String USERNAME = "User Name";
    private final String KEY = "Secret";
    private final LocalCredentialBundle credentials;
    private final String[] columnNames = {NAME, USERNAME, KEY};

    public CredentialPickerTableModel(LocalCredentialBundle credentials) {
      this.credentials = credentials;
    }

    @Override
    public int getRowCount() {
      return credentials.size();
    }

    @Override
    public int getColumnCount() {
      return columnNames.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      return credentials.get(rowIndex);
    }

    @Override
    public String getColumnName(int column) {
      return columnNames[column];
    }

    @Override
    public Class<?> getColumnClass(int i) {
      return LocalCredential.class;
    }

    @Override
    public void disposeInner() {
    }
  }

  @Nullable
  public LocalCredential getSelectedCredential() {
    int selectedRow = credentialsTable.getSelectedRow();
    if (selectedRow == -1) return null;
    return (LocalCredential) credentialsTable.getModel().getValueAt(selectedRow, 0);
  }

}
