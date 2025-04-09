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

package com.dbn.editor.json.ui;

import com.dbn.common.dispose.Disposer;
import com.dbn.common.environment.options.listener.EnvironmentManagerListener;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Json;
import com.dbn.data.value.JsonValue;
import com.dbn.editor.json.JsonDataEditor;
import com.dbn.editor.json.JsonFileCache;
import com.dbn.editor.json.model.JsonDataEditorModelCell;
import com.dbn.object.DBJsonView;
import com.dbn.vfs.file.DBContentVirtualFile;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.FocusChangeListener;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkListener;

import static com.dbn.common.ui.util.UserInterface.updateScrollPanes;
import static com.dbn.common.util.Commons.nvl;
import static com.intellij.openapi.editor.EditorModificationUtil.setReadOnlyHint;

public class JsonDataContentEditorForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel editorPanel;
    private JPanel headerPanel;


    private WeakRef<JsonDataEditorModelCell> selectedCell;
    private EditorEx editor;
    private String originalContent;


    public JsonDataContentEditorForm(JsonDataEditorForm parent) {
        super(parent);

        ProjectEvents.subscribe(EnvironmentManagerListener.TOPIC, environmentManagerListener());
        initJsonContentEditor();
    }

    private EnvironmentManagerListener environmentManagerListener() {
        return new EnvironmentManagerListener() {
            @Override
            public void configurationChanged(Project project) {
                updateEditorState();
            }

            @Override
            public void editModeChanged(Project project, DBContentVirtualFile databaseContentFile) {
                updateEditorState();
            }
        };
    }

    private JsonDataEditorForm getPrentForm() {
        return getParentComponent();
    }


    private void initJsonContentEditor() {
        Project project = ensureProject();
        DBJsonView jsonView = getPrentForm().getJsonView();

        PsiFile jsonFile = JsonFileCache.getJsonContentPsiFile(jsonView);
        Document document = Documents.ensureDocument(jsonFile);
        Documents.setText(document, "");

        editor = Editors.createEditor(document, project, jsonFile.getVirtualFile(), jsonFile.getFileType());
        editor.setEmbeddedIntoDialogWrapper(true);
        Disposer.register(this, editor);

        JScrollPane editorScrollPane = editor.getScrollPane();
        editorScrollPane.setViewportBorder(Borders.insetBorder(4));

        EditorSettings settings = editor.getSettings();
        settings.setHighlightSelectionOccurrences(true);
        settings.setLineNumbersShown(true);
        settings.setFoldingOutlineShown(true);
        settings.setLineMarkerAreaShown(true);
        settings.setRightMarginShown(false);
        settings.setDndEnabled(false);
        settings.setUseTabCharacter(true);
        settings.setCaretRowShown(false);
        settings.setVirtualSpace(true);

        editor.addFocusListener(new FocusChangeListener() {
            @Override
            public void focusLost(@NotNull Editor editor) {
                updateCellValue();
            }
        });


        editorPanel.add(editor.getComponent());
        updateScrollPanes(editorPanel);
    }

    public void selectRecord(JsonDataEditorModelCell cell) {
        updateCellValue();

        this.selectedCell = WeakRef.of(cell);

        if (cell == null) {
            originalContent = "";
            Documents.setText(editor, originalContent, false);

        } else {
            JsonValue userValue = cell.getUserValue();
            originalContent = Json.removeJsonProperties(userValue.getData(), "_metadata");

            Documents.setText(editor, originalContent, true);
            editor.getContentComponent().requestFocus();
        }

        updateEditorState();
    }

    public boolean isEditorContentChanged() {
        JsonDataEditorModelCell selectedCell = getSelectedCell();
        if (selectedCell == null) return false;

        String originalContent = nvl(this.originalContent, "");
        String editorContent = editor.getDocument().getText();
        return !Json.checkJsonContentsEqual(originalContent, editorContent);
    }

    public void updateCellValue() {
        JsonDataEditorModelCell selectedCell = getSelectedCell();
        if (selectedCell == null) return;
        if (!isEditorContentChanged()) return;

        String editorContent = editor.getDocument().getText();
        JsonValue userValue = new JsonValue(editorContent);
        selectedCell.updateUserValue(userValue, false);
    }

    public JsonDataEditorModelCell getSelectedCell() {
        return WeakRef.get(selectedCell);
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Override
    public void disposeInner() {
        Editors.releaseEditor(editor);
        editor = null;
        super.disposeInner();
    }

    public void updateEditorState() {
        JsonDataEditor jsonDataEditor = getPrentForm().getJsonDataEditor();
        boolean connected = jsonDataEditor.isConnected();
        boolean locked = jsonDataEditor.isEditingLocked();
        boolean editable = !jsonDataEditor.isReadonly();
        boolean selected = getSelectedCell() != null;

        String readonlyHint =
                !selected ? "No content selected" :
                //locked ? "<html>Editing is locked. <a href=''>Unlock</a></html>" :
                locked ? "Editor is locked" :
                !editable ? "View or database environment is readonly" :
                !connected ? "Not connected to database" : null;

        boolean readonly = !editable || !connected || !selected;
        editor.setViewer(readonly);

        HyperlinkListener unlockListener = null; /* locked ? e -> {
            if (e.getEventType() != ACTIVATED) return;
            jsonDataEditor.toggleEditingLock();
        : null}*/;

        setReadOnlyHint(editor, readonlyHint, unlockListener);
    }
}
