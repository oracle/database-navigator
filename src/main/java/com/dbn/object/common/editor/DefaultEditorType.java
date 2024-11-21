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

package com.dbn.object.common.editor;

import com.dbn.common.ui.Presentable;
import com.dbn.object.type.DBObjectType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import static com.dbn.nls.NlsResources.txt;

@Getter
@AllArgsConstructor
public enum DefaultEditorType implements Presentable{
    CODE(txt("app.editor.const.DefaultEditorType_CODE")),
    DATA(txt("app.editor.const.DefaultEditorType_DATA")),
    SPEC(txt("app.editor.const.DefaultEditorType_SPEC")),
    BODY(txt("app.editor.const.DefaultEditorType_BODY")),
    SELECTION(txt("app.editor.const.DefaultEditorType_SELECTION"));

    public static final DefaultEditorType[] EMPTY_ARRAY = new DefaultEditorType[0];

    private final String name;

    public static DefaultEditorType[] getEditorTypes(DBObjectType objectType) {
        switch (objectType){
            case VIEW: return new DefaultEditorType[]{CODE, DATA, SELECTION};
            case PACKAGE: return new DefaultEditorType[]{SPEC, BODY, SELECTION};
            case TYPE: return new DefaultEditorType[]{SPEC, BODY, SELECTION};
        }
        return EMPTY_ARRAY;
    }

    @Override
    public String toString() {
        return name;
    }
}
