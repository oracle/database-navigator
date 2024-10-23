package com.dbn.common.action;

//import com.intellij.openapi.actionSystem.ActionUpdateThread;

import com.dbn.nls.NlsSupport;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class BasicAction extends AnAction implements BackgroundUpdateAware, DumbAware, NlsSupport {

    public BasicAction() {
    }

    public BasicAction(@Nullable Icon icon) {
        super(icon);
    }

    public BasicAction(@Nullable String text) {
        super(text);
    }

    public BasicAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
        super(text, description, icon);
    }

    @NotNull
    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return resolveActionUpdateThread();
    }

}
