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
import com.dbn.common.ui.table.Tables;
import com.dbn.common.ui.util.Lists;
import com.dbn.common.ui.util.TextFields;
import com.dbn.common.util.Strings;
import com.intellij.openapi.ui.ValidationInfo;
import org.jetbrains.annotations.NotNull;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusEvent.Cause;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.dbn.common.ui.util.ClientProperty.HAS_VALIDATION_LISTENERS;
import static com.dbn.common.ui.util.ClientProperty.LOADING;
import static com.dbn.common.ui.util.ClientProperty.VALIDATION_INFO;
import static com.dbn.common.ui.util.ClientProperty.VISITED;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.util.Commons.isEmpty;
import static com.dbn.common.util.Commons.isOneOf;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public final class DBNFormValidatorImpl extends WeakRefWrapper<DBNDialog> implements DBNFormValidator {
    private final List<WrappedValidator<?>> validators = new ArrayList<>();

    public DBNFormValidatorImpl(DBNDialog dialog) {
        super(dialog);
    }

    public DBNDialog getDialog() {
        return getTarget();
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
        addValidation(textField, f -> validator.test(getText(f)), message);
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

        } else if (component instanceof CheckBoxList) {
            CheckBoxList checkBoxList = (CheckBoxList) component;
            addValidationListeners(checkBoxList);

        } else if (component instanceof JTable) {
            JTable table = (JTable) component;
            addValidationListeners(table);

        } else if (component instanceof JList) {
            JList list = (JList) component;
            addValidationListeners(list);

        } else if (component instanceof JComboBox) {
            JComboBox comboBox = (JComboBox) component;
            addValidationListeners(comboBox);

        } else if (component instanceof JSpinner) {
            JSpinner spinner = (JSpinner) component;
            addValidationListeners(spinner);
        }
        // ...
    }

    private void addValidationListeners(JTable table) {
        addListener(table, c -> Tables.onModelChange(c, e -> validateInput(c)));
    }

    private void addValidationListeners(JList list) {
        addListener(list, c -> Lists.onModelChange(c, e -> validateInput(c)));
    }

    private void addValidationListeners(JTextComponent textField) {
        // add document-listener to perform validation on text change and enable / disable dialog button
        addListener(textField, c -> onTextChange(c, e ->  validateInput(c)));
    }

    private void addValidationListeners(JComboBox comboBox) {
        // add item listener to perform validation on selection change
        addListener(comboBox, c -> c.addItemListener(e -> validateInput(c)));
    }

    private void addValidationListeners(CheckBoxList checkBoxList) {
        addListener(checkBoxList, l -> l.addActionListener(e -> validateInput(l)));
    }

    private void addValidationListeners(JSpinner spinner) {
        // add item listener to perform validation on selection change
        addListener(spinner, c -> onTextChange(c, e -> validateInput(c)));
    }

    private <T extends JComponent> void addListener(T component, Consumer<T> listener) {
        if (HAS_VALIDATION_LISTENERS.is(component)) return;
        HAS_VALIDATION_LISTENERS.set(component, true);

        listener.accept(component);

        // add focus listener to perform validation when focus is gained or lost
        addFocusValidationListeners(component);
        addVisibilityChangeListener(component);
    }

    private void addFocusValidationListeners(JComponent component) {
        JComponent focusComponent = component;
        if (component instanceof JSpinner) {
            JSpinner spinner = (JSpinner) component;
            focusComponent = TextFields.getTextField(spinner);
            if (focusComponent == null) focusComponent = spinner;
        }

        focusComponent.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                // ignore temporary focus loss events (e.g. JComboBox losing focus in favor of the popup)
                if (e.isTemporary()) return;

                Cause focusCause = e.getCause();
                if (focusCause == Cause.UNKNOWN) return;
                if (focusCause == Cause.ACTIVATION) return;

                Component oppositeComponent = e.getOppositeComponent();
                if (oppositeComponent instanceof JButton) {
                    // ignore validation if cancel button is pressed
                    JButton button = (JButton) oppositeComponent;

                    DBNDialog dialog = getDialog();
                    if (dialog.isCancelButton(button) && focusCause == Cause.MOUSE_EVENT) return;
                }

                VISITED.set(component, true);
                validateInput(component);
            }
        });
    }

    private static void addVisibilityChangeListener(JComponent component) {
        // reset the VISITED flag when component visibility changes
        component.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                VISITED.reset(component);
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                VISITED.reset(component);
            }
        });
    }


    public void validateInput(JComponent component) {
        DBNDialog dialog = getTarget();
        dialog.validateInput(component);
    }

    private static <C extends JComponent> List<ValidationInfo> validateTarget(C target, Predicate<C> validator, String message) {
        boolean valid = validator.test(target);
        if (valid) {
            resetInfo(target);
            return emptyList();
        } else {
            ValidationInfo info = new ValidationInfo(message, target);
            recordInfo(target, info);
            return singletonList(info);
        }
    }

    private static <C extends JComponent> List<ValidationInfo> validateTarget(Function<C, String> validator, C target) {
        String error = validator.apply(target);
        if (error == null) {
            resetInfo(target);
            return emptyList();
        } else {
            ValidationInfo info = new ValidationInfo(error, target);
            recordInfo(target, info);
            return singletonList(info);
        }
    }

    private static <C extends JComponent> void resetInfo(C target) {
        VALIDATION_INFO.reset(target);
    }

    private static <C extends JComponent> void recordInfo(C target, ValidationInfo info) {
        VALIDATION_INFO.set(target, info);
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
            if (!shouldValidate(target)) {
                VALIDATION_INFO.reset(target);
                continue;
            }

            // prevent multiple validation issues on same field (e.g. "empty value" and "invalid value pattern")
            if (invalidFields.contains(target)) continue;

            if (isEmpty(components) || isOneOf(target, components)) {
                List<ValidationInfo> infos = validator.validate();
                if (infos.isEmpty()) continue;

                invalidFields.add(target);
                result.addAll(infos);
            } else {
                // restore available infos from the out-of-scope components
                ValidationInfo info = VALIDATION_INFO.get(target);
                if (info != null) {
                    invalidFields.add(target);
                    result.add(info);
                }
            }
        }

        return result;
    }

    private static boolean shouldValidate(JComponent component) {
        if (!component.isShowing()) return false; // skip conditionally hidden components
        if (!component.isEnabled()) return false; // skip disabled fields
        if (LOADING.is(component)) return false; // skip loading components
        return true;
    }

    public boolean isVisitedField(JComponent component) {
        if (component == null) return false;
        if (VISITED.is(component)) return true;

        if (component instanceof JTextComponent) {
            JTextComponent textComponent = (JTextComponent) component;
            return Strings.isNotEmptyOrSpaces(getText(textComponent));
        }

        if (component instanceof JComboBox) {
            JComboBox comboBox = (JComboBox) component;
            return comboBox.getSelectedItem() != null;
        }

        if (component instanceof CheckBoxList) {
            CheckBoxList checkBoxList = (CheckBoxList) component;
            return checkBoxList.hasSelection();
        }
        return false;
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
