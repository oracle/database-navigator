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

package com.dbn.plugin.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.plugin.PluginConflictManager;
import com.dbn.plugin.PluginConflictResolution;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

public class PluginConflictResolutionDialog extends DBNDialog<PluginConflictResolutionForm> {
    public PluginConflictResolutionDialog() {
        super(null, "Plugin Conflict Resolution", true);
        setModal(true);
        setResizable(false);
        //setDefaultSize(700, 400);
        getCancelAction().setEnabled(false);
        renameAction(getOKAction(), "Continue");
        init();
    }


    @Override
    protected String getDimensionServiceKey() {
        return null;
    }

    @NotNull
    @Override
    protected PluginConflictResolutionForm createForm() {
        return new PluginConflictResolutionForm(this);
    }

    protected void renameAction(String name) {
        renameAction(getOKAction(), name);
    }

    @Override
    @NotNull
    protected final Action[] createActions() {
        return new Action[]{
                getOKAction()
        };
    }

    @Override
    protected void doOKAction() {
        PluginConflictResolutionForm resolutionForm = getForm();
        PluginConflictResolution resolution = resolutionForm.getChosenResolution();
        if (resolution == null) {
            resolutionForm.showErrorMessage();
            return;
        }

        PluginConflictManager conflictManager = PluginConflictManager.getInstance();
        conflictManager.applyConflictResolution(resolution);
        super.doOKAction();
    }

    @Override
    public void doCancelAction() {
        // do not allow closing the dialog from X
    }
}
