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
import com.dbn.common.editor.WrappingTextEditor;
import com.dbn.common.environment.options.listener.EnvironmentManagerListener;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Json;
import com.dbn.data.value.JsonValue;
import com.dbn.editor.json.JsonDataEditor;
import com.dbn.editor.json.JsonFileCache;
import com.dbn.editor.json.model.JsonDataEditorModelCell;
import com.dbn.object.DBJsonView;
import com.dbn.vfs.file.DBContentVirtualFile;
import com.intellij.openapi.command.undo.UndoUtil;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.FocusChangeListener;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.KeyboardFocusManager;

import static com.dbn.common.ui.util.UserInterface.updateScrollPanes;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Documents.resetText;
import static com.dbn.common.util.Editors.enableSelectionOccurrenceHighlights;
import static com.dbn.nls.NlsResources.txt;

public class JsonDataContentEditorForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel editorPanel;
    private JPanel headerPanel;


    private WeakRef<JsonDataEditorModelCell> selectedCell;
    private @Getter EditorEx editor;
    private @Getter TextEditor textEditor;
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

    private JsonDataEditorForm getParentForm() {
        return getParentComponent();
    }


    private void initJsonContentEditor() {
        Project project = ensureProject();
        DBJsonView jsonView = getParentForm().getJsonView();

        PsiFile jsonFile = JsonFileCache.getJsonContentPsiFile(jsonView);
        VirtualFile virtualFile = jsonFile.getVirtualFile();
        UndoUtil.setForceUndoFlag(virtualFile, true);

        Document document = Documents.ensureDocument(jsonFile);
        resetText(project, document, "");

        editor = Editors.createEditor(document, project, virtualFile, jsonFile.getFileType());
        textEditor = new WrappingTextEditor(editor, "JSON Content");
        editor.setEmbeddedIntoDialogWrapper(true);
        Disposer.register(this, editor);

        Editors.updateEditorScrollPane(editor, null);

        EditorSettings settings = editor.getSettings();
        enableSelectionOccurrenceHighlights(editor);
        //settings.setHighlightSelectionOccurrences(true);
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
            resetText(editor, originalContent, false);

        } else {
            JsonValue userValue = cell.getUserValue();
            originalContent = Json.removeJsonProperties(userValue.getData(), "_metadata");

            resetText(editor, originalContent, true);
        }

        updateEditorState();
    }

    public void focusEditor() {
        if (editor == null) return;
        editor.getContentComponent().requestFocus();
    }

    public boolean isEditorFocused() {
        if (editor == null) return false;

        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        return focusOwner != null && SwingUtilities.isDescendingFrom(focusOwner, editor.getComponent());
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
        textEditor = null;
        super.disposeInner();
    }

    public void updateEditorState() {
        JsonDataEditor jsonDataEditor = getParentForm().getJsonDataEditor();
        boolean connected = jsonDataEditor.isConnected();
        boolean locked = jsonDataEditor.isEditingLocked();
        boolean editable = !jsonDataEditor.isReadonly();
        boolean selected = getSelectedCell() != null;

        String readonlyHint =
                !selected ? txt("app.dataEditor.hint.ContentNotSelected") :
                //locked ? "<html>Editing is locked. <a href=''>Unlock</a></html>" :
                locked ? txt("app.dataEditor.hint.EditingLocked") :
                !editable ? txt("app.dataEditor.hint.ReadonlyViewOrEnvironment") :
                !connected ? txt("app.dataEditor.hint.NotConnected") : null;

        boolean readonly = !editable || !connected || !selected;

        Editors.setEditorReadonly(editor, readonly);
        Editors.setEditorReadonlyHint(editor, readonlyHint);
    }
}
