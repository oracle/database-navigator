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

package com.dbn.assistant.profile.ui;

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.object.DBAIProfile;
import com.intellij.ui.ColoredTableCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.DefaultListSelectionModel;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.Font;

public class ProfileDetailsForm extends DBNFormBase {
    private JPanel mainPanel;
    private JTextField providerTextField;
    private JTextField modelTextField;
    private JTextField credentialTextField;
    private JTable objectsTable;
    private JTextField profileNameTextField;
    private JCheckBox enabledCheckBox;

    public ProfileDetailsForm(@NotNull ProfileManagementForm parent, DBAIProfile profile) {
        super(parent);

        initializeFields(profile);
        initializeTable(profile);
    }

    private void initializeFields(DBAIProfile profile) {
        enabledCheckBox.setSelected(profile.isEnabled());
        profileNameTextField.setText(profile.getName());
        modelTextField.setText(profile.getModel().getId());
        credentialTextField.setText(profile.getCredentialName());
        providerTextField.setText(profile.getProvider().getName());
    }

    private void initializeTable(DBAIProfile profile) {
        objectsTable.setSelectionModel(new NullSelectionModel());
        objectsTable.setDefaultRenderer(Object.class, createObjectTableRenderer());
        String[] columnNames = {
                txt("profile.mgmt.obj_table.header.name"),
                txt("profile.mgmt.obj_table.header.owner")};

        Object[][] data = profile.getObjects().stream()
                .map(obj -> new Object[]{obj.getName(), obj.getSchemaName()})
                .toArray(Object[][]::new);
        DefaultTableModel tableModel = new DefaultTableModel(data, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return (column == 1 && row == 0);
            }
        };
        objectsTable.setModel(tableModel);
    }

    private @NotNull TableCellRenderer createObjectTableRenderer() {
        return new ColoredTableCellRenderer() {

            @Override
            protected void customizeCellRenderer(@NotNull JTable table, @Nullable Object value, boolean selected, boolean hasFocus, int row, int column) {
                if (value != null) {
                    append(value.toString(), table.isEnabled() ?
                            SimpleTextAttributes.REGULAR_ATTRIBUTES :
                            SimpleTextAttributes.GRAY_ATTRIBUTES);
                    setFont(getFont().deriveFont(Font.PLAIN));
                } else {
                    append("<all>", table.isEnabled() ?
                            SimpleTextAttributes.REGULAR_ITALIC_ATTRIBUTES :
                            SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES);
                }

            }
        };
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    private static class NullSelectionModel extends DefaultListSelectionModel {
        @Override
        public void setSelectionInterval(int index0, int index1) {
            super.setSelectionInterval(-1, -1);
        }
    }
}
