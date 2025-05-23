package com.dbn.event.listener.action;

import com.dbn.common.icon.Icons;
import com.dbn.editor.session.action.AbstractSessionBrowserAction;
import com.dbn.event.listener.ui.EventListenersForm;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;

import static com.dbn.event.listener.action.EventListenerActionUtil.getListenersForm;
import static com.dbn.nls.NlsResources.txt;

public class EventListenerDeleteAction extends AbstractSessionBrowserAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        EventListenersForm registrationsForm = getListenersForm(e);


        if (registrationsForm == null) return;

        registrationsForm.deleteSelectedRegistrations();
    }

    @Override
    public void update(AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        EventListenersForm registrationsForm = getListenersForm(e);
        boolean rowSelected = registrationsForm.getListenersTable().getSelectedRows().length > 0;
        presentation.setEnabled(registrationsForm != null && !registrationsForm.isLoading() && rowSelected);
        presentation.setText(txt("app.eventRegistration.action.Delete"));
        presentation.setIcon(Icons.ACTION_DELETE);
    }
}
