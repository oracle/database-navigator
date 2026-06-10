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

package com.dbn.object.type;


import com.dbn.common.constant.Constant;
import com.dbn.common.ui.Presentable;

import static com.dbn.nls.NlsResources.txt;

/**
 * This enum is for listing the possible ways of creating a new credential
 *
 * @author Ayoub Aarrasse (Oracle)
 */
public enum DBCredentialType implements Constant<DBCredentialType>, Presentable {
    PASSWORD(txt("app.objects.const.DBCredentialType_PASSWORD")),
    TOKEN(txt("app.objects.const.DBCredentialType_TOKEN")),
    OCI(txt("app.objects.const.DBCredentialType_OCI"));

    private final String name;

    DBCredentialType(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
