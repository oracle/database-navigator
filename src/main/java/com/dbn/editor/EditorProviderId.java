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

public enum EditorProviderId {
    CODE("0.CODE"),
    CODE_SPEC("0.CODE_SPEC"),
    CODE_BODY("1.CODE_BODY"),
    DATA("1.DATA"),
    DDL0("3.DDL"),
    DDL1("4.DDL"),
    DDL2("5.DDL"),
    CONSOLE("0.CONSOLE"),
    SESSION_BROWSER("0.SESSION_BROWSER"),
    DBN_SQL("2.DBN_SQL");

    EditorProviderId(String id) {
        this.id = id;
    }

    private final String id;

    public String getId() {
        return id;
    }
}
