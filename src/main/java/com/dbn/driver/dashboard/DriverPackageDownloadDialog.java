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

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.driver.download.metadata.DriverPackage;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import static com.dbn.common.util.Messages.showErrorDialog;
import static com.dbn.common.util.Strings.isEmptyOrSpaces;
import static com.dbn.nls.NlsResources.txt;

class DriverPackageDownloadDialog extends DBNDialog<DriverPackageDownloadForm> {
    private final DriverPackage driverPackage;
    private String downloadPath;

    DriverPackageDownloadDialog(Project project, DriverPackage driverPackage) {
        super(project, txt("msg.driver.title.DownloadDriverPackage"), true);
        this.driverPackage = driverPackage;
        setModal(true);
        init();
    }

    @NotNull
    @Override
    protected DriverPackageDownloadForm createForm() {
        return new DriverPackageDownloadForm(this, driverPackage);
    }

    @NotNull
    @Override
    protected Action[] initializeActions() {
        renameAction(getOKAction(), txt("msg.shared.button.Download"));
        return actions(getOKAction(), getCancelAction());
    }

    @Override
    protected void doOKAction() {
        if (!resolveDownloadPath()) return;
        super.doOKAction();
    }

    private boolean resolveDownloadPath() {
        String rootValue = getForm().getDownloadRoot().trim();
        if (isEmptyOrSpaces(rootValue)) {
            showErrorDialog(getProject(), txt("app.driver.error.DownloadLocationRequired"));
            return false;
        }

        String packageFolder = getForm().getPackageFolder().trim();
        if (isEmptyOrSpaces(packageFolder)) {
            showErrorDialog(getProject(), txt("app.driver.error.PackageFolderRequired"));
            return false;
        }

        if (packageFolder.contains("/") || packageFolder.contains("\\") || packageFolder.contains(":")) {
            showErrorDialog(getProject(), txt("app.driver.error.InvalidPackageFolder"));
            return false;
        }

        Path root;
        try {
            root = Path.of(rootValue).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            showErrorDialog(getProject(), txt("app.driver.error.InvalidDownloadRoot"));
            return false;
        }
        if (Files.exists(root) && !Files.isDirectory(root)) {
            showErrorDialog(getProject(), txt("app.driver.error.InvalidDownloadRoot"));
            return false;
        }

        try {
            Path folder = Path.of(packageFolder);
            if (folder.isAbsolute() || folder.getNameCount() != 1 ||
                    ".".equals(packageFolder) || "..".equals(packageFolder)) {
                showErrorDialog(getProject(), txt("app.driver.error.InvalidPackageFolder"));
                return false;
            }

            Path target = root.resolve(folder).normalize();
            if (target.equals(root) || !target.startsWith(root)) {
                showErrorDialog(getProject(), txt("app.driver.error.InvalidPackageFolder"));
                return false;
            }

            downloadPath = target.toString();
            return true;
        } catch (InvalidPathException e) {
            showErrorDialog(getProject(), txt("app.driver.error.InvalidPackageFolder"));
            return false;
        }
    }

    String getDownloadPath() {
        return downloadPath;
    }
}
