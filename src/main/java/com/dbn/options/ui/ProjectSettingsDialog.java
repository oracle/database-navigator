package com.dbn.options.ui;

import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.DatabaseType;
import com.dbn.connection.config.ConnectionConfigType;
import com.dbn.connection.config.tns.TnsImportData;
import com.dbn.connection.config.ui.ConnectionBundleSettingsForm;
import com.dbn.options.ConfigId;
import com.dbn.options.ProjectSettings;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.help.HelpManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.util.Alarm;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import java.awt.event.ActionEvent;

import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Getter
public class ProjectSettingsDialog extends DBNDialog<ProjectSettingsForm> {
    private JButton applyButton;
    private final ProjectSettings projectSettings;

    public ProjectSettingsDialog(Project project, ConfigId configId) {
        this(project);
        selectSettings(configId);
    }

    public ProjectSettingsDialog(Project project, @Nullable ConnectionId connectionId) {
        this(project);
        selectConnectionSettings(connectionId);
    }

    public ProjectSettingsDialog(Project project, @NotNull DatabaseType databaseType, @NotNull ConnectionConfigType configType) {
        this(project);
        ConnectionId connectionId = getConnectionSettingsEditor().createNewConnection(databaseType, configType);
        selectConnectionSettings(connectionId);
    }

    public ProjectSettingsDialog(Project project, @NotNull TnsImportData importData) {
        this(project);
        getConnectionSettingsEditor().importTnsNames(importData);
        selectConnectionSettings(null);
    }

    @NotNull
    private ConnectionBundleSettingsForm getConnectionSettingsEditor() {
        return nd(projectSettings.getConnectionSettings().getSettingsEditor());
    }

    public ProjectSettingsDialog(Project project) {
        super(project, project.isDefault() ? "Default Settings" : "Settings", true);
        setModal(true);
        setResizable(true);
        //setHorizontalStretch(1.5f);

        ProjectSettings projectSettings = ProjectSettings.get(project);
        this.projectSettings = projectSettings.clone();
        this.projectSettings.createCustomComponent();
        setDefaultSize(1200, 960);
        init();
    }

    @NotNull
    @Override
    protected ProjectSettingsForm createForm() {
        return projectSettings.ensureSettingsEditor();
    }

    @Override
    @NotNull
    protected final Action[] createActions() {
        return new Action[]{
                getOKAction(),
                getCancelAction(),
                new ApplyAction(),
                getHelpAction()
        };
    }

    @Override
    protected JButton createJButtonForAction(Action action) {
        if (action instanceof ApplyAction) {
            applyButton = new JButton();
            applyButton.setAction(action);
            applyButton.setEnabled(false);
            return applyButton;
        }
        return super.createJButtonForAction(action);
    }

    @Override
    public void doCancelAction() {
        //projectSettings.reset();
        super.doCancelAction();
        projectSettings.disposeUIResources();
    }

    @Override
    public void doOKAction() {
        try {
            projectSettings.apply();
            super.doOKAction();
            projectSettings.disposeUIResources();
        } catch (ConfigurationException e) {
            conditionallyLog(e);
            Messages.showErrorDialog(getProject(), e.getMessage());
        }

    }

    public void doApplyAction() {
        try {
            projectSettings.apply();
            applyButton.setEnabled(false);
            setCancelButtonText("Close");
        } catch (ConfigurationException e) {
            conditionallyLog(e);
            Messages.showErrorDialog(getProject(), e.getTitle(), e.getMessage());
        }
    }

    @Override
    protected void doHelpAction() {
        HelpManager.getInstance().invokeHelp(projectSettings.getHelpTopic());
    }

    private class ApplyAction extends AbstractAction {
        private final Alarm alarm = Dispatch.alarm(getForm());
        private final Runnable reloader = new Runnable() {
            @Override
            public void run() {
                if (isShowing()) {
                    boolean isModified = projectSettings.isModified();
                    applyButton.setEnabled(isModified);
                    //setCancelButtonText(isModified ? "Cancel" : "Close");
                    addReloadRequest();
                }
            }
        };

        private void addReloadRequest() {
            alarm.addRequest(reloader, 500, ModalityState.stateForComponent(getWindow()));
        }

        public ApplyAction() {
            renameAction(this, "Apply");
            addReloadRequest();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            doApplyAction();
        }
    }

    public void selectConnectionSettings(@Nullable ConnectionId connectionId) {
        ProjectSettingsForm settingsEditor = projectSettings.getSettingsEditor();
        if (settingsEditor != null) {
            settingsEditor.selectConnectionSettings(connectionId);
        }
    }

    public void selectSettings(ConfigId configId) {
        ProjectSettingsForm globalSettingsEditor = projectSettings.getSettingsEditor();
        if (globalSettingsEditor != null) {
            globalSettingsEditor.selectSettingsEditor(configId);
        }
    }
}
