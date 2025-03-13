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

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.DatabaseType;
import com.dbn.driver.download.DriverDownloadManager;
import com.dbn.driver.download.DriverPackageDownloader;
import com.dbn.driver.download.metadata.DriverPackage;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;
import java.util.List;

import static com.dbn.common.ui.util.ComboBoxes.getSelection;

public class DownloadManagerDialog extends DBNDialog<DownloadManagerForm> {
    private final DatabaseType databaseType;
    private final List<DriverPackage> driverPackages;

    public DownloadManagerDialog(Project project, DatabaseType databaseType, List<DriverPackage> driverPackages) {
        super(project, "Libraries Download Manager", true);
        this.databaseType = databaseType;
        this.driverPackages = driverPackages;
        renameAction(getOKAction(), "Download");
        setModal(true);
        setResizable(true);
        init();
    }

    @NotNull
    @Override
    protected DownloadManagerForm createForm() {
        return new DownloadManagerForm(this, databaseType, driverPackages);
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        return new Action[]{
                getOKAction(),
                getCancelAction()
        };
    }

    @Override
    public void doOKAction() {
        handleDownloadButtonClick();
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
    }

    private void handleDownloadButtonClick() {
        DownloadManagerForm form = getForm();
        DriverPackage driverPackage = getSelection(form.libraryPackageComboBox);
        if (driverPackage == null) return;

        String downloadPath = getSelectedDownloadPath();

        DriverDownloadManager downloadManager = DriverDownloadManager.getInstance();
        downloadManager.setPackageDownloadPath(driverPackage.getId(), downloadPath);

        DriverPackageDownloader downloader = new DriverPackageDownloader();
        downloader.downloadDriverPackage(getProject(), driverPackage, (String errorMessage) -> {
            if(errorMessage.isBlank()) this.close(0);
            else {
                form.errorHintLabel.setText(errorMessage);
                form.errorHintLabel.setVisible(true);
            }
        });
    }

    public String getSelectedDownloadPath() {
        return getForm().libraryPathTextField.getText();
    }
}
