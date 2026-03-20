/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.assistant.tool.impl;

import com.dbn.assistant.AssistantType;
import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.tool.AssistantToolBase;
import com.dbn.assistant.tool.spec.SemanticSearchTool;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.Resources;
import com.dbn.connection.info.ConnectionInfo;
import com.dbn.object.DBTable;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.vector.DatabaseVectorManager;
import com.intellij.openapi.project.Project;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.assistant.AssistantContextUtil.getAssistantState;
import static com.dbn.object.type.DBVectorDistanceMetric.COSINE;

public class SemanticSearchToolImpl extends AssistantToolBase implements SemanticSearchTool {

    @Override
    public List<SemanticSearchResult> performSemanticSearch(String query, int maxResults) {
        ConnectionHandler connection = getConnection();
        ConnectionInfo connectionInfo = connection.getConnectionInfo();
        if (connectionInfo == null) throw new IllegalStateException("Could not connect to database");

        Project project = getProject();
        DatabaseVectorManager vectorManager = DatabaseVectorManager.getInstance(project);
        ResultSet resultSet = null;
        List<SemanticSearchResult> searchResults = new ArrayList<>();
        try {

            AssistantState assistantState = getAssistantState(getConnectionId(), AssistantType.PUBLIC);
            verify(assistantState, "Invalid assistant state");

            DBObjectRef<DBTable> embeddingTable = assistantState.getEmbeddingTable();
            verify(assistantState, "No embedding table selected");

            String schemaName = embeddingTable.getSchemaName();
            String tableName = embeddingTable.getObjectName();
            resultSet = vectorManager.performSimilaritySearch(connection, schemaName, tableName, query, COSINE, maxResults);

            while (resultSet.next()) {
                SemanticSearchResult searchResult = new SemanticSearchResult();
                searchResult.setId(resultSet.getString("ID"));
                searchResult.setContent(resultSet.getString("CONTENT"));
                searchResult.setScore(resultSet.getDouble("DISTANCE"));
                searchResults.add(searchResult);
            }
        } catch (Exception e) {
            throw new RuntimeException("Search request failed: " + e.getMessage());
        } finally {
            Resources.close(resultSet);
        }
        return searchResults;
    }
}
