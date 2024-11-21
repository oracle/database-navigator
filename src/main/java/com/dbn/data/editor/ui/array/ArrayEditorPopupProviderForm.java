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

package com.dbn.data.editor.ui.array;

import com.dbn.common.action.DataKeys;
import com.dbn.common.color.Colors;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.misc.DBNScrollPane;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Messages;
import com.dbn.common.util.TextAttributes;
import com.dbn.data.editor.ui.TextFieldPopupProviderForm;
import com.dbn.data.editor.ui.TextFieldPopupType;
import com.dbn.data.editor.ui.TextFieldWithPopup;
import com.dbn.data.editor.ui.UserValueHolder;
import com.dbn.data.grid.color.DataGridTextAttributesKeys;
import com.dbn.data.value.ArrayValue;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.dbn.common.util.Actions.createActionToolbar;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class ArrayEditorPopupProviderForm extends TextFieldPopupProviderForm {
    private JPanel mainPanel;
    private JPanel rightActionPanel;
    private JPanel leftActionPanel;
    private DBNScrollPane listScrollPane;

    private final ArrayEditorList list;

    public ArrayEditorPopupProviderForm(TextFieldWithPopup<?> textField, boolean autoPopup) {
        super(textField, autoPopup, true);

        ActionToolbar actionToolbarLeft = createActionToolbar(leftActionPanel, "DBNavigator.ActionGroup.Arrays.LeftControls", "",true);
        ActionToolbar actionToolbarRight = createActionToolbar(leftActionPanel, "DBNavigator.ActionGroup.Arrays.RightControls", "",true);
        Arrays.asList(actionToolbarLeft, actionToolbarRight).forEach(tb -> tb.getActions().forEach(a -> registerAction(a)));

        leftActionPanel.add(actionToolbarLeft.getComponent(), BorderLayout.WEST);
        rightActionPanel.add(actionToolbarRight.getComponent(), BorderLayout.EAST);
        list = new ArrayEditorList(this);
        list.initTableGutter();
        list.addKeyListener(this);
        list.setBackground(Colors.getEditorBackground());

        listScrollPane.setViewportView(list);
        listScrollPane.setBorder(Borders.COMPONENT_OUTLINE_BORDER);

        mainPanel.addKeyListener(this);

        updateComponentColors();
        Colors.subscribe(this, () -> updateComponentColors());
    }

    private void updateComponentColors() {
        UserInterface.changePanelBackground(mainPanel, Colors.getPanelBackground());

        SimpleTextAttributes textAttributes = TextAttributes.getSimpleTextAttributes(DataGridTextAttributesKeys.DEFAULT_PLAIN_DATA);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return list;
    }

    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        if (DataKeys.ARRAY_EDITOR_POPUP_PROVIDER_FORM.is(dataId)) return this;
        return null;
    }

    public boolean isChanged() {
        return list.getModel().isChanged();
    }

    @Override
    public JBPopup createPopup() {
        JTextField textField = getTextField();

        List<String> stringValues = new ArrayList<>();
        UserValueHolder<Object> userValueHolder = getEditorComponent().getUserValueHolder();
        Project project = getProject();
        try {
            Object userValue = userValueHolder.getUserValue();
            ArrayValue array = (ArrayValue) userValue;
            List<String> values = array == null ? null : array.read();
            if (values != null) {
                stringValues.addAll(values);
            }
        } catch (SQLException e) {
            conditionallyLog(e);
            Messages.showErrorDialog(project, e.getLocalizedMessage(), e);
            return null;
        }
        list.setStringValues(stringValues);
        if (list.getModel().getRowCount() > 0) {
            list.selectCell(0,0);
        }

        //editorTextArea.setText(text);
        //if (textField.isEditable()) editorTextArea.setCaretPosition(textField.getCaretPosition());
        //editorTextArea.setSelectionStart(textField.getSelectionStart());
        //editorTextArea.setSelectionEnd(textField.getSelectionEnd());
        //editorTextArea.getDocument().addDocumentListener(new DocumentListener());
        JComponent component = getComponent();
        component.setPreferredSize(new Dimension(Math.max(200, textField.getWidth() + 32), 200));

        ComponentPopupBuilder popupBuilder = JBPopupFactory.getInstance().createComponentPopupBuilder(component, list);
        popupBuilder.setRequestFocus(true);
        popupBuilder.setResizable(true);
        popupBuilder.setDimensionServiceKey(project, "ArrayEditor." + userValueHolder.getName(), false);
        return popupBuilder.createPopup();
    }

    @Override
    public String getKeyShortcutName() {
        return IdeActions.ACTION_SHOW_INTENTION_ACTIONS;
    }

    @Override
    public String getDescription() {
        return "Array Editor";
    }

    @Override
    public TextFieldPopupType getPopupType() {
        return TextFieldPopupType.TEXT_EDITOR;
    }

    @Nullable
    @Override
    public Icon getButtonIcon() {
        return Icons.DATA_EDITOR_BROWSE;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.isConsumed()) return;

        if (e.getKeyCode() == 27) {
            if (list.isEditing()) {
                list.stopCellEditing();
            }
        } else {
            super.keyPressed(e);
        }

    }

    public ArrayEditorList getEditorList() {
        return list;
    }
}
