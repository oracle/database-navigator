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

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.driver.download.metadata.DriverPackage;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;

public class DriverPackageInfoDialog extends DBNDialog<DriverPackageInfoForm> {
    DriverPackage driverPackage;
    public DriverPackageInfoDialog(Project project, String title, boolean canBeParent, DriverPackage driverPackage) {
        super(project, title, canBeParent);
        this.driverPackage = driverPackage;
        setResizable(false);
        setModal(true);
        init();
    }

    @Override
    protected @NotNull DriverPackageInfoForm createForm() {
            return new DriverPackageInfoForm(this, driverPackage);
    }

    @NotNull
    @Override
    protected Action [] createActions() {
        return new Action[]{
                new AbstractAction("Close") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        close(OK_EXIT_CODE);
                    }
                }
        };
    }
}
