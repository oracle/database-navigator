package com.dbn.editor.console;

import com.dbn.common.listener.DBNFileEditorManagerListener;
import com.dbn.common.util.Editors;
import com.dbn.editor.console.ui.SQLConsoleEditorToolbarForm;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.util.Files.isDbConsoleFile;

public class SQLConsoleEditorListener extends DBNFileEditorManagerListener {
  @Override
  public void whenFileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
    if (isNotValid(file)) return;
    if (!isDbConsoleFile(file)) return;

    SQLConsoleEditor fileEditor = (SQLConsoleEditor) source.getSelectedEditor(file);
    if (isNotValid(fileEditor)) return;

    Project project = source.getProject();
    SQLConsoleEditorToolbarForm toolbarForm = new SQLConsoleEditorToolbarForm(project, fileEditor);
    Editors.addEditorToolbar(fileEditor, toolbarForm);
//
//    Document document = FileDocumentManager.getInstance().getDocument(file);
//    if (document != null) {
//      document.addDocumentListener(new EditorAIQueryListener(source.getProject()));
//    }
  }
}
