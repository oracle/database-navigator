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

package com.dbn.data.editor.ui;

import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.table.DBNTable;
import com.dbn.common.ui.util.Keyboard;
import com.dbn.common.util.Strings;
import com.dbn.data.editor.text.ui.TextEditorDialog;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JTextField;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import static com.dbn.common.color.Colors.getLabelDisabledForeground;
import static com.dbn.nls.NlsResources.txt;

@Getter
public class TextFieldWithTextEditor extends TextFieldWithEditor {

    public TextFieldWithTextEditor(@NotNull Project project, @Nullable DBNTable parentTable) {
        super(project, parentTable, txt("msg.dataEditor.title.TextEditor"), createToolTipText());
        setBounds(0, 0, 0, 0);

        JTextField textField = getTextField();

        textField.setEditable(false);
        textField.addKeyListener(keyListener);
        getButton().addKeyListener(keyListener);
        addKeyListener(keyListener);

        customizeTextField(textField);
    }

    @NotNull
    private static String createToolTipText() {
        Shortcut[] shortcuts = Keyboard.getShortcuts(IdeActions.ACTION_SHOW_INTENTION_ACTIONS);
        String shortcutText = KeymapUtil.getShortcutsText(shortcuts);
        return txt("app.dataEditor.tooltip.OpenTextEditor", shortcutText);
    }

    @Override
    public void openEditor() {
        TextEditorDialog.show(getProject(), this);
    }

    private final KeyListener keyListener = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent keyEvent) {
            Shortcut[] shortcuts = Keyboard.getShortcuts(IdeActions.ACTION_SHOW_INTENTION_ACTIONS);
            if (!keyEvent.isConsumed() && Keyboard.match(shortcuts, keyEvent)) {
                keyEvent.consume();
                openEditor();
            }
        }
    };

    /********************************************************
     *                 TextEditorListener                   *
     ********************************************************/
    @Override
    public void afterUpdate() {
        Object userValue = getUserValueHolder().getUserValue();
        if (userValue instanceof String text) {
            Dispatch.run(this, () -> {
                setEditable(text.length() < 1000 && text.indexOf('\n') == -1);
                setText(text);
            });
        }
    }

}
