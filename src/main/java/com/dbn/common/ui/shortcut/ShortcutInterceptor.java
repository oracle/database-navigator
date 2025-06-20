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
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Component;
import java.util.Map;
import java.util.Objects;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.dispose.Checks.isValid;
import static com.intellij.openapi.actionSystem.AnAction.getEventProject;

@Slf4j
@Getter
public abstract class ShortcutInterceptor implements AnActionListener {
    private final String delegateActionId;
    private final Class<? extends AnAction> delegateActionType;
    private static final Map<Class, AnActionEvent> eventTrace = ContainerUtil.createConcurrentWeakValueMap();

    public ShortcutInterceptor(String delegateActionId) {
        this.delegateActionId = delegateActionId;
        AnAction delegateAction = getDelegateAction();

        if (delegateAction == null) {
            log.error("Delegate action not found for id \"{}\"", delegateActionId);
            this.delegateActionType = null;
        } else {
            this.delegateActionType = delegateAction.getClass();
        }
    }

    @Nullable
    protected AnAction getDelegateAction() {
        return ActionManager.getInstance().getAction(delegateActionId);
    }

    private boolean matchesDelegateShortcuts(AnActionEvent event) {
        Shortcut[] shortcuts = Keyboard.getShortcuts(delegateActionId);
        return Keyboard.match(shortcuts, event);
    }

    private boolean isValidContext(AnActionEvent event) {
        Project project = getEventProject(event);
        return isValid(project);
    }

    protected abstract boolean canDelegate(AnActionEvent event);

    private boolean isDelegatedAction(AnAction action) {
        return Objects.equals(delegateActionType, action.getClass());
    }

    private boolean isNewEvent(AnActionEvent event) {
        // BUGDB-37974361 - multiple interceptor invocations (one invocation per open project)
        if (eventTrace.get(delegateActionType) == event) return false;
        eventTrace.put(delegateActionType, event);

        return true;
    }

    protected boolean preventDelegation(AnAction action, AnActionEvent event) {
        if (delegateActionType == null) return true;
        if (isNotValid(action)) return true;
        if (isNotValid(event)) return true;
        if (isDelegatedAction(action)) return true; // the action itself is being invoked (no further delegation)
        if (!matchesDelegateShortcuts(event)) return true; // event not matching delegate shortcut
        if (!canDelegate(event)) return true; // delegate action may be disabled
        if (!isValidContext(event)) return true;
        if (!isNewEvent(event)) return true;
        return false;
    }

    protected void invokeDelegateAction(@NotNull AnActionEvent event) {
        AnAction delegateAction = getDelegateAction();
        if (delegateAction == null) return;

        Component component = event.getData(PlatformDataKeys.CONTEXT_COMPONENT);
        ActionManager.getInstance().tryToExecute(delegateAction, event.getInputEvent(), component, null, false);
    }
}
