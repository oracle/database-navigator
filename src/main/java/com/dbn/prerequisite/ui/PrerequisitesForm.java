/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.prerequisite.ui;

import com.dbn.common.action.DataKeys;
import com.dbn.common.message.MessageType;
import com.dbn.common.message.TitledMessage;
import com.dbn.common.operation.DatabaseOperationType;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.messages.DBNMessageForm;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Viewers;
import com.dbn.connection.ConnectionHandler;
import com.dbn.language.sql.SQLFileType;
import com.dbn.language.sql.SQLLanguage;
import com.dbn.prerequisite.event.PrerequisiteEvent;
import com.dbn.prerequisite.event.PrerequisiteEventListener;
import com.dbn.prerequisite.event.PrerequisiteEventType;
import com.dbn.prerequisite.model.Prerequisite;
import com.dbn.prerequisite.model.PrerequisiteBundle;
import com.dbn.vfs.DatabaseFileViewProvider;
import com.dbn.vfs.file.DBLooseContentVirtualFile;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.List;
import java.util.Map;

import static com.dbn.common.ui.Layouts.verticalBoxLayout;
import static com.dbn.prerequisite.model.PrerequisiteStatus.SATISFIED;
import static com.dbn.prerequisite.model.PrerequisiteStatus.UNKNOWN;
import static com.dbn.prerequisite.model.PrerequisiteStatus.UNSATISFIED;

@SuppressWarnings("unchecked")
public class PrerequisitesForm extends DBNFormBase implements PrerequisiteEventListener {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel hintPanel;
    private JPanel detailsPanel;
    private JPanel advicePanel;
    private JPanel messagePanel;

    private final Map<String, PrerequisiteDetailForm> detailForms = ContainerUtil.createConcurrentWeakValueMap();
    private final @Getter PrerequisiteBundle prerequisites;
    private EditorEx viewer;
    private DBNMessageForm messageForm;

    public PrerequisitesForm(PrerequisitesDialog dialog) {
        super(dialog);
        prerequisites = dialog.getPrerequisites();
        prerequisites.addEventListener(this);

        initHeaderPanel();
        initMessagePanel();
        initDetailsPanel();
        initAdvicePanel();

        scheduleEvaluation();
    }

    private void scheduleEvaluation() {
        whenShown(() -> prerequisites.evaluateAll(true));
    }

    private void initAdvicePanel() {
        Project project = prerequisites.getProject();
        ConnectionHandler connection = prerequisites.getConnection();
        DatabaseOperationType operationType = prerequisites.getOperation().getType();
        String content = prerequisites.createAdviceContent();

        String fileName = connection.getConnectionId() + "" + operationType;

        DBLooseContentVirtualFile adviceFile = new DBLooseContentVirtualFile(connection, fileName, SQLFileType.INSTANCE, content);
        DatabaseFileViewProvider viewProvider = new DatabaseFileViewProvider(project, adviceFile, true);
        PsiFile advicePsiFile = adviceFile.initializePsiFile(viewProvider, SQLLanguage.INSTANCE);

        Document document = Documents.ensureDocument(advicePsiFile);

        viewer = Viewers.createViewer(document, project, adviceFile, SQLFileType.INSTANCE);
        viewer.setEmbeddedIntoDialogWrapper(true);

        Editors.initEditorHighlighter(this.viewer, SQLLanguage.INSTANCE, connection);
        Editors.updateEditorScrollPane(viewer);

        EditorSettings settings = viewer.getSettings();
        settings.setFoldingOutlineShown(false);
        settings.setLineMarkerAreaShown(false);
        settings.setLineNumbersShown(false);
        settings.setVirtualSpace(false);
        settings.setDndEnabled(false);
        settings.setAdditionalLinesCount(2);
        settings.setRightMarginShown(false);
        settings.setCaretRowShown(false);
        viewer.getComponent().setFocusable(false);
        advicePanel.add(viewer.getComponent());
    }

    private void initDetailsPanel() {
        verticalBoxLayout(detailsPanel);
        List<Prerequisite> prerequisites = this.prerequisites.getPrerequisites();
        for (Prerequisite prerequisite : prerequisites) {
            PrerequisiteDetailForm detailForm = new PrerequisiteDetailForm(this, prerequisite);
            detailsPanel.add(detailForm.getMainComponent());
        }
    }

    private void initHeaderPanel() {
        ConnectionHandler connection = prerequisites.getConnection();
        DBNHeaderForm headerForm = new DBNHeaderForm(this, connection);
        headerPanel.add(headerForm.getMainComponent());
    }

    private void initMessagePanel() {
        TitledMessage message = getMessage();
        messageForm = new DBNMessageForm(this, message);
        messagePanel.add(messageForm.getComponent());
    }

    private void updateMessagePanel() {
        TitledMessage message = getMessage();
        messageForm.setMessage(message);
    }

    private TitledMessage getMessage() {
        String description = prerequisites.getOperation().getType().getDescription();
        if (prerequisites.isEvaluated()) {
            int total = prerequisites.size();

            int unknown = prerequisites.count(UNKNOWN);
            int satisfied = prerequisites.count(SATISFIED);
            int unsatisfied = prerequisites.count(UNSATISFIED);

            if (satisfied == total) {
                return new TitledMessage(MessageType.SUCCESS,
                        description + " - Requirements met",
                        "All requirements for performing the operation \"" + description + "\" are met\n");
            }

            if (unsatisfied == total) {
                return new TitledMessage(MessageType.ERROR,
                        description + " - Requirements not met",
                        "None of the requirements for performing the operation \"" + description + "\" are met.\n" +
                                "Please request the missing privileges from your database administrator.");
            }

            if (unknown == total) {
                return new TitledMessage(MessageType.ERROR,
                        description + " - Failed to verify requirements",
                        "Could not verify any of the requirements for performing the operation \"" + description + "\".\n  " +
                                "Please check the connectivity or database access rights.");

            }

            if (satisfied > 0) {
                return new TitledMessage(MessageType.WARNING,
                        description + " - Requirements partially met",
                        "Some of the requirements for performing the operation \"" + description + "\" are not met.\n" +
                                "Please request the missing privileges from your database administrator.");
            }

        }
        return new TitledMessage(MessageType.INFO,
                description + " - Verifying requirements...",
                "Verifying requirements for performing the operation \"" + description + "\"\n");
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Override
    public void eventOccurred(PrerequisiteEvent event) {
        Dispatch.run(mainPanel, () -> {
            processEvent(event);
            UserInterface.repaint(mainPanel);
        });
    }

    private void processEvent(PrerequisiteEvent event) {
        Prerequisite prerequisite = event.getPrerequisite();
        PrerequisiteEventType type = event.getType();
        switch (type) {
            case EVALUATION_STARTED: onVerificationStarted(prerequisite); break;
            case EVALUATION_FINISHED: onVerificationFinished(prerequisite); break;
        }
    }

    private void onVerificationStarted(@Nullable Prerequisite prerequisite) {
        if (prerequisite != null) return; // ignore item level
        updateMessagePanel();
    }

    private void onVerificationFinished(Prerequisite prerequisite) {
        if (prerequisite != null) return; // ignore item level
        updateMessagePanel();
    }

    public Object getData(@NotNull String dataId) {
        if (DataKeys.PREREQUISITES_FORM.is(dataId)) return this;
        return null;
    }

    @Override
    public void disposeInner() {
        prerequisites.removeEventListener(this);
        detailForms.values().forEach(f -> prerequisites.removeEventListener(f));

        Editors.releaseEditor(viewer);
        super.disposeInner();
    }
}
