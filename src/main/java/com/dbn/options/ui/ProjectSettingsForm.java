package com.dbn.options.ui;

import com.dbn.assistant.settings.AssistantSettings;
import com.dbn.browser.options.DatabaseBrowserSettings;
import com.dbn.code.common.completion.options.CodeCompletionSettings;
import com.dbn.common.options.BasicConfiguration;
import com.dbn.common.options.Configuration;
import com.dbn.common.options.ui.CompositeConfigurationEditorForm;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.ui.tab.DBNTabbedPane;
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
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class ProjectSettingsForm extends CompositeConfigurationEditorForm<ProjectSettings> {
    private JPanel mainPanel;
    private JPanel tabsPanel;
    private final DBNTabbedPane<ConfigurationEditorForm<?>> configurationTabs;

    public ProjectSettingsForm(ProjectSettings globalSettings) {
        super(globalSettings);

        configurationTabs = new DBNTabbedPane<>(this);
        //configurationTabs.setAdjustBorders(false);

        tabsPanel.add(configurationTabs, BorderLayout.CENTER);

        ConnectionBundleSettings connectionSettings = globalSettings.getConnectionSettings();
        DatabaseBrowserSettings browserSettings = globalSettings.getBrowserSettings();
        NavigationSettings navigationSettings = globalSettings.getNavigationSettings();
        CodeEditorSettings codeEditorSettings = globalSettings.getCodeEditorSettings();
        CodeCompletionSettings codeCompletionSettings = globalSettings.getCodeCompletionSettings();
        DataGridSettings dataGridSettings = globalSettings.getDataGridSettings();
        DataEditorSettings dataEditorSettings = globalSettings.getDataEditorSettings();
        ExecutionEngineSettings executionEngineSettings = globalSettings.getExecutionEngineSettings();
        OperationSettings operationSettings = globalSettings.getOperationSettings();
        DDLFileSettings ddlFileSettings = globalSettings.getDdlFileSettings();
        AssistantSettings assistantSettings = globalSettings.getAssistantSettings();
        GeneralProjectSettings generalSettings = globalSettings.getGeneralSettings();

        addSettingsPanel(connectionSettings);
        addSettingsPanel(browserSettings);
        addSettingsPanel(navigationSettings);
        addSettingsPanel(codeEditorSettings);
        addSettingsPanel(codeCompletionSettings);
        addSettingsPanel(dataGridSettings);
        addSettingsPanel(dataEditorSettings);
        addSettingsPanel(executionEngineSettings);
        addSettingsPanel(operationSettings);
        addSettingsPanel(ddlFileSettings);
        addSettingsPanel(assistantSettings);
        addSettingsPanel(generalSettings);
        globalSettings.reset();

        tabsPanel.setFocusable(true);
   }

    private void addSettingsPanel(BasicConfiguration<?, ?> configuration) {
        JComponent component = configuration.createComponent();
        JBScrollPane scrollPane = new JBScrollPane(component);
        configurationTabs.addTab(configuration.getDisplayName(), scrollPane, configuration.getSettingsEditor());
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    void selectConnectionSettings(@Nullable ConnectionId connectionId) {
        ConnectionBundleSettings connectionSettings = getConfiguration().getConnectionSettings();
        ConnectionBundleSettingsForm settingsEditor = connectionSettings.getSettingsEditor();
        if (settingsEditor != null) {
            settingsEditor.selectConnection(connectionId);
            selectSettingsEditor(ConfigId.CONNECTIONS);
        }
    }

    void selectSettingsEditor(ConfigId configId) {
        Configuration<?, ?> configuration = getConfiguration().getConfiguration(configId);
        if (configuration == null) return;

        ConfigurationEditorForm<?> settingsEditor = configuration.getSettingsEditor();
        if (settingsEditor == null) return;

        configurationTabs.selectTab(settingsEditor);
    }


    @NotNull
    public Configuration<?, ?> getActiveConfiguration() {
        ConfigurationEditorForm<?> settingsEditor = configurationTabs.getSelectedContent();
        return settingsEditor.getConfiguration();
    }
}
