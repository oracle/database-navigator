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

package com.dbn.execution.explain.result;

import com.dbn.common.action.DataKeys;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.icon.Icons;
import com.dbn.common.util.Commons;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionRef;
import com.dbn.connection.ResultSets;
import com.dbn.connection.SchemaId;
import com.dbn.execution.ExecutionResultBase;
import com.dbn.execution.explain.result.ui.ExplainPlanResultForm;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.common.psi.ExecutablePsiElement;
import com.dbn.language.sql.SQLLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dbn.common.dispose.Disposer.replace;

@Getter
@Setter
public class ExplainPlanResult extends ExecutionResultBase<ExplainPlanResultForm> {
    private String planId;
    private Date timestamp;
    private ExplainPlanEntry root;
    private final ConnectionRef connection;
    private final VirtualFile virtualFile;
    private final SchemaId currentSchema;
    private final String errorMessage;
    private final String statementText;
    private final String resultName;

    public ExplainPlanResult(ExecutablePsiElement executablePsiElement, ResultSet resultSet) throws SQLException {
        this(executablePsiElement, (String) null);
        // entries must be sorted by PARENT_ID NULLS FIRST, ID
        Map<Integer, ExplainPlanEntry> entries = new HashMap<>();
        ConnectionHandler connection = getConnection();
        List<String> explainColumnNames = ResultSets.getColumnNames(resultSet);

        while (resultSet.next()) {
            ExplainPlanEntry entry = new ExplainPlanEntry(connection, resultSet, explainColumnNames);
            Integer id = entry.getId();
            Integer parentId = entry.getParentId();
            entries.put(id, entry);
            if (parentId == null) {
                root = entry;
            } else {
                ExplainPlanEntry parentEntry = entries.get(parentId);
                parentEntry.addChild(entry);
                entry.setParent(parentEntry);
            }
        }
    }

    public ExplainPlanResult(ExecutablePsiElement executablePsiElement, String errorMessage) {
        DBLanguagePsiFile psiFile = executablePsiElement.getFile();
        ConnectionHandler connection = Failsafe.nn(psiFile.getConnection());
        this.connection = connection.ref();
        this.currentSchema = psiFile.getSchemaId();
        this.virtualFile = psiFile.getVirtualFile();
        this.resultName = Commons.nvl(executablePsiElement.createSubjectList(), "Explain Plan");
        this.errorMessage = errorMessage;
        this.statementText = executablePsiElement.getText();
    }

    @Override
    public ConnectionId getConnectionId() {
        return connection.getConnectionId();
    }

    @Override
    @NotNull
    public ConnectionHandler getConnection() {
        return ConnectionRef.ensure(connection);
    }

    @Override
    public DBLanguagePsiFile createPreviewFile() {
        ConnectionHandler connection = getConnection();
        SchemaId currentSchema = getCurrentSchema();
        DBLanguageDialect languageDialect = connection.getLanguageDialect(SQLLanguage.INSTANCE);
        return DBLanguagePsiFile.createFromText(
                getProject(),
                "preview",
                languageDialect,
                statementText,
                connection,
                currentSchema);
    }

    @NotNull
    @Override
    public Project getProject() {
        return getConnection().getProject();
    }

    @Nullable
    @Override
    public ExplainPlanResultForm createForm() {
        return new ExplainPlanResultForm(this);
    }

    @Override
    @NotNull
    public String getName() {
        return resultName;
    }

    @Override
    public Icon getIcon() {
        return Icons.EXPLAIN_PLAN_RESULT;
    }

    public boolean isError() {
        return errorMessage != null;
    }

    /********************************************************
     *                    Data Provider                     *
     ********************************************************/
    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        if (DataKeys.EXPLAIN_PLAN_RESULT.is(dataId)) return this;
        return null;
    }

    /********************************************************
     *                    Disposable                   *
     *******************************************************  */
    @Override
    public void disposeInner() {
        root = replace(root, null);
        super.disposeInner();
    }
}
