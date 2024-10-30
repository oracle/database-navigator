package com.dbn.editor.code.ui;

import com.dbn.common.environment.EnvironmentManager;
import com.dbn.common.message.MessageType;
import com.dbn.common.util.Messages;
import com.dbn.editor.DBContentType;
import com.dbn.editor.code.SourceCodeEditor;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.options.ConfigId;
import com.dbn.options.ProjectSettingsManager;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.openapi.project.Project;

import static com.dbn.common.util.Conditional.when;

public class SourceCodeReadonlyNotificationPanel extends SourceCodeEditorNotificationPanel{
    public SourceCodeReadonlyNotificationPanel(DBSchemaObject object, SourceCodeEditor sourceCodeEditor) {
        super(object, isReadonly(sourceCodeEditor) ? MessageType.NEUTRAL : MessageType.WARNING);
        DBSourceCodeVirtualFile sourceCodeFile = sourceCodeEditor.getVirtualFile();
        String environmentName = sourceCodeFile.getEnvironmentType().getName();

        Project project = object.getProject();
        DBContentType contentType = sourceCodeEditor.getContentType();

        if (isReadonly(sourceCodeEditor)) {
            setText("READONLY CODE - This is meant to prevent accidental code changes in \"" + environmentName + "\" environments (check environment settings)");
            createActionLabel("Edit Mode", () ->
                    Messages.showQuestionDialog(project,
                            "Enable edit-mode",
                            "Are you sure you want to enable editing for " + object.getQualifiedNameWithType(),
                            new String[]{"Yes", "Cancel"}, 0,
                            option -> when(option == 0, () -> {
                                EnvironmentManager environmentManager = EnvironmentManager.getInstance(project);
                                environmentManager.enableEditing(object, contentType);
                            })));
        } else {
            setText("EDITABLE CODE! - Edit-mode enabled (the environment \"" + environmentName + "\" is configured as \"Readonly Code\")");
            createActionLabel("Cancel Editing", () -> {
                EnvironmentManager environmentManager = EnvironmentManager.getInstance(project);
                environmentManager.disableEditing(object, contentType);
            });
        }

        createActionLabel("Settings", () -> {
            ProjectSettingsManager settingsManager = ProjectSettingsManager.getInstance(project);
            settingsManager.openProjectSettings(ConfigId.GENERAL);
        });
    }

    private static boolean isReadonly(SourceCodeEditor sourceCodeEditor) {
        Project project = sourceCodeEditor.getProject();
        EnvironmentManager environmentManager = EnvironmentManager.getInstance(project);
        return environmentManager.isReadonly(sourceCodeEditor.getVirtualFile());
    }
}
