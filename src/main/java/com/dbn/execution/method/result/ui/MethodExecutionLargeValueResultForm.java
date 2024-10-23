package com.dbn.execution.method.result.ui;

import com.dbn.common.action.ComboBoxAction;
import com.dbn.common.action.Lookups;
import com.dbn.common.action.ProjectAction;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.util.*;
import com.dbn.data.editor.text.TextContentType;
import com.dbn.data.value.LargeObjectValue;
import com.dbn.editor.data.options.DataEditorQualifiedEditorSettings;
import com.dbn.editor.data.options.DataEditorSettings;
import com.dbn.execution.method.ArgumentValue;
import com.dbn.object.DBArgument;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.ui.IdeBorderFactory;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

import static com.dbn.common.util.Commons.nvl;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class MethodExecutionLargeValueResultForm extends DBNFormBase {
    private JPanel actionsPanel;
    private JPanel mainPanel;
    private JPanel largeValuePanel;

    private final DBObjectRef<DBArgument> argument;
    private EditorEx editor;
    private TextContentType contentType;

    MethodExecutionLargeValueResultForm(MethodExecutionResultForm parent, DBArgument argument, ArgumentValue argumentValue) {
        super(parent);
        this.argument = DBObjectRef.of(argument);

        String text = "";
        Project project = getProject();
        Object value = argumentValue.getValue();
        if (value instanceof LargeObjectValue) {
            LargeObjectValue largeObjectValue = (LargeObjectValue) value;
            try {
                text = largeObjectValue.read();
            } catch (SQLException e) {
                conditionallyLog(e);
                Messages.showWarningDialog(project, "Load error", "Could not load value for argument " + argument.getName() + ". Cause: " + e.getMessage());
            }
        } else if (value instanceof String) {
            text = (String) value;
        }

        text = Strings.removeCharacter(nvl(text, ""), '\r');
        Document document = Documents.createDocument(text);

        String contentTypeName = argument.getDataType().getContentTypeName();
        contentType = TextContentType.get(project, contentTypeName);

        if (contentType == null) contentType = TextContentType.getPlainText(project);

        editor = Editors.createEditor(document, project, null, contentType.getFileType());
        editor.getContentComponent().setFocusTraversalKeysEnabled(false);

        largeValuePanel.add(editor.getComponent(), BorderLayout.CENTER);


        largeValuePanel.setBorder(IdeBorderFactory.createBorder());

        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel,
                "DBNavigator.Place.MethodExecutionResult.LobContentTypeEditor", true,
                new ContentTypeComboBoxAction());
        actionsPanel.add(actionToolbar.getComponent(), BorderLayout.WEST);


/*
        ActionToolbar actionToolbar = ActionUtil.createActionToolbar("", true,
                new CursorResultFetchNextRecordsAction(executionResult, resultTable),
                new CursorResultViewRecordAction(resultTable),
                ActionUtil.SEPARATOR,
                new CursorResultExportAction(resultTable, argument));

        actionsPanel.add(actionToolbar.getComponent());
*/
    }

    public void setContentType(TextContentType contentType) {
        Editors.initEditorHighlighter(editor, contentType);
    }

    public DBArgument getArgument() {
        return argument.get();
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public class ContentTypeComboBoxAction extends ComboBoxAction {

        ContentTypeComboBoxAction() {
            Presentation presentation = getTemplatePresentation();
            presentation.setText(contentType.getName());
            presentation.setIcon(contentType.getIcon());
        }



        @Override
        @NotNull
        protected DefaultActionGroup createPopupActionGroup(@NotNull JComponent button, @NotNull DataContext dataContext) {
            Project project = Lookups.getProject(button);
            DataEditorQualifiedEditorSettings qualifiedEditorSettings = DataEditorSettings.getInstance(project).getQualifiedEditorSettings();

            DefaultActionGroup actionGroup = new DefaultActionGroup();
            for (TextContentType contentType : qualifiedEditorSettings.getContentTypes()) {
                if (contentType.isSelected()) {
                    actionGroup.add(new ContentTypeSelectAction(contentType));
                }

            }
            return actionGroup;
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            Presentation presentation = e.getPresentation();
            presentation.setText(contentType.getName());
            presentation.setIcon(contentType.getIcon());
        }
    }

    @Getter
    public class ContentTypeSelectAction extends ProjectAction {
        private final TextContentType contentType;

        ContentTypeSelectAction(TextContentType contentType) {
            this.contentType = contentType;
        }

        @Override
        protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
            Presentation presentation = e.getPresentation();
            presentation.setText(contentType.getName());
            presentation.setIcon(contentType.getIcon());
        }

        @Override
        protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
            Editors.initEditorHighlighter(editor, contentType);
            MethodExecutionLargeValueResultForm.this.contentType = contentType;
        }
    }

    @Override
    public void disposeInner() {
        Editors.releaseEditor(editor);
        editor = null;
    }
}
