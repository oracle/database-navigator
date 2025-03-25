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

package com.dbn.editor.json.model;

import com.dbn.data.model.resultSet.ResultSetColumnInfo;
import com.dbn.data.model.resultSet.ResultSetDataModelHeader;
import com.dbn.data.type.DBDataType;
import com.dbn.editor.json.JsonDataEditor;
import com.dbn.object.DBJsonView;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

public class JsonDataEditorModelHeader extends ResultSetDataModelHeader<ResultSetColumnInfo> {
    JsonDataEditorModelHeader(JsonDataEditor jsonDataEditor, @Nullable ResultSet resultSet) throws SQLException {
        DBJsonView jsonView = jsonDataEditor.getJsonView();
        DBDataType dbDataType = DBDataType.get(jsonView.getConnection(), "VARCHAR", 4000, 0, 0, false);
        ResultSetColumnInfo columnInfo = new ResultSetColumnInfo(jsonView.getJsonColumnName(), dbDataType, 1, 1);
        addColumnInfo(columnInfo);
    }
}
