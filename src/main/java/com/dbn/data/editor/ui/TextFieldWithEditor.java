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

import com.dbn.common.color.Colors;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.table.DBNTable;
import com.dbn.common.ui.util.Mouse;
import com.dbn.data.grid.color.BasicTableTextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBTextField;
import lombok.Getter;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.awt.BorderLayout;
import java.awt.Color;

/** A text field with a button that opens a dedicated editor for its value. */
@Getter
public abstract class TextFieldWithEditor extends TextFieldWithButtons {
    private final JComponent button;

    protected TextFieldWithEditor(
            @NotNull Project project,
            @Nullable DBNTable parentTable,
            @NotNull @Nls String buttonName,
            @NotNull @Nls String buttonToolTip) {
        super(project, parentTable);

        button = createButton(Icons.DATA_EDITOR_BROWSE, buttonName);
        button.setToolTipText(buttonToolTip);
        button.addMouseListener(Mouse.listener().onClick(e -> openEditor()));
        add(button, BorderLayout.EAST);
    }

    public void setEditable(boolean editable) {
        super.setEditable(editable);

        JBTextField textField = getTextField();
        if (editable) {
            textField.setForeground(Colors.getTextFieldForeground());
        } else {
            BasicTableTextAttributes attributes = BasicTableTextAttributes.get();
            Color foreground = attributes.getReadonlyData().getFgColor();
            textField.setForeground(foreground);
        }

        DBNTable parentTable = getParentTable();
        if (parentTable != null) {
            Color background = textField.getBackground();
            setBackground(background);
            getButton().setBackground(background);
        }
    }

    public abstract void openEditor();

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        button.setEnabled(enabled);
    }
}
