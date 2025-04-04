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

package com.dbn.editor.json.model;

import com.dbn.common.exception.Exceptions;
import com.dbn.common.util.Json;
import com.dbn.common.util.Naming;
import com.dbn.connection.Resources;
import com.dbn.connection.Savepoints;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.jdbc.DBNResultSet;
import com.dbn.data.type.DBDataType;
import com.dbn.data.value.ValueAdapter;
import com.dbn.editor.data.model.ResultSetAdapter;
import com.dbn.object.DBJsonView;
import lombok.Getter;
import lombok.Value;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.text.MessageFormat.format;

@NonNls
public class JsonDataResultSetAdapter extends ResultSetAdapter<JsonDataEditorModel> {
    private DBNConnection connection;
    private JsonEntity currentEntity;

    JsonDataResultSetAdapter(JsonDataEditorModel model, DBNResultSet resultSet) {
        super(model);
        this.connection = resultSet.getConnection();
    }

    @Override
    public synchronized void scroll(int rowIndex) throws SQLException {
        if (isInsertMode()) return;
        if (isObsolete()) return;

        JsonDataEditorModelRow modelRow = getModel().getRowAtResultSetIndex(rowIndex);
        JsonDataEditorModelCell modelCell = modelRow == null ? null : modelRow.getCellAtIndex(0);
        if (modelCell == null) throw new SQLException("Could not scroll to row index " + rowIndex);

        String content = modelCell.getJsonContent();
        String originalContent = modelCell.getOriginalJsonContent();

        currentEntity = new JsonEntity(content);

        List<String> keyAttributes = getJsonView().getKeyAttributeNames();
        Map<String, Object> keyValues = Json.getJsonPropertyValues(originalContent, keyAttributes);// use original content to resolve the key attributes
        for (String keyProperty : keyValues.keySet()) {
            Object keyValue = keyValues.get(keyProperty);
            currentEntity.addKeyProperty(keyProperty, keyValue);
        }
    }

    @Override
    public synchronized void updateRow() throws SQLException {
        if (isInsertMode()) return;
        if (isObsolete()) return;

        if (isUseSavePoints()) {
            Savepoints.run(connection, () -> this.executeUpdate());
        } else {
            executeUpdate();
        }
    }

    @Override
    public synchronized void refreshRow() {
        // not supported
    }


    @Override
    public synchronized void startInsertRow() {
        if (isInsertMode()) return;
        if (isObsolete()) return;

        setInsertMode(true);
        currentEntity = new JsonEntity("");
    }

    @Override
    public synchronized void cancelInsertRow() {
        if (!isInsertMode()) return;
        if (isObsolete()) return;

        setInsertMode(false);
        currentEntity = null;
    }

    @Override
    public synchronized void insertRow() throws SQLException {
        if (!isInsertMode()) return;
        if (isObsolete()) return;

        if (isUseSavePoints()) {
            Savepoints.run(connection, () -> {
                executeInsert();
                setInsertMode(false);
            });
        } else {
            executeInsert();
            setInsertMode(false);
        }
    }

    @Override
    public synchronized void deleteRow() throws SQLException {
        if (isInsertMode()) return;
        if (isObsolete()) return;

        if (isUseSavePoints()) {
            Savepoints.run(connection, () -> executeDelete());
        } else {
            executeDelete();
        }
    }

    private boolean isObsolete() {
        return Resources.isObsolete(connection);
    }

    @Override
    public synchronized void setValue(final int columnIndex, @NotNull final ValueAdapter valueAdapter, @Nullable final Object value) {
        Exceptions.unsupported();
    }

    @Override
    public synchronized void setValue(final int columnIndex, @NotNull final DBDataType dataType, @Nullable final Object value) {
        Exceptions.unsupported();    }

    private void executeInsert() throws SQLException {
        String jsonViewName = getJsonViewName();
        String jsonColumnName = getJsonView().getJsonColumnName();

        String insertStatement = format(
                "insert into {0} ( {1} ) values ( ? )",
                jsonViewName,
                jsonColumnName);

        PreparedStatement preparedStatement = connection.prepareStatement(insertStatement);
        preparedStatement.setString(1, currentEntity.getContent());
        preparedStatement.executeUpdate();
    }

    private void executeUpdate() throws SQLException {
        String jsonViewName = getJsonViewName();
        String jsonColumnName = getJsonColumnName();
        List<JsonProperty> keyProperties = getJsonKeyProperties();

        String condition = createWhereCondition(jsonColumnName, keyProperties);

        @NonNls
        String updateStatement = format(
                "update {0} set {1} = ? where {2}",
                jsonViewName,
                jsonColumnName,
                condition);

        PreparedStatement preparedStatement = connection.prepareStatement(updateStatement);
        preparedStatement.setString(1, currentEntity.getContent());

        for (int i = 0; i < keyProperties.size(); i++) {
            JsonProperty keyCell = keyProperties.get(i);
            preparedStatement.setObject(i + 2, keyCell.getValue());
        }
        preparedStatement.executeUpdate();
    }

    private void executeDelete() throws SQLException {
        String jsonViewName = getJsonViewName();
        String jsonColumnName = getJsonView().getJsonColumnName();
        List<JsonProperty> keyProperties = getJsonKeyProperties();

        String condition = createWhereCondition(jsonColumnName, keyProperties);

        @NonNls
        String deleteStatement = format(
                "delete from {0} where {1}",
                jsonViewName,
                condition);

        PreparedStatement preparedStatement = connection.prepareStatement(deleteStatement);
        for (int i = 0; i < keyProperties.size(); i++) {
            JsonProperty keyCell = keyProperties.get(i);
            preparedStatement.setObject(i, keyCell.getValue());
        }
        preparedStatement.executeUpdate();

    }

    private static String createWhereCondition(String jsonColumnName, List<JsonProperty> keyProperties) {
        return keyProperties
                .stream()
                .map(p -> format("JSON_VALUE({0}, {1}) = ?", jsonColumnName, p.asQuotedPath()))
                .collect(Collectors.joining(" and "));
    }

    private List<JsonProperty> getJsonKeyProperties() throws SQLException {
        List<JsonProperty> keyProperties = currentEntity.getKeyProperties();
        if (keyProperties.isEmpty()) {
            throw new SQLException("Could not resolve key attributes for json duality view manipulation");
        }
        return keyProperties;
    }


    @Value
    private static class JsonProperty {
        private final String key;
        private final Object value;

        JsonProperty(String key, Object value) {
            this.key = key;
            this.value = value;
        }

        public String asQuotedPath() {
            return Naming.singleQuoted("$." + key);
        }
    }

    @Getter
    private static class JsonEntity {
        private final Set<JsonProperty> keyProperties = new LinkedHashSet<>();
        private final String content;

        public JsonEntity(String content) {
            this.content = content;
        }

        List<JsonProperty> getKeyProperties() {
            return new ArrayList<>(keyProperties);
        }

        void addKeyProperty(String key, Object value) {
            JsonProperty cell = new JsonProperty(key, value);
            keyProperties.add(cell);
        }

    }

    private String getJsonViewName() {
        return getJsonView().getQualifiedName(true);
    }

    private String getJsonColumnName() {
        return getJsonView().getJsonColumnName();
    }

    private DBJsonView getJsonView() {
        return getModel().getJsonView();
    }

    @Override
    public void disposeInner() {
        currentEntity = null;
        connection = null;
        super.disposeInner();
    }
}
