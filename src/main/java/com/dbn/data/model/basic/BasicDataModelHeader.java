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


import com.dbn.common.collections.CaseInsensitiveStringKeyMap;
import com.dbn.common.dispose.Disposed;
import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.data.model.ColumnInfo;
import com.dbn.data.model.DataModelHeader;
import com.dbn.data.type.DBDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.dbn.common.dispose.Disposer.replace;

public class BasicDataModelHeader<T extends ColumnInfo> extends StatefulDisposableBase implements DataModelHeader<T> {
    private List<T> columnInfos = new ArrayList<>();
    private Map<String, T> nameIndex = new CaseInsensitiveStringKeyMap<>();


    protected void addColumnInfo(T columnInfo) {
        columnInfos.add(columnInfo);
        nameIndex.put(columnInfo.getName(), columnInfo);
    }

    @Override
    public List<T> getColumnInfos() {
        return columnInfos;
    }

    @Override
    public T getColumnInfo(int index) {
        return columnInfos.get(index);
    }

    @Override
    public T getColumnInfo(String name) {
        return nameIndex.get(name);
    }

    @Override
    public int getColumnIndex(String name) {
        T columnInfo = getColumnInfo(name);
        return columnInfo == null ? -1 : columnInfo.getIndex();
    }

    @Override
    public String getColumnName(int index) {
        return getColumnInfo(index).getName();
    }

    @Override
    public DBDataType getColumnDataType(int index) {
        return getColumnInfo(index).getDataType();
    }

    @Override
    public int getColumnCount() {
        return columnInfos.size();
    }


    /********************************************************
     *                    Disposable                        *
     *******************************************************  */
    @Override
    public void disposeInner() {
        columnInfos = replace(columnInfos, Disposed.list());
        nameIndex = replace(nameIndex, Disposed.map());
        nullify();
    }
}
