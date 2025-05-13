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

package com.dbn.common.util;

import com.dbn.common.compatibility.Workaround;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import java.awt.event.InputEvent;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.dbn.common.ui.util.ClientProperty.ACTION_TOOLBAR;
import static com.intellij.openapi.actionSystem.ActionPlaces.POPUP;
import static com.intellij.openapi.actionSystem.ActionPlaces.TOOLBAR;

@UtilityClass
public class Actions {
    public static final AnAction SEPARATOR = Separator.getInstance();

    public static ActionToolbar createActionToolbar(@NotNull JComponent component, boolean horizontal, String name){
        ActionManager actionManager = ActionManager.getInstance();
        ActionGroup actionGroup = (ActionGroup) actionManager.getAction(name);
        ActionToolbar toolbar = actionManager.createActionToolbar(TOOLBAR, actionGroup, horizontal);

        linkActionToolbar(component, toolbar);
        markImportantToolbar(toolbar);
        return toolbar;
    }

    public static ActionToolbar createActionToolbar(@NotNull JComponent component, boolean horizontal, ActionGroup actionGroup){
        ActionManager actionManager = ActionManager.getInstance();
        ActionToolbar toolbar = actionManager.createActionToolbar(TOOLBAR, actionGroup, horizontal);
        linkActionToolbar(component, toolbar);
        markImportantToolbar(toolbar);
        return toolbar;
    }

    public static ActionToolbar createActionToolbar(@NotNull JComponent component, boolean horizontal, AnAction... actions){
        ActionManager actionManager = ActionManager.getInstance();
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        for (AnAction action : actions) {
            if (action == SEPARATOR)
                actionGroup.addSeparator(); else
                actionGroup.add(action);
        }

        ActionToolbar toolbar = actionManager.createActionToolbar(TOOLBAR, actionGroup, horizontal);
        linkActionToolbar(component, toolbar);
        markImportantToolbar(toolbar);
        return toolbar;
    }

    private static void linkActionToolbar(@NotNull JComponent component, ActionToolbar toolbar) {
        ACTION_TOOLBAR.set(component, toolbar, true);
        toolbar.setTargetComponent(component);
    }

    @Workaround
    private static void markImportantToolbar(ActionToolbar toolbar) {
        // ToolWindowImpl decides to recursively remove all toolbars under certain circumstances (this seems to solve the issue)
        // TODO only happening for the new "DB Events" tool window. investigate why
        toolbar.getComponent().putClientProperty("ActionToolbarImpl.importantToolbar", Boolean.TRUE);
    }


    public static ActionPopupMenu createActionPopupMenu(@NotNull JComponent component, ActionGroup actionGroup){
        ActionManager actionManager = ActionManager.getInstance();
        ActionPopupMenu popupMenu = actionManager.createActionPopupMenu(POPUP, actionGroup);
        popupMenu.setTargetComponent(component);
        return popupMenu;
    }

    public static String adjustActionName(@NotNull String name) {
        return name.replaceAll("_", "__");
    }

    public static boolean isConsumed(AnActionEvent event) {
        InputEvent inputEvent = event.getInputEvent();
        if (inputEvent == null) return false;
        return inputEvent.isConsumed();
    }

    public static void consume(AnActionEvent event) {
        InputEvent inputEvent = event.getInputEvent();
        if (inputEvent == null) return;
        inputEvent.consume();
    }

    public static List<AnAction> getActions(ActionToolbar actionToolbar) {
        ActionGroup actionGroup = actionToolbar.getActionGroup();
        if (actionGroup instanceof DefaultActionGroup) {
            ActionManager actionManager = ActionManager.getInstance();
            DefaultActionGroup defaultActionGroup = (DefaultActionGroup) actionGroup;
            AnAction[] actions = defaultActionGroup.getChildren(null, actionManager);
            return Arrays
                    .stream(actions)
                    .filter(a -> !isSeparator(a))
                    .collect(Collectors.toList());
        }
        return actionToolbar.getActions()
                .stream()
                .filter(a -> !isSeparator(a))
                .collect(Collectors.toList());
    }

    public static boolean isSeparator(AnAction action) {
        return action instanceof Separator;
    }
}
