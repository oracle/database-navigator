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

package com.dbn.common.ui.util;

import com.dbn.common.color.Colors;
import com.dbn.common.routine.Consumer;
import com.dbn.common.util.Strings;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.NlsContexts.StatusText;
import com.intellij.openapi.util.NlsContexts.Tooltip;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBTextField;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.function.Function;

import static com.dbn.common.ui.util.ClientProperty.FIELD_ERROR;
import static com.dbn.common.util.Strings.isEmpty;

@UtilityClass
public class TextFields {

    public static void onTextChange(TextFieldWithBrowseButton textField, Consumer<DocumentEvent> consumer) {
        onTextChange(textField.getTextField(), consumer);
    }

    public static void onTextChange(JTextComponent textField, Consumer<DocumentEvent> consumer) {
        addDocumentListener(textField, new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                consumer.accept(e);
            }
        });
    }

    public static void onTextChange(JSpinner spinner, Consumer<DocumentEvent> consumer) {
        JTextField textField = getTextField(spinner);
        onTextChange(textField, consumer);
    }

    @Nullable
    public static JTextField getTextField(JSpinner spinner) {
        JComponent editor = spinner.getEditor();

        if (editor instanceof JSpinner.DefaultEditor defaultEditor) {
            return defaultEditor.getTextField();
        }
        return null;
    }

    public static void addDocumentListener(JTextComponent textField, DocumentListener documentListener) {
        if (textField == null) return;
        textField.getDocument().addDocumentListener(documentListener);
    }

    public static String getText(@Nullable TextFieldWithBrowseButton textComponent) {
        if (textComponent == null) return "";
        return getText(textComponent.getTextField());

    }
    public static String getText(@Nullable JTextComponent textComponent) {
        if (textComponent == null) return "";
        String text = textComponent.getText();
        return text == null ? "" : text.trim();
    }


    public static boolean isEmptyText(TextFieldWithBrowseButton textComponent) {
        if (textComponent == null) return false;
        return isEmptyText(textComponent.getTextField());
    }

    public static boolean isEmptyText(JTextComponent textComponent) {
        if (textComponent == null) return true;

        String text = textComponent.getText();
        return Strings.isEmptyOrSpaces(text);
    }

    public static void limitTextLength(JTextComponent textComponent, int maxLength) {
        textComponent.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                String text = textComponent.getText();
                if (text.length() == maxLength) {
                    e.consume();
                } else if (text.length() > maxLength) {
                    text = text.substring(0, maxLength);
                    textComponent.setText(text);
                    e.consume();
                }
            }
        });
    }

    public static void installNumericFilter(JTextComponent textComponent, boolean signed) {
        Document document = textComponent.getDocument();
        if (document instanceof AbstractDocument abstractDocument) {
            abstractDocument.setDocumentFilter(new NumericDocumentFilter(signed));
        }
    }

    public static void setText(JTextComponent textComponent, String text) {
        textComponent.setText(text == null ? "" : text.trim());
    }

    /**
     * @deprecated Use {@link PasswordFields#setPassword(JPasswordField, char[])} to bind password fields,
     * and {@link PasswordFields#getPassword(JPasswordField, char[])} or
     * {@link PasswordFields#isPasswordChanged(JPasswordField, char[])} when applying form changes.
     */
    @Deprecated
    public static void setPassword(JPasswordField textComponent, char[] password) {
        PasswordFields.setPassword(textComponent, password);
    }

    public static void setText(TextFieldWithBrowseButton textComponent, String text) {
        setText(textComponent.getTextField(), text);
    }

    public static void setTextSilently(TextFieldWithBrowseButton textComponent, String text) {
        setTextSilently(textComponent.getTextField(), text);
    }

    public static void setTextSilently(JTextComponent textComponent, String text) {
        Document document = textComponent.getDocument();
        if (document instanceof AbstractDocument abstractDocument) {
            DocumentListener[] documentListeners = abstractDocument.getDocumentListeners();
            try {
                Arrays.stream(documentListeners).forEach(document::removeDocumentListener);
                textComponent.setText(text);
                textComponent.revalidate();
                textComponent.repaint();
            } finally {
                Arrays.stream(documentListeners).forEach(document::addDocumentListener);
            }
        } else {
            textComponent.setText(text);
        }

    }

    public static void updateFieldError(JTextComponent textComponent, @Nullable @Tooltip String error) {
        FIELD_ERROR.set(textComponent, error);
        textComponent.setForeground(error == null ? Colors.getTextFieldForeground() : JBColor.RED);
        textComponent.setToolTipText(error);
    }

    public static void setEmptyText(JTextField textField, @StatusText String emptyText) {
        if (isEmpty(emptyText)) return;
        if (textField instanceof JBTextField jbTextField) {
            jbTextField.getEmptyText().setText(emptyText);
        }
    }

    public static void installErrorHighlighting(TextFieldWithBrowseButton textField, Function<String, @Tooltip String> verifier) {
        installErrorHighlighting(textField.getTextField(), verifier);
    }

    public static void installErrorHighlighting(JTextField textComponent, Function<String, @Tooltip String> verifier) {
        onTextChange(textComponent, e -> {
            String errorMessage = verifier.apply(textComponent.getText());
            updateFieldError(textComponent, errorMessage);
        });

    }

    private static final class NumericDocumentFilter extends DocumentFilter {
        private final boolean signed;

        private NumericDocumentFilter(boolean signed) {
            this.signed = signed;
        }

        @Override
        public void insertString(FilterBypass bypass, int offset, String text, AttributeSet attributes) throws BadLocationException {
            replace(bypass, offset, 0, text, attributes);
        }

        @Override
        public void replace(FilterBypass bypass, int offset, int length, String text, AttributeSet attributes) throws BadLocationException {
            Document document = bypass.getDocument();
            String current = document.getText(0, document.getLength());
            String replacement = text == null ? "" : text;
            String value = current.substring(0, offset) + replacement + current.substring(offset + length);
            if (isValid(value)) {
                bypass.replace(offset, length, replacement, attributes);
            }
        }

        @Override
        public void remove(FilterBypass bypass, int offset, int length) throws BadLocationException {
            replace(bypass, offset, length, "", null);
        }

        private boolean isValid(String value) {
            if (value.isEmpty()) return true;

            int start = signed && value.charAt(0) == '-' ? 1 : 0;
            if (start == value.length()) return signed;

            for (int i = start; i < value.length(); i++) {
                char character = value.charAt(i);
                if (character < '0' || character > '9') return false;
            }
            return start == 0 || value.charAt(0) == '-';
        }
    }
}
