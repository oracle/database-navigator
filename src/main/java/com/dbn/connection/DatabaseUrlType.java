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

package com.dbn.connection;

import com.dbn.common.constant.Constant;
import com.dbn.common.ui.Presentable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import static com.dbn.nls.NlsResources.txt;

@Getter
@AllArgsConstructor
public enum DatabaseUrlType implements Presentable, Constant<DatabaseUrlType> {
    TNS(txt("cfg.connection.const.DatabaseUrlType_TNS")),
    SID(txt("cfg.connection.const.DatabaseUrlType_SID")),
    SERVICE(txt("cfg.connection.const.DatabaseUrlType_SERVICE")),
    LDAP(txt("cfg.connection.const.DatabaseUrlType_LDAP")),
    LDAPS(txt("cfg.connection.const.DatabaseUrlType_LDAPS")),
    DATABASE(txt("cfg.connection.const.DatabaseUrlType_DATABASE")),
    CUSTOM(txt("cfg.connection.const.DatabaseUrlType_CUSTOM")),
    FILE(txt("cfg.connection.const.DatabaseUrlType_FILE"));

    private final String name;
}
