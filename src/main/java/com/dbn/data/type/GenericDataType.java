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

package com.dbn.data.type;

import com.dbn.common.constant.Constant;
import com.dbn.common.ui.Presentable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import static com.dbn.nls.NlsResources.txt;

@Getter
@AllArgsConstructor
public enum GenericDataType implements Presentable, Constant<GenericDataType> {
    LITERAL(txt("app.data.const.GenericDataType_LITERAL")),
    NUMERIC(txt("app.data.const.GenericDataType_NUMERIC")),
    DATE_TIME(txt("app.data.const.GenericDataType_DATE_TIME")),
    CLOB(txt("app.data.const.GenericDataType_CLOB")),
    NCLOB(txt("app.data.const.GenericDataType_NCLOB")),
    BLOB(txt("app.data.const.GenericDataType_BLOB")),
    ROWID(txt("app.data.const.GenericDataType_ROWID")),
    REF(txt("app.data.const.GenericDataType_REF")),
    FILE(txt("app.data.const.GenericDataType_FILE")),
    BOOLEAN(txt("app.data.const.GenericDataType_BOOLEAN")),
    OBJECT(txt("app.data.const.GenericDataType_OBJECT")),
    CURSOR(txt("app.data.const.GenericDataType_CURSOR")),
    TABLE(txt("app.data.const.GenericDataType_TABLE")),
    ARRAY(txt("app.data.const.GenericDataType_ARRAY")),
    COLLECTION(txt("app.data.const.GenericDataType_COLLECTION")),
    XMLTYPE(txt("app.data.const.GenericDataType_XMLTYPE")),
    PROPRIETARY(txt("app.data.const.GenericDataType_PROPRIETARY")),
    COMPLEX(txt("app.data.const.GenericDataType_COMPLEX")),
    ;

    private final String name;

    public boolean is(GenericDataType... genericDataTypes) {
        for (GenericDataType genericDataType : genericDataTypes) {
            if (this == genericDataType) return true;
        }
        return false;
    }

    public boolean isLOB() {
        return is(BLOB, CLOB, NCLOB, XMLTYPE);
    }


}
