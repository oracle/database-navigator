package com.dbn.liquibase.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.liquibase.model.LiquibaseWorkspaceBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;
import javax.swing.JComponent;

import static com.dbn.nls.NlsResources.txt;

/** Project-level Liquibase workspace overview for its named workspaces. */
public class LiquibaseWorkspaceBundleSettingsDialog extends DBNDialog<LiquibaseWorkspaceBundleSettingsForm> {
    private final LiquibaseWorkspaceBundle workspaces;

    public LiquibaseWorkspaceBundleSettingsDialog(LiquibaseWorkspaceBundle workspaces) {
        super(workspaces.getProject(), txt("msg.liquibase.title.WorkspaceSettings"), true);
        this.workspaces = workspaces.clone();
        init();
    }

    @NotNull
    public LiquibaseWorkspaceBundle getWorkspaces() {
        return workspaces;
    }

    @NotNull
    @Override
    protected LiquibaseWorkspaceBundleSettingsForm createForm() {
        return new LiquibaseWorkspaceBundleSettingsForm(this);
    }

    @Override
    @NotNull
    protected Action[] initializeActions() {
        renameAction(getOKAction(), txt("msg.liquibase.button.Update"));
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
