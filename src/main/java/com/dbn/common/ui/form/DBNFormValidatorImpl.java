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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.dbn.common.ui.util.ClientProperty.HAS_VALIDATION_LISTENERS;
import static com.dbn.common.util.Commons.isEmpty;
import static com.dbn.common.util.Commons.isOneOf;
import static com.dbn.common.util.Commons.nvl;

public final class DBNFormValidatorImpl extends WeakRefWrapper<DBNDialog> implements DBNFormValidator {
    private final List<DBNFormFieldValidator<?>> validators = new ArrayList<>();

    public DBNFormValidatorImpl(DBNDialog dialog) {
        super(dialog);
    }

    @Override
    public <C extends JComponent> void addValidation(C component, Predicate<C> validator, String message) {
        validators.add(new DBNFormFieldValidator<>(component, target -> validateTarget(target, validator, message)));
    }

    @Override
    public <C extends JComponent> void addValidation(C component, Function<C, String> validator) {
        validators.add(new DBNFormFieldValidator<>(component, c -> validateTarget(validator, c)));
    }

    @Override
    public void addTextValidation(JTextComponent textField, Function<JTextComponent, String> validator) {
        addValidation(textField, validator);
        addValidationListeners(textField);
    }

    @Override
    public void addTextValidation(JTextComponent textField, Predicate<String> validator, String message) {
        addValidation(textField, f -> validator.test(f.getText()), message);
        addValidationListeners(textField);
    }

    private void addValidationListeners(JTextComponent textField) {
        if (HAS_VALIDATION_LISTENERS.is(textField)) return;
        HAS_VALIDATION_LISTENERS.set(textField, true);

        DBNDialog dialog = getTarget();

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

    public void validateInput(JComponent component) {
        DBNDialog dialog = getTarget();
        dialog.validateInput(component);

    }

    private static <C extends JComponent> void validateTarget(C target, Predicate<C> validator, String message) throws ValidationException {
        boolean valid = validator.test(target);
        if (!valid) throw new ValidationException(message);
    }


    private static <C extends JComponent> void validateTarget(Function<C, String> validator, C target) throws ValidationException {
        String error = validator.apply(target);
        if (error != null) throw new ValidationException(error);
    }

    /**
     * Validates the specified Swing components based on the registered validation rules
     * and returns a list of validation errors, if any. If no components are specified,
     * the method validates all components with associated validation rules.
     *
     * @param components the components to validate; if no components are provided, all registered components will be validated
     * @return a list of {@link ValidationInfo} instances representing validation errors; an empty list if all validations pass
     */
    @NotNull
    public List<ValidationInfo> buildValidationInfo(JComponent... components) {
        List<ValidationInfo> result = null;
        Set<JComponent> invalidFields = new HashSet<>();
        for (DBNFormFieldValidator<?> validator : validators) {
            JComponent target = validator.getTarget();
            if (invalidFields.contains(target)) continue;
            try {
                if (isEmpty(components) || isOneOf(target, components)) {
                    validator.validate();
                }

            } catch (ValidationException e) {
                invalidFields.add(target);

                String message = e.getMessage();
                ValidationInfo validationInfo = new ValidationInfo(message, target);

                result = nvl(result, () -> new ArrayList<>());
                result.add(validationInfo);
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
