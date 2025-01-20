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

package com.dbn.code.common.style.options;

import com.dbn.code.common.style.options.ui.CodeStyleCaseSettingsForm;
import com.dbn.common.options.BasicConfiguration;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setBooleanAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public abstract class CodeStyleCaseSettings extends BasicConfiguration<DBLCodeStyleSettings, CodeStyleCaseSettingsForm> {
    private final Map<String, CodeStyleCaseOption> options = new LinkedHashMap<>();
    private boolean enabled = true;

    public CodeStyleCaseSettings(DBLCodeStyleSettings parent) {
        super(parent);
        addOption("KEYWORD_CASE", CodeStyleCase.LOWER, false);
        addOption("FUNCTION_CASE", CodeStyleCase.LOWER, false);
        addOption("PARAMETER_CASE", CodeStyleCase.LOWER, false);
        addOption("DATATYPE_CASE", CodeStyleCase.LOWER, false);
        addOption("OBJECT_CASE", CodeStyleCase.PRESERVE, true);
    }

    private void addOption(@NonNls String id, CodeStyleCase option, boolean ignoreMixedCase) {
        options.put(id, new CodeStyleCaseOption(id, option, ignoreMixedCase));
    }


    @Override
    public String getDisplayName() {
        return txt("cfg.codeStyle.title.CaseOptions");
    }

    public CodeStyleCaseOption getKeywordCaseOption() {
        return getCodeStyleCaseOption("KEYWORD_CASE");
    }

    public CodeStyleCaseOption getFunctionCaseOption() {
        return getCodeStyleCaseOption("FUNCTION_CASE");
    }

    public CodeStyleCaseOption getParameterCaseOption() {
        return getCodeStyleCaseOption("PARAMETER_CASE");
    }

    public CodeStyleCaseOption getDatatypeCaseOption() {
        return getCodeStyleCaseOption("DATATYPE_CASE");
    }


    public CodeStyleCaseOption getObjectCaseOption() {
        return getCodeStyleCaseOption("OBJECT_CASE");
    }

    private CodeStyleCaseOption getCodeStyleCaseOption(@NonNls String name) {
        return options.get(name);
    }

    /*********************************************************
     *                     Configuration                     *
     *********************************************************/
    @Override
    @NotNull
    public CodeStyleCaseSettingsForm createConfigurationEditor() {
        return new CodeStyleCaseSettingsForm(this);
    }

    @Override
    public String getConfigElementName() {
        return "case-options";
    }

    @Override
    public void readConfiguration(Element element) {
        enabled = booleanAttribute(element, "enabled", enabled);
        for (Element child : element.getChildren()) {
            String name = stringAttribute(child, "name");
            CodeStyleCaseOption option = getCodeStyleCaseOption(name);
            if (option != null) {
                option.readConfiguration(child);
            }
        }
    }

    @Override
    public void writeConfiguration(Element element) {
        setBooleanAttribute(element, "enabled", enabled);
        for (CodeStyleCaseOption option : options.values()) {
            Element optionElement = newElement(element,"option");
            option.writeConfiguration(optionElement);
        }
    }
}
