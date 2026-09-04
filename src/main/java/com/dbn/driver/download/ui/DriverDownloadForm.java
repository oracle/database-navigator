/*
 * Copyright 2025 Oracle and/or its affiliates
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
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.DatabaseType;
import com.dbn.driver.download.DownloadSession;
import com.dbn.driver.download.DriverDownloadManager;
import com.dbn.driver.download.metadata.DriverPackage;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.util.FileChoosers.addSingleFolderChooser;
import static com.dbn.nls.NlsResources.txt;

public class DriverDownloadForm extends DBNFormBase {
    private JPanel mainPanel;
    private JLabel libraryPackageLabel;
    JComboBox<DriverPackage> libraryPackageComboBox;
    private JLabel libraryPathLabel;
    TextFieldWithBrowseButton libraryPathTextField;
    JLabel errorHintLabel;
    private JButton infoButton;
    private final EmptyProgressIndicator progressIndicator = new EmptyProgressIndicator();
    private final DownloadSession downloadSession = new DownloadSession(progressIndicator).withDownloadSize(1);

    public DriverDownloadForm(@Nullable Disposable parent, DatabaseType databaseType, List<DriverPackage> driverPackages) {
        super(parent);

        populateDriverLibraryComboBox(driverPackages);
        infoButton.addActionListener(e -> handleInfoButtonClick());
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

        addSingleFolderChooser(
                getProject(),
                libraryPathTextField,
                txt("cfg.connection.title.SelectDriverLibrary"),
                txt("cfg.connection.text.LibraryDriverClasses"));

        libraryPathTextField.setText(getSelectedPackageLocation());
        libraryPackageComboBox.addActionListener(e -> prepareSelectedPackage());
        prepareSelectedPackage();
        preloadRemainingPackages();
    }

    private String getSelectedPackageLocation() {
        return DriverDownloadManager.getDriverPackageLocation(getSelectedPackageId());
    }

    private String getSelectedPackageId() {
        DriverPackage driverPackage = getSelection(libraryPackageComboBox);
        return driverPackage == null ? "" : driverPackage.getId();
    }

    private void handleInfoButtonClick() {
        DriverPackage driverPackage = getSelection(libraryPackageComboBox);
        if (driverPackage == null) return;
        if (!driverPackage.isDetailsAvailable()) return;

        Dialogs.show(() -> new DriverPackageInfoDialog(getProject(), "Driver Package Info", true, driverPackage));
    }

    private void prepareSelectedPackage() {
        DriverPackage driverPackage = getSelection(libraryPackageComboBox);
        libraryPathTextField.setText(getSelectedPackageLocation());
        if (driverPackage == null) {
            updatePackageActions(false);
            return;
        }

        if (driverPackage.isDetailsAvailable()) {
            updatePackageActions(true);
            return;
        }

        updatePackageActions(false);
        if (driverPackage.isDetailsResolving()) return;

        Background.run(() -> {
            resolvePackageDetails(driverPackage);
            Dispatch.run(mainPanel, () -> {
                if (getSelection(libraryPackageComboBox) == driverPackage) {
                    libraryPathTextField.setText(getSelectedPackageLocation());
                    updatePackageActions(driverPackage.isDetailsAvailable());
                }
            });
        });
    }

    private void preloadRemainingPackages() {
        List<DriverPackage> driverPackages = new ArrayList<>();
        for (int i = 0; i < libraryPackageComboBox.getItemCount(); i++) {
            driverPackages.add(libraryPackageComboBox.getItemAt(i));
        }

        Background.run(() -> {
            for (DriverPackage driverPackage : driverPackages) {
                if (driverPackage == null) continue;
                if (driverPackage.isDetailsAvailable()) continue;
                if (driverPackage.isDetailsResolving()) continue;

                resolvePackageDetails(driverPackage);
                Dispatch.run(mainPanel, () -> {
                    if (getSelection(libraryPackageComboBox) == driverPackage) {
                        libraryPathTextField.setText(getSelectedPackageLocation());
                        updatePackageActions(driverPackage.isDetailsAvailable());
                    }
                });
            }
        });
    }

    private void resolvePackageDetails(DriverPackage driverPackage) {
        DriverDownloadManager.getInstance().resolveDriverPackageDetails(driverPackage, downloadSession);
    }

    private void updatePackageActions(boolean enabled) {
        DriverDownloadDialog dialog = ensureParentDialog();
        dialog.setDownloadEnabled(enabled);
        infoButton.setEnabled(enabled);
    }

}
