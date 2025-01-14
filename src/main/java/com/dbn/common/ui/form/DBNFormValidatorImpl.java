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

package com.dbn.common.ui.form;

import com.dbn.common.ref.WeakRefWrapper;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.validation.ValidationException;
import com.dbn.common.validation.Validator;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.DocumentAdapter;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.event.DocumentEvent;
import javax.swing.text.JTextComponent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import static com.dbn.common.util.Commons.isEmpty;
import static com.dbn.common.util.Commons.isOneOf;

final class DBNFormValidatorImpl extends WeakRefWrapper<DBNForm> implements DBNFormValidator {
    private final List<DBNFormFieldValidator<?>> validators = new ArrayList<>();

    public DBNFormValidatorImpl(DBNForm form) {
        super(form);
    }

    @Override
    public <C extends JComponent> void addValidation(C component, Predicate<C> validator, String message) {
        validators.add(new DBNFormFieldValidator<>(component, target -> validateTarget(target, validator, message)));
    }

    @Override
    public void addTextValidation(JTextComponent textField, Predicate<String> validator, String message) {
        addValidation(textField, textComponent -> validator.test(textField.getText()), message);

        DBNDialog dialog = getTarget().getParentDialog();
        if (dialog != null) {
            // add document listener to perform validation on text change and enable / disable dialog button
            textField.getDocument().addDocumentListener(new DocumentAdapter() {
                @Override
                protected void textChanged(@NotNull DocumentEvent e) {
                    dialog.validateInput(textField);
                }
            });

            // add focus listener to perform validation on focus gained
            textField.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    dialog.validateInput(textField);
                }
            });
        }
    }

    private static <C extends JComponent> void validateTarget(C target, Predicate<C> validator, String message) throws ValidationException {
        boolean valid = validator.test(target);
        if (!valid) throw new ValidationException(message);
    }

    @NotNull
    @Override
    public List<ValidationInfo> validateForm(JComponent... components) {
        List<ValidationInfo> result = null;
        for (DBNFormFieldValidator<?> validator : validators) {
            JComponent target = validator.getTarget();
            try {
                if (isEmpty(components) || isOneOf(target, components)) {
                    validator.validate();
                }

            } catch (ValidationException e) {
                if (result == null) result = new ArrayList<>();

                String message = e.getMessage();
                result.add(new ValidationInfo(message, target));
            }
        }

        return result == null ? Collections.emptyList() : result;
    }

    private static class DBNFormFieldValidator<T extends JComponent> extends WeakRefWrapper<T> {
        private final Validator<T> validator;

        public DBNFormFieldValidator(T component, Validator<T> validator) {
            super(component);
            this.validator = validator;
        }

        public void validate() throws ValidationException {
            T target = getTarget();
            validator.validate(target);
        }
    }
}
