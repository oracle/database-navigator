package com.dbn.mcp.ui;

import com.dbn.common.icon.Icons;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.link.DBNHyperlinkLabel;
import com.dbn.common.ui.misc.DBNExpandableTextField;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.language.sql.SQLFileType;
import com.dbn.language.sql.SQLLanguage;
import com.dbn.mcp.model.McpServerDefinition;
import com.dbn.mcp.model.McpToolDefinition;
import com.dbn.mcp.model.McpToolParam;
import com.dbn.mcp.model.McpToolParamType;
import com.dbn.mcp.util.McpToolDefinitions;
import com.dbn.mcp.util.McpToolDescription;
import com.dbn.mcp.vfs.McpToolSqlVirtualFile;
import com.dbn.vfs.DatabaseFileViewProvider;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractAction;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.link.Hyperlinks.onHyperlinkAccess;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.setText;
import static com.dbn.common.util.Messages.showErrorDialog;
import static com.dbn.common.util.Strings.isEmpty;
import static com.dbn.common.util.Strings.isNotEmptyOrSpaces;
import static com.dbn.mcp.util.SqlParameterParser.parseOccurrences;
import static com.dbn.mcp.util.SqlParameterParser.stripColon;
import static com.dbn.mcp.util.SqlParameterParser.uniqueInOrder;
import static com.intellij.util.ui.UIUtil.getContextHelpForeground;

public class McpToolDefinitionForm extends DBNFormBase {
    private JPanel mainPanel;
    private JTextField nameTextField;
    private JPanel sqlEditorPanel;
    private JTable paramsTable;
    private JScrollPane paramsScrollPane;
    private DBNExpandableTextField descriptionTextField;
    private JLabel verifiedLabel;
    private DBNHyperlinkLabel verifyHyperlink;
    private JPanel headerPanel;
    private JPanel hintPanel;

    private final ConnectionRef connection;
    private Document document;
    private EditorEx editor;
    private boolean suppressDocEvents;


    private final @Getter McpServerDefinition serverDefinition;
    private final @Getter McpToolDefinition toolDefinition;
    private ParamTableModel paramsModel;

    private String verifiedStatement;

    public McpToolDefinitionForm(
            Disposable parent,
            @NotNull ConnectionHandler connection,
            @NotNull McpServerDefinition serverDefinition,
            @Nullable McpToolDefinition toolDefinition) {

        super(parent);
        this.connection = connection.ref();
        this.serverDefinition = serverDefinition;
        this.toolDefinition = toolDefinition == null ? new McpToolDefinition() : toolDefinition;
        if (this.toolDefinition.isVerified()) {
            verifiedStatement = this.toolDefinition.getStatement();
        }

        initHeaderPanel();
        initHintPanel();
        initParamsTable();
        initVerificationFields();

        resetFormChanges();
        whenFirstShown(this::initStatementEditor);
    }

    private ConnectionHandler getConnection() {
        return connection.ensure();
    }

    private void initHeaderPanel() {
        ConnectionHandler connection = getConnection();
        DBNHeaderForm headerForm = new DBNHeaderForm(this, connection);
        headerPanel.add(headerForm.getComponent());
    }

    private void initHintPanel() {
        TextContent hintContent = TextContent.plain(
                "MCP Tool Builder turns a SQL statement into a callable tool for this MCP server.\n\n" +
                "Name and describe the tool so MCP clients can choose it correctly. Use named SQL " +
                "parameters such as :employee_id; the parameter list is derived from the statement " +
                "and lets you define types, required flags, descriptions, and test values. Run Verify " +
                "to execute the query with sample values before saving.");
        hintPanel.add(new DBNHintForm(this, hintContent, null, true).getComponent());
    }

    @Override
    protected void initValidation() {
        addTextValidation(nameTextField, field -> validateToolName(field.getText()));
        addTextValidation(descriptionTextField, field -> McpToolDescription.validationError(field.getText()));
        addValidation(sqlEditorPanel, c -> getSqlStatement().isBlank() ? txt("msg.mcp.error.SqlQueryEmpty") : null);
    }

    private String validateToolName(String value) {
        Set<String> toolNames = getUsedToolNames();
        return McpToolDefinitions.validationError(value, toolNames);
    }

    private Set<String> getUsedToolNames() {
        return serverDefinition
                .getToolNames()
                .stream()
                .filter(n -> !n.equalsIgnoreCase(toolDefinition.getName()))
                .collect(Collectors.toSet());
    }

    private void initParamsTable() {
        paramsModel = new ParamTableModel(toolDefinition, false);
        paramsTable.setModel(paramsModel);
        paramsTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        paramsTable.setDefaultEditor(McpToolParamType.class, new DefaultCellEditor(new JComboBox<>(McpToolParamType.values())));

        paramsTable.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteParam");
        paramsTable.getActionMap().put("deleteParam", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelectedParam();
            }
        });
    }

    private void initVerificationFields() {
        verifiedLabel.setForeground(getContextHelpForeground());
        verifiedLabel.setIcon(Icons.COMMON_CHECK);


        verifyHyperlink.setHyperlinkText("Verify");
        onHyperlinkAccess(verifyHyperlink, e -> {
            if (getSqlStatement().isBlank()) {
                showErrorDialog(getProject(), txt("msg.mcp.error.SqlQueryRequired"));
                return;
            }
            try {
                openSqlTestDialog();
            } catch (Exception ex) {
                showErrorDialog(getProject(), txt("msg.mcp.error.SqlTesterOpenFailed"), ex);
            }
        });
    }

    public void openSqlTestDialog() {
        ConnectionHandler connection = getConnection();
        List<McpToolParam> testParams = copyRows(paramsModel.getRows());
        McpToolVerificationDialog dialog = new McpToolVerificationDialog(connection, getSqlStatement(), testParams);
        dialog.show();
        applyTestValues(dialog.getParamRows());
        if (dialog.isStatementVerified()) {
            verifiedStatement = getSqlStatement();
        }
        updateFieldAvailability();
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
        ConnectionHandler connection = getConnection();
        Project project = connection.getProject();
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
            updateFieldAvailability();
            validateFormFields();
        });

        sqlEditorPanel.add(editor.getComponent(), BorderLayout.CENTER);
    }

    private void configureEditor(EditorEx editor) {
        editor.setEmbeddedIntoDialogWrapper(true);
        //editor.setPlaceholder("SELECT * FROM employees WHERE department_id = :dept_id"); // TODO interface already too crowded
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

    public boolean isStatementVerified() {
        if (isEmpty(verifiedStatement)) return false;

        String statement = getSqlStatement();
        if (isEmpty(statement)) return false;

        String verified = verifiedStatement.replaceAll("\\s+", "");
        String current = statement.replaceAll("\\s+", "");
        return Objects.equals(verified, current);
    }

    @Override
    protected void initFieldAvailability() {
        DBNFormFieldAdapter fieldAdapter = getFieldAdapter();
        fieldAdapter.initFieldsVisibility(() -> isStatementVerified(), array(verifiedLabel));
        fieldAdapter.initFieldsVisibility(() -> isNotEmptyOrSpaces(getSqlStatement()) && !isStatementVerified(), array(verifyHyperlink));
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

    @Override
    public void resetFormChanges() {
        setText(nameTextField, toolDefinition.getName());
        setText(descriptionTextField, toolDefinition.getDescription());
        //Documents.setText(document, toolDefinition.getStatement());
    }

    @Override
    public void applyFormChanges() {
        toolDefinition.setName(getText(nameTextField));
        toolDefinition.setDescription(getText(descriptionTextField));
        toolDefinition.setStatement(getSqlStatement());
        toolDefinition.setVerified(isStatementVerified());
    }

    private String getSqlStatement() {
        return document == null ? toolDefinition.getStatement() : Documents.getText(document);
    }
}
