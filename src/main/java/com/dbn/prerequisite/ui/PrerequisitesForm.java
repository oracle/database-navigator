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
import com.dbn.common.operation.DatabaseOperationType;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
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

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Color;
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

    private final Map<String, PrerequisiteDetailForm> detailForms = ContainerUtil.createConcurrentWeakValueMap();
    private final @Getter PrerequisiteBundle prerequisiteBundle;
    private EditorEx viewer;

    public PrerequisitesForm(PrerequisitesDialog dialog) {
        super(dialog);
        prerequisiteBundle = dialog.getPrerequisites();
        prerequisiteBundle.addEventListener(this);

        initHeaderPanel();
        initDetailsPanel();
        initAdvicePanel();
        whenShown(() -> prerequisiteBundle.evaluateAll());
    }

    private void initAdvicePanel() {
        Project project = prerequisiteBundle.getProject();
        ConnectionHandler connection = prerequisiteBundle.getConnection();
        DatabaseOperationType operationType = prerequisiteBundle.getOperation().getType();
        String content = prerequisiteBundle.createAdviceContent();

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
        List<Prerequisite> prerequisites = prerequisiteBundle.getPrerequisites();
        for (Prerequisite prerequisite : prerequisites) {
            PrerequisiteDetailForm detailForm = new PrerequisiteDetailForm(this, prerequisite);
            detailsPanel.add(detailForm.getMainComponent());
        }
    }

    private void initHeaderPanel() {
        ConnectionHandler connection = prerequisiteBundle.getConnection();
        String title = connection.getName() + " - " + prerequisiteBundle.getOperation().getType().getDescription();
        Color color = connection.getEnvironmentType().getColor();
        DBNHeaderForm headerForm = new DBNHeaderForm(this, title, connection.getIcon(), color);
        headerPanel.add(headerForm.getMainComponent());
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
        PrerequisiteEventType type = event.getType();
        switch (type) {
            case EVALUATION_STARTED: onVerificationStarted(); break;
            case EVALUATION_FINISHED: onVerificationFinished(); break;
            case EVALUATION_FAILED: onVerificationFailed(); break;
        }
    }

    private void onVerificationStarted() {
    }

    private void onVerificationFinished() {
    }

    private void onVerificationFailed() {

    }

    public Object getData(@NotNull String dataId) {
        if (DataKeys.PREREQUISITES_FORM.is(dataId)) return this;
        return null;
    }

    @Override
    public void disposeInner() {
        Editors.releaseEditor(viewer);
        super.disposeInner();
    }
}
