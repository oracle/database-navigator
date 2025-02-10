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

import com.intellij.openapi.ui.ValidationInfo;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.text.JTextComponent;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The DBNFormValidator interface defines methods for adding validation logic to swing components
 * and performing validation checks on forms consisting of multiple components.
 * The validations are used to ensure form inputs adhere to given constraints.
 */
public interface DBNFormValidator {
    DBNFormValidator SURROGATE = new DBNFormValidatorSurrogate();

    /**
     * Adds a validation rule to a specified Swing component. The validation rule
     * is defined by a predicate that evaluates the component's state, along with
     * an associated error message to display if the validation fails.
     *
     * @param <C>        the type of the Swing component being validated, which must extend {@link JComponent}.
     * @param component  the Swing component to which the validation rule will be applied.
     * @param validator  a {@link Predicate} that evaluates the validity of the component's state.
     *                   It returns {@code true} if the component is valid, and {@code false} otherwise.
     * @param message    the error message to display if the validation fails.
     */
    <C extends JComponent> void addValidation(C component, Predicate<C> validator, String message);

    <C extends JComponent> void addValidation(C component, Function<C, String> validator);

    /**
     * Adds a text validation rule to a specified JTextComponent. The validation rule
     * is defined by a predicate that evaluates the validity of the text input, along with
     * an associated error message to display if the validation fails.
     *
     * @param textField the text field component to which the validation rule will be applied
     * @param validator a {@link Predicate} that evaluates the validity of the text input.
     *                  It returns {@code true} if the input is valid, and {@code false} otherwise
     * @param message   the error message to display if the validation fails
     */
    void addTextValidation(JTextComponent textField, Predicate<String> validator, String message);

    void addTextValidation(JTextComponent textField, Function<JTextComponent, String> validator);

    /**
     * Validates the specified Swing components based on the registered validation rules
     * and returns a list of validation errors, if any. If no components are specified,
     * the method validates all components with associated validation rules.
     *
     * @param components the components to validate; if no components are provided, all registered components will be validated
     * @return a list of {@link ValidationInfo} instances representing validation errors; an empty list if all validations pass
     */
    @NotNull
    List<ValidationInfo> validateForm(JComponent... components);
}
