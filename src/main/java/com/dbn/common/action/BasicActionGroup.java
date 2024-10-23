package com.dbn.common.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.dispose.Failsafe.guarded;

public abstract class BasicActionGroup extends DefaultActionGroup implements BackgroundUpdateAware, DumbAware {

    public BasicActionGroup() {
        setPopup(true);
    }

    @Override
    @NotNull
    public final AnAction[] getChildren(AnActionEvent e) {
        if (e == null) return AnAction.EMPTY_ARRAY;
        return guarded(AnAction.EMPTY_ARRAY, this, a -> a.loadChildren(e));
    }

    @NotNull
    protected abstract AnAction[] loadChildren(AnActionEvent e);

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
    }

    @NotNull
    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return resolveActionUpdateThread();
    }

}
