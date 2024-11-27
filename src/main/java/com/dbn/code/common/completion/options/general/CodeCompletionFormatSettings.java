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

package com.dbn.code.common.completion.options.general;

import com.dbn.code.common.completion.options.CodeCompletionSettings;
import com.dbn.code.common.completion.options.general.ui.CodeCompletionFormatSettingsForm;
import com.dbn.common.options.BasicConfiguration;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.options.setting.Settings.getBoolean;
import static com.dbn.common.options.setting.Settings.setBoolean;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class CodeCompletionFormatSettings extends BasicConfiguration<CodeCompletionSettings, CodeCompletionFormatSettingsForm> {
    private boolean enforceCodeStyleCase = true;

    public CodeCompletionFormatSettings(CodeCompletionSettings parent) {
        super(parent);
    }

    /****************************************************
     *                   Configuration                  *
     ****************************************************/
   @Override
   @NotNull
   public CodeCompletionFormatSettingsForm createConfigurationEditor() {
       return new CodeCompletionFormatSettingsForm(this);
   }

    @Override
    public String getConfigElementName() {
        return "format";
    }

    @Override
    public void readConfiguration(Element element) {
        enforceCodeStyleCase = getBoolean(element, "enforce-code-style-case", enforceCodeStyleCase);
    }

    @Override
    public void writeConfiguration(Element element) {
        setBoolean(element, "enforce-code-style-case", enforceCodeStyleCase);
    }

}
