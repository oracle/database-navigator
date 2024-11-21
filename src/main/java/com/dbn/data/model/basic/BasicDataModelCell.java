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

package com.dbn.data.model.basic;

import com.dbn.common.dispose.Nullifier;
import com.dbn.common.locale.Formatter;
import com.dbn.data.editor.text.TextContentType;
import com.dbn.data.model.ColumnInfo;
import com.dbn.data.model.DataModelCell;
import com.dbn.data.model.DataModelState;
import com.dbn.data.type.DBDataType;
import com.dbn.data.value.ArrayValue;
import com.dbn.data.value.LargeObjectValue;
import com.dbn.editor.data.model.RecordStatus;
import com.dbn.editor.data.model.RecordStatusHolder;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.dispose.Failsafe.nd;

@Getter
public class BasicDataModelCell<
        R extends BasicDataModelRow<M, ? extends BasicDataModelCell<R, M>>,
        M extends BasicDataModel<R, ? extends BasicDataModelCell<R, M>>>
        extends RecordStatusHolder
        implements DataModelCell<R, M> {

    private R row;
    private final int index;
    private Object userValue;
    private String presentableValue;

    public BasicDataModelCell(Object userValue, R row, int index) {
        this.userValue = userValue;
        this.row = row;
        this.index = index;
    }

    @Override
    protected RecordStatus[] properties() {
        return RecordStatus.VALUES;
    }

    @Override
    public Project getProject() {
        return getRow().getProject();
    }

    @Override
    public TextContentType getContentType() {
        DataModelState state = getModel().getState();
        String contentTypeName = state.getTextContentTypeName(getColumnInfo().getName());
        if (contentTypeName == null) {
            DBDataType dataType = getColumnInfo().getDataType();
            if (dataType.isNative()) {
                contentTypeName = dataType.getNativeType().getDefinition().getContentTypeName();
            }
        }

        return TextContentType.get(getProject(), contentTypeName);
    }

    @Override
    public void setContentType(TextContentType contentType) {
        DataModelState state = getModel().getState();
        state.setTextContentType(getColumnInfo().getName(), contentType.getName());
    }

    @Override
    @NotNull
    public R getRow() {
        return nd(row);
    }

    @Override
    public void setUserValue(Object userValue) {
        this.userValue = userValue;
        this.presentableValue = null;
    }

    @Override
    public void updateUserValue(Object userValue, boolean bulk) {
        setUserValue(userValue);
    }

    public boolean isLobValue() {
        return userValue instanceof LargeObjectValue;
    }

    public boolean isArrayValue() {
        return userValue instanceof ArrayValue;
    }

    @Override
    public String getPresentableValue() {
        if (userValue == null) {
            if (presentableValue != null) {
                presentableValue = null;
            }
        } else {
            if (presentableValue == null) {
                Formatter formatter = getFormatter();
                presentableValue = formatter.formatObject(userValue);
            }
            return presentableValue;
        }
        return null;
    }

    @NotNull
    @Override
    public M getModel() {
        return getRow().getModel();
    }

    @Override
    public String getName() {
        return getColumnInfo().getName();
    }

    @Override
    public DBDataType getDataType() {
        return getColumnInfo().getDataType();
    }

    @Override
    public DBObjectType getObjectType() {
        return DBObjectType.COLUMN;
    }

    @Override
    public ColumnInfo getColumnInfo() {
        return getModel().getColumnInfo(index);
    }

    public String toString() {
        // IMPORTANT return user value for copy to clipboard support
        return getPresentableValue();
    }

    @NotNull
    public Formatter getFormatter() {
        return getModel().getFormatter();
    }

    @Override
    public void disposeInner() {
        row = null;
        Nullifier.nullify(this);

    }
}
