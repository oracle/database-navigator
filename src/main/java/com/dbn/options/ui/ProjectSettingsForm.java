/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.options.ui;

import com.dbn.assistant.settings.AssistantSettings;
import com.dbn.browser.options.DatabaseBrowserSettings;
import com.dbn.code.common.completion.options.CodeCompletionSettings;
import com.dbn.common.options.Configuration;
import com.dbn.common.options.ui.CompositeConfigurationEditorForm;
import com.dbn.common.ui.list.ColoredListCellRenderer;
import com.dbn.common.ui.util.Fonts;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.config.ConnectionBundleSettings;
import com.dbn.connection.config.ui.ConnectionBundleSettingsForm;
import com.dbn.connection.operation.options.OperationSettings;
import com.dbn.data.grid.options.DataGridSettings;
import com.dbn.ddl.options.DDLFileSettings;
import com.dbn.editor.code.options.CodeEditorSettings;
import com.dbn.editor.data.options.DataEditorSettings;
import com.dbn.execution.common.options.ExecutionEngineSettings;
import com.dbn.navigation.options.NavigationSettings;
import com.dbn.options.ConfigId;
import com.dbn.options.ProjectSettings;
import com.dbn.options.general.GeneralProjectSettings;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.breadcrumbs.Breadcrumbs;
import com.intellij.ui.components.breadcrumbs.Crumb;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import java.awt.Component;
import java.util.List;
import java.util.function.Consumer;

import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.common.ui.CardLayouts.addCard;
import static com.dbn.common.ui.CardLayouts.getCard;
import static com.dbn.common.ui.CardLayouts.showCard;
import static com.dbn.common.ui.util.Borderless.markBorderless;
import static com.dbn.common.ui.util.Lists.onSelectionChange;
import static com.dbn.nls.NlsResources.txt;
import static javax.swing.ListSelectionModel.SINGLE_SELECTION;

public class ProjectSettingsForm extends CompositeConfigurationEditorForm<ProjectSettings> {
    private JPanel mainPanel;
    private JPanel configsPanel;
    private JBList<Configuration> configList;
    private JBScrollPane configsScrollPane;
    private JLabel titleLabel;
    private JLabel subtitleLabel;
    private Breadcrumbs selectionBreadcrumbs;

    DefaultListModel<Configuration> configListModel = new DefaultListModel<>();

    public ProjectSettingsForm(ProjectSettings projectSettings) {
        super(projectSettings);

        initConfigSelector();

        ConnectionBundleSettings connectionSettings = projectSettings.getConnectionSettings();
        DatabaseBrowserSettings browserSettings = projectSettings.getBrowserSettings();
        NavigationSettings navigationSettings = projectSettings.getNavigationSettings();
        CodeEditorSettings codeEditorSettings = projectSettings.getCodeEditorSettings();
        CodeCompletionSettings codeCompletionSettings = projectSettings.getCodeCompletionSettings();
        DataGridSettings dataGridSettings = projectSettings.getDataGridSettings();
        DataEditorSettings dataEditorSettings = projectSettings.getDataEditorSettings();
        ExecutionEngineSettings executionEngineSettings = projectSettings.getExecutionEngineSettings();
        OperationSettings operationSettings = projectSettings.getOperationSettings();
        DDLFileSettings ddlFileSettings = projectSettings.getDdlFileSettings();
        AssistantSettings assistantSettings = projectSettings.getAssistantSettings();
        GeneralProjectSettings generalSettings = projectSettings.getGeneralSettings();

        configListModel.addElement(connectionSettings);
        configListModel.addElement(browserSettings);
        configListModel.addElement(navigationSettings);
        configListModel.addElement(codeEditorSettings);
        configListModel.addElement(codeCompletionSettings);
        configListModel.addElement(dataGridSettings);
        configListModel.addElement(dataEditorSettings);
        configListModel.addElement(executionEngineSettings);
        configListModel.addElement(operationSettings);
        configListModel.addElement(ddlFileSettings);
        configListModel.addElement(assistantSettings);
        configListModel.addElement(generalSettings);

        projectSettings.reset();
        configsPanel.setFocusable(true);
   }

    private void initConfigSelector() {
        titleLabel.setFont(Fonts.regular(2));
        selectionBreadcrumbs.setFont(Fonts.regular(1));

        Project project = getProject();
        boolean defaultProject = project != null && project.isDefault();
        subtitleLabel.setText(
                defaultProject ?
                        txt("msg.settings.title.DefaultProjectSettings") :
                        txt("msg.settings.title.ProjectSettings"));

        markBorderless(configList);
        configList.setModel(configListModel);
        configList.setCellRenderer(listCellRenderer());
        configList.setSelectionMode(SINGLE_SELECTION);
        onSelectionChange(configList, listSelectionHandler());
    }

    private Consumer<ListSelectionEvent> listSelectionHandler() {
        return e -> {
            if (e.getValueIsAdjusting()) return;
            initConfigurationPanel();
        };
    }

    private void initConfigurationPanel() {
        Configuration<?, ?> configuration = getSelectedConfiguration();
        ensureConfigurationPanel(configuration);

        String configurationId = configuration.getId();
        showCard(configsPanel, configurationId);
        selectionBreadcrumbs.setCrumbs(List.of(
                new Crumb.Impl(null, "Settings", null),
                new Crumb.Impl(null, configuration.getDisplayName(), null)
                ));

    }

    private void ensureConfigurationPanel(Configuration<?, ?> configuration) {
        String configurationId = configuration.getId();
        Component configurationCard = getCard(configsPanel, configurationId);
        if (configurationCard == null) {
            JComponent component = nd(configuration.createComponent());
            JBScrollPane scrollPane = new JBScrollPane(component);
            configsPanel.add(scrollPane);

            addCard(configsPanel, component, configuration.getId());
        }
    }

    private static ColoredListCellRenderer<Configuration> listCellRenderer() {
        return new ColoredListCellRenderer<>() {
            @Override
            protected void customize(@NotNull JList<? extends Configuration> list, Configuration value, int index, boolean selected, boolean hasFocus) {
                append(value.getDisplayName());
            }
        };
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return configList;
    }

    void selectConnectionSettings(@Nullable ConnectionId connectionId) {
        selectSettingsEditor(ConfigId.CONNECTIONS);

        ConnectionBundleSettings connectionSettings = getConfiguration().getConnectionSettings();
        ensureConfigurationPanel(connectionSettings);

        ConnectionBundleSettingsForm settingsEditor = connectionSettings.getSettingsEditor();
        if (settingsEditor == null) return;

        settingsEditor.selectConnection(connectionId);
    }

    void selectSettingsEditor(ConfigId configId) {
        Configuration<?, ?> configuration = getConfiguration().getConfiguration(configId);
        if (configuration == null) return;

        int configIndex = configListModel.indexOf(configuration);
        if (configIndex == -1) return;

        whenFirstShown(() -> configList.setSelectedIndex(configIndex));
    }


    @NotNull
    public Configuration<?, ?> getSelectedConfiguration() {
        int selectedIndex = configList.getSelectedIndex();
        selectedIndex = Math.max(selectedIndex, 0);

        return configListModel.get(selectedIndex);
    }
}
