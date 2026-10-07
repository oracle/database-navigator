/*
 * Copyright 2026 Oracle and/or its affiliates
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
import com.dbn.common.ui.util.Mouse;
import com.intellij.openapi.project.Project;

import javax.swing.JComponent;
import java.awt.BorderLayout;
import java.awt.Dimension;

import static com.dbn.nls.NlsResources.txt;

public class TextFieldWithTypeEditor extends TextFieldWithButtons {
    private final JComponent button;
    private Runnable openAction;

    public TextFieldWithTypeEditor(Project project) {
        super(project);

        button = createButton(Icons.DATA_EDITOR_BROWSE, txt("msg.dataEditor.title.EditRecord"));
        button.setToolTipText(txt("msg.dataEditor.title.EditRecord"));
        button.addMouseListener(Mouse.listener().onClick(e -> {
            if (openAction != null) openAction.run();
        }));
        add(button, BorderLayout.EAST);
    }

    public void setOpenAction(Runnable openAction) {
        this.openAction = openAction;
    }

    public void setButtonSize(Dimension size) {
        button.setPreferredSize(size);
        button.setMaximumSize(size);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        button.setEnabled(enabled);
    }

    @Override
    public void setEditable(boolean editable) {
        super.setEditable(false);
    }
}
