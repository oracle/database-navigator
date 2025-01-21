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

package com.dbn.editor.code.diff;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Progress;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionAction;
import com.dbn.editor.code.SourceCodeEditor;
import com.dbn.editor.code.SourceCodeManager;
import com.dbn.editor.code.content.SourceCodeContent;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.vfs.file.DBLooseContentVirtualFile;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.diff.DiffManager;
import com.intellij.diff.DiffRequestFactory;
import com.intellij.diff.InvalidDiffRequestException;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.merge.MergeRequest;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.component.Components.projectService;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.vfs.file.status.DBFileStatus.SAVING;

@State(
    name = SourceCodeDiffManager.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class SourceCodeDiffManager extends ProjectComponentBase implements PersistentState {

    public static final String COMPONENT_NAME = "DBNavigator.Project.SourceCodeDiffManager";

    protected SourceCodeDiffManager(Project project) {
        super(project, COMPONENT_NAME);
    }

    public static SourceCodeDiffManager getInstance(@NotNull Project project) {
        return projectService(project, SourceCodeDiffManager.class);
    }

    public void openCodeMergeDialog(String databaseContent, DBSourceCodeVirtualFile sourceCodeFile, SourceCodeEditor fileEditor, MergeAction action) {
        Dispatch.run(() -> {
            Project project = getProject();
            SourceCodeDiffContent leftContent = new SourceCodeDiffContent("Database version", databaseContent);
            SourceCodeDiffContent targetContent = new SourceCodeDiffContent("Merge result", sourceCodeFile.getOriginalContent());
            SourceCodeDiffContent rightContent = new SourceCodeDiffContent("Your version", sourceCodeFile.getContent());
            MergeContent mergeContent = new MergeContent(leftContent, targetContent, rightContent );
            try {
                DiffRequestFactory diffRequestFactory = DiffRequestFactory.getInstance();
                MergeRequest mergeRequest = diffRequestFactory.createMergeRequest(
                        project,
                        sourceCodeFile,
                        mergeContent.getByteContents(),
                        "Version conflict resolution for " + sourceCodeFile.getObject().getQualifiedNameWithType(),
                        mergeContent.getTitles(),
                        mergeResult -> {
                            if (action == MergeAction.SAVE) {
                                switch (mergeResult) {
                                    case LEFT:
                                    case RIGHT:
                                    case RESOLVED:
                                        SourceCodeManager sourceCodeManager = SourceCodeManager.getInstance(project);
                                        sourceCodeManager.storeSourceToDatabase(sourceCodeFile, fileEditor, null);
                                        ProjectEvents.notify(project,
                                                SourceCodeDifManagerListener.TOPIC,
                                                (listener) -> listener.contentMerged(sourceCodeFile, action));
                                        break;
                                    case CANCEL:
                                        sourceCodeFile.set(SAVING, false);
                                        break;
                                }
                            } else if (action == MergeAction.MERGE) {
                                switch (mergeResult) {
                                    case LEFT:
                                    case RIGHT:
                                    case RESOLVED:
                                        sourceCodeFile.markAsMerged();
                                        ProjectEvents.notify(project,
                                                SourceCodeDifManagerListener.TOPIC,
                                                (listener) -> listener.contentMerged(sourceCodeFile, action));
                                        break;
                                    case CANCEL:
                                        break;
                                }
                            }
                        });

                DiffManager diffManager = DiffManager.getInstance();
                diffManager.showMerge(project, mergeRequest);
            } catch (InvalidDiffRequestException e) {
                conditionallyLog(e);
            }
        });
    }


    public void openDiffWindow(@NotNull DBSourceCodeVirtualFile sourceCodeFile, String referenceText, String referenceTitle, String windowTitle) {
        DBSchemaObject object = sourceCodeFile.getObject();
        FileType fileType = sourceCodeFile.getFileType();
        DBLooseContentVirtualFile counterContent = new DBLooseContentVirtualFile(object, referenceText, fileType);
        Project project = getProject();
        DiffContent originalContent = new SourceCodeFileContent(project, sourceCodeFile);
        DiffContent changedContent = new SourceCodeFileContent(project, counterContent);

        String title =
                object.getSchema().getName() + "." +
                        object.getName() + " " +
                        object.getTypeName() + " - " + windowTitle;
        SimpleDiffRequest diffRequest = new SimpleDiffRequest(
                title,
                changedContent,
                originalContent,
                referenceTitle,
                "Your version");

        Dispatch.run(() -> DiffManager.getInstance().showDiff(project, diffRequest));
    }


    public void opedDatabaseDiffWindow(DBSourceCodeVirtualFile sourceCodeFile) {
        DBSchemaObject object = sourceCodeFile.getObject();
        ConnectionAction.invoke(txt("msg.codeEditor.title.ComparingChanges"), false, sourceCodeFile,
                action -> Progress.prompt(getProject(), object, true,
                        txt("prc.codeEditor.title.LoadingSourceCode"),
                        txt("prc.codeEditor.text.LoadingSourceCodeOf", object.getQualifiedNameWithType()),
                        progress -> {
                            Project project = getProject();
                            try {
                                SourceCodeManager sourceCodeManager = SourceCodeManager.getInstance(project);
                                SourceCodeContent sourceCodeContent = sourceCodeManager.loadSourceFromDatabase(object, sourceCodeFile.getContentType());
                                CharSequence referenceText = sourceCodeContent.getText();

                                if (!action.isCancelled()) {
                                    openDiffWindow(sourceCodeFile, referenceText.toString(), "Database version", "Local version vs. database version");
                                }

                            } catch (Exception e1) {
                                conditionallyLog(e1);
                                Messages.showErrorDialog(
                                        project, "Could not load sourcecode for " +
                                                object.getQualifiedNameWithType() + " from database.", e1);
                            }
                        }));
    }


    @Nullable
    @Override
    public Element getComponentState() {
        return null;
    }

    @Override
    public void loadComponentState(@NotNull Element state) {

    }
}
