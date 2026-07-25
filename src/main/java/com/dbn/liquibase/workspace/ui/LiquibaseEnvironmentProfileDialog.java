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
import com.dbn.liquibase.workspace.LiquibaseEnvironmentProfile;
import com.dbn.liquibase.workspace.LiquibaseEnvironmentProfileBundle;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

import static com.dbn.nls.NlsResources.txt;

/** Dialog for editing one named Liquibase environment profile. */
public class LiquibaseEnvironmentProfileDialog extends DBNDialog<LiquibaseEnvironmentProfileForm> {
    private final LiquibaseEnvironmentProfileBundle bundle;
    private final LiquibaseEnvironmentProfile profile;
    private final boolean newProfile;

    public LiquibaseEnvironmentProfileDialog(
            @NotNull LiquibaseEnvironmentProfileBundle bundle,
            @NotNull LiquibaseEnvironmentProfile profile,
            boolean newProfile,
            @NotNull Project project) {
        super(project, txt("app.liquibase.title.EnvironmentProfiles"), true);
        this.bundle = bundle;
        this.profile = profile.clone();
        this.newProfile = newProfile;
        init();
    }

    @NotNull
    @Override
    protected LiquibaseEnvironmentProfileForm createForm() {
        return new LiquibaseEnvironmentProfileForm(this, profile);
    }

    @Override
    @NotNull
    protected Action[] initializeActions() {
        renameAction(getOKAction(), txt(newProfile ? "msg.shared.button.Create" : "msg.shared.button.Update"));
        return actions(getOKAction(), getCancelAction());
    }

    @Override
    protected void doOKAction() {
        getForm().applyFormChanges();
        bundle.replaceProfile(profile);
        super.doOKAction();
    }
}
