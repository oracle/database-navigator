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

package com.dbn.code.common.style.options.ui;

import com.dbn.code.common.style.options.ProjectCodeStyleSettings;
import com.dbn.common.icon.Icons;
import com.dbn.common.options.Configuration;
import com.dbn.common.options.ui.CompositeConfigurationEditorForm;
import com.dbn.common.ui.tab.DBNTabbedPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;

public class CodeStyleSettingsForm extends CompositeConfigurationEditorForm<ProjectCodeStyleSettings> {
    private JPanel mainPanel;
    private final DBNTabbedPane languageTabs;

    public CodeStyleSettingsForm(ProjectCodeStyleSettings settings) {
        super(settings);
        languageTabs = new DBNTabbedPane(this);
        //languageTabs.setAdjustBorders(false);
        mainPanel.add(languageTabs, BorderLayout.CENTER);
        addSettingsPanel(settings.getSQLCodeStyleSettings(), Icons.FILE_SQL);
        addSettingsPanel(settings.getPSQLCodeStyleSettings(), Icons.FILE_PLSQL);
    }

    private void addSettingsPanel(Configuration configuration, Icon icon) {
        JComponent component = configuration.createComponent();
        languageTabs.addTab(configuration.getDisplayName(), icon, component);
    }


    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
