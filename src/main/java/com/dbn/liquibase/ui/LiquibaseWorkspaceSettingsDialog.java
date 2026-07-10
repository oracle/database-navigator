package com.dbn.liquibase.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.liquibase.model.LiquibaseWorkspace;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

import static com.dbn.nls.NlsResources.txt;

/** Project-level Liquibase workspace overview for all database connections. */
public class LiquibaseWorkspaceSettingsDialog extends DBNDialog<LiquibaseWorkspaceSettingsForm> {
    private final LiquibaseWorkspace workspace;

    public LiquibaseWorkspaceSettingsDialog(LiquibaseWorkspace workspace) {
        super(workspace.getProject(), txt("msg.liquibase.title.WorkspaceSettings"), true);
        this.workspace = workspace;
        init();
    }

    @NotNull
    public LiquibaseWorkspace getWorkspace() {
        return workspace;
    }

    @NotNull
    @Override
    protected LiquibaseWorkspaceSettingsForm createForm() {
        return new LiquibaseWorkspaceSettingsForm(this);
    }

    @Override
    @NotNull
    protected Action[] initializeActions() {
        renameAction(getOKAction(), txt("msg.liquibase.button.Update"));
        return actions(getOKAction(), getCancelAction());
    }

    @Override
    protected void doOKAction() {
        getForm().applyFormChanges();
        super.doOKAction();
    }

    @Override
    public void doCancelAction() {
        getForm().cancelFormChanges();
        close(0);
    }
}
