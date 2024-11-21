/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.common.ui.shortcut;

import com.dbn.common.util.Actions;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.AnActionResult;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static com.dbn.common.dispose.Checks.isNotValid;

/**
 * Complementary implementation of a {@link ShortcutInterceptor}
 * Invokes an additional action if the given shortcut is intercepted. Does not prevent the original action from being invoked
 */
public abstract class ComplementaryShortcutInterceptor extends ShortcutInterceptor {

    public ComplementaryShortcutInterceptor(String delegateActionId) {
        super(delegateActionId);
    }

    @Override
    public void afterActionPerformed(@NotNull AnAction action, @NotNull AnActionEvent event, @NotNull AnActionResult result) {
        attemptDelegation(action, event);
    }

    private void attemptDelegation(AnAction action, AnActionEvent event) {
        if (isNotValid(action)) return;
        if (isNotValid(event)) return;
        if (Objects.equals(delegateActionClass, action.getClass())) return; // action invoked already
        if (!Actions.isConsumed(event)) return; // event not consumed, there is still hope
        if (!matchesDelegateShortcuts(event)) return; // event not matching delegate shortcut
        if (!canDelegateExecute(event)) return; // delegate action may be disabled
        if (!isValidContext(event)) return;

        invokeDelegateAction(event);
    }

}
