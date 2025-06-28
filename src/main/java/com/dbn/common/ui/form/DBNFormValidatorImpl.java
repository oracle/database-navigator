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
import com.dbn.common.ui.list.CheckBoxList;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.DocumentAdapter;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.event.DocumentEvent;
import javax.swing.text.JTextComponent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.dbn.common.ui.util.ClientProperty.HAS_VALIDATION_LISTENERS;
import static com.dbn.common.ui.util.ClientProperty.VISITED;
import static com.dbn.common.util.Commons.isEmpty;
import static com.dbn.common.util.Commons.isOneOf;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public final class DBNFormValidatorImpl extends WeakRefWrapper<DBNDialog> implements DBNFormValidator {
    private final List<WrappedValidator<?>> validators = new ArrayList<>();

    public DBNFormValidatorImpl(DBNDialog dialog) {
        super(dialog);
    }

    @Override
    public boolean hasValidators() {
        return !validators.isEmpty();
    }

    @Override
    public <C extends JComponent> boolean hasValidators(C component) {
        if (validators.isEmpty()) return false;
        if (component == null) return true; // at least one validator given above

        return validators.stream().anyMatch(v -> v.getTarget() == component);
    }

    public <C extends JComponent> void removeValidators(C component) {
        validators.removeIf(v -> v.getTarget() == component);
    }

    @Override
    public <C extends JComponent> void addValidator(C component, Function<C, List<ValidationInfo>> validator) {
        validators.add(new WrappedValidator<>(component, validator));
        initEventValidation(component);
    }

    @Override
    public <C extends JComponent> void addValidation(C component, Predicate<C> validator, String message) {
        addValidator(component, target -> validateTarget(target, validator, message));
    }

    @Override
    public <C extends JComponent> void addValidation(C component, Function<C, String> validator) {
        addValidator(component, c -> validateTarget(validator, c));
    }

    @Override
    public void addTextValidation(JTextComponent textField, Function<JTextComponent, String> validator) {
        addValidation(textField, validator);
    }

    @Override
    public void addTextValidation(JTextComponent textField, Predicate<String> validator, String message) {
        addValidation(textField, f -> validator.test(f.getText()), message);
    }

    @Override
    public void addSelectionValidation(JComboBox comboBox, String message) {
        addValidation(comboBox, c -> c.getSelectedItem() != null, message);
    }

    @Override
    public void addSelectionValidation(CheckBoxList checkBoxList, String message) {
        addValidation(checkBoxList, l -> l.hasSelection(), message);
    }

    private <C extends JComponent> void initEventValidation(C component) {
        if (component instanceof JTextComponent) {
            JTextComponent textField = (JTextComponent) component;
            addValidationListeners(textField);
        } else if (component instanceof JTable) {
            JTable table = (JTable) component;
            addValidationListeners(table);
        } else if (component instanceof JComboBox) {
            JComboBox comboBox = (JComboBox) component;
            addValidationListeners(comboBox);
        } else if (component instanceof CheckBoxList) {
            CheckBoxList checkBoxList = (CheckBoxList) component;
            addValidationListeners(checkBoxList);
        }
        // ...
    }

    private void addValidationListeners(JTable table) {
        table.getModel().addTableModelListener(e -> {
            validateInput(table);
        });
    }

    private void addValidationListeners(JTextComponent textField) {
        if (HAS_VALIDATION_LISTENERS.is(textField)) return;
        HAS_VALIDATION_LISTENERS.set(textField, true);

        // add document listener to perform validation on text change and enable / disable dialog button
        textField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                validateInput(textField);
            }
        });

        // add focus listener to perform validation when focus is gained or lost
        addFocusValidationListeners(textField);
    }

    private void addValidationListeners(JComboBox comboBox) {
        if (HAS_VALIDATION_LISTENERS.is(comboBox)) return;
        HAS_VALIDATION_LISTENERS.set(comboBox, true);

        // add action listener to perform validation on selection change
        comboBox.addActionListener(e -> validateInput(comboBox));

        // add focus listener to perform validation when focus is gained or lost
        addFocusValidationListeners(comboBox);
    }

    private void addValidationListeners(CheckBoxList checkBoxList) {
        if (HAS_VALIDATION_LISTENERS.is(checkBoxList)) return;
        HAS_VALIDATION_LISTENERS.set(checkBoxList, true);

        checkBoxList.addActionListener(e -> validateInput(checkBoxList));

        addFocusValidationListeners(checkBoxList);
    }

    private void addFocusValidationListeners(JComponent component) {
        component.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (VISITED.isNot(component)) {
                    VISITED.set(component, true);
                } else {
                    validateInput(component);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (e.isTemporary()) return; // ignore temporary focus loss events (e.g. JCheckBox losing focus in favor of the popup)
                validateInput(component);
            }
        });
    }

    public void validateInput(JComponent component) {
        DBNDialog dialog = getTarget();
        dialog.validateInput(component);

    }

    private static <C extends JComponent> List<ValidationInfo> validateTarget(C target, Predicate<C> validator, String message) {
        boolean valid = validator.test(target);
        if (valid) return emptyList();
        
        ValidationInfo info = new ValidationInfo(message, target);
        return singletonList(info);
    }


    private static <C extends JComponent> List<ValidationInfo> validateTarget(Function<C, String> validator, C target) {
        String error = validator.apply(target);
        if (error == null) return emptyList();

        ValidationInfo info = new ValidationInfo(error, target);
        return singletonList(info);
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
        List<ValidationInfo> result = new ArrayList<>();
        Set<JComponent> invalidFields = new HashSet<>();
        for (WrappedValidator<?> validator : validators) {
            JComponent target = validator.getTarget();

            // prevent multiple validation issues on same field (e.g. "empty value" and "invalid value pattern")
            if (invalidFields.contains(target)) continue;

            if (isEmpty(components) || isOneOf(target, components)) {
                List<ValidationInfo> infos = validator.validate();
                if (infos.isEmpty()) continue;

                invalidFields.add(target);
                result.addAll(infos);
            }
        }

        return result;
    }

    private static class WrappedValidator<T extends JComponent> extends WeakRefWrapper<T> {
        private final Function<T, List<ValidationInfo>> validator;


        WrappedValidator(@NotNull T target, Function<T, List<ValidationInfo>> validator) {
            super(target);
            this.validator = validator;
        }

        List<ValidationInfo> validate() {
            T target = getTarget();
            return validator.apply(target);
        }
    }
}
