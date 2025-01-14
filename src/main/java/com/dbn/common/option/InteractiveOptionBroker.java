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

package com.dbn.common.option;


import com.dbn.common.icon.Icons;
import com.dbn.common.options.PersistentConfiguration;
import com.dbn.common.options.setting.Settings;
import com.dbn.common.routine.Consumer;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Messages;
import com.intellij.openapi.project.Project;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode
public class InteractiveOptionBroker<T extends InteractiveOption> implements DoNotAskOption, PersistentConfiguration{
    private final String configName;
    private final @Nls String title;
    private final @Nls String message;
    private final T defaultOption;
    private T selectedOption;
    private T lastUsedOption;
    private final List<T> options;

    @SafeVarargs
    public InteractiveOptionBroker(
            String configName,
            @Nls String title,
            @Nls String message,
            @NotNull T defaultOption,
            T... options) {
        this.configName = configName;
        this.title = title;
        this.message = message;
        this.options = Arrays.asList(options);
        this.defaultOption = defaultOption;
    }

    @Override
    public boolean isToBeShown() {
        return true;
    }

    @Override
    public void setToBeShown(boolean keepAsking, int selectedIndex) {
        T selectedOption = getOption(selectedIndex);
        if (keepAsking || selectedOption.isAsk() || selectedOption.isCancel()) {
            this.selectedOption = null;
        } else {
            this.selectedOption = selectedOption;
        }
    }

    public void set(T selectedOption) {
        assert !selectedOption.isCancel();
        this.selectedOption = selectedOption;
    }

    @NotNull
    public T get() {
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

    @NotNull
    @Override
    public String getDoNotShowMessage() {
        return "Remember option";
    }

    public void resolve(Project project, Object[] messageArgs, Consumer<T> consumer) {
        Dispatch.run(() -> {
            T option;
            if (selectedOption != null && !selectedOption.isAsk()) {
                option = selectedOption;
            } else {
                int lastUsedOptionIndex = 0;
                if (lastUsedOption != null) {
                    lastUsedOptionIndex = options.indexOf(lastUsedOption);
                }

                int optionIndex = Messages.showDialog(
                        project,
                        txt(message, messageArgs),
                        txt(title),
                        toStringOptions(options),
                        lastUsedOptionIndex, Icons.DIALOG_QUESTION, this);

                option = getOption(optionIndex);
                if (!option.isCancel() && !option.isAsk()) {
                    lastUsedOption = option;
                }
            }
            if (option != null) {
                consumer.accept(option);
            }
        });
    }

    @NotNull
    private T getOption(int index) {
        return index == -1 ? options.get(options.size() -1) : options.get(index);
    }

    public static String[] toStringOptions(List<? extends InteractiveOption> options) {
        String[] stringOptions = new String[options.size()];
        for (int i = 0; i < options.size(); i++) {
            stringOptions[i] = options.get(i).getName();
        }
        return stringOptions;
    }


    /*******************************************************
     *              PersistentConfiguration                *
     *******************************************************/
    @Override
    public void readConfiguration(Element element) {
        T option = (T) Settings.getEnum(element, configName, (Enum)defaultOption);
        set(option);
    }

    @Override
    public void writeConfiguration(Element element) {
        Settings.setEnum(element, configName, (Enum) get());
    }
}
