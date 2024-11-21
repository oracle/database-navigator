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

package com.dbn.data.find.action;

import com.dbn.data.find.DataSearchComponent;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.project.DumbAware;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

public class NextOccurrenceAction extends DataSearchHeaderAction implements DumbAware {

    public NextOccurrenceAction(DataSearchComponent searchComponent, JComponent component, boolean isSearchComponent) {
        super(searchComponent);

        ActionManager actionManager = ActionManager.getInstance();
        copyFrom(actionManager.getAction(IdeActions.ACTION_NEXT_OCCURENCE));
        Set<Shortcut> shortcuts = new HashSet<>();
        ContainerUtil.addAll(shortcuts, actionManager.getAction(IdeActions.ACTION_FIND_NEXT).getShortcutSet().getShortcuts());

        if (isSearchComponent) {
            ContainerUtil.addAll(shortcuts, actionManager.getAction(IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN).getShortcutSet().getShortcuts());
            shortcuts.add(new KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), null));
        }
        registerShortcutsToComponent(shortcuts, this, component);
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
        getSearchComponent().searchForward();
    }

    @Override
    public void update(final AnActionEvent e) {
        e.getPresentation().setEnabled(getSearchComponent().hasMatches());
    }
}
