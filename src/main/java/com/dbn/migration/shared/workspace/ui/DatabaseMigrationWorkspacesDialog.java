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

package com.dbn.migration.shared.workspace.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.migration.shared.engine.DatabaseMigrationEngineType;
import com.dbn.migration.shared.workspace.DatabaseMigrationWorkspace;
import com.dbn.migration.shared.workspace.DatabaseMigrationWorkspaceBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;
import javax.swing.JComponent;
import java.util.function.Supplier;

import static com.dbn.nls.NlsResources.txt;

/**
 * Project-level workspace overview for a database migration engine.
 *
 * <p>The dialog edits a cloned workspace bundle and delegates engine-specific
 * workspace details to the supplied form factory.</p>
 */
public class DatabaseMigrationWorkspacesDialog<
        W extends DatabaseMigrationWorkspace,
        B extends DatabaseMigrationWorkspaceBundle<W>>
        extends DBNDialog<DatabaseMigrationWorkspacesForm<W, B>> {
    private final B workspaces;
    private final DatabaseMigrationEngineType engineType;
    private final DatabaseMigrationWorkspaceFormFactory<W, B> workspaceFormFactory;

    public DatabaseMigrationWorkspacesDialog(
            @NotNull B workspaces,
            @NotNull DatabaseMigrationEngineType engineType,
            @NotNull Supplier<B> workspaceCloner,
            @NotNull DatabaseMigrationWorkspaceFormFactory<W, B> workspaceFormFactory) {
        super(workspaces.getProject(), engineType.getWorkspaceTitle(), true);
        this.workspaces = workspaceCloner.get();
        this.engineType = engineType;
        this.workspaceFormFactory = workspaceFormFactory;
        setDefaultSize(800, 600);
        init();
    }

    @NotNull
    public B getWorkspaces() {
        return workspaces;
    }

    @NotNull
    public DatabaseMigrationEngineType getEngineType() {
        return engineType;
    }

    @NotNull
    public DatabaseMigrationWorkspaceFormFactory<W, B> getWorkspaceFormFactory() {
        return workspaceFormFactory;
    }

    @NotNull
    @Override
    protected DatabaseMigrationWorkspacesForm<W, B> createForm() {
        return new DatabaseMigrationWorkspacesForm<>(this);
    }

    @Override
    @NotNull
    protected Action[] initializeActions() {
        renameAction(getOKAction(), txt("msg.shared.button.OK"));
        updateDialogButtons();
        return actions(
                getOKAction(),
                getCancelAction());
    }

    @Override
    protected void doOKAction() {
        applyFormChanges();
        super.doOKAction();
    }

    @Override
    public void validateInput(JComponent component) {
        super.validateInput(component);
        updateDialogButtons();
    }

    public void updateDialogButtons() {
        if (isDisposed()) return;

        boolean changed = getForm().isFormChanged();
        getOKAction().setEnabled(changed);
        setCancelButtonText(txt(changed ? "msg.shared.button.Cancel" : "msg.shared.button.Close"));
    }

    @Override
    public void doCancelAction() {
        getForm().cancelFormChanges();
        super.doCancelAction();
    }
}
