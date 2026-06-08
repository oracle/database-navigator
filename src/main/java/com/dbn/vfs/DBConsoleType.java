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

package com.dbn.vfs;

import com.dbn.common.constant.Constant;
import com.dbn.common.icon.Icons;
import lombok.Getter;

import javax.swing.Icon;

import static com.dbn.nls.NlsResources.txt;

@Getter
public enum DBConsoleType implements Constant<DBConsoleType> {
    STANDARD(txt("app.vfs.const.DBConsoleType_STANDARD"), Icons.FILE_SQL_CONSOLE),
    DEBUG(txt("app.vfs.const.DBConsoleType_DEBUG"), Icons.FILE_SQL_DEBUG_CONSOLE),
    SEARCH(txt("app.vfs.const.DBConsoleType_SEARCH"), Icons.FILE_SEARCH_CONSOLE);

    private final String name;
    private final Icon icon;

    DBConsoleType(String name, Icon icon) {
        this.name = name;
        this.icon = icon;
    }
}
