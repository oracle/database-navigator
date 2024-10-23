package com.dbn.common.action;

//import com.intellij.openapi.actionSystem.ActionUpdateThread;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.project.DumbAware;
import com.intellij.ui.AnActionButton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class BasicActionButton extends AnActionButton implements BackgroundUpdateAware, DumbAware {

    public BasicActionButton() {
    }

    public BasicActionButton(@Nullable String text) {
        super(text);
    }

    public BasicActionButton(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
        super(text, description, icon);
    }

    @NotNull
    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return resolveActionUpdateThread();
    }
}
