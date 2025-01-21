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
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.project.DumbAware;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import static com.dbn.nls.NlsResources.txt;

public class ShowHistoryAction extends DataSearchHeaderAction implements DumbAware {
    private JTextField searchField;


    public ShowHistoryAction(final JTextField searchField, DataSearchComponent searchComponent) {
        super(searchComponent);
        this.searchField = searchField;
        Presentation presentation = getTemplatePresentation();
        presentation.setIcon(AllIcons.Actions.Search);
        presentation.setText(txt("app.data.action.SearchHistory"));

        ArrayList<Shortcut> shortcuts = new ArrayList<>();
        ContainerUtil.addAll(shortcuts, ActionManager.getInstance().getAction("IncrementalSearch").getShortcutSet().getShortcuts());
        shortcuts.add(new KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_DOWN_MASK), null));

        registerCustomShortcutSet(new CustomShortcutSet(shortcuts.toArray(new Shortcut[0])), searchField);
        searchField.registerKeyboardAction(actionEvent -> {
            if (searchField.getText().isEmpty()) {
                getSearchComponent().showHistory(false, searchField);
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), JComponent.WHEN_FOCUSED);
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
        getSearchComponent().showHistory(e.getInputEvent() instanceof MouseEvent, searchField);
    }


}
