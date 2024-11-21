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
public enum AuthenticationType implements Constant<AuthenticationType>, Presentable {
    NONE(txt("cfg.connection.const.AuthenticationType_NONE")),
    USER(txt("cfg.connection.const.AuthenticationType_USER")),
    USER_PASSWORD(txt("cfg.connection.const.AuthenticationType_USER_PASSWORD")),
    OS_CREDENTIALS(txt("cfg.connection.const.AuthenticationType_OS_CREDENTIALS")),
    TOKEN(txt("cfg.connection.const.AuthenticationType_TOKEN"));

    private final String name;
}
