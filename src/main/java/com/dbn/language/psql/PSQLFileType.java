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

package com.dbn.language.psql;

import com.dbn.common.icon.Icons;
import com.dbn.common.util.Files;
import com.dbn.editor.DBContentType;
import com.dbn.language.common.DBLanguageFileType;

import javax.swing.Icon;

public class PSQLFileType extends DBLanguageFileType {
    public static final PSQLFileType INSTANCE = new PSQLFileType();

    private PSQLFileType() {
        super(PSQLLanguage.INSTANCE,
                Files.PSQL_FILE_EXTENSIONS,
                "PSQL file (DBN)",
                DBContentType.CODE);
    }

    @Override
    public Icon getIcon() {
        return Icons.FILE_PLSQL;
    }


}