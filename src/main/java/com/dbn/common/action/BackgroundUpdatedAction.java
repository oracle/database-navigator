package com.dbn.common.action;

import com.dbn.common.compatibility.Compatibility;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.ActionUpdateThreadAware;
import org.jetbrains.annotations.NotNull;

@Compatibility
public interface BackgroundUpdatedAction extends ActionUpdateThreadAware {

    @Override
    default @NotNull ActionUpdateThread getActionUpdateThread() {
        return isUpdateInBackground() ? ActionUpdateThread.BGT : ActionUpdateThread.EDT;
    }

    @Compatibility
    default boolean isUpdateInBackground() {
        return true;
    }

}
