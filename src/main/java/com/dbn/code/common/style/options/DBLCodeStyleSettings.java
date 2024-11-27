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

import com.dbn.common.options.CompositeConfiguration;
import com.dbn.common.options.Configuration;
import com.dbn.common.options.ui.CompositeConfigurationEditorForm;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.psi.codeStyle.CodeStyleConfigurable;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.options.setting.Settings.newElement;

@Getter
@EqualsAndHashCode(callSuper = false)
public abstract class DBLCodeStyleSettings<P extends DBLCodeStyleSettings, T extends CompositeConfigurationEditorForm>
        extends CompositeConfiguration<P, T> implements CodeStyleConfigurable {

    private final CodeStyleCaseSettings caseSettings = createCaseSettings(this);
    private final CodeStyleFormattingSettings formattingSettings = createAttributeSettings(this);

    protected DBLCodeStyleSettings(P parent) {
        super(parent);
    }

    protected abstract CodeStyleCaseSettings createCaseSettings(DBLCodeStyleSettings parent);
    protected abstract CodeStyleFormattingSettings createAttributeSettings(DBLCodeStyleSettings parent);

    @Override
    public void reset(@NotNull CodeStyleSettings settings) {

    }

    @Override
    public void apply(@NotNull CodeStyleSettings settings) throws ConfigurationException {

    }

    /*********************************************************
    *                     Configuration                     *
    *********************************************************/
    @Override
    protected Configuration[] createConfigurations() {
        return new Configuration[] {
                caseSettings,
                formattingSettings};
    }

    protected abstract String getElementName();

    @Override
    public void readConfiguration(Element element) {
        Element child = element.getChild(getElementName());
        if (child != null) {
            readConfiguration(child, caseSettings);
            readConfiguration(child, formattingSettings);
        }
    }

    @Override
    public void writeConfiguration(Element element) {
         Element child = newElement(element, getElementName());
         writeConfiguration(child, caseSettings);
         writeConfiguration(child, formattingSettings);
     }


}
