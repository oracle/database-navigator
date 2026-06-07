/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.common.option;

import com.dbn.common.options.PersistentConfiguration;
import com.dbn.common.routine.Consumer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts.Checkbox;

import javax.swing.Icon;

public interface OptionBroker<T> extends RememberOption, PersistentConfiguration {
    void resolve(Project project, Object[] messageArgs, Consumer<T> consumer);

    OptionBroker<T> withIcon(Icon icon);

    OptionBroker<T> withDoNotShowMessage(@Checkbox String doNotShowMessage);

    T getSelectedOption();

    void selectOption(T option);
}
