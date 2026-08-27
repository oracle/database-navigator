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

package com.dbn.liquibase.workspace.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.liquibase.workspace.LiquibaseEnvironmentProfileBundle;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

import static com.dbn.nls.NlsResources.txt;

/** Overview dialog for the named environment profiles of a DBN project. */
public class LiquibaseEnvironmentProfilesDialog extends DBNDialog<LiquibaseEnvironmentProfilesForm> {
    private final LiquibaseEnvironmentProfileBundle originalBundle;
    private final LiquibaseEnvironmentProfileBundle bundle;

    public LiquibaseEnvironmentProfilesDialog(
            @NotNull Project project,
            @NotNull LiquibaseEnvironmentProfileBundle bundle) {
        super(project, txt("app.liquibase.title.EnvironmentProfiles"), true);
        this.originalBundle = bundle;
        this.bundle = bundle.clone();
        setDefaultSize(760, 520);
        init();
    }

    @NotNull
    public LiquibaseEnvironmentProfileBundle getBundle() {
        return bundle;
    }

    @NotNull
    @Override
    protected LiquibaseEnvironmentProfilesForm createForm() {
        return new LiquibaseEnvironmentProfilesForm(this);
    }

    @Override
    @NotNull
    protected Action[] initializeActions() {
        renameAction(getOKAction(), txt("msg.shared.button.OK"));
        return actions(getOKAction(), getCancelAction());
    }

    @Override
    protected void doOKAction() {
        getForm().applyFormChanges();
        originalBundle.replaceProfiles(bundle);
        super.doOKAction();
    }
}
