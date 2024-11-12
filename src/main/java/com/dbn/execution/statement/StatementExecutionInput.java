package com.dbn.execution.statement;

import com.dbn.common.latent.Latent;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.connection.session.DatabaseSession;
import com.dbn.execution.ExecutionOption;
import com.dbn.execution.ExecutionTarget;
import com.dbn.execution.LocalExecutionInput;
import com.dbn.execution.common.options.ExecutionEngineSettings;
import com.dbn.execution.statement.options.StatementExecutionSettings;
import com.dbn.execution.statement.processor.StatementExecutionProcessor;
import com.dbn.execution.statement.variables.StatementExecutionVariablesBundle;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.psi.ExecutableBundlePsiElement;
import com.dbn.language.common.psi.ExecutablePsiElement;
import com.dbn.language.sql.SQLLanguage;
import com.intellij.psi.PsiElement;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.dispose.Disposer.replace;
import static com.dbn.database.DatabaseFeature.DATABASE_LOGGING;

@Getter
@Setter
public class StatementExecutionInput extends LocalExecutionInput {
    private StatementExecutionProcessor executionProcessor;
    private StatementExecutionVariablesBundle executionVariables;

    private String originalStatementText;
    private String executableStatementText;
    private boolean bulkExecution = false;

    private final Latent<ExecutablePsiElement> executablePsiElement = Latent.basic(() -> {
        ConnectionHandler connection = getConnection();
        if (isNotValid(connection)) return null;

        DBLanguagePsiFile psiFile = getExecutionProcessor().getPsiFile();
        if (isNotValid(psiFile)) return null;

        DBLanguageDialect languageDialect = psiFile.getLanguageDialect();
        if (languageDialect == null) return null;

        DBLanguagePsiFile previewFile = DBLanguagePsiFile.createFromText(
                getProject(),
                "preview",
                languageDialect,
                getOriginalStatementText(),
                connection,
                getTargetSchemaId());
        if (isNotValid(previewFile)) return null;

        PsiElement firstChild = previewFile.getFirstChild();
        if (firstChild instanceof ExecutableBundlePsiElement) {
            ExecutableBundlePsiElement rootPsiElement = (ExecutableBundlePsiElement) firstChild;
            List<ExecutablePsiElement> executablePsiElements = rootPsiElement.getExecutablePsiElements();
            return executablePsiElements.isEmpty() ? null : executablePsiElements.get(0);
        }

        return null;
    });


    public StatementExecutionInput(String originalStatementText, String executableStatementText, StatementExecutionProcessor executionProcessor) {
        super(executionProcessor.getProject(), ExecutionTarget.STATEMENT);
        this.executionProcessor = executionProcessor;
        ConnectionHandler connection = executionProcessor.getConnection();
        SchemaId currentSchema = executionProcessor.getTargetSchema();
        DatabaseSession targetSession = executionProcessor.getTargetSession();

        this.setTargetConnection(connection);
        this.setTargetSchemaId(currentSchema);
        this.setTargetSession(targetSession);
        this.originalStatementText = originalStatementText;
        this.executableStatementText = executableStatementText;

        if (DATABASE_LOGGING.isSupported(connection)) {
            getOptions().set(ExecutionOption.ENABLE_LOGGING, connection.isLoggingEnabled());
        }
    }

    @Override
    protected StatementExecutionContext createExecutionContext() {
        return new StatementExecutionContext(this);
    }

    public int getExecutableLineNumber() {
        return executionProcessor == null ? 0 : executionProcessor.getExecutableLineNumber();
    }

    public void setOriginalStatementText(String originalStatementText) {
        this.originalStatementText = originalStatementText;
        executablePsiElement.reset();
    }

    @Nullable
    public ExecutablePsiElement getExecutablePsiElement() {
        return executablePsiElement.get();
    }

    public void setExecutionVariables(StatementExecutionVariablesBundle executionVariables) {
        this.executionVariables = replace(this.executionVariables, executionVariables);
    }

    public DBLanguagePsiFile createPreviewFile() {
        ConnectionHandler connection = getConnection();
        SchemaId schema = getTargetSchemaId();
        DBLanguageDialect languageDialect = connection == null ?
                SQLLanguage.INSTANCE.getMainLanguageDialect() :
                connection.getLanguageDialect(SQLLanguage.INSTANCE);

        return DBLanguagePsiFile.createFromText(
                getProject(),
                "preview",
                languageDialect,
                executableStatementText,
                connection,
                schema);
    }

    @Override
    @Nullable
    public ConnectionHandler getConnection() {
        return getTargetConnection();
    }

    @Override
    public boolean hasExecutionVariables() {
        return true;
    }

    @Override
    public boolean isSchemaSelectionAllowed() {
        return false;
    }

    @Override
    public boolean isSessionSelectionAllowed() {
        return false;
    }

    public void setTargetConnection(ConnectionHandler connection) {
        super.setTargetConnection(connection);
        if (DATABASE_LOGGING.isSupported(connection)) {
            getOptions().set(ExecutionOption.ENABLE_LOGGING, connection.isLoggingEnabled());
        }
    }

    public ConnectionId getConnectionId() {
        return getTargetConnection() == null ? null : getTargetConnection().getConnectionId();
    }

    public String getStatementDescription() {
        ExecutablePsiElement executablePsiElement = getExecutablePsiElement();
        return executablePsiElement == null ? "SQL Statement" : executablePsiElement.getPresentableText();
    }

    @Override
    public boolean isDatabaseLogProducer() {
        ExecutablePsiElement executablePsiElement = getExecutablePsiElement();
        return executablePsiElement != null && executablePsiElement.elementType.is(ElementTypeAttribute.DATABASE_LOG_PRODUCER);
    }


    public StatementExecutionSettings getStatementExecutionSettings() {
        ExecutionEngineSettings settings = ExecutionEngineSettings.getInstance(getProject());
        return settings.getStatementExecutionSettings();
    }

    public int getResultSetFetchBlockSize() {
        return getStatementExecutionSettings().getResultSetFetchBlockSize();
    }

}
