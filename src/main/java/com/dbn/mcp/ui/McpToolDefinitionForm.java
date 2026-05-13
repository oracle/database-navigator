package com.dbn.mcp.ui;

import com.dbn.common.color.Colors;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.language.sql.SQLFileType;
import com.dbn.language.sql.SQLLanguage;
import com.dbn.mcp.model.McpServerDefinition;
import com.dbn.mcp.model.McpToolDefinition;
import com.dbn.mcp.model.McpToolParam;
import com.dbn.mcp.model.McpToolParamType;
import com.dbn.mcp.util.McpToolDefinitions;
import com.dbn.mcp.util.McpToolDescription;
import com.dbn.mcp.util.McpToolName;
import com.dbn.mcp.vfs.McpToolSqlVirtualFile;
import com.dbn.vfs.DatabaseFileViewProvider;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.ui.components.fields.ExpandableTextField;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
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
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.setText;
import static com.dbn.mcp.util.SqlParameterParser.parseOccurrences;
import static com.dbn.mcp.util.SqlParameterParser.stripColon;
import static com.dbn.mcp.util.SqlParameterParser.uniqueInOrder;

public class McpToolDefinitionForm extends DBNFormBase {
    private JPanel mainPanel;
    private JTextField nameTextField;
    private JPanel sqlEditorPanel;
    private JTable paramsTable;
    private JScrollPane paramsScrollPane;
    private JButton testSqlButton;
    private JLabel sqlTestStatusLabel;
    private ExpandableTextField descriptionTextField;

    private final ConnectionHandler connection;
    private Document document;
    private EditorEx editor;
    private boolean suppressDocEvents;
    private String lastTestedSql;
    private boolean hasSqlTestResult;
    private boolean lastSqlTestPassed;


    private final @Getter McpServerDefinition serverDefinition;
    private final @Getter McpToolDefinition toolDefinition;
    private ParamTableModel paramsModel;

    public McpToolDefinitionForm(
            Disposable parent,
            @NotNull ConnectionHandler connection,
            @NotNull McpServerDefinition serverDefinition,
            @Nullable McpToolDefinition toolDefinition) {

        super(parent);
        this.connection = connection;
        this.serverDefinition = serverDefinition;
        this.toolDefinition = toolDefinition == null ? new McpToolDefinition() : toolDefinition;

        initParamsTable();
        initTestButton();

        updateSqlTestStatus();
        resetFormChanges();
        whenFirstShown(this::initStatementEditor);
    }

    @Override
    protected void initValidation() {
        addTextValidation(nameTextField, field -> validateToolName(field.getText()));
        addTextValidation(descriptionTextField, field -> McpToolDescription.validationError(field.getText()));
        addValidation(sqlEditorPanel, c -> getSqlStatement().isBlank() ? "Please enter a SQL query" : null);
    }

    private String validateToolName(String value) {
        return McpToolDefinitions.validationError(value, serverDefinition.getToolNames());
    }

    private void initParamsTable() {
        paramsModel = new ParamTableModel(toolDefinition, false);
        paramsTable.setModel(paramsModel);
        paramsTable.setDefaultEditor(McpToolParamType.class, new DefaultCellEditor(new JComboBox<>(McpToolParamType.values())));

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
            if (getSqlStatement().isBlank()) {
                Messages.showErrorDialog(getProject(), "Please enter the SQL query first, then run Test SQL Query.");
                return;
            }
            try {
                openSqlTestDialog();
            } catch (Exception ex) {
                Messages.showErrorDialog(getProject(), "Failed to open SQL tester", ex);
            }
        });
    }

    public void openSqlTestDialog() {
        List<McpToolParam> testParams = copyRows(paramsModel.getRows());
        ToolDefinitionSqlTestDialog dialog = new ToolDefinitionSqlTestDialog(connection, getSqlStatement(), testParams);
        dialog.show();
        applyTestValues(dialog.getParamRows());
        if (dialog.hasVerificationRun()) {
            hasSqlTestResult = true;
            lastSqlTestPassed = dialog.isLastVerificationSuccessful();
            lastTestedSql = getSqlStatement();
        }
        updateSqlTestStatus();
        validateFormFields();
    }

    private void applyTestValues(List<McpToolParam> testRows) {
        Map<String, McpToolParam> testedByName = new LinkedHashMap<>();
        for (McpToolParam row : testRows) {
            testedByName.put(stripColon(row.getName()), row);
        }

        for (McpToolParam row : paramsModel.getRows()) {
            McpToolParam tested = testedByName.get(stripColon(row.getName()));
            if (tested != null) {
                row.setTestValue(tested.getTestValue());
            }
        }

        paramsModel.fireTableDataChanged();
    }

    private static List<McpToolParam> copyRows(List<McpToolParam> rows) {
        List<McpToolParam> copy = new ArrayList<>();
        for (McpToolParam row : rows) {
            copy.add(new McpToolParam(row.getName(), row.getType(), row.getTestValue(), row.getDescription(), row.isRequired()));
        }
        return copy;
    }

    private void initStatementEditor() {
        Project project = ensureProject();
        McpToolSqlVirtualFile sqlFile = new McpToolSqlVirtualFile(connection, toolDefinition.getStatement());
        DatabaseFileViewProvider viewProvider = new DatabaseFileViewProvider(project, sqlFile, true);
        PsiFile psiFile = sqlFile.initializePsiFile(viewProvider, SQLLanguage.INSTANCE);

        document = Documents.ensureDocument(psiFile);
        editor = Editors.createEditor(document, project, sqlFile, SQLFileType.INSTANCE);
        Editors.initEditorHighlighter(editor, SQLLanguage.INSTANCE, connection);
        configureEditor(editor);

        Documents.onDocumentChanged(document, this, e -> {
            if (suppressDocEvents) return;
            refreshParams();
            updateSqlTestStatus();
            validateFormFields();
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
        settings.setLineNumbersShown(false);
        settings.setCaretRowShown(false);
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
        String newSql = getSqlStatement().replaceAll(":" + Pattern.quote(name) + "\\b", "");

        suppressDocEvents = true;
        try {
            Documents.setText(document, newSql);
        } finally {
            suppressDocEvents = false;
        }
        refreshParams();
    }

    private void refreshParams() {
        refreshParams(getSqlStatement());
    }

    private void refreshParams(String sqlText) {
        List<String> uniqueParams = uniqueInOrder(parseOccurrences(sqlText));

        Map<String, McpToolParam> existing = new LinkedHashMap<>();
        for (McpToolParam row : paramsModel.getRows()) {
            existing.put(stripColon(row.getName()), row);
        }

        paramsModel.getRows().clear();
        for (String name : uniqueParams) {
            McpToolParam prev = existing.get(name);
            paramsModel.getRows().add(prev != null
                    ? new McpToolParam(":" + name, prev.getType(), prev.getTestValue(), prev.getDescription(), prev.isRequired())
                    : new McpToolParam(":" + name, McpToolParamType.STRING, "", "", false));
        }
        paramsModel.fireTableDataChanged();
    }

    private void updateSqlTestStatus() {
        String currentSql = getSqlStatement();

        if (!hasSqlTestResult || lastTestedSql == null) {
            sqlTestStatusLabel.setForeground(UIUtil.getContextHelpForeground());
            sqlTestStatusLabel.setText("Not tested yet. Open tester to verify SQL and preview results.");
            return;
        }

        if (!Objects.equals(lastTestedSql, currentSql)) {
            sqlTestStatusLabel.setForeground(UIUtil.getContextHelpForeground());
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


    @NotNull
    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Override
    public void disposeInner() {
        Editors.releaseEditor(editor);
        editor = null;
        document = null;
        super.disposeInner();
    }

    public boolean hasPassingTestForCurrentSql() {
        String currentSql = getSqlStatement();
        return hasSqlTestResult &&
                lastSqlTestPassed &&
                lastTestedSql != null &&
                Objects.equals(lastTestedSql, currentSql);
    }

    public String getSqlTestStatusSummary() {
        String currentSql = getSqlStatement();
        if (!hasSqlTestResult || lastTestedSql == null) {
            return "not tested";
        }
        if (!Objects.equals(lastTestedSql, currentSql)) {
            return "changed since last test";
        }
        return lastSqlTestPassed ? "test passed" : "last test failed";
    }

    @Override
    public void resetFormChanges() {
        setText(nameTextField, toolDefinition.getName());
        setText(descriptionTextField, toolDefinition.getDescription());
        //Documents.setText(document, toolDefinition.getStatement());
    }

    @Override
    public void applyFormChanges() {
        toolDefinition.setName(McpToolName.normalize(getText(nameTextField)));
        toolDefinition.setDescription(McpToolDescription.normalize(getText(descriptionTextField)));
        toolDefinition.setStatement(getSqlStatement());
    }

    private String getSqlStatement() {
        return document == null ? toolDefinition.getStatement() : Documents.getText(document);
    }
}
