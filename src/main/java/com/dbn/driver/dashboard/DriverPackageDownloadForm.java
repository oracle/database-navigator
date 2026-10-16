/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.driver.dashboard;

import com.dbn.common.color.Colors;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.info.DBNCommentLabel;
import com.dbn.common.ui.util.Tooltips;
import com.dbn.driver.download.DriverDownloadManager;
import com.dbn.driver.download.metadata.DriverPackage;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import static com.dbn.common.ui.form.DBNFormState.initPersistence;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.util.FileChoosers.addSingleFolderChooser;
import static com.dbn.nls.NlsResources.txt;

class DriverPackageDownloadForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private TextFieldWithBrowseButton downloadRootField;
    private JTextField packageFolderField;
    private DBNCommentLabel downloadPathLabel;

    DriverPackageDownloadForm(DriverPackageDownloadDialog parent, DriverPackage driverPackage) {
        super(parent);
        packageFolderField.setText(driverPackage.getId());

        String packageName = driverPackage.getName() == null ? driverPackage.getId() : driverPackage.getName();
        DBNHeaderForm headerForm = new DBNHeaderForm(this, packageName, null, Colors.lafDarker(Colors.getPanelBackground(), 5));
        headerPanel.add(headerForm.getComponent());

        addSingleFolderChooser(
                getProject(),
                downloadRootField,
                txt("msg.driver.title.DownloadDriverPackage"),
                txt("app.driver.text.DownloadLocationHint"));
        Tooltips.setToolTipText(downloadRootField, txt("app.driver.text.DownloadLocationHint"));

        onTextChange(downloadRootField, event -> updateDownloadPath());
        onTextChange(packageFolderField, event -> updateDownloadPath());
        updateDownloadPath();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Override
    protected void initStatePersistence() {
        DriverDownloadManager manager = DriverDownloadManager.getInstance();
        initPersistence(
                downloadRootField,
                manager.getUserInterfaceState("DOWNLOAD_FORM"),
                "download-root",
                DriverDownloadManager.getDriverPackagesLocation());
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return packageFolderField;
    }

    String getDownloadRoot() {
        return downloadRootField.getText();
    }

    String getPackageFolder() {
        return packageFolderField.getText();
    }

    private void updateDownloadPath() {
        String root = downloadRootField.getText().trim();
        String folder = packageFolderField.getText().trim();
        if (root.isEmpty() ||
                folder.isEmpty()) {
            downloadPathLabel.setText("");
            return;
        }

        try {
            Path downloadRoot = Path.of(root).toAbsolutePath().normalize();
            if (Files.exists(downloadRoot) && !Files.isDirectory(downloadRoot)) {
                downloadPathLabel.setText("");
                return;
            }

            Path packageFolder = Path.of(folder);
            if (folder.contains("/") ||
                    folder.contains("\\") ||
                    folder.contains(":") ||
                    packageFolder.isAbsolute() ||
                    packageFolder.getNameCount() != 1 ||
                    ".".equals(folder) ||
                    "..".equals(folder)) {
                downloadPathLabel.setText("");
                return;
            }

            Path downloadPath = downloadRoot.resolve(packageFolder).normalize();
            if (downloadPath.equals(downloadRoot) ||
                    !downloadPath.startsWith(downloadRoot)) {
                downloadPathLabel.setText("");
                return;
            }

            downloadPathLabel.setText(downloadPath.toString());
        } catch (InvalidPathException e) {
            downloadPathLabel.setText("");
        }
    }
}
