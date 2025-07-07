package com.dbn.event.registration.action;

import com.dbn.common.icon.Icons;
import com.dbn.editor.session.action.AbstractSessionBrowserAction;
import com.dbn.event.registration.ui.EventRegistrationsForm;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;

import static com.dbn.event.registration.action.EventRegistrationActionUtil.getListenersForm;
import static com.dbn.nls.NlsResources.txt;

public class EventRegistrationFindAction extends AbstractSessionBrowserAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        EventRegistrationsForm registrationsForm = getListenersForm(e);

//        SessionBrowser sessionBrowser = getSessionBrowser(e);
        if (registrationsForm != null) {
            registrationsForm.showSearchHeader();
//            sessionBrowser.showSearchHeader();
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        EventRegistrationsForm registrationsForm = getListenersForm(e);

        Presentation presentation = e.getPresentation();
        presentation.setEnabled(registrationsForm != null && !registrationsForm.isLoading());
        presentation.setText(txt("app.dataEditor.action.Find"));
        presentation.setIcon(Icons.ACTION_FIND);
    }
}