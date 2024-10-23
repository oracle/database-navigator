package com.dbn.common.ui.util;

import com.dbn.common.color.Colors;
import com.dbn.common.routine.Consumer;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import static com.dbn.common.ui.util.ClientProperty.FIELD_ERROR;

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

    public static void addDocumentListener(JTextComponent textField, DocumentListener documentListener) {
        if (textField == null) return;
        textField.getDocument().addDocumentListener(documentListener);
    }

    public static String getText(JTextComponent textComponent) {
        return textComponent.getText().trim();
    }

    public static boolean isEmptyText(JTextComponent textComponent) {
        return textComponent.getText().trim().isEmpty();
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

    public static void updateFieldError(JTextComponent textComponent, @Nullable String error) {
        FIELD_ERROR.set(textComponent, error);
        textComponent.setForeground(error == null ? Colors.getTextFieldForeground() : JBColor.RED);
        textComponent.setToolTipText(error);
    }
}
