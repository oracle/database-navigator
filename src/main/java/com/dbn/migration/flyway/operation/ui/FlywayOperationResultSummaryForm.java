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

package com.dbn.migration.flyway.operation.ui;

import com.dbn.common.message.MessageType;
import com.dbn.common.message.TitledMessage;
import com.dbn.common.task.TaskStatus;
import com.dbn.common.thread.Background;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.link.DBNHyperlinkLabel;
import com.dbn.common.ui.messages.DBNMessageForm;
import com.dbn.common.ui.window.ProjectViews;
import com.dbn.common.util.Editors;
import com.dbn.common.util.TimeUtil;
import com.dbn.migration.flyway.operation.FlywayOperationResult;
import com.dbn.migration.flyway.workspace.FlywayWorkspace;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.flywaydb.core.api.output.ValidateResult;
import org.jetbrains.annotations.NotNull;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.dbn.common.task.TaskStatus.CANCELLED;
import static com.dbn.common.task.TaskStatus.DONE;
import static com.dbn.common.task.TaskStatus.RUNNING;
import static com.dbn.common.ui.link.Hyperlinks.onHyperlinkAccess;
import static com.dbn.common.ui.util.Tooltips.setToolTipText;
import static com.dbn.nls.NlsResources.txt;

/**
 * Summary panel for Flyway migration validation.
 */
public class FlywayOperationResultSummaryForm extends DBNFormBase {
    private JPanel mainPanel;
    private JLabel operationLabel;
    private JLabel connectionLabel;
    private JLabel schemaLabel;
    private JLabel workspaceLabel;
    private DBNHyperlinkLabel migrationsPathLink;
    private JLabel configurationFileLabel;
    private DBNHyperlinkLabel configurationFileLink;
    private JLabel validationStatusLabel;
    private JLabel validatedMigrationsLabel;
    private JLabel invalidMigrationsLabel;
    private JLabel durationLabel;
    private JPanel messagePanel;

    private final FlywayOperationResult result;
    private final Timer durationTimer;
    private DBNMessageForm messageForm;

    public FlywayOperationResultSummaryForm(
            @NotNull Disposable parent,
            @NotNull FlywayOperationResult result) {
        super(parent, result.getProject());
        this.result = result;
        durationTimer = new Timer(1000, e -> updateDuration());
        durationTimer.setRepeats(true);

        initMessageForm();
        initLabels();
        onHyperlinkAccess(migrationsPathLink, e -> openMigrationsPath());
        onHyperlinkAccess(configurationFileLink, e -> openConfigurationFile());
        result.addListener(() -> dispatch(this::updateForm));
    }

    private void initLabels() {
        operationLabel.setText(result.getOperation().getName());
        connectionLabel.setIcon(result.getConnection().getIcon());
        connectionLabel.setText(result.getConnection().getName());
        schemaLabel.setIcon(result.getInput().getRelevantSchema().getIcon());
        schemaLabel.setText(result.getInput().getRelevantSchema().getName());
        updateWorkspaceLabels();
        updateConfigurationFileLink();
        updateForm();
    }

    private void updateWorkspaceLabels() {
        FlywayWorkspace workspace = result.getInput().getWorkspace();
        if (workspace == null) {
            workspaceLabel.setText("");
            migrationsPathLink.setHyperlinkText("");
            setToolTipText(migrationsPathLink, null);
            migrationsPathLink.setVisible(false);
            return;
        }

        workspaceLabel.setText(workspace.getName());
        Path migrationsPath = result.getInput().getWorkspacePaths().getMigrationsPath();
        if (!Files.isDirectory(migrationsPath)) {
            migrationsPathLink.setHyperlinkText("");
            setToolTipText(migrationsPathLink, null);
            migrationsPathLink.setVisible(false);
            return;
        }

        String relativePath = result.getInput().getWorkspacePaths().getRelativePath(migrationsPath);
        migrationsPathLink.setHyperlinkText(relativePath.isEmpty() ? "." : relativePath);
        setToolTipText(migrationsPathLink, migrationsPath.toString());
        migrationsPathLink.setVisible(true);
    }

    private void openMigrationsPath() {
        Path migrationsPath = result.getInput().getWorkspacePaths().getMigrationsPath();
        if (!Files.isDirectory(migrationsPath)) return;

        ProjectViews.selectPath(result.getProject(), migrationsPath);
    }

    private void updateConfigurationFileLink() {
        FlywayWorkspace workspace = result.getInput().getWorkspace();
        configurationFileLabel.setVisible(workspace != null);
        configurationFileLink.setVisible(false);
        configurationFileLink.setHyperlinkText("");
        configurationFileLink.setToolTipText(null);
        if (workspace == null) return;

        Path configurationPath = result.getInput().getWorkspacePaths().getConfigurationFilePath();
        if (!Files.isRegularFile(configurationPath)) return;

        configurationFileLink.setHyperlinkText(configurationPath.getFileName().toString());
        configurationFileLink.setToolTipText(configurationPath.toString());
        configurationFileLink.setVisible(true);
    }

    private void openConfigurationFile() {
        Path configurationPath = result.getInput().getWorkspacePaths().getConfigurationFilePath();
        Background.run(() -> {
            LocalFileSystem fileSystem = LocalFileSystem.getInstance();
            VirtualFile file = fileSystem.refreshAndFindFileByIoFile(configurationPath.toFile());
            if (file == null) return;

            Editors.openFileEditor(result.getProject(), file, true);
        });
    }

    private void initMessageForm() {
        messageForm = new DBNMessageForm(this, createMessage());
        messagePanel.add(messageForm.getComponent());
    }

    private void updateForm() {
        if (isDisposed()) return;
        updateWorkspaceLabels();
        updateConfigurationFileLink();
        updateValidationLabels();
        messageForm.setMessage(createMessage());
        updateDuration();
    }

    private void updateValidationLabels() {
        ValidateResult validationResult = result.getValidationResult();
        validationStatusLabel.setText(validationResult == null
                ? result.getStatus().getName()
                : validationResult.validationSuccessful ? txt("app.flyway.const.ValidationSuccessful") : txt("app.flyway.const.ValidationFailed"));
        validatedMigrationsLabel.setText(validationResult == null
                ? "0"
                : Integer.toString(validationResult.validateCount));
        invalidMigrationsLabel.setText(validationResult == null
                ? "0"
                : Integer.toString(validationResult.invalidMigrations.size()));
    }

    private void updateDuration() {
        durationLabel.setText(TimeUtil.presentableDuration(result.getExecutionDuration(), true));
        if (result.getStatus() == RUNNING) {
            if (!durationTimer.isRunning()) durationTimer.start();
        } else {
            durationTimer.stop();
        }
    }

    @NotNull
    private TitledMessage createMessage() {
        TaskStatus status = result.getStatus();
        MessageType messageType = status == RUNNING ? MessageType.PROCESSING :
                status == DONE ? MessageType.SUCCESS :
                        status == CANCELLED ? MessageType.WARNING : MessageType.ERROR;
        String title = txt("msg.flyway.title.OperationStatus", result.getOperation().getName(), status.getName());
        String message = txt("msg.flyway.text.OperationStatus_" + status.name(), result.getInput().getRelevantSchema().getName());
        return new TitledMessage(messageType, title, message);
    }

    @Override
    public void disposeInner() {
        durationTimer.stop();
        super.disposeInner();
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
