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
import com.dbn.common.message.TitledMessage;
import com.dbn.common.operation.DatabaseOperation;
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
import com.dbn.prerequisite.model.PrerequisiteGroup;
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

@SuppressWarnings("unchecked")
public class PrerequisitesForm extends DBNFormBase implements PrerequisiteEventListener {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel hintPanel;
    private JPanel detailsPanel;
    private JPanel advicePanel;
    private JPanel messagePanel;

    private final Map<String, PrerequisiteDetailForm> detailForms = ContainerUtil.createConcurrentWeakValueMap();
    private final @Getter PrerequisiteGroup prerequisiteGroup;
    private EditorEx viewer;
    private DBNMessageForm messageForm;

    public PrerequisitesForm(PrerequisitesDialog dialog) {
        super(dialog);
        prerequisiteGroup = dialog.getPrerequisiteGroup();
        prerequisiteGroup.addEventListener(this);

        initHeaderPanel();
        initMessagePanel();
        initDetailsPanel();
        initAdvicePanel();

        scheduleEvaluation();
    }

    private void scheduleEvaluation() {
        whenShown(() -> prerequisiteGroup.evaluateAll(true));
    }

    private void initAdvicePanel() {
        Project project = prerequisiteGroup.getProject();
        ConnectionHandler connection = prerequisiteGroup.getConnection();
        DatabaseOperation operation = prerequisiteGroup.getOperation();
        String content = prerequisiteGroup.createAdviceContent();

        String fileName = connection.getConnectionId() + "" + operation;

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
        List<Prerequisite> prerequisites = prerequisiteGroup.getPrerequisites();
        for (Prerequisite prerequisite : prerequisites) {
            PrerequisiteDetailForm detailForm = new PrerequisiteDetailForm(this, prerequisite);
            detailsPanel.add(detailForm.getMainComponent());
        }
    }

    private void initHeaderPanel() {
        ConnectionHandler connection = prerequisiteGroup.getConnection();
        DBNHeaderForm headerForm = new DBNHeaderForm(this, connection);
        headerPanel.add(headerForm.getMainComponent());
    }

    private void initMessagePanel() {
        TitledMessage message = prerequisiteGroup.createStatusMessage();
        messageForm = new DBNMessageForm(this, message);
        messagePanel.add(messageForm.getComponent());
    }

    private void updateMessagePanel() {
        TitledMessage message = prerequisiteGroup.createStatusMessage();
        messageForm.setMessage(message);
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
        prerequisiteGroup.removeEventListener(this);
        detailForms.values().forEach(f -> prerequisiteGroup.removeEventListener(f));

        Editors.releaseEditor(viewer);
        super.disposeInner();
    }
}
