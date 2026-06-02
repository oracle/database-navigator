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

package com.dbn.driver.download.ui;

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.driver.download.metadata.Library;
import com.dbn.driver.download.metadata.LibraryDeveloper;
import com.dbn.driver.download.metadata.LibraryLicense;
import com.intellij.ide.BrowserUtil;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Cursor;
import java.awt.Insets;
import java.util.List;

public class LibraryInfoForm extends DBNFormBase {
    private final Library library;
    private JPanel libraryInfoPanel;
    public LibraryInfoForm(Library library){
        super(null);
        this.library = library;
    }
    private JPanel setupDynamicFields() {
        List<LibraryDeveloper> devs = library.getDevelopers();
        List<LibraryLicense> licenses = library.getLicenses();
        int rowCount = devs.size() + licenses.size() + 1;

        libraryInfoPanel.setLayout(new GridLayoutManager(rowCount, 2, new Insets(0, 0, 0, 0), -1, -1));

        libraryInfoPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEmptyBorder(2, 2, 10, 2),
                library.getArtifactId() + " - " + library.getVersion()
        ));
        if(!devs.isEmpty() && devs.get(0).getName() != null) {
            addLabel("Developers:", 1, 0);
            for (int i = 0; i < devs.size(); i++) {
                addField(i + 1, 1, devs.get(i).getName(), devs.get(i).getUrl());
            }
        }

        if(!licenses.isEmpty() && licenses.get(0).getName() != null) {
            addLabel("Licenses:", devs.size() + 1, 0);
            for (int i = 0; i < licenses.size(); i++) {
                addField(i + 1 + devs.size(), 1, licenses.get(i).getName(), licenses.get(i).getUrl());
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

    private void addField(int row, int col, String text, String url) {
        if (url == null || url.isEmpty()) {
            JLabel label = new JLabel(text);
            libraryInfoPanel.add(label, new GridConstraints(
                    row, col, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                    GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        } else {
            HyperlinkLabel hyperlink = new HyperlinkLabel(text);
            hyperlink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            hyperlink.addHyperlinkListener(e -> BrowserUtil.browse(url));

            libraryInfoPanel.add(hyperlink, new GridConstraints(
                    row, col, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                    GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        }

        libraryInfoPanel.revalidate();
        libraryInfoPanel.repaint();
    }
    @Override
    protected JPanel getMainComponent() {
        return setupDynamicFields();
    }
}
