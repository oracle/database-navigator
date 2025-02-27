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

package com.dbn.connection.config.ui;

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.DatabaseType;
import com.dbn.driver.download.metadata.DriverPackage;
import com.dbn.driver.download.ui.DriverPackageInfoDialog;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.HyperlinkLabel;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.util.List;

import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.util.Files.getPluginDeploymentRoot;

public class DownloadManagerForm extends DBNFormBase {
    private JPanel mainPanel;
    private JLabel libraryPackageLabel;
    JComboBox<DriverPackage> libraryPackageComboBox;
    private JLabel libraryPathLabel;
    TextFieldWithBrowseButton libraryPathTextField;
    private HyperlinkLabel libraryInfoLink;
    JLabel errorHintLabel;
    private final DatabaseType databaseType;
    private static final FileChooserDescriptor LIBRARY_FILE_DESCRIPTOR = new FileChooserDescriptor(false, true, false, false, false, false);


    public DownloadManagerForm(@Nullable Disposable parent, DatabaseType databaseType, List<DriverPackage> driverPackages) {
        super(parent);
        this.databaseType = databaseType;

        populateDriverLibraryComboBox(driverPackages);
        libraryInfoLink.setHyperlinkText(txt("cfg.connection.link.LibraryInfo"));
        libraryInfoLink.addHyperlinkListener(e -> {
            handleInfoButtonClick();
        });
        errorHintLabel.setForeground(Color.RED);
        errorHintLabel.setVisible(false);
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }


    private void populateDriverLibraryComboBox(List<DriverPackage> driverPackages) {
        libraryPackageComboBox.removeAllItems(); // Clear the ComboBox first

        for (DriverPackage driverPackage : driverPackages) {
            libraryPackageComboBox.addItem(driverPackage);
        }
        libraryPathTextField.addBrowseFolderListener(
                txt("cfg.connection.title.SelectDriverLibrary"),
                txt("cfg.connection.text.LibraryDriverClasses"),
                null, LIBRARY_FILE_DESCRIPTOR);
        libraryPathTextField.setText(getPluginDeploymentRoot().getPath()+"/driver-packages");
    }

    private void handleInfoButtonClick() {
        Dialogs.show(() -> new DriverPackageInfoDialog(getProject(), "Driver Package Info", true, getSelection(libraryPackageComboBox)));
    }

}
