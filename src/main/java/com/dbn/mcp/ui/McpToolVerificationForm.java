package com.dbn.mcp.ui;

import com.dbn.common.color.Colors;
import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.misc.DBNScrollPane;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.PooledConnection;
import com.dbn.connection.Resources;
import com.dbn.connection.jdbc.DBNResultSet;
import com.dbn.connection.jdbc.DBNStatement;
import com.dbn.data.grid.ui.table.resultSet.ResultSetTable;
import com.dbn.data.model.resultSet.ResultSetDataModel;
import com.dbn.data.record.RecordViewInfo;
import com.dbn.data.type.GenericDataType;
import com.dbn.execution.statement.variables.StatementExecutionVariable;
import com.dbn.execution.statement.variables.StatementExecutionVariablesBundle;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.sql.SQLFileType;
import com.dbn.language.sql.SQLLanguage;
import com.dbn.mcp.model.McpToolParam;
import com.dbn.mcp.model.McpToolParamType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.dbn.common.ui.util.Buttons.onButtonClickAsync;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.mcp.util.SqlParameterParser.parseOccurrences;
import static com.dbn.mcp.util.SqlParameterParser.stripColon;
import static com.dbn.mcp.util.SqlParameterParser.uniqueInOrder;

public class McpToolVerificationForm extends DBNFormBase {
    private static final int PREVIEW_ROW_LIMIT = 200;

    private JPanel mainPanel;
    private JPanel headerPanel;
    private DBNScrollPane variablesScrollPane;
    private JPanel variablesPanel;
    private JPanel executionOptionsPanel;
    private JButton verifyButton;
    private JPanel previewPanel;
    private JPanel outputPanel;
    private DBNScrollPane outputScrollPane;

    private final ConnectionHandler connection;
    private final String statement;

    private final List<McpToolVerificationParamForm> variableValueForms = DisposableContainers.list(this);
    private StatementExecutionVariablesBundle executionVariables = new StatementExecutionVariablesBundle(Collections.emptyList());
    private final Map<String, McpToolParam> paramMetadata = new LinkedHashMap<>();

    private ResultSetTable outputTable;
    private EditorEx previewViewer;
    private Document previewDocument;
    private boolean verificationRun;
    private boolean lastVerificationSuccessful;

    public McpToolVerificationForm(@NotNull Disposable parent,
                                   @NotNull ConnectionHandler connection,
                                   @NotNull String statement,
                                   @NotNull List<McpToolParam> params) {
        super(parent);
        this.connection = connection;
        this.statement = statement;

        for (McpToolParam row : params) {
            String name = stripColon(row.getName());
            paramMetadata.put(name, new McpToolParam(":" + name, row.getType(), row.getTestValue(), row.getDescription(), row.isRequired()));
        }

        initHeaderPanel();
        initVariablesPanel();
        initVerifyButton();
        initOutputPanel();

        whenFirstShown(() -> {
            initResolvedPreviewViewer();
            rebuildVariablesFromSql(statement);
            updateResolvedPreview();
        });
    }

    private void initHeaderPanel() {
        DBNHeaderForm headerForm = new DBNHeaderForm(this, connection);
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);
    }

    private void initVariablesPanel() {
        variablesPanel.setLayout(new BoxLayout(variablesPanel, BoxLayout.Y_AXIS));
    }

    private void initResolvedPreviewViewer() {
        Project project = getProject();
        if (project == null) return;

        DBLanguageDialect languageDialect = connection.getLanguageDialect(SQLLanguage.INSTANCE);
        DBLanguagePsiFile previewFile = DBLanguagePsiFile.createFromText(
                project,
                "mcp-preview.sql",
                languageDialect,
                "",
                connection,
                null);

        previewDocument = previewFile == null ? Documents.createDocument("") : Documents.ensureDocument(previewFile);

        previewViewer = Editors.createEditor(previewDocument, project, null, SQLFileType.INSTANCE);
        previewViewer.setEmbeddedIntoDialogWrapper(true);
        Editors.initEditorHighlighter(previewViewer, SQLLanguage.INSTANCE, connection);
        Editors.setEditorReadonly(previewViewer, true);

        EditorSettings settings = previewViewer.getSettings();
        settings.setFoldingOutlineShown(false);
        settings.setLineMarkerAreaShown(false);
        settings.setLineNumbersShown(false);
        settings.setVirtualSpace(false);
        settings.setDndEnabled(false);
        settings.setAdditionalLinesCount(2);
        settings.setRightMarginShown(false);

        Editors.updateEditorScrollPane(previewViewer);
        Editors.installEditorLayoutUpdater(previewViewer, this);

        previewPanel.add(previewViewer.getComponent());
    }

    private void rebuildVariablesFromSql(String sqlText) {
        Map<String, VariableState> previous = snapshotVariableStates();

        clearVariableForms();
        executionVariables = new StatementExecutionVariablesBundle(Collections.emptyList());

        List<String> parameterNames = uniqueInOrder(parseOccurrences(sqlText));
        for (String parameterName : parameterNames) {
            McpToolParam metadata = paramMetadata.computeIfAbsent(parameterName,
                    name -> new McpToolParam(":" + name, McpToolParamType.STRING, "", "", false));
            VariableState state = previous.get(parameterName);

            StatementExecutionVariable variable = new StatementExecutionVariable();
            variable.setName(parameterName);
            variable.setDataType(state == null ? mapGenericType(metadata.getType()) : state.dataType);
            variable.setValue(state == null ? normalize(metadata.getTestValue()) : state.value);
            executionVariables.getVariables().add(variable);

            Project project = getProject();
            if (project == null) project = connection.getProject();
            McpToolVerificationParamForm paramForm =
                    new McpToolVerificationParamForm(this, project, variable, this::updateResolvedPreview);
            variableValueForms.add(paramForm);
            variablesPanel.add(paramForm.getComponent());
            onTextChange(paramForm.getEditorComponent(), e -> updateResolvedPreview());
        }

        updateFieldAlignment();
        Dimension preferredSize = variablesScrollPane.getPreferredSize();
        preferredSize.setSize(preferredSize.getWidth() + 20, preferredSize.getHeight());
        variablesScrollPane.setPreferredSize(preferredSize);
        variablesPanel.revalidate();
        variablesPanel.repaint();
    }

    private Map<String, VariableState> snapshotVariableStates() {
        Map<String, VariableState> state = new LinkedHashMap<>();
        for (StatementExecutionVariable variable : executionVariables.getVariables()) {
            GenericDataType dataType = variable.getDataType();
            String value = variable.getPreviewValueProvider().getValue();
            state.put(variable.getName(), new VariableState(dataType, normalize(value)));
        }
        return state;
    }

    private void clearVariableForms() {
        while (!variableValueForms.isEmpty()) {
            McpToolVerificationParamForm form = variableValueForms.remove(0);
            variablesPanel.remove(form.getComponent());
        }
    }

    private static GenericDataType mapGenericType(McpToolParamType type) {
        if (type == McpToolParamType.INTEGER || type == McpToolParamType.NUMBER) {
            return GenericDataType.NUMERIC;
        }
        return GenericDataType.LITERAL;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private void initVerifyButton() {
        onButtonClickAsync(verifyButton, () -> verifyQuery(), q -> applyQueryResult(q));
    }

    private void initOutputPanel() {
        RecordViewInfo recordViewInfo = new RecordViewInfo("Query data", null);
        ResultSetDataModel dataModel = new ResultSetDataModel<>(connection);
        outputTable = new ResultSetTable<>(this, dataModel, true, recordViewInfo);
        outputScrollPane.setViewportView(outputTable);
        outputTable.installValuePopupAddon();
        outputPanel.setBorder(Borders.lineBorder(Colors.getOutlineColor()));
        outputTable.setLoading(false);
    }

    @Override
    protected void initFieldAlignment() {
        FieldAlignerData alignerData = getFieldAlignerData();
        alignerData.registerForms(() -> variableValueForms);
    }

    private void applyQueryResult(QueryVerificationResult result) {
        try {
            outputTable.setModel(result.data);
            verificationRun = true;
            lastVerificationSuccessful = result.error == null;
            if (result.error != null) {
                Messages.showErrorDialog(connection.getProject(), "Failed to verify query", result.error);
            }
        } finally {
            stopActivityNotifier();
        }
    }

    private QueryVerificationResult verifyQuery() {
        QueryVerificationResult emptyResult = new QueryVerificationResult(new ResultSetDataModel(connection), null);
        if (getStatement().isBlank()) {
            Messages.showErrorDialog(getProject(), "Please provide a SQL query in the tool definition before running preview.");
            return emptyResult;
        }
        startActivityNotifier();

        try {
            ResultSetDataModel result = executeStatement();
            return new QueryVerificationResult(result, null);
        } catch (Exception e) {
            return emptyResult;
        }
    }

    private ResultSetDataModel executeStatement() throws SQLException {
        syncVariablesFromInput();

        String sql = executionVariables.prepareStatementText(connection, getStatement(), false);
        if (executionVariables.hasErrors()) {
            throw new IllegalStateException(buildValidationMessage());
        }

        return PooledConnection.call(connection.createConnectionContext(), conn -> {
            DBNStatement<?> sqlStatement = null;
            DBNResultSet resultSet = null;
            try {
                sqlStatement = conn.createStatement();
                sqlStatement.setFetchSize(PREVIEW_ROW_LIMIT);

                boolean hasResultSet = sqlStatement.execute(sql);
                if (!hasResultSet) {
                    throw new IllegalStateException("Only queries returning rows can be previewed.");
                }

                resultSet = sqlStatement.getResultSet();
                ResultSetDataModel dataModel = new ResultSetDataModel(resultSet, connection, -1);
                dataModel.fetchNextRecords(PREVIEW_ROW_LIMIT, false);
                return dataModel;
            } finally {
                Resources.close(resultSet);
                Resources.close(sqlStatement);
            }
        });
    }

    private void syncVariablesFromInput() {
        for (McpToolVerificationParamForm variableForm : variableValueForms) {
            variableForm.saveValue();
        }
    }

    private String buildValidationMessage() {
        StringBuilder builder = new StringBuilder("Please fix SQL parameter values before executing:\n");
        for (StatementExecutionVariable variable : executionVariables.getVariables()) {
            String error = executionVariables.getError(variable);
            if (error != null) {
                builder.append("- :").append(variable.getName()).append(" - ").append(error).append('\n');
            }
        }
        return builder.toString().trim();
    }

    private void updateResolvedPreview() {
        if (previewDocument == null) return;

        String sqlText = getStatement();
        if (sqlText.isBlank()) {
            Documents.setText(previewDocument, "");
            return;
        }

        syncVariablesFromInput();
        String previewText = executionVariables.prepareStatementText(connection, sqlText, true);

        if (executionVariables.hasErrors()) {
            StringBuilder builder = new StringBuilder(previewText);
            builder.append("\n\n-- Value issues\n");
            for (StatementExecutionVariable variable : executionVariables.getVariables()) {
                String error = executionVariables.getError(variable);
                if (error != null) {
                    builder.append("-- :").append(variable.getName()).append(" - ").append(error).append('\n');
                }
            }
            previewText = builder.toString();
        }

        Documents.setText(previewDocument, previewText);
    }

    private void startActivityNotifier() {
        outputTable.setLoading(true);
    }

    private void stopActivityNotifier() {
        outputTable.setLoading(false);
    }

    public List<McpToolParam> getParamRows() {
        syncVariablesFromInput();

        List<McpToolParam> rows = new ArrayList<>();
        List<String> parameterNames = uniqueInOrder(parseOccurrences(getStatement()));
        for (String parameterName : parameterNames) {
            McpToolParam metadata = paramMetadata.computeIfAbsent(parameterName,
                    name -> new McpToolParam(":" + name, McpToolParamType.STRING, "", "", false));

            StatementExecutionVariable variable = executionVariables.getVariable(parameterName);
            String testValue = variable == null ? metadata.getTestValue() : normalize(variable.getValue());
            rows.add(new McpToolParam(
                    ":" + parameterName,
                    metadata.getType(),
                    testValue,
                    metadata.getDescription(),
                    metadata.isRequired()));
        }
        return rows;
    }

    public String getStatement() {
        return statement == null ? "" : statement;
    }

    public boolean hasVerificationRun() {
        return verificationRun;
    }

    public boolean isLastVerificationSuccessful() {
        return lastVerificationSuccessful;
    }

    @NotNull
    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Override
    public void disposeInner() {
        Editors.releaseEditor(previewViewer);
        previewViewer = null;
        previewDocument = null;
        super.disposeInner();
    }

    private static class VariableState {
        private final GenericDataType dataType;
        private final String value;

        private VariableState(GenericDataType dataType, String value) {
            this.dataType = dataType;
            this.value = value;
        }
    }

    private static class QueryVerificationResult {
        private final ResultSetDataModel data;
        private final Exception error;

        private QueryVerificationResult(ResultSetDataModel data, Exception error) {
            this.data = data;
            this.error = error;
        }
    }
}
