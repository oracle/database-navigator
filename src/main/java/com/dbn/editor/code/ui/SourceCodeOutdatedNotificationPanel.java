package com.dbn.editor.code.ui;

import com.dbn.common.message.MessageType;
import com.dbn.common.util.Messages;
import com.dbn.database.DatabaseFeature;
import com.dbn.editor.code.SourceCodeEditor;
import com.dbn.editor.code.SourceCodeManager;
import com.dbn.editor.code.content.SourceCodeContent;
import com.dbn.editor.code.diff.MergeAction;
import com.dbn.editor.code.diff.SourceCodeDiffManager;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.openapi.project.Project;
import com.intellij.util.text.DateFormatUtil;

import static com.dbn.common.util.Strings.toLowerCase;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class SourceCodeOutdatedNotificationPanel extends SourceCodeEditorNotificationPanel{
    public SourceCodeOutdatedNotificationPanel(DBSourceCodeVirtualFile sourceCodeFile, SourceCodeEditor sourceCodeEditor) {
        super(sourceCodeFile.getObject(), MessageType.WARNING);
        DBSchemaObject editableObject = sourceCodeFile.getObject();
        Project project = editableObject.getProject();
        String presentableChangeTime =
                DatabaseFeature.OBJECT_CHANGE_MONITORING.isSupported(editableObject) ?
                        toLowerCase(DateFormatUtil.formatPrettyDateTime(sourceCodeFile.getDatabaseChangeTimestamp())) : "";


        String text = "Outdated version";
        boolean mergeRequired = sourceCodeFile.isMergeRequired();
        if (sourceCodeFile.isModified() && !mergeRequired) {
            text += " (MERGED)";
        }
        text += ". The " + editableObject.getQualifiedNameWithType() + " was changed in database by another user (" + presentableChangeTime + ")";

        setText(text);
        createActionLabel("Show Diff", new Runnable() {
            @Override
            public void run() {
                if (!project.isDisposed()) {
                    SourceCodeDiffManager diffManager = SourceCodeDiffManager.getInstance(project);
                    diffManager.opedDatabaseDiffWindow(sourceCodeFile);
                }
            }
        });

        if (mergeRequired) {
            createActionLabel("Merge", new Runnable() {
                @Override
                public void run() {
                    if (!project.isDisposed()) {
                        try {
                            SourceCodeManager sourceCodeManager = SourceCodeManager.getInstance(project);
                            SourceCodeDiffManager diffManager = SourceCodeDiffManager.getInstance(project);
                            SourceCodeContent sourceCodeContent = sourceCodeManager.loadSourceFromDatabase(editableObject, sourceCodeEditor.getContentType());
                            String databaseContent = sourceCodeContent.getText().toString();
                            diffManager.openCodeMergeDialog(databaseContent, sourceCodeFile, sourceCodeEditor, MergeAction.MERGE);
                        }catch (Exception e) {
                            conditionallyLog(e);
                            Messages.showErrorDialog(project, "Could not load sources from database.", e);

                        }
                    }
                }
            });
        }

        createActionLabel(sourceCodeFile.isModified() ? "Revert local changes" : "Reload", () -> {
            if (!project.isDisposed()) {
                SourceCodeManager sourceCodeManager = SourceCodeManager.getInstance(project);
                sourceCodeManager.loadSourceCode(sourceCodeFile, true);
            }
        });
    }
}
