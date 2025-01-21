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

package com.dbn.object.filter.custom.ui;

import com.dbn.common.color.Colors;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.ComponentAligner;
import com.dbn.common.util.Actions;
import com.dbn.language.sql.SQLFileType;
import com.dbn.object.filter.custom.ObjectFilter;
import com.dbn.object.filter.custom.ui.action.DeleteObjectFilterAction;
import com.dbn.object.filter.custom.ui.action.EditObjectFilterAction;
import com.dbn.object.filter.custom.ui.action.ToggleObjectFilterStatusAction;
import com.dbn.object.filter.custom.ui.action.ViewObjectFilterAction;
import com.dbn.object.type.DBObjectType;
import com.intellij.ide.highlighter.HighlighterFactory;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.FocusChangeListener;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.ui.EditorSettingsProvider;
import com.intellij.ui.EditorTextField;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import static com.dbn.common.util.Strings.cachedUpperCase;
import static com.intellij.openapi.fileTypes.SyntaxHighlighterFactory.getSyntaxHighlighter;

public class ObjectFilterExpressionForm extends DBNFormBase implements ComponentAligner.Form {
    private JPanel mainPanel;
    private JLabel objectTypeLabel;
    private JPanel actionsPanel;
    private EditorTextField expressionTextField;

    private final ObjectFilter<?> filter;

    private final SyntaxHighlighter sqlSyntaxHighlighter = getSyntaxHighlighter(SQLFileType.INSTANCE, getProject(), null);
    private final SyntaxHighlighter txtSyntaxHighlighter = getSyntaxHighlighter(PlainTextFileType.INSTANCE, getProject(), null);

    public ObjectFilterExpressionForm(ObjectFilterSettingsForm parent, ObjectFilter<?> filter) {
        super(parent);
        this.filter = filter;

        DBObjectType objectType = filter.getObjectType();
        objectTypeLabel.setIcon(objectType.getIcon());
        objectTypeLabel.setText(cachedUpperCase(objectType.getName()));


        expressionTextField.setText(normalizeExpression(filter.getExpression()));
        expressionTextField.addSettingsProvider(createEditorSettingsProvider());
        expressionTextField.setCaretPosition(0);

        actionsPanel.add(createActionToolbar(), BorderLayout.CENTER);
        updateEditorField();
    }

    private JComponent createActionToolbar() {
        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, true,
                new ToggleObjectFilterStatusAction(this),
                new ViewObjectFilterAction(this),
                new EditObjectFilterAction(this),
                new DeleteObjectFilterAction(this));
        return actionToolbar.getComponent();
    }

    private @NotNull EditorSettingsProvider createEditorSettingsProvider() {
        return editor -> {
            editor.setViewer(true);
            editor.getComponent().setPreferredSize(new Dimension(-1, 24));

            editor.setBackgroundColor(getFilter().isActive() ?
                    Colors.getReadonlyEditorBackground() :
                    Colors.getTextFieldDisabledBackground());

            editor.addFocusListener(new FocusChangeListener() {
                @Override
                public void focusGained(@NotNull Editor editor) {
                    setHighlighter(editor, sqlSyntaxHighlighter);
                }

                @Override
                public void focusLost(@NotNull Editor editor) {
                    setHighlighter(editor, txtSyntaxHighlighter);
                }
            });
        };
    }

    private static void setHighlighter(Editor editor, SyntaxHighlighter syntaxHighlighter) {
        EditorColorsScheme colorsScheme = editor.getColorsScheme();
        EditorHighlighter highlighter = HighlighterFactory.createHighlighter(syntaxHighlighter, colorsScheme);
        ((EditorEx)editor).setHighlighter(highlighter);
    }

    public void setExpression(String expression) {
        expression = normalizeExpression(expression);
        if (expression.equals(expressionTextField.getText())) return;

        expressionTextField.setText(expression);
        markModified();
    }

    private static String normalizeExpression(@NonNls String expression) {
        return expression.replaceAll("\\s+", " ").trim();
    }

    @Override
    public Component[] getAlignableComponents() {
        return new Component[] {objectTypeLabel};
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public ObjectFilter<?> getFilter() {
        return Failsafe.nn(filter);
    }

    public boolean isActive() {
        return getFilter().isActive();
    }

    @NotNull
    public ObjectFilterSettingsForm getParentForm() {
        return ensureParentComponent();
    }

    public void remove() {
        markModified();
        getParentForm().removeFilterPanel(getFilter());
    }

    public void setActive(boolean active) {
        ObjectFilter<?> filter = getFilter();
        if (filter.isActive() == active) return;
        filter.setActive(active);

        markModified();
        updateEditorField();
    }

    private void markModified() {
        ObjectFilter<?> filter = getFilter();
        ObjectFilterSettingsForm settingsForm = getParentForm();
        settingsForm.markModified(filter);
    }

    private void updateEditorField() {
        boolean active = getFilter().isActive();
        expressionTextField.setEnabled(active);
        EditorEx editor = (EditorEx) expressionTextField.getEditor();
        if (editor == null) return;

        editor.setBackgroundColor(active ?
                Colors.getTextFieldBackground() :
                Colors.getTextFieldDisabledBackground());
    }
}
