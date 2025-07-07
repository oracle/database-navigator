package com.dbn.event.registration.action;

import com.dbn.common.icon.Icons;
import com.dbn.editor.session.action.AbstractSessionBrowserAction;
import com.dbn.event.registration.ui.EventRegistrationsForm;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;

import static com.dbn.event.registration.action.EventRegistrationActionUtil.getListenersForm;
import static com.dbn.nls.NlsResources.txt;

public class EventRegistrationDeleteAction extends AbstractSessionBrowserAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        EventRegistrationsForm registrationsForm = getListenersForm(e);


        if (registrationsForm == null) return;

        registrationsForm.deleteSelectedRegistrations();
    }

    @Override
    public void update(AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        EventRegistrationsForm registrationsForm = getListenersForm(e);
        boolean rowSelected = registrationsForm.getListenersTable().getSelectedRows().length > 0;
        presentation.setEnabled(registrationsForm != null && !registrationsForm.isLoading() && rowSelected);
        presentation.setText(txt("app.eventRegistration.action.Delete"));
        presentation.setIcon(Icons.ACTION_DELETE);
    }
}
