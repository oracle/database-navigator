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


import com.dbn.common.options.setting.Settings;
import com.dbn.common.routine.Consumer;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.util.Messages;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

import static com.dbn.common.util.Modality.nonModal;

@Getter
@Setter
public class InteractiveOptionBroker<T extends InteractiveOption> extends OptionBrokerBase<T>{
    private T lastUsedOption;
    private final List<T> options;

    @SafeVarargs
    public InteractiveOptionBroker(
            @NonNls String configName,
            @Nls String title,
            @Nls String message,
            @NotNull T defaultOption,
            T... options) {
        super(configName, title, message, defaultOption);
        this.options = Arrays.asList(options);
        setDoNotShowMessage("Remember option");
    }

    @Override
    public boolean isToBeShown() {
        return true;
    }

    @Override
    public void setToBeShown(boolean keepAsking, int selectedIndex) {
        T selectedOption = getOption(selectedIndex);
        if (keepAsking || selectedOption.isAsk() || selectedOption.isCancel()) {
            setSelectedOption(null);
        } else {
            setSelectedOption(selectedOption);
        }
    }

    @Override
    protected boolean canSelectOption(T option) {
        return !option.isCancel();
    }

    public void resolve(Project project, Object[] messageArgs, Consumer<T> consumer) {
        Dispatch.run(nonModal(), () -> doResolve(project, messageArgs, consumer));
    }

    private void doResolve(Project project, Object[] messageArgs, Consumer<T> consumer) {
        T option;
        T selectedOption = getSelectedOption();
        if (selectedOption != null && !selectedOption.isAsk()) {
            option = selectedOption;
        } else {
            int lastUsedOptionIndex = 0;
            if (lastUsedOption != null) {
                lastUsedOptionIndex = options.indexOf(lastUsedOption);
            }

            int optionIndex = Messages.showDialog(
                    project,
                    txt(getMessage(), messageArgs),
                    txt(getTitle()),
                    toStringOptions(options),
                    lastUsedOptionIndex,
                    getIcon(), this);

            option = getOption(optionIndex);
            if (!option.isCancel() && !option.isAsk()) {
                lastUsedOption = option;
            }
        }
        consumer.accept(option);
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
        T option = (T) Settings.getEnum(element, getConfigName(), (Enum) getDefaultOption());
        selectOption(option);
    }

    @Override
    public void writeConfiguration(Element element) {
        Settings.setEnum(element, getConfigName(), (Enum) getOption());
    }
}
