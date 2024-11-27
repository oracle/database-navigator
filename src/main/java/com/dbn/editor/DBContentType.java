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

package com.dbn.editor;

import com.dbn.common.util.Enumerations;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;

@Getter
public enum DBContentType {
    NONE("No Content"),
    DATA("Data", EditorProviderId.DATA),

    CODE("Code", EditorProviderId.CODE),
    CODE_SPEC("Spec", EditorProviderId.CODE_SPEC),
    CODE_BODY("Body", "BODY", EditorProviderId.CODE_BODY),
    CODE_SPEC_AND_BODY("Spec and Body", new DBContentType[]{CODE_SPEC, CODE_BODY}),
    CODE_AND_DATA("Code and Data", new DBContentType[]{CODE, DATA});

    private DBContentType[] subContentTypes = new DBContentType[0];
    private final String description;
    private String objectTypeSubname;
    private EditorProviderId editorProviderId;

    DBContentType(String description, DBContentType[] subContentTypes) {
        this.description = description;
        this.subContentTypes = subContentTypes;
    }

    DBContentType(String description) {
        this.description = description;
    }

    DBContentType(String description, EditorProviderId editorProviderId) {
        this.description = description;
        this.editorProviderId = editorProviderId;
    }

    DBContentType(String description, String objectTypeSubname, EditorProviderId editorProviderId) {
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

    public String toString() {
        return description;
    }

    public boolean isOneOf(DBContentType ... contentTypes){
        return Enumerations.isOneOf(this, contentTypes);
    }

    public static DBContentType get(DBObjectType objectType) {
        switch (objectType) {
            case FUNCTION:
            case PROCEDURE:
            case TRIGGER:
            case DATASET_TRIGGER:
            case DATABASE_TRIGGER:
            case JAVA_CLASS: return CODE;
            case PACKAGE:
            case TYPE: return CODE_SPEC_AND_BODY;
            case VIEW:
            case MATERIALIZED_VIEW: return CODE_AND_DATA;
            case TABLE: return DATA;
            default: return NONE;
        }
    }

    public boolean has(DBContentType contentType) {
        switch (contentType) {
            case DATA: return this == DATA || this == CODE_AND_DATA;
            case CODE: return this == CODE || this == CODE_AND_DATA || this == CODE_SPEC_AND_BODY;
            default:   return false;
        }
    }
}
