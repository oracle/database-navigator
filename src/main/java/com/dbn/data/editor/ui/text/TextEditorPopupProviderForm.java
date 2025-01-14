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

package com.dbn.data.editor.ui.text;

import com.dbn.common.action.DataKeys;
import com.dbn.common.color.Colors;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.misc.DBNScrollPane;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.Keyboard;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Messages;
import com.dbn.common.util.TextAttributes;
import com.dbn.data.editor.ui.TextFieldPopupProviderForm;
import com.dbn.data.editor.ui.TextFieldPopupType;
import com.dbn.data.editor.ui.TextFieldWithPopup;
import com.dbn.data.editor.ui.UserValueHolder;
import com.dbn.data.grid.color.DataGridTextAttributesKeys;
import com.dbn.data.value.LargeObjectValue;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.util.Arrays;

import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.util.Actions.createActionToolbar;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class TextEditorPopupProviderForm extends TextFieldPopupProviderForm {
    private JPanel mainPanel;
    private JPanel rightActionPanel;
    private JPanel leftActionPanel;
    private JTextArea editorTextArea;
    private DBNScrollPane textEditorScrollPane;

    private @Getter @Setter boolean changed;

    public TextEditorPopupProviderForm(TextFieldWithPopup<?> textField, boolean autoPopup) {
        super(textField, autoPopup, true);
        editorTextArea.setBorder(JBUI.Borders.empty(4));
        editorTextArea.addKeyListener(this);
        editorTextArea.setWrapStyleWord(true);


        textEditorScrollPane.setBorder(Borders.COMPONENT_OUTLINE_BORDER);

        ActionToolbar leftActionToolbar = Actions.createActionToolbar(leftActionPanel, true);
        leftActionPanel.add(leftActionToolbar.getComponent(), BorderLayout.WEST);

        ActionToolbar rightActionToolbar = createActionToolbar(leftActionPanel, true, "DBNavigator.ActionGroup.TextEditor.Controls");
        rightActionPanel.add(rightActionToolbar.getComponent(), BorderLayout.EAST);

        Arrays.asList(leftActionToolbar, rightActionToolbar).forEach(tb -> tb.getActions().forEach(a -> registerAction(a)));

        updateComponentColors();
        Colors.subscribe(this, () -> updateComponentColors());
    }

    private void updateComponentColors() {
        UserInterface.changePanelBackground(mainPanel, Colors.getPanelBackground());

        SimpleTextAttributes textAttributes = TextAttributes.getSimpleTextAttributes(DataGridTextAttributesKeys.DEFAULT_PLAIN_DATA);
        editorTextArea.setBackground(Commons.nvl(
                textAttributes.getBgColor(),
                Colors.getTextFieldBackground()));

        editorTextArea.setForeground(Commons.nvl(
                textAttributes.getFgColor(),
                Colors.getTextFieldForeground()));
    }


    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return editorTextArea;
    }

    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        if (DataKeys.TEXT_EDITOR_POPUP_PROVIDER_FORM.is(dataId)) return this;
        return null;
    }

    @Override
    public JBPopup createPopup() {
        JTextField textField = getTextField();
        String text = "";
        UserValueHolder userValueHolder = getEditorComponent().getUserValueHolder();
        if (textField.isEditable()) {
            text = textField.getText();
        } else {
            Object userValue = userValueHolder.getUserValue();
            if (userValue instanceof String) {
                text = (String) userValue;
            } else if (userValue instanceof LargeObjectValue) {
                LargeObjectValue largeObjectValue = (LargeObjectValue) userValue;
                try {
                    text = Commons.nvl(largeObjectValue.read(), "");
                } catch (SQLException e) {
                    conditionallyLog(e);
                    Messages.showErrorDialog(getProject(), e.getLocalizedMessage(), e);
                    return null;
                }
            }
        }

        editorTextArea.setText(text);
        changed = false;
        if (textField.isEditable()) editorTextArea.setCaretPosition(textField.getCaretPosition());
        editorTextArea.setSelectionStart(textField.getSelectionStart());
        editorTextArea.setSelectionEnd(textField.getSelectionEnd());
        onTextChange(editorTextArea, e -> changed = true);

        JComponent component = getComponent();
        component.setPreferredSize(new Dimension(Math.max(200, textField.getWidth() + 32), 160));

        ComponentPopupBuilder popupBuilder = JBPopupFactory.getInstance().createComponentPopupBuilder(component, editorTextArea);
        popupBuilder.setRequestFocus(true);
        popupBuilder.setResizable(true);
        popupBuilder.setDimensionServiceKey(getProject(), "TextEditor." + userValueHolder.getName(), false);
        return popupBuilder.createPopup();
    }

    @Override
    public String getKeyShortcutName() {
        return IdeActions.ACTION_SHOW_INTENTION_ACTIONS;
    }

    @Override
    public String getDescription() {
        return "Text Editor";
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
        super.keyPressed(e);
        if (!e.isConsumed()) {
            if (Keyboard.match(getShortcuts(), e)) {
                editorTextArea.replaceSelection("\n");
            }
        }
    }

    String getText() {
        return editorTextArea.getText();
    }

}
