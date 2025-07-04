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
public abstract class NetworkAccessPrerequisite extends PrerequisiteDefinitionProviderBase {

    protected abstract @NonNls String getHost();
    protected abstract @NonNls String getPrivilege();

    protected NetworkAccessPrerequisite(PrerequisiteType prerequisiteType) {
        super(prerequisiteType);
    }

    @Override
    protected @Nullable PrerequisiteEvaluator createEvaluator() {
        return context -> {
            String schemaName = context.ensureConnection().getUserName();
            String host = getHost();
            String privilege = getPrivilege();

            DatabaseMetadataInterface metadataInterface = context.getMetadataInterface();
            return DatabaseInterfaceInvoker.load(Priority.HIGH,
                    txt("prc.prerequisite.title.CheckingHostAcePrivilege"),
                    txt("prc.prerequisite.text.CheckingHostAcePrivilege", schemaName, host, privilege),
                    context.getProject(),
                    context.getConnectionId(),
                    c -> metadataInterface.hasHostAcePrivilege(schemaName, host, privilege, c));
        };
    }

    @Override
    protected PrerequisiteDefinition createDefinition(PrerequisiteEvaluator evaluator, PrerequisiteResolver resolver) {
        String schemaName = ""; // TODO get schema name from connection
        String host = getHost();
        String privilege = getPrivilege();

        return new PrerequisiteDefinitionBase(
                txt("prc.prerequisite.title.CheckingHostAcePrivilege"),
                txt("prc.prerequisite.text.CheckingHostAcePrivilege", schemaName, host, privilege),
                getPrerequisiteType(),
                PrerequisiteCategory.GRANT,
                evaluator,
                resolver);
    }

    @Override
    protected @Nullable PrerequisiteResolver createResolver() {
        return context -> {
            String schemaName = context.ensureConnection().getUserName();
            String host = getHost();
            String privilege = getPrivilege();

            DatabaseMetadataInterface metadataInterface = context.getMetadataInterface();
            DatabaseInterfaceInvoker.execute(Priority.HIGH,
                    txt("prc.prerequisite.title.GrantingHostAcePrivilege"),
                    txt("prc.prerequisite.text.GrantingHostAcePrivilege", schemaName, host, privilege),
                    context.getProject(),
                    context.getConnectionId(),
                    c -> metadataInterface.grantHostAcePrivilege(schemaName, host, privilege, c));
        };
    }
}
