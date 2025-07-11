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

import com.dbn.common.icon.Icons;
import com.dbn.common.util.Commons;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nls;

import javax.swing.Icon;

@Getter
@Setter
public abstract class OptionBrokerBase<T> implements OptionBroker<T> {
    private final String configName;
    private final @Nls String title;
    private final @Nls String message;

    private Icon icon = Icons.DIALOG_QUESTION;
    private String doNotShowMessage = "Do not ask again";

    private final T defaultOption;
    private T selectedOption;

    public OptionBrokerBase(String configName, String title, String message, T defaultOption) {
        this.configName = configName;
        this.title = title;
        this.message = message;
        this.defaultOption = defaultOption;
    }

    @Override
    public OptionBroker<T> withIcon(Icon icon) {
        this.icon = icon;
        return this;
    }

    @Override
    public OptionBroker<T> withDoNotShowMessage(@Nls String doNotShowMessage) {
        this.doNotShowMessage = doNotShowMessage;
        return this;
    }

    public T getOption() {
        return Commons.nvl(selectedOption, defaultOption);
    }

    @Override
    public boolean canBeHidden() {
        return true;
    }

    @Override
    public boolean shouldSaveOptionsOnCancel() {
        return false;
    }
}
