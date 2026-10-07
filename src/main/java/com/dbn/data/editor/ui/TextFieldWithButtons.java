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
import com.dbn.common.ref.WeakRef;
import com.dbn.common.ui.misc.DBNButton;
import com.dbn.common.ui.panel.DBNPanelImpl;
import com.dbn.common.ui.table.DBNTable;
import com.dbn.common.ui.util.Accessibility;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.TextFields;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

import static com.dbn.common.util.Unsafe.cast;

@Getter
@Setter
public abstract class TextFieldWithButtons extends DBNPanelImpl implements DataEditorComponent {
    private final JBTextField textField;
    private final ProjectRef project;
    private UserValueHolder<?> userValueHolder;
    private final WeakRef<DBNTable> parentTable;
    private @Nullable WeakRef<Object> contextObject;

    protected TextFieldWithButtons(@NotNull Project project, @Nullable DBNTable parentTable) {
        this.project = ProjectRef.of(project);
        this.parentTable = WeakRef.of(parentTable);

        setLayout(new BorderLayout());
        this.textField = new JBTextField();
        this.textField.setMargin(JBUI.insets(0, 1));

        Dimension preferredSize = textField.getPreferredSize();
        Dimension maximumSize = new Dimension((int) preferredSize.getWidth(), (int) preferredSize.getHeight());

        textField.setMaximumSize(maximumSize);
        add(textField, BorderLayout.CENTER);
    }

    @Nullable
    public DBNTable getParentTable() {
        return WeakRef.get(parentTable);
    }


    public void setContextObject(@Nullable Object contextObject) {
        this.contextObject = WeakRef.of(contextObject);
    }

    @Nullable
    public Object getContextObject() {
        return WeakRef.get(contextObject);
    }

    @NotNull
    public Project getProject() {
        return project.ensure();
    }

    public void customizeTextField(JTextField textField) {
        DBNTable parentTable = getParentTable();
        if (parentTable != null) {
            textField.setBorder(Borders.EMPTY_BORDER);
            textField.setMargin(JBUI.emptyInsets());
            textField.setPreferredSize(new Dimension(textField.getPreferredSize().width, parentTable.getRowHeight()));
        } else {
            Dimension preferredSize = textField.getPreferredSize();
            textField.setPreferredSize(new Dimension(300, preferredSize.height));
        }

    }

    public JComponent createButton(Icon icon, @Nls String name) {
        DBNTable parentTable = getParentTable();
        if (parentTable == null) {
            JButton button = new JButton(icon);
            Accessibility.setAccessibleName(button, name);

            int side = (int) textField.getPreferredSize().getHeight();
            Dimension size = new Dimension(side, side);
            button.setPreferredSize(size);
            button.setMaximumSize(size);

            return button;
        } else {
            DBNButton button = new DBNButton(icon, name);
            button.setBorder(Borders.insetBorder(1));
            button.setOpaque(false);
            int rowHeight = parentTable.getRowHeight();
            button.setPreferredSize(new Dimension(Math.max(20, rowHeight), rowHeight - 2));
            parentTable.addPropertyChangeListener(e -> {
                Object newProperty = e.getNewValue();
                if (newProperty instanceof Font) {
                    int rowHeight1 = parentTable.getRowHeight();
                    button.setPreferredSize(new Dimension(Math.max(20, rowHeight1), parentTable.getRowHeight() - 2));
                }
            });

            return button;
        }
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        if (textField != null) textField.setFont(font);
    }


    public void setBorder(Border border) {
        super.setBorder(border);
    }

    public void setEmptyText(String text) {
        if (textField != null) textField.getEmptyText().setText(text);
    }

    @Override
    public void setBackground(Color color) {
        super.setBackground(color);
        if (textField != null) textField.setBackground(color);
    }

    @Override
    public void setEnabled(boolean enabled) {
        textField.setEnabled(enabled);
    }

    public void setEditable(boolean editable){
        textField.setEditable(editable);
        DBNTable parentTable = getParentTable();
        if (parentTable != null) {
            setBackground(getTextField().getBackground());
        }
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
        return TextFields.getText(textField);
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
