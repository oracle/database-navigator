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

package com.dbn.debugger.prerequisite;

import com.dbn.common.util.Commons;
import com.dbn.prerequisite.definition.PrerequisiteDefinition;
import com.dbn.prerequisite.definition.PrerequisiteDefinitionBase;
import com.dbn.prerequisite.definition.PrerequisiteDefinitionProvider;
import com.dbn.prerequisite.evaluation.PrerequisiteEvaluator;
import com.dbn.prerequisite.model.PrerequisiteCategory;
import com.dbn.prerequisite.model.PrerequisiteType;
import org.jetbrains.annotations.NotNull;

public class DebugPrerequisiteDefinitions implements PrerequisiteDefinitionProvider {

    public static final PrerequisiteType EXECUTION_OF_DBMS_DEBUG_JDWP = PrerequisiteType.get("EXECUTION_OF_DBMS_DEBUG_JDWP");

    public DebugPrerequisiteDefinitions() {
    }

    @Override
    public PrerequisiteDefinition[] getDefinitions() {
        return Commons.list(
                debugJdwpPackageExecution()
        );
    }

    private @NotNull PrerequisiteDefinitionBase debugJdwpPackageExecution() {
        return new PrerequisiteDefinitionBase(
                "DBMS_DEBUG_JDWP execution rights",
                "User is granted execution to the debugger package DMBS_DEBUG_JDWP",
                EXECUTION_OF_DBMS_DEBUG_JDWP,
                PrerequisiteCategory.GRANT,
                createEvaluator());
    }

    private PrerequisiteEvaluator createEvaluator() {
        return null;
    }
}
