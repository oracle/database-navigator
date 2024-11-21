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

package com.dbn.editor.code.options;

import com.dbn.common.options.BasicConfiguration;
import com.dbn.common.project.ProjectSupplier;
import com.dbn.editor.code.options.ui.CodeEditorGeneralSettingsForm;
import com.intellij.openapi.project.Project;
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
public class CodeEditorGeneralSettings
        extends BasicConfiguration<CodeEditorSettings, CodeEditorGeneralSettingsForm>
        implements ProjectSupplier {

    private boolean showObjectsNavigationGutter = false;
    private boolean showSpecDeclarationNavigationGutter = true;
    private boolean enableSpellchecking = true;
    private boolean enableReferenceSpellchecking = false;

    CodeEditorGeneralSettings(CodeEditorSettings parent) {
        super(parent);
    }

    @Override
    public String getDisplayName() {
        return txt("cfg.codeEditor.title.GeneralSettings");
    }

    @Override
    public String getHelpTopic() {
        return "codeEditor";
    }

    @NotNull
    public Project getProject() {
        return getParent().getProject();
    }

    /****************************************************
     *                   Configuration                  *
     ****************************************************/
    @Override
    @NotNull
    public CodeEditorGeneralSettingsForm createConfigurationEditor() {
        return new CodeEditorGeneralSettingsForm(this);
    }

    @Override
    public String getConfigElementName() {
        return "general";
    }

    @Override
    public void readConfiguration(Element element) {
        showObjectsNavigationGutter = getBoolean(element, "show-object-navigation-gutter", showObjectsNavigationGutter);
        showSpecDeclarationNavigationGutter = getBoolean(element, "show-spec-declaration-navigation-gutter", showSpecDeclarationNavigationGutter);
        enableSpellchecking = getBoolean(element, "enable-spellchecking", enableSpellchecking);
        enableReferenceSpellchecking = getBoolean(element, "enable-reference-spellchecking", enableReferenceSpellchecking);
    }

    @Override
    public void writeConfiguration(Element element) {
        setBoolean(element, "show-object-navigation-gutter", showObjectsNavigationGutter);
        setBoolean(element, "show-spec-declaration-navigation-gutter", showSpecDeclarationNavigationGutter);
        setBoolean(element, "enable-spellchecking", enableSpellchecking);
        setBoolean(element, "enable-reference-spellchecking", enableReferenceSpellchecking);
    }
}
