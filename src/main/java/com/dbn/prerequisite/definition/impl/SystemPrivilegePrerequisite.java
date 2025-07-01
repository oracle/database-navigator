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

package com.dbn.prerequisite.definition.impl;

import com.dbn.common.Priority;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.database.interfaces.DatabaseMetadataInterface;
import com.dbn.prerequisite.definition.PrerequisiteDefinition;
import com.dbn.prerequisite.definition.PrerequisiteDefinitionBase;
import com.dbn.prerequisite.definition.PrerequisiteDefinitionProviderBase;
import com.dbn.prerequisite.evaluation.PrerequisiteEvaluator;
import com.dbn.prerequisite.model.PrerequisiteCategory;
import com.dbn.prerequisite.model.PrerequisiteType;
import com.dbn.prerequisite.resolution.PrerequisiteResolver;
import lombok.Getter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import static com.dbn.nls.NlsResources.txt;

@Getter
public abstract class SystemPrivilegePrerequisite extends PrerequisiteDefinitionProviderBase {
    protected SystemPrivilegePrerequisite(PrerequisiteType prerequisiteType) {
        super(prerequisiteType);
    }

    protected abstract @NonNls String getPrivilegeName();


    @Override
    public PrerequisiteDefinition createDefinition(PrerequisiteEvaluator evaluator, PrerequisiteResolver resolver) {
        String privilegeName = getPrivilegeName();
        return new PrerequisiteDefinitionBase(
                txt("app.prerequisite.title.SystemPrivilege", privilegeName),
                txt("app.prerequisite.text.SystemPrivilege", privilegeName),
                getPrerequisiteType(),
                PrerequisiteCategory.GRANT,
                evaluator,
                resolver);
    }

    @Override
    protected PrerequisiteEvaluator createEvaluator() {
        return context -> {
            String privilegeName = getPrivilegeName();

            DatabaseMetadataInterface metadataInterface = context.getMetadataInterface();
            return DatabaseInterfaceInvoker.load(Priority.HIGH,
                    txt("prc.prerequisite.title.CheckingSystemPrivilege"),
                    txt("prc.prerequisite.text.CheckingSystemPrivilege", privilegeName),
                    context.getProject(),
                    context.getConnectionId(),
                    c -> metadataInterface.hasSystemPrivilege(privilegeName, c));
        };
    }

    @Override
    protected @Nullable PrerequisiteResolver createResolver() {
        return null;
    }
}
