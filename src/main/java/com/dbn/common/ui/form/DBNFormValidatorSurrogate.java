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

import com.dbn.common.ui.list.CheckBoxList;
import com.intellij.openapi.ui.ValidationInfo;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nls;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.text.JTextComponent;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The DBNFormValidatorSurrogate class is an implementation of the DBNFormValidator interface.
 * This surrogate class provides a no-op implementation for all methods and is typically used
 * as a placeholder or default implementation when no actual validation logic is required.
 * Any attempt to register validators to this surrogate will be logged as invalid state.
 *
 * @author Dan Cioca (Oracle)
 */
@Slf4j
public class DBNFormValidatorSurrogate implements DBNFormValidator{
    @Override
    public boolean hasValidators() {
        return false;
    }

    @Override
    public <C extends JComponent> boolean hasValidators(C component) {
        return false;
    }

    @Override
    public <C extends JComponent> void removeValidators(C component) {
        notSupported();
    }

    @Override
    public <C extends JComponent> void addValidation(C component, Predicate<C> validator, @Nls String message) {
        notSupported();
    }

    @Override
    public <C extends JComponent> void addValidation(C component, Function<C, String> validator) {
        notSupported();
    }

    @Override
    public <C extends JComponent> void addValidator(C component, Function<C, List<ValidationInfo>> validator) {
        notSupported();
    }

    @Override
    public void addTextValidation(JTextComponent textField, Predicate<String> validator, @Nls String message) {
        notSupported();
    }

    @Override
    public void addSelectionValidation(JComboBox comboBox, @Nls String message) {
        notSupported();
    }

    @Override
    public void addSelectionValidation(CheckBoxList checkBoxList, @Nls String message) {
        notSupported();
    }

    @Override
    public void addTextValidation(JTextComponent textField, Function<JTextComponent, String> validator) {
        notSupported();
    }

    @Override
    public void validateInput(JComponent component) {}

    private static void notSupported() {
        log.error("Failed to initialize validator", new IllegalStateException("Form validator not bound to dialog. Validation not supported."));
    }

    @Override
    public boolean isVisitedField(JComponent component) {
        return false;
    }
}
