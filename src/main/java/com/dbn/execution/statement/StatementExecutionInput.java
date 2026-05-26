/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.execution.statement;

import com.dbn.common.latent.Latent;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.connection.jdbc.DBNPreparedStatement;
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

import java.sql.SQLException;
import java.util.List;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.dispose.Disposer.replace;
import static com.dbn.database.DatabaseFeature.DATABASE_LOGGING;

@Getter
@Setter
public class StatementExecutionInput extends LocalExecutionInput {
    private StatementExecutionProcessor executionProcessor;
    private StatementExecutionVariablesBundle executionVariables;

    private String rawStatementText;
    private String statementText;
    private boolean bulkExecution = false;

    private final Latent<String> previewStatementText = Latent.basic(() -> initPreviewStatementText());
    private final Latent<String> executableStatementText = Latent.basic(() -> initExecutableStatementText());
    private final Latent<ExecutablePsiElement> executablePsiElement = Latent.basic(() -> initExecutablePsiElement());


    public StatementExecutionInput(String rawStatementText, String statementText, StatementExecutionProcessor executionProcessor) {
        super(executionProcessor.getProject(), ExecutionTarget.STATEMENT);
        this.executionProcessor = executionProcessor;
        ConnectionHandler connection = executionProcessor.getConnection();
        SchemaId currentSchema = executionProcessor.getTargetSchema();
        DatabaseSession targetSession = executionProcessor.getTargetSession();

        this.setTargetConnection(connection);
        this.setTargetSchemaId(currentSchema);
        this.setTargetSession(targetSession);
        this.rawStatementText = rawStatementText;
        this.statementText = statementText;

        if (DATABASE_LOGGING.isSupported(connection)) {
            getOptions().set(ExecutionOption.ENABLE_LOGGING, connection.isLoggingEnabled());
        }
    }

    public String getPreviewStatementText() {
        return previewStatementText.get();
    }

    public String getExecutableStatementText() {
        return executableStatementText.get();
    }

    @Nullable
    public ExecutablePsiElement getExecutablePsiElement() {
        return executablePsiElement.get();
    }

    private String initPreviewStatementText() {
        if (executionVariables == null) return statementText;

        ConnectionHandler connection = getConnection();
        if (connection == null) return statementText;

        return executionVariables.preparePreviewStatementText(connection, statementText);
    }

    private String initExecutableStatementText() {
        if (executionVariables == null) return statementText;
        return executionVariables.prepareExecutableStatementText(statementText);
    }

    @Nullable
    private ExecutablePsiElement initExecutablePsiElement() {
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
                rawStatementText,
                connection,
                getTargetSchemaId());
        if (isNotValid(previewFile)) return null;

        PsiElement firstChild = previewFile.getFirstChild();
        if (firstChild instanceof ExecutableBundlePsiElement rootPsiElement) {
            List<ExecutablePsiElement> executablePsiElements = rootPsiElement.getExecutablePsiElements();
            return executablePsiElements.isEmpty() ? null : executablePsiElements.get(0);
        }

        return null;
    }

    @Override
    protected StatementExecutionContext createExecutionContext() {
        return new StatementExecutionContext(this);
    }

    public int getExecutableLineNumber() {
        return executionProcessor == null ? 0 : executionProcessor.getExecutableLineNumber();
    }

    public void updateStatementText(ExecutablePsiElement executablePsiElement) {
        this.rawStatementText = executablePsiElement.getText();
        this.statementText = executablePsiElement.getExecutableStatementText();
        resetExecutionContext();
    }

    @Override
    public void resetExecutionContext() {
        super.resetExecutionContext();
        this.previewStatementText.reset();
        this.executableStatementText.reset();
        this.executablePsiElement.reset();
    }

    public void setExecutionVariables(StatementExecutionVariablesBundle executionVariables) {
        this.executionVariables = replace(this.executionVariables, executionVariables);
        this.previewStatementText.reset();
        this.executableStatementText.reset();
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
                getPreviewStatementText(),
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

    public void bindExecutionVariables(ConnectionHandler connection, DBNPreparedStatement statement) throws SQLException {
        if (executionVariables == null) return;
        executionVariables.bindVariables(connection, statement);
    }
}
