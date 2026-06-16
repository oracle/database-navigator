/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.connection.config.datasource.prerequisite;

import com.dbn.prerequisite.definition.impl.ObjectPrivilegePrerequisite;

import static com.dbn.prerequisite.shared.PrerequisiteTypes.SELECT_DATA_SOURCE_CONFIG_STORE;

public class ObjectPrivilege_SELECT_DATA_SOURCE_CONFIG_STORE extends ObjectPrivilegePrerequisite {

    public ObjectPrivilege_SELECT_DATA_SOURCE_CONFIG_STORE() {
        super(SELECT_DATA_SOURCE_CONFIG_STORE, "SELECT", "SYS", "DATA_SOURCE_CONFIG_STORE");
    }
}
