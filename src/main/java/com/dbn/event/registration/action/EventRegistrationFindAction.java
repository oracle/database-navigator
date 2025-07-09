package com.dbn.event.registration.action;

import com.dbn.common.action.BasicAction;
import com.dbn.common.icon.Icons;
import com.dbn.event.registration.ui.EventRegistrationsForm;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;

import static com.dbn.event.registration.action.EventRegistrationActionUtil.getRegistrationsForm;
import static com.dbn.nls.NlsResources.txt;

public class EventRegistrationFindAction extends BasicAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        EventRegistrationsForm registrationsForm = getRegistrationsForm(e);
        if (registrationsForm != null) {
            registrationsForm.showSearchHeader();
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        EventRegistrationsForm registrationsForm = getRegistrationsForm(e);

        Presentation presentation = e.getPresentation();
        presentation.setEnabled(registrationsForm != null && !registrationsForm.isLoading());
        presentation.setText(txt("app.dataEditor.action.Find"));
        presentation.setIcon(Icons.ACTION_FIND);
    }
}