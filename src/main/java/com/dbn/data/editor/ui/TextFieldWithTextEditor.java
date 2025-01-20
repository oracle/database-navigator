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

import com.dbn.common.icon.Icons;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.util.Keyboard;
import com.dbn.common.ui.util.Mouse;
import com.dbn.common.util.Strings;
import com.dbn.data.editor.text.ui.TextEditorDialog;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;

@Getter
public class TextFieldWithTextEditor extends TextFieldWithButtons {
    private final JComponent button;
    private final String displayValue;

    public TextFieldWithTextEditor(@NotNull Project project) {
        this(project, null);
    }

    public TextFieldWithTextEditor(@NotNull Project project, String displayValue) {
        super(project);
        this.displayValue = displayValue;
        setBounds(0, 0, 0, 0);

        button = createButton(Icons.DATA_EDITOR_BROWSE, "Text Editor");
        button.addMouseListener(mouseListener);
        Shortcut[] shortcuts = Keyboard.getShortcuts(IdeActions.ACTION_SHOW_INTENTION_ACTIONS);
        String shortcutText = KeymapUtil.getShortcutsText(shortcuts);

        button.setToolTipText("Open editor (" + shortcutText + ')');
        add(button, BorderLayout.EAST);

        JTextField textField = getTextField();
        if (Strings.isNotEmpty(displayValue)) {
            textField.setText(displayValue);
            textField.setEnabled(false);
            textField.setDisabledTextColor(UIUtil.getLabelDisabledForeground());
        }
        //textField.setPreferredSize(new Dimension(150, -1));
        textField.addKeyListener(keyListener);
        textField.setEditable(false);

        button.addKeyListener(keyListener);
        addKeyListener(keyListener);

        customizeTextField(textField);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        button.setEnabled(enabled);
    }

    public void openEditor() {
        TextEditorDialog.show(getProject(), this);
    }

    /********************************************************
     *                      KeyListener                     *
     ********************************************************/
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
     *                    ActionListener                    *
     ********************************************************/
    private final ActionListener actionListener = e -> openEditor();

    private final MouseListener mouseListener = Mouse.listener().onClick(e -> openEditor());

    /********************************************************
     *                 TextEditorListener                   *
     ********************************************************/
    @Override
    public void afterUpdate() {
        Object userValue = getUserValueHolder().getUserValue();
        if (userValue instanceof String && Strings.isEmpty(displayValue)) {
            Dispatch.run(() -> {
                String text = (String) userValue;
                setEditable(text.length() < 1000 && text.indexOf('\n') == -1);
                setText(text);
            });
        }
    }

}
