/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * This software is dual-licensed to you under the Universal Permissive License
 *  (UPL) 1.0 as shown at https://oss.oracle.com/licenses/upl or Apache License
 *   2.0 as shown at http://www.apache.org/licenses/LICENSE-2.0. You may choose
 *   either license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.dbn.driver.packages.ui;

import com.dbn.common.color.Colors;
import com.dbn.common.ui.form.DBNCollapsibleForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.Cursors;
import com.dbn.common.util.Strings;
import com.dbn.driver.packages.Developer;
import com.dbn.driver.packages.Library;
import com.dbn.driver.packages.License;
import com.intellij.ide.BrowserUtil;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LibraryInfoForm extends DBNFormBase implements DBNCollapsibleForm {
    private Library library;
    private JPanel libraryInfoPanel;
    public LibraryInfoForm(Library library){
        super(null);
        this.library = library;
    }
    private JPanel setupDynamicFields() {
        List<Developer> devs = library.getDevelopers();
        List<License> licenses = library.getLicenses();
        int rowCount = devs.size() + licenses.size() + 1;
        libraryInfoPanel = new JPanel();
        libraryInfoPanel.setLayout(new GridLayoutManager(rowCount, 2, new Insets(0, 0, 0, 0), -1, -1));
//        libraryInfoPanel.setBorder(BorderFactory.createTitledBorder(new EmptyBorder(10, 10, 10, 10), library.getArtifactId()+"-"+library.getVersion()));
        addLabel("Name:", 0, 0);
        addTextField(0, 1, "-", null);
        if(!devs.isEmpty()) {
            addLabel("Developers:", 1, 0);
            for (int i = 0; i < devs.size(); i++) {
                addTextField(i + 1, 1, devs.get(i).getName(), devs.get(i).getUrl());
            }
        }

        if(!licenses.isEmpty()) {
            addLabel("Licenses:", devs.size() + 1, 0);
            for (int i = 0; i < licenses.size(); i++) {
                addTextField(i + 1 + devs.size(), 1, licenses.get(i).getName(), licenses.get(i).getUrl());
            }
        }
        return libraryInfoPanel;

    }

    private void addLabel(String labelText, int row, int col) {
        JLabel label = new JLabel(labelText);
        libraryInfoPanel.add(label, new GridConstraints(
                row, col, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED,
                null, null, null, 0, false));
    }

    private void addTextField(int row, int col, String text, String url) {
        JTextField textField = new JTextField(15); // Adjust preferred size as needed
        textField.setText(text);
        textField.setBorder(Borders.EMPTY_BORDER);
        textField.setBackground(Colors.getPanelBackground());
        textField.setEditable(false);

        if (url == null) {
            textField.setCursor(Cursors.textCursor());
        } else {
            textField.setFont(textField.getFont().deriveFont(Font.PLAIN));
            textField.setCursor(Cursors.handCursor());

            // Apply underline to the font for text fields with URLs
            Font originalFont = textField.getFont();
            Map<TextAttribute, Object> attributes = new HashMap<>(originalFont.getAttributes());
            attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_LOW_ONE_PIXEL);
            textField.setFont(originalFont.deriveFont(attributes));

            // Add mouse listener for hover effect
            textField.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    BrowserUtil.browse(url);
                }
            });
        }

        if (Strings.isNotEmpty(text)) {
            FontMetrics fontMetrics = textField.getFontMetrics(textField.getFont());
            int width = fontMetrics.charsWidth(text.toCharArray(), 0, text.length()) + 40;
            textField.setMinimumSize(new Dimension(Math.min(width, 600), -1));
        }

        // Add JTextField in the specified row and column
        libraryInfoPanel.add(textField, new GridConstraints(
                row, col, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        // Refresh the panel to display the new text field
        libraryInfoPanel.revalidate();
        libraryInfoPanel.repaint();
    }
    @Override
    protected JPanel getMainComponent() {
        return setupDynamicFields();
    }

    @Override
    public String getCollapsedTitle() {
        return library.getArtifactId()+"-"+library.getVersion();
    }

    @Override
    public String getCollapsedTitleDetail() {
        return "";
    }

    @Override
    public String getExpandedTitle() {
        return library.getArtifactId()+"-"+library.getVersion();
    }
}
