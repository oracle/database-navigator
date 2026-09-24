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

package com.dbn.editor;

import com.dbn.common.util.Enumerations;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import static com.dbn.nls.NlsResources.txt;

@Getter
public enum DBContentType {
    NONE(txt("app.editor.const.DBContentType_NONE")),
    DATA(txt("app.editor.const.DBContentType_DATA"), EditorProviderId.DATA),
    JSON(txt("app.editor.const.DBContentType_JSON"), EditorProviderId.JSON),

    CODE(txt("app.editor.const.DBContentType_CODE"), EditorProviderId.CODE),
    CODE_SPEC(txt("app.editor.const.DBContentType_CODE_SPEC"), EditorProviderId.CODE_SPEC),
    CODE_BODY(txt("app.editor.const.DBContentType_CODE_BODY"), "BODY", EditorProviderId.CODE_BODY),
    CODE_SPEC_AND_BODY(txt("app.editor.const.DBContentType_CODE_SPEC_AND_BODY"), new DBContentType[]{CODE_SPEC, CODE_BODY}),
    CODE_AND_DATA(txt("app.editor.const.DBContentType_CODE_AND_DATA"), new DBContentType[]{CODE, DATA}),
    CODE_AND_JSON(txt("app.editor.const.DBContentType_CODE_AND_JSON"), new DBContentType[]{CODE, JSON});

    private DBContentType[] subContentTypes = new DBContentType[0];
    private final @Nls String description;
    private String objectTypeSubname;
    private EditorProviderId editorProviderId;

    DBContentType(@Nls String description, DBContentType[] subContentTypes) {
        this.description = description;
        this.subContentTypes = subContentTypes;
    }

    DBContentType(@Nls String description) {
        this.description = description;
    }

    DBContentType(@Nls String description, EditorProviderId editorProviderId) {
        this.description = description;
        this.editorProviderId = editorProviderId;
    }

    DBContentType(@Nls String description, String objectTypeSubname, EditorProviderId editorProviderId) {
        this.description = description;
        this.objectTypeSubname = objectTypeSubname;
        this.editorProviderId = editorProviderId;
    }

    public boolean isBundle() {
        return subContentTypes.length > 0;
    }

    public boolean isNone() {
        return this == NONE;
    }

    public boolean isCode() {
        return this == CODE || this == CODE_SPEC || this == CODE_BODY || this == CODE_SPEC_AND_BODY;
    }

    public boolean isData() {
        return this == DATA; 
    }

    public boolean isJsonData() {
        return this == JSON;
    }

    /**
     * Returns the database content qualifier used when addressing this source unit.
     *
     * @param objectType the database object type
     * @return the qualified database object type, or {@code null} when no qualifier applies
     */
    @NonNls
    @Nullable
    public String getContentQualifier(DBObjectType objectType) {
        return switch (objectType) {
            case JAVA_CLASS -> "JAVA SOURCE";
            case JAVA_RESOURCE -> "JAVA RESOURCE";
            case FUNCTION -> "FUNCTION";
            case PROCEDURE -> "PROCEDURE";
            case VIEW -> "VIEW";
            case DATASET_TRIGGER -> "TRIGGER";
            case DATABASE_TRIGGER -> "TRIGGER";
            case PACKAGE ->
                    this == CODE_SPEC ? "PACKAGE" :
                    this == CODE_BODY ? "PACKAGE BODY" : null;
            case TYPE ->
                    this == CODE_SPEC ? "TYPE" :
                    this == CODE_BODY ? "TYPE BODY" : null;
            default -> null;
        };
    }

    public @Nls String toString() {
        return description;
    }

    public boolean isOneOf(DBContentType ... contentTypes){
        return Enumerations.isOneOf(this, contentTypes);
    }

    public static DBContentType get(DBObjectType objectType) {
        return switch (objectType) {
            case FUNCTION,
                 PROCEDURE,
                 TRIGGER,
                 DATASET_TRIGGER,
                 DATABASE_TRIGGER,
                 JAVA_RESOURCE,
                 JAVA_CLASS -> CODE;
            case PACKAGE,
                 TYPE -> CODE_SPEC_AND_BODY;
            case VIEW,
                 JSON_VIEW,
                 MATERIALIZED_VIEW -> CODE_AND_DATA;
            case TABLE -> DATA;
            default -> NONE;
        };
    }

    public boolean has(DBContentType contentType) {
        return switch (contentType) {
            case DATA -> this == DATA || this == CODE_AND_DATA;
            case JSON -> this == JSON || this == CODE_AND_JSON;
            case CODE -> this == CODE || this == CODE_AND_DATA || this == CODE_SPEC_AND_BODY;
            default -> false;
        };
    }
}
