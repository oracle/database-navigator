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

package com.dbn.editor.data.model;

import com.dbn.common.latent.Latent;
import com.dbn.common.util.RefreshableValue;
import com.dbn.data.model.resultSet.ResultSetColumnInfo;
import com.dbn.data.type.DBDataType;
import com.dbn.data.type.GenericDataType;
import com.dbn.editor.data.options.DataEditorSettings;
import com.dbn.object.DBColumn;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import static com.dbn.common.dispose.Nullifier.nullify;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.editor.data.DatasetEditorUtils.loadDistinctColumnValues;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class DatasetEditorColumnInfo extends ResultSetColumnInfo  {
    private final boolean primaryKey;
    private final boolean foreignKey;
    private final boolean identity;

    private transient final DBObjectRef<DBColumn> column;
    private transient Latent<List<String>> possibleValues = Latent.basic(() -> loadPossibleValues());
    private transient final RefreshableValue<Boolean> auditColumn = new RefreshableValue<>(2000) {
        @Override
        protected Boolean load() {
            DBColumn column = getColumn();
            return column.isAudit();
        }
    };

    DatasetEditorColumnInfo(DBColumn column, int columnIndex, int resultSetColumnIndex) {
        super(column.getName(), column.getDataType(), columnIndex, resultSetColumnIndex);
        this.column = DBObjectRef.of(column);
        this.primaryKey = column.isPrimaryKey();
        this.foreignKey = column.isForeignKey();
        this.identity = column.isIdentity();
    }

    @NotNull
    public DBColumn getColumn() {
        return DBObjectRef.ensure(column);
    }

    public boolean isAuditColumn() {
        return auditColumn.get();
    }

    public List<String> getPossibleValues() {
        return possibleValues.get();
    }

    private List<String> loadPossibleValues() {
        List<String> values = null;
        DBColumn column = getColumn();
        Project project = column.getProject();
        int valuesThreshold = getPossibleValuesThreshold(project);

        if (column.isForeignKey()) {
            DBColumn foreignKeyColumn = column.getForeignKeyColumn();
            if (foreignKeyColumn != null) {
                values = loadDistinctColumnValues(foreignKeyColumn, valuesThreshold);
            }
        } else {
            values = loadDistinctColumnValues(column, valuesThreshold);
        }
        return nvl(values, Collections.emptyList());
    }

    private static int getPossibleValuesThreshold(Project project) {
        DataEditorSettings dataEditorSettings = DataEditorSettings.getInstance(project);
        int valuesThreshold = dataEditorSettings.getValueListPopupSettings().getElementCountThreshold();
        return valuesThreshold;
    }

    @Override
    public void dispose() {
        possibleValues.reset();
        nullify(this);
    }

    @Override
    public boolean isSortable() {
        DBDataType type = getColumn().getDataType();
        return type != null && type.isNative() &&
                type.getGenericDataType().is(
                        GenericDataType.LITERAL,
                        GenericDataType.NUMERIC,
                        GenericDataType.DATE_TIME);
    }

}
