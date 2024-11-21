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

import com.dbn.common.ui.util.Keyboard;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.dispose.Checks.isValid;
import static com.intellij.openapi.actionSystem.AnAction.getEventProject;

@Getter
public abstract class ShortcutInterceptor implements AnActionListener {
    protected final String delegateActionId;
    protected final Class<? extends AnAction> delegateActionClass;

    public ShortcutInterceptor(String delegateActionId) {
        this.delegateActionId = delegateActionId;
        this.delegateActionClass = getDelegateAction().getClass();
    }

    protected AnAction getDelegateAction() {
        return ActionManager.getInstance().getAction(delegateActionId);
    }

    protected boolean matchesDelegateShortcuts(AnActionEvent event) {
        Shortcut[] shortcuts = Keyboard.getShortcuts(delegateActionId);
        return Keyboard.match(shortcuts, event);
    }

    protected boolean isValidContext(AnActionEvent event) {
        Project project = getEventProject(event);
        return isValid(project);
    }

    protected abstract boolean canDelegateExecute(AnActionEvent event);

    protected void invokeDelegateAction(@NotNull AnActionEvent event) {
        AnAction delegateAction = getDelegateAction();
        AnActionEvent delegateEvent = new AnActionEvent(
                event.getInputEvent(),
                event.getDataContext(),
                event.getPlace(),
                new Presentation(),
                ActionManager.getInstance(), 0);

        delegateAction.actionPerformed(delegateEvent);
    }
}
