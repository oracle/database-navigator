package com.dbn.event.registration.action;

import com.dbn.common.action.BasicAction;
import com.dbn.common.icon.Icons;
import com.dbn.event.registration.ui.EventRegistrationsForm;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;

import static com.dbn.event.registration.action.EventRegistrationActionUtil.getRegistrationsForm;
import static com.dbn.nls.NlsResources.txt;

public class EventRegistrationDeleteAction extends BasicAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        EventRegistrationsForm registrationsForm = getRegistrationsForm(e);
        if (registrationsForm == null) return;

        registrationsForm.deleteSelectedRegistrations();
    }

    @Override
    public void update(AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        presentation.setEnabled(isEnabled(e));
        presentation.setText(txt("app.eventRegistration.action.Delete"));
        presentation.setIcon(Icons.ACTION_DELETE);
    }

    private boolean isEnabled(AnActionEvent e) {
        EventRegistrationsForm registrationsForm = getRegistrationsForm(e);
        if (registrationsForm == null) return false;
        if (registrationsForm.isLoading()) return false;

        return registrationsForm.getRegistrationsTable().getSelectedRows().length > 0;
    }
}
