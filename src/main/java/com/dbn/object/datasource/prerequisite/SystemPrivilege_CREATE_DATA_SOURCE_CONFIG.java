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

package com.dbn.object.datasource.prerequisite;

import com.dbn.prerequisite.definition.impl.SystemPrivilegePrerequisite;
import com.dbn.prerequisite.model.PrerequisiteType;

import static com.dbn.prerequisite.shared.PrerequisiteTypes.CREATE_ANY_DATA_SOURCE_CONFIG;
import static com.dbn.prerequisite.shared.PrerequisiteTypes.CREATE_DATA_SOURCE_CONFIG;

public class SystemPrivilege_CREATE_DATA_SOURCE_CONFIG extends SystemPrivilegePrerequisite {

    public SystemPrivilege_CREATE_DATA_SOURCE_CONFIG() {
        super(CREATE_DATA_SOURCE_CONFIG, "CREATE DATA SOURCE CONFIG");
    }

    @Override
    public PrerequisiteType getAlternativeType() {
        return CREATE_ANY_DATA_SOURCE_CONFIG;
    }
}
