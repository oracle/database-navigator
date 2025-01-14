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

package com.dbn.common.options;

import com.dbn.common.action.Lookups;
import com.dbn.common.compatibility.Compatibility;
import com.dbn.common.options.setting.Settings;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.project.ProjectSupplier;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.dispose.Checks.isValid;

@Slf4j
public abstract class AbstractConfiguration<P extends Configuration, E extends ConfigurationEditorForm> implements Configuration<P, E> {

    /*****************************************************************
     *                         DOM utilities                         *
     ****************************************************************/
    protected void writeConfiguration(Element element, Configuration configuration) {
        String elementName = configuration.getConfigElementName();
        if (elementName == null) return;

        Element childElement = Settings.newElement(element, elementName);
        configuration.writeConfiguration(childElement);
    }


    protected void readConfiguration(Element element, Configuration configuration) {
        if (element == null) return;

        String elementName = configuration.getConfigElementName();
        if (elementName == null) return;

        Element childElement = element.getChild(elementName);
        if (childElement == null) return;

        configuration.readConfiguration(childElement);
    }

    @NotNull
    public E createConfigurationEditor() {
        throw new UnsupportedOperationException();
    };

    @Nullable
    @Compatibility
    //@Override
    public JComponent getPreferredFocusedComponent() {
        E settingsEditor = getSettingsEditor();
        if (isNotValid(settingsEditor)) return null;

        return settingsEditor.getPreferredFocusedComponent();
    }

    @Override
    public final Project resolveProject() {
        if (this instanceof ProjectSupplier) {
            ProjectSupplier projectSupplier = (ProjectSupplier) this;
            Project project = projectSupplier.getProject();
            if (project != null) {
                return project;
            }
        }

        Configuration parent = this.getParent();
        if (parent != null) {
            Project project = parent.resolveProject();
            if (project != null) {
                return project;
            }
        }

        ConfigurationEditorForm settingsEditor = this.getSettingsEditor();
        if (isValid(settingsEditor)) {
            return Lookups.getProject(settingsEditor.getComponent());
        }
        return null;
    }
}
