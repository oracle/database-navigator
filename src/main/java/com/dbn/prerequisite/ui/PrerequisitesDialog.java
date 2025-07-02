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

package com.dbn.prerequisite.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.prerequisite.event.PrerequisiteEvent;
import com.dbn.prerequisite.event.PrerequisiteEventListener;
import com.dbn.prerequisite.model.PrerequisiteBundle;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

@Getter
public class PrerequisitesDialog extends DBNDialog<PrerequisitesForm> implements PrerequisiteEventListener {
    private final PrerequisiteBundle prerequisites;

    public PrerequisitesDialog(PrerequisiteBundle prerequisites) {
        super(prerequisites.getProject(), "Prerequisite Verification", false);
        this.setModal(false);
        this.prerequisites = prerequisites;

        setDefaultSize(600, 600);
        renameAction(getCancelAction(), "Close");

        init();
    }

    @NotNull
    @Override
    protected PrerequisitesForm createForm() {
        return new PrerequisitesForm(this);
    }

    @Override
    protected final Action[] createActions() {
        return createActions(
                getCancelAction());
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
    }


    @Override
    public void eventOccurred(PrerequisiteEvent event) {
    }
}