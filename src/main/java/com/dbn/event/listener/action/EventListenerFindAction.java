package com.dbn.event.listener.action;

import com.dbn.common.icon.Icons;
import com.dbn.editor.session.SessionBrowser;
import com.dbn.editor.session.action.AbstractSessionBrowserAction;
import com.dbn.event.listener.ui.EventListenersForm;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;

import static com.dbn.event.listener.action.EventListenerActionUtil.getListenersForm;
import static com.dbn.nls.NlsResources.txt;

public class EventListenerFindAction extends AbstractSessionBrowserAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        EventListenersForm registrationsForm = getListenersForm(e);

//        SessionBrowser sessionBrowser = getSessionBrowser(e);
        if (registrationsForm != null) {
            registrationsForm.showSearchHeader();
//            sessionBrowser.showSearchHeader();
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        EventListenersForm registrationsForm = getListenersForm(e);

        Presentation presentation = e.getPresentation();
        presentation.setEnabled(registrationsForm != null && !registrationsForm.isLoading());
        presentation.setText(txt("app.dataEditor.action.Find"));
        presentation.setIcon(Icons.ACTION_FIND);
    }
}