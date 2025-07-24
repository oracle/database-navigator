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
import com.dbn.prerequisite.resolution.PrerequisiteAdvice;
import com.dbn.prerequisite.resolution.PrerequisiteAdvisor;
import com.dbn.prerequisite.resolution.PrerequisiteResolver;
import lombok.Getter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.nls.NlsResources.txt;

@Getter
public abstract class ObjectPrivilegePrerequisite extends PrerequisiteDefinitionProviderBase {
    private final String privilegeName;
    private final String ownerName;
    private final String objectName;

    protected ObjectPrivilegePrerequisite(
            PrerequisiteType prerequisiteType,
            @NonNls String privilegeName,
            @NonNls String ownerName,
            @NonNls String objectName) {

        super(prerequisiteType);
        this.privilegeName = privilegeName;
        this.ownerName = ownerName;
        this.objectName = objectName;
    }

    @Override
    public PrerequisiteType getAlternativeType() {
        return null;
    }

    @NotNull
    @Override
    public PrerequisiteDefinition createDefinition(
            PrerequisiteEvaluator evaluator,
            PrerequisiteResolver resolver,
            PrerequisiteAdvisor advisor) {

        return new PrerequisiteDefinitionBase(
                txt("app.prerequisite.title.ObjectPrivilege", privilegeName, ownerName, objectName),
                txt("app.prerequisite.text.ObjectPrivilege", privilegeName, ownerName, objectName),
                getType(),
                getAlternativeType(),
                PrerequisiteCategory.GRANT,
                evaluator,
                resolver,
                advisor);
    }

    @NotNull
    @Override
    protected PrerequisiteEvaluator createEvaluator() {
        return context -> {
            DatabaseMetadataInterface metadataInterface = context.getMetadataInterface();
            return DatabaseInterfaceInvoker.load(Priority.HIGH,
                    txt("prc.prerequisite.title.CheckingObjectPrivilege"),
                    txt("prc.prerequisite.text.CheckingObjectPrivilege", privilegeName, ownerName, objectName),
                    context.getProject(),
                    context.getConnectionId(),
                    c -> metadataInterface.hasObjectPrivilege(privilegeName, ownerName, objectName, c));
        };
    }

    @Nullable
    @Override
    protected PrerequisiteResolver createResolver() {
        // users cannot grant object privileges to themselves, hence no "resolver"
        return null;
    }

    @Override
    @NotNull
    protected PrerequisiteAdvisor createAdvisor() {
        return context -> {
            String privilegeName = getPrivilegeName();
            String ownerName = getOwnerName();
            String objectName = getObjectName();
            String userName = context.getUserName();

            return new PrerequisiteAdvice(
                    "Request privilege",
                    "" + privilegeName + " privilege on " + ownerName + "." + objectName + " object",
                    String.format("grant %s on %s.%s to %s;", privilegeName, ownerName, objectName, userName));
        };
    }

    @Override
    public String toString() {
        return privilegeName + " " + ownerName + "." + objectName;
    }
}
