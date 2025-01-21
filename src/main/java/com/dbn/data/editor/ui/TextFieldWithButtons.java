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

import com.dbn.common.project.ProjectRef;
import com.dbn.common.ui.panel.DBNPanelImpl;
import com.dbn.common.ui.util.Accessibility;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.text.Document;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import static com.dbn.common.util.Unsafe.cast;

@Getter
@Setter
public abstract class TextFieldWithButtons extends DBNPanelImpl implements DataEditorComponent {
    private final JTextField textField;
    private final ProjectRef project;
    private UserValueHolder<?> userValueHolder;

    public TextFieldWithButtons(Project project) {
        this.project = ProjectRef.of(project);

        setLayout(new BorderLayout());
        this.textField = new JBTextField();
        this.textField.setMargin(JBUI.insets(0, 1));

        Dimension preferredSize = textField.getPreferredSize();
        Dimension maximumSize = new Dimension((int) preferredSize.getWidth(), (int) preferredSize.getHeight());

        textField.setMaximumSize(maximumSize);
        add(textField, BorderLayout.CENTER);

    }

    @NotNull
    public Project getProject() {
        return project.ensure();
    }

    public void customizeTextField(JTextField textField) {}

    public JComponent createButton(Icon icon, String name) {
        JButton button = new JButton(icon);
        Accessibility.setAccessibleName(button, name);

        int height = (int) textField.getPreferredSize().getHeight();
        int width = height;

        Dimension size = new Dimension(width, height);
        button.setPreferredSize(size);
        button.setMaximumSize(size);

        button.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (false && e.getKeyCode() == KeyEvent.VK_SPACE) button.doClick();
            }
        });

        return button;
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        if (textField != null) textField.setFont(font);
    }


    public void setBorder(Border border) {
        super.setBorder(border);
    }

    @Override
    public void setBackground(Color color) {
        super.setBackground(color);
        if (textField != null) textField.setBackground(color);
    }

    @Override
    public void setEnabled(boolean enabled) {
        textField.setEditable(enabled);
    }

    public void setEditable(boolean editable){
        textField.setEditable(editable);
    }

    public boolean isEditable() {
        return textField.isEditable();
    }

    public boolean isSelected() {
        Document document = textField.getDocument();
        return document.getLength() > 0 &&
               textField.getSelectionStart() == 0 &&
               textField.getSelectionEnd() == document.getLength();
    }

    public void clearSelection() {
        if (isSelected()) {
            textField.setSelectionStart(0);
            textField.setSelectionEnd(0);
            textField.setCaretPosition(0);
        }
    }

    public String getText() {
        return textField.getText();
    }

    public void setText(String text) {
        textField.setText(text);
    }

    public <T> UserValueHolder<T> getUserValueHolder() {
        return cast(userValueHolder);
    }

    public <T> void setUserValueHolder(UserValueHolder<T> userValueHolder) {
        this.userValueHolder = userValueHolder;
    }
}
