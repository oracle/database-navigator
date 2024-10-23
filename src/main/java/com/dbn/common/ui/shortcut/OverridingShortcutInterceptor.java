package com.dbn.common.ui.shortcut;

import com.dbn.common.exception.ProcessDeferredException;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static com.dbn.common.dispose.Checks.isNotValid;

/**
 * Overriding implementation of a {@link ShortcutInterceptor}
 * Invokes an action if the given shortcut is intercepted and prevents the original action from being invoked by throwing {@link ProcessDeferredException}
 */
public abstract class OverridingShortcutInterceptor extends ShortcutInterceptor {
    public OverridingShortcutInterceptor(String delegateActionId) {
        super(delegateActionId);
    }

    @Override
    public void beforeActionPerformed(@NotNull AnAction action, @NotNull AnActionEvent event) {
        attemptDelegation(action, event);
    }

    private void attemptDelegation(AnAction action, AnActionEvent event) {
        if (isNotValid(action)) return;
        if (isNotValid(event)) return;
        if (Objects.equals(delegateActionClass, action.getClass())) return; // action is being invoked (no delegation needed)
        if (!matchesDelegateShortcuts(event)) return; // event not matching delegate shortcut
        if (!canDelegateExecute(event)) return; // delegate action may be disabled
        if (!isValidContext(event)) return;

        invokeDelegateAction(event);
        throw new ProcessDeferredException("Shortcut override - Event delegated to " + getDelegateActionId());
    }
}
