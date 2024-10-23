package com.dbn.common.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.common.dispose.Failsafe.guarded;

public abstract class ProjectAction extends BasicAction {

    public ProjectAction() {}

    @Deprecated // TODO move presentation in "update"
    public ProjectAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
        super(text, description, icon);
    }

    @Override
    public final void update(@NotNull AnActionEvent e) {
        guarded(this, a -> {
            Project project = a.resolveProject(e);
            if (isValid(project)) a.update(e, project);
        });
    }

    @Override
    public final void actionPerformed(@NotNull AnActionEvent e) {
        guarded(this, a -> {
            Project project = a.resolveProject(e);
            if (isValid(project)) a.actionPerformed(e, project);
        });
    }

    @Nullable
    private Project resolveProject(@NotNull AnActionEvent e) {
        Project project = getProject();
        if (project == null) project = Lookups.getProject(e);
        return project;
    }

    /**
     * fallback when project cannot be loaded from the data context (TODO check why)
     */
    @Nullable
    public Project getProject() {
        return null;
    }

    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
    }

    protected abstract void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project);
}

