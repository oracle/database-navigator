package com.dbn.options;

import com.dbn.assistant.settings.AssistantSettings;
import com.dbn.browser.options.DatabaseBrowserSettings;
import com.dbn.code.common.completion.options.CodeCompletionSettings;
import com.dbn.code.common.style.options.ProjectCodeStyleSettings;
import com.dbn.common.icon.Icons;
import com.dbn.common.options.CompositeProjectConfiguration;
import com.dbn.common.options.Configuration;
import com.dbn.common.options.ConfigurationHandle;
import com.dbn.common.options.ProjectConfiguration;
import com.dbn.common.util.Cloneable;
import com.dbn.connection.config.ConnectionBundleSettings;
import com.dbn.connection.operation.options.OperationSettings;
import com.dbn.data.grid.options.DataGridSettings;
import com.dbn.ddl.options.DDLFileSettings;
import com.dbn.editor.code.options.CodeEditorSettings;
import com.dbn.editor.data.options.DataEditorSettings;
import com.dbn.execution.common.options.ExecutionEngineSettings;
import com.dbn.navigation.options.NavigationSettings;
import com.dbn.options.general.GeneralProjectSettings;
import com.dbn.options.ui.ProjectSettingsForm;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jdom.Element;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

@Getter
@EqualsAndHashCode(callSuper = false)
public class ProjectSettings
        extends CompositeProjectConfiguration<ProjectConfiguration, ProjectSettingsForm>
        implements SearchableConfigurable.Parent, Cloneable<ProjectSettings> {

    private final @Getter(lazy = true) GeneralProjectSettings generalSettings           = new GeneralProjectSettings(this);
    private final @Getter(lazy = true) DatabaseBrowserSettings browserSettings          = new DatabaseBrowserSettings(this);
    private final @Getter(lazy = true) NavigationSettings navigationSettings            = new NavigationSettings(this);
    private final @Getter(lazy = true) DataGridSettings dataGridSettings                = new DataGridSettings(this);
    private final @Getter(lazy = true) DataEditorSettings dataEditorSettings            = new DataEditorSettings(this);
    private final @Getter(lazy = true) CodeEditorSettings codeEditorSettings            = new CodeEditorSettings(this);
    private final @Getter(lazy = true) CodeCompletionSettings codeCompletionSettings    = new CodeCompletionSettings(this);
    private final @Getter(lazy = true) ProjectCodeStyleSettings codeStyleSettings       = new ProjectCodeStyleSettings(this);
    private final @Getter(lazy = true) ExecutionEngineSettings executionEngineSettings  = new ExecutionEngineSettings(this);
    private final @Getter(lazy = true) OperationSettings operationSettings              = new OperationSettings(this);
    private final @Getter(lazy = true) DDLFileSettings ddlFileSettings                  = new DDLFileSettings(this);
    private final @Getter(lazy = true) AssistantSettings assistantSettings              = new AssistantSettings(this);
    private final @Getter(lazy = true) ConnectionBundleSettings connectionSettings      = new ConnectionBundleSettings(this);

    public ProjectSettings(Project project) {
        super(project);
    }

    public static ProjectSettings get(Project project) {
        return project.isDefault() ?
                DefaultProjectSettingsManager.getInstance().getProjectSettings() :
                ProjectSettingsManager.getInstance(project).getProjectSettings();
    }

    public static ProjectSettings getDefault() {
        return DefaultProjectSettingsManager.getInstance().getProjectSettings();
    }

    @Override
    public String getHelpTopic() {
        ProjectSettingsForm settingsEditor = getSettingsEditor();
        if (settingsEditor == null) {
            return super.getHelpTopic();
        } else {
            Configuration selectedConfiguration = settingsEditor.getActiveConfiguration();
            return selectedConfiguration.getHelpTopic();
        }
    }

    @Override
    public void apply() throws ConfigurationException {
        super.apply();
    }

    @NotNull
    @Override
    public JComponent createComponent() {
        return new JPanel();
    }

    public JComponent createCustomComponent() {
        return super.createComponent();
    }


    @Override
    @Nls
    public String getDisplayName() {
        return txt("cfg.project.title.DatabaseNavigator");
    }

    @Override
    @Nullable
    public Icon getIcon() {
        return Icons.DATABASE_NAVIGATOR;
    }

    @NotNull
    @Override
    public Configurable[] getConfigurables() {
        return getConfigurations();
    }

    @Nullable
    public Configuration getConfiguration(ConfigId settingsId) {
        for (Configurable configurable : getConfigurables()) {
            TopLevelConfig topLevelConfig = (TopLevelConfig) configurable;
            if (topLevelConfig.getConfigId() == settingsId) {
                return (Configuration) configurable;
            }
        }
        return null;
    }


    /*********************************************************
     *                    Configuration                      *
     *********************************************************/
    @Override
    @NotNull
    public ProjectSettingsForm createConfigurationEditor() {
        return new ProjectSettingsForm(this);
    }

    @Override
    protected Configuration[] createConfigurations() {
        return new Configuration[] {
                getConnectionSettings(),
                getBrowserSettings(),
                getNavigationSettings(),
                //getCodeStyleSettings(),
                getDataGridSettings(),
                getDataEditorSettings(),
                getCodeEditorSettings(),
                getCodeCompletionSettings(),
                getExecutionEngineSettings(),
                getOperationSettings(),
                getDdlFileSettings(),
                getAssistantSettings(),
                getGeneralSettings()};
    }

    /*********************************************************
    *              SearchableConfigurable.Parent             *
    *********************************************************/
    @Override
    public boolean hasOwnContent() {
        return false;
    }

    @Override
    @NotNull
    public String getId() {
        return "DBNavigator.Project.Settings";
    }

    @Override
    public Runnable enableSearch(String option) {
        return null;
    }

    @Override
    public ProjectSettings clone() {
        try {
            ConfigurationHandle.setTransitory(true);
            Element element = new Element("project-settings");
            writeConfiguration(element);
            ProjectSettings projectSettings = new ProjectSettings(getProject());
            projectSettings.readConfiguration(element);
            return projectSettings;
        } finally {
            ConfigurationHandle.setTransitory(false);
        }
    }
}
