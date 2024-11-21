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

import com.dbn.common.dispose.Failsafe;
import com.dbn.common.util.Strings;
import com.dbn.data.model.ColumnInfo;
import com.dbn.data.type.DBDataType;
import com.dbn.data.type.GenericDataType;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class BasicColumnInfo implements ColumnInfo {
    protected String name;
    protected int index;
    protected DBDataType dataType;

    public BasicColumnInfo(String name, DBDataType dataType, int index) {
        this.name = Strings.intern(name);
        this.index = index;
        this.dataType = dataType;
    }

    @Override
    @NotNull
    public DBDataType getDataType() {
        return Failsafe.nn(dataType);
    }

    @Override
    public void dispose() {
    }

    @Override
    public boolean isSortable() {
        DBDataType dataType = getDataType();
        return dataType.isNative() &&
                dataType.getGenericDataType().is(
                        GenericDataType.LITERAL,
                        GenericDataType.NUMERIC,
                        GenericDataType.DATE_TIME);
    }


}
