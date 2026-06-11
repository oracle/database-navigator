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
import com.intellij.openapi.util.NlsContexts.Checkbox;
import com.intellij.openapi.util.NlsContexts.DialogMessage;
import com.intellij.openapi.util.NlsContexts.DialogTitle;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;

import javax.swing.Icon;

import static com.dbn.common.util.Commons.nvl;
import static com.dbn.nls.NlsResources.txt;

@Getter
@Setter
@Slf4j
public abstract class OptionBrokerBase<T> implements OptionBroker<T> {
    private final @NonNls String configName;
    private final @DialogTitle String title;
    private final @DialogMessage String message;

    private Icon icon = Icons.DIALOG_QUESTION;
    private @Checkbox String doNotShowMessage = txt("msg.shared.option.DoNotAskAgain");

    private final T defaultOption;
    private T selectedOption;

    public OptionBrokerBase(
            @NonNls String configName,
            @DialogTitle String title,
            @DialogMessage String message,
            T defaultOption) {
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
    public OptionBroker<T> withDoNotShowMessage(@Checkbox String doNotShowMessage) {
        this.doNotShowMessage = doNotShowMessage;
        return this;
    }

    public T getOption() {
        return nvl(selectedOption, defaultOption);
    }

    public void selectOption(T option) {
        if (canSelectOption(option)) {
            this.selectedOption = option;
        } else {
            log.error("Cannot select option: {}", option);
        }
    }

    @Override
    public boolean canBeHidden() {
        return true;
    }

    @Override
    public boolean shouldSaveOptionsOnCancel() {
        // index of "cancel" option may not match the DialogWrapper#CANCEL_EXIT_CODE
        return true;
    }

    protected abstract boolean canSelectOption(T option);
}
