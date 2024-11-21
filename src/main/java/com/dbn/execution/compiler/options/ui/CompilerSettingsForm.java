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

package com.dbn.execution.compiler.options.ui;

import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.ui.Presentable;
import com.dbn.execution.compiler.CompileDependenciesOption;
import com.dbn.execution.compiler.CompileType;
import com.dbn.execution.compiler.options.CompilerSettings;
import com.dbn.nls.NlsResources;
import com.intellij.openapi.options.ConfigurationException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;

public class CompilerSettingsForm extends ConfigurationEditorForm<CompilerSettings> {
    private JPanel mainPanel;
    private JComboBox<CompileType> compileTypeComboBox;
    private JComboBox<CompileDependenciesOption> compileDependenciesComboBox;
    private JComboBox<ShowControlOption> showControlsComboBox;


    public CompilerSettingsForm(CompilerSettings settings) {
        super(settings);

        initComboBox(showControlsComboBox,
                ShowControlOption.ALWAYS,
                ShowControlOption.WHEN_INVALID);

        initComboBox(compileTypeComboBox,
                CompileType.NORMAL,
                CompileType.DEBUG,
                CompileType.KEEP,
                CompileType.ASK);

        initComboBox(compileDependenciesComboBox,
                CompileDependenciesOption.YES,
                CompileDependenciesOption.NO,
                CompileDependenciesOption.ASK);


        resetFormChanges();

        registerComponent(mainPanel);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        CompilerSettings settings = getConfiguration();
        settings.setCompileType(getSelection(compileTypeComboBox));
        settings.setCompileDependenciesOption(getSelection(compileDependenciesComboBox));
        ShowControlOption showControlOption = getSelection(showControlsComboBox);
        settings.setAlwaysShowCompilerControls(showControlOption != null && showControlOption.getValue());
    }

    @Override
    public void resetFormChanges() {
        CompilerSettings settings = getConfiguration();
        setSelection(compileTypeComboBox, settings.getCompileType());
        setSelection(compileDependenciesComboBox, settings.getCompileDependenciesOption());
        setSelection(showControlsComboBox,
                settings.isAlwaysShowCompilerControls() ?
                        ShowControlOption.ALWAYS:
                        ShowControlOption.WHEN_INVALID);
    }

    @Getter
    @AllArgsConstructor
    private enum ShowControlOption implements Presentable {
        ALWAYS(NlsResources.txt("cfg.compiler.const.ShowControlOption_ALWAYS"), true),
        WHEN_INVALID(NlsResources.txt("cfg.compiler.const.ShowControlOption_WHEN_INVALID"), false);

        private final String name;
        private final Boolean value;
    }
}
