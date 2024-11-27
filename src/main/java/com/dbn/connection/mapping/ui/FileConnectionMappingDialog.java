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

package com.dbn.connection.mapping.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

public class FileConnectionMappingDialog extends DBNDialog<FileConnectionMappingForm> {

    public FileConnectionMappingDialog(Project project) {
        super(project, "File connection mappings", true);
        setModal(false);
        setResizable(true);
        setDefaultSize(1200, 700);
        renameAction(getCancelAction(), "Close");
        init();
    }

    @NotNull
    @Override
    protected FileConnectionMappingForm createForm() {
        return new FileConnectionMappingForm(this);
    }

    @Override
    @NotNull
    protected final Action[] createActions() {
        return new Action[]{
                getCancelAction(),
                getHelpAction()
        };
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
    }
}
