package com.dbn.mcp.ui;

import com.dbn.common.color.Colors;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.language.sql.SQLFileType;
import com.dbn.language.sql.SQLLanguage;
import com.dbn.mcp.model.ParamRow;
import com.dbn.mcp.model.ParamType;
import com.dbn.mcp.model.ToolDefinitionModel;
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
import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractAction;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import static com.dbn.common.ui.util.Buttons.onButtonClick;
import static com.dbn.common.util.Strings.isNotEmptyOrSpaces;
import static com.dbn.common.util.Strings.isWord;
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
    private JButton testSqlButton;
    private JLabel sqlTestStatusLabel;

    private final ConnectionHandler connection;
    private ParamTableModel paramsModel;
    private Document document;
    private EditorEx editor;
    private String cachedSqlText;
    private boolean suppressDocEvents;
    private String lastTestedSql;
    private boolean hasSqlTestResult;
    private boolean lastSqlTestPassed;

    public ToolDefinitionCreateForm(Disposable parent, @NotNull ConnectionHandler connection) {
        this(parent, connection, null);
    }

    public ToolDefinitionCreateForm(Disposable parent, @NotNull ConnectionHandler connection, @Nullable ToolDefinitionModel existing) {
        super(parent);
        this.connection = connection;
        initParamsTable();
        initTestButton();

        if (existing != null) {
            toolName.setText(existing.getName());
            toolDescriptionTextArea.setText(existing.getDescription());
            cachedSqlText = existing.getStatement();

            if (existing.getParamsModel() != null) {
                for (ParamRow row : existing.getParamsModel().getRows()) {
                    paramsModel.getRows().add(new ParamRow(row.getName(), row.getType(), row.getTestValue(), row.getDescription(), row.isRequired()));
                }
                paramsModel.fireTableDataChanged();
            }
        }

        updateSqlTestStatus();
        whenFirstShown(this::initEditor);
    }

    @Override
    protected void initValidation() {
        addTextValidation(toolName, n -> isNotEmptyOrSpaces(n), "Please enter a tool name");
        addTextValidation(toolName, n -> !n.contains(" "), "Tool name cannot contain spaces");
        addTextValidation(toolName, n -> isWord(n), "Tool name can only contain letters, digits, and underscores");
        addValidation(sqlEditorPanel, c -> getSqlText().isBlank() ? "Please enter a SQL query" : null);
    }

    private void initParamsTable() {
        paramsModel = new ParamTableModel(false);
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

    private void initTestButton() {
        testSqlButton.setToolTipText("Open a dedicated SQL tester dialog for parameter input and preview results.");
        onButtonClick(testSqlButton, e -> {
            try {
                openSqlTestDialog();
            } catch (Exception ex) {
                Messages.showErrorDialog(getProject(), "Failed to open SQL tester", ex);
            }
        });
    }

    public void openSqlTestDialog() {
        List<ParamRow> testParams = copyRows(paramsModel.getRows());
        ToolDefinitionSqlTestDialog dialog = new ToolDefinitionSqlTestDialog(connection, getSqlText(), testParams);
        dialog.show();
        applyTestValues(dialog.getParamRows());
        if (dialog.hasVerificationRun()) {
            hasSqlTestResult = true;
            lastSqlTestPassed = dialog.isLastVerificationSuccessful();
            lastTestedSql = getSqlText();
        }
        updateSqlTestStatus();
        validateFormFields();
    }

    private void applyTestValues(List<ParamRow> testRows) {
        Map<String, ParamRow> testedByName = new LinkedHashMap<>();
        for (ParamRow row : testRows) {
            testedByName.put(stripColon(row.getName()), row);
        }

        for (ParamRow row : paramsModel.getRows()) {
            ParamRow tested = testedByName.get(stripColon(row.getName()));
            if (tested != null) {
                row.setTestValue(tested.getTestValue());
            }
        }

        paramsModel.fireTableDataChanged();
    }

    private static List<ParamRow> copyRows(List<ParamRow> rows) {
        List<ParamRow> copy = new ArrayList<>();
        for (ParamRow row : rows) {
            copy.add(new ParamRow(row.getName(), row.getType(), row.getTestValue(), row.getDescription(), row.isRequired()));
        }
        return copy;
    }

    private void initEditor() {
        Project project = getProject();
        if (project == null) return;

        McpToolSqlVirtualFile sqlFile = new McpToolSqlVirtualFile(connection, "");
        DatabaseFileViewProvider viewProvider = new DatabaseFileViewProvider(project, sqlFile, true);
        PsiFile psiFile = sqlFile.initializePsiFile(viewProvider, SQLLanguage.INSTANCE);

        document = Documents.ensureDocument(psiFile);
        if (cachedSqlText != null) setSqlText(cachedSqlText);
        editor = Editors.createEditor(document, project, sqlFile, SQLFileType.INSTANCE);
        Editors.initEditorHighlighter(editor, SQLLanguage.INSTANCE, connection);
        configureEditor(editor);

        document.addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                if (suppressDocEvents) return;
                refreshParams();
                updateSqlTestStatus();
                validateFormFields();
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

        String name = stripColon(paramsModel.getRows().get(row).getName());
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
        refreshParams(getSqlText());
    }

    private void refreshParams(String sqlText) {
        List<String> uniqueParams = uniqueInOrder(parseOccurrences(sqlText));

        Map<String, ParamRow> existing = new LinkedHashMap<>();
        for (ParamRow row : paramsModel.getRows()) {
            existing.put(stripColon(row.getName()), row);
        }

        paramsModel.getRows().clear();
        for (String name : uniqueParams) {
            ParamRow prev = existing.get(name);
            paramsModel.getRows().add(prev != null
                    ? new ParamRow(":" + name, prev.getType(), prev.getTestValue(), prev.getDescription(), prev.isRequired())
                    : new ParamRow(":" + name, ParamType.STRING, "", "", false));
        }
        paramsModel.fireTableDataChanged();
    }

    private void updateSqlTestStatus() {
        String currentSql = getSqlText();

        if (!hasSqlTestResult || lastTestedSql == null) {
            sqlTestStatusLabel.setForeground(Colors.HINT_COLOR);
            sqlTestStatusLabel.setText("Not tested yet. Open tester to verify SQL and preview results.");
            return;
        }

        if (!Objects.equals(lastTestedSql, currentSql)) {
            sqlTestStatusLabel.setForeground(Colors.HINT_COLOR);
            sqlTestStatusLabel.setText("Query changed since last test. Please run tester again.");
            return;
        }

        if (lastSqlTestPassed) {
            sqlTestStatusLabel.setForeground(Colors.SUCCESS_COLOR);
            sqlTestStatusLabel.setText("Test passed for current query.");
        } else {
            sqlTestStatusLabel.setForeground(Colors.FAILURE_COLOR);
            sqlTestStatusLabel.setText("Last test failed for current query.");
        }
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

    public boolean hasPassingTestForCurrentSql() {
        String currentSql = getSqlText();
        return hasSqlTestResult &&
                lastSqlTestPassed &&
                lastTestedSql != null &&
                Objects.equals(lastTestedSql, currentSql);
    }

    public String getSqlTestStatusSummary() {
        String currentSql = getSqlText();
        if (!hasSqlTestResult || lastTestedSql == null) {
            return "not tested";
        }
        if (!Objects.equals(lastTestedSql, currentSql)) {
            return "changed since last test";
        }
        return lastSqlTestPassed ? "test passed" : "last test failed";
    }

    public ToolDefinitionModel getToolDefinitionModel() {
        ToolDefinitionModel model = new ToolDefinitionModel(paramsModel);
        model.setName(toolName.getText());
        model.setDescription(toolDescriptionTextArea.getText());
        model.setStatement(getSqlText());
        return model;
    }
}
