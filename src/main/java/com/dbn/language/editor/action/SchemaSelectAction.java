package com.dbn.language.editor.action;

import com.dbn.common.action.BackgroundUpdate;
import com.dbn.common.action.Lookups;
import com.dbn.connection.SchemaId;
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.common.psi.PsiUtil;
import com.dbn.object.DBSchema;
import com.dbn.object.action.AnObjectAction;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@BackgroundUpdate
public class SchemaSelectAction extends AnObjectAction<DBSchema> {
    SchemaSelectAction(DBSchema schema) {
        super(schema);
    }

    @Override
    protected void actionPerformed(
            @NotNull AnActionEvent e,
            @NotNull Project project,
            @NotNull DBSchema object) {

        Editor editor = Lookups.getEditor(e);
        if (editor != null) {
            FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
            contextManager.setDatabaseSchema(editor, SchemaId.from(object));
        }
    }

    @Override
    protected void update(
            @NotNull AnActionEvent e,
            @NotNull Presentation presentation,
            @NotNull Project project,
            @Nullable DBSchema target) {

        super.update(e, presentation, project, target);
        boolean enabled = false;
        VirtualFile virtualFile = Lookups.getVirtualFile(e);
        if (virtualFile instanceof DBEditableObjectVirtualFile) {
            enabled = false;//objectFile.getObject().getSchema() == schema;
        } else if (virtualFile != null){
            PsiFile currentFile = PsiUtil.getPsiFile(project, virtualFile);
            enabled = currentFile instanceof DBLanguagePsiFile;
        }

        presentation.setEnabled(enabled);

    }
}
