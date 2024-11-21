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

package com.dbn.debugger.common.settings;

import com.dbn.debugger.common.settings.ui.DBProgramDebuggerSettingsForm;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;

import javax.swing.Icon;
import javax.swing.JComponent;

import static com.dbn.common.dispose.Disposer.replace;

public class DBProgramDebuggerConfigurable implements Configurable {

    private transient DBProgramDebuggerSettingsForm component;

    @Override
    @Nls
    public String getDisplayName() {
        return "Database";
    }

    public Icon getIcon() {
        return null;
    }

    @Override
    public String getHelpTopic() {
        return null;
    }

    @Override
    public JComponent createComponent() {
        component = new DBProgramDebuggerSettingsForm();
        return component.getComponent();
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {
    }

    @Override
    public void reset() {
    }

    @Override
    public void disposeUIResources() {
        component = replace(component, null);
    }
}
