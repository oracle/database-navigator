package com.dbn.common.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

import static com.dbn.common.dispose.Checks.isValid;

public abstract class ContextAction<T> extends ProjectAction {

    public ContextAction() {}

    public ContextAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
        super(text, description, icon);
    }

    protected abstract T getTarget(@NotNull AnActionEvent e);

    @Override
    protected final void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        T context = getTarget(e);
        if (isValid(context)) {
            actionPerformed(e, project, context);
        }
    }

    @Override
    protected final void update(@NotNull AnActionEvent e, @NotNull Project project) {
        T target = getTarget(e);
        boolean enabled = isValid(target);

        Presentation presentation = e.getPresentation();
        presentation.setEnabled(enabled);
        update(e, presentation, project, target);
    }

    protected abstract void actionPerformed(
            @NotNull AnActionEvent e,
            @NotNull Project project,
            @NotNull T target);

    protected void update(
            @NotNull AnActionEvent e,
            @NotNull Presentation presentation,
            @NotNull Project project,
            @Nullable T target){};

}
