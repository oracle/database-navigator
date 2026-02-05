package com.dbn.mcp.ui;

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.connection.ConnectionHandler;
import com.dbn.language.sql.SQLFileType;
import com.dbn.language.sql.SQLLanguage;
import com.dbn.mcp.McpServerInputForm.ParamRow;
import com.dbn.mcp.McpServerInputForm.ParamType;
import com.dbn.mcp.models.ToolDefinitionModel;
import com.dbn.mcp.vfs.McpToolSqlVirtualFile;
import com.dbn.vfs.DatabaseFileViewProvider;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.ui.components.JBTextArea;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.dbn.mcp.util.SqlParameterParser.parseOccurrences;
import static com.dbn.mcp.util.SqlParameterParser.stripColon;
import static com.dbn.mcp.util.SqlParameterParser.uniqueInOrder;

public class ToolDefinitionCreateForm extends DBNFormBase {
    private JPanel mainPanel;
    private JTextField toolName;
    private JBTextArea toolDescriptionTextArea;
    private JPanel sqlEditorPanel;
    private JTable paramsTable;
  private JScrollPane paramsScrollPane;

  private final ConnectionHandler connection;
    private ParamTableModel paramsModel;
    private Document document;
    private EditorEx editor;
    private String cachedSqlText;
    private boolean suppressDocEvents;

    public ToolDefinitionCreateForm(Disposable parent, @NotNull ConnectionHandler connection) {
        super(parent);
        this.connection = connection;
        initParamsTable();
        whenShown(this::initEditor);
    }

    private void initParamsTable() {
        paramsModel = new ParamTableModel();
        paramsTable.setModel(paramsModel);
        paramsTable.setDefaultEditor(ParamType.class, new DefaultCellEditor(new JComboBox<>(ParamType.values())));

        paramsTable.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteParam");
        paramsTable.getActionMap().put("deleteParam", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelectedParam();
            }
        });
    }

    private void initEditor() {
        Project project = getProject();
        if (project == null) return;

        McpToolSqlVirtualFile sqlFile = new McpToolSqlVirtualFile(connection, "");
        DatabaseFileViewProvider viewProvider = new DatabaseFileViewProvider(project, sqlFile, true);
        PsiFile psiFile = sqlFile.initializePsiFile(viewProvider, SQLLanguage.INSTANCE);

        document = Documents.ensureDocument(psiFile);
        editor = Editors.createEditor(document, project, sqlFile, SQLFileType.INSTANCE);
        Editors.initEditorHighlighter(editor, SQLLanguage.INSTANCE, connection);
        configureEditor(editor);

        document.addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                if (!suppressDocEvents) refreshParams();
            }
        });

        sqlEditorPanel.add(editor.getComponent(), BorderLayout.CENTER);
    }

    private void configureEditor(EditorEx editor) {
        editor.setEmbeddedIntoDialogWrapper(true);
        editor.setPlaceholder("SELECT * FROM employees WHERE department_id = :dept_id");
        editor.setShowPlaceholderWhenFocused(true);
        Editors.updateEditorScrollPane(editor);

        EditorSettings settings = editor.getSettings();
        settings.setFoldingOutlineShown(false);
        settings.setLineMarkerAreaShown(false);
        settings.setLineNumbersShown(true);
        settings.setCaretRowShown(true);
        settings.setVirtualSpace(false);
        settings.setDndEnabled(false);
        settings.setAdditionalLinesCount(2);
        settings.setRightMarginShown(false);
        settings.setUseTabCharacter(false);
        settings.setTabSize(2);
    }

    private void deleteSelectedParam() {
        int row = paramsTable.getSelectedRow();
        if (row < 0 || row >= paramsModel.getRowCount()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        String name = stripColon(paramsModel.getRows().get(row).name);
        String newSql = getSqlText().replaceAll(":" + Pattern.quote(name) + "\\b", "");

        suppressDocEvents = true;
        try {
            setSqlText(newSql);
        } finally {
            suppressDocEvents = false;
        }
        refreshParams();
    }

    private void refreshParams() {
        List<String> uniqueParams = uniqueInOrder(parseOccurrences(getSqlText()));

        Map<String, ParamRow> existing = new LinkedHashMap<>();
        for (ParamRow row : paramsModel.getRows()) {
            existing.put(stripColon(row.name), row);
        }

        paramsModel.getRows().clear();
        for (String name : uniqueParams) {
            ParamRow prev = existing.get(name);
            paramsModel.getRows().add(prev != null
                    ? new ParamRow(":" + name, prev.type, prev.defaultValue)
                    : new ParamRow(":" + name, ParamType.String, ""));
        }
        paramsModel.fireTableDataChanged();
    }

    private String getSqlText() {
        return document != null ? document.getText() : (cachedSqlText != null ? cachedSqlText : "");
    }

    private void setSqlText(String text) {
        if (document == null) return;
        WriteCommandAction.runWriteCommandAction(getProject(), () -> document.setText(text));
    }

    @NotNull
    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Override
    public void disposeInner() {
        if (document != null) cachedSqlText = document.getText();
        Editors.releaseEditor(editor);
        editor = null;
        document = null;
        super.disposeInner();
    }

    public ToolDefinitionModel getToolDefinitionModel() {
        ToolDefinitionModel model = new ToolDefinitionModel(paramsModel);
        model.setName(toolName.getText());
        model.setDescription(toolDescriptionTextArea.getText());
        model.setSql(getSqlText());
        return model;
    }
}
