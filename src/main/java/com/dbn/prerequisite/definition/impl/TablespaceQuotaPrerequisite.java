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
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.nls.NlsResources.txt;

public abstract class TablespaceQuotaPrerequisite extends PrerequisiteDefinitionProviderBase {

    protected TablespaceQuotaPrerequisite(PrerequisiteType prerequisiteType) {
        super(prerequisiteType);
    }

    @Override
    public PrerequisiteType getAlternativeType() {
        return null;
    }

    @Override
    protected @Nullable PrerequisiteResolver createResolver() {
        return null;
    }

    @NotNull
    @Override
    protected PrerequisiteEvaluator createEvaluator() {
        return context -> {
            DatabaseMetadataInterface metadataInterface = context.getMetadataInterface();
            return DatabaseInterfaceInvoker.load(Priority.HIGH,
                    txt("prc.prerequisite.title.CheckingTablespacePrivilege"),
                    txt("prc.prerequisite.text.CheckingTablespacePrivilege", "SYSTEM"),
                    context.getProject(),
                    context.getConnectionId(),
                    c -> metadataInterface.hasTablespaceQuota(c));
        };
    }

    @NotNull
    @Override
    protected PrerequisiteDefinition createDefinition(PrerequisiteEvaluator evaluator, PrerequisiteResolver resolver, PrerequisiteAdvisor advisor) {
        return new PrerequisiteDefinitionBase(
                txt("app.prerequisite.title.TablespaceQuota"),
                txt("app.prerequisite.text.TablespaceQuota"),
                getType(),
                getAlternativeType(),
                PrerequisiteCategory.GRANT,
                evaluator,
                resolver,
                advisor);
    }

    @NotNull
    protected PrerequisiteAdvisor createAdvisor() {
        return context -> {
            String userName = context.getUserName();
            return new PrerequisiteAdvice(
                    txt("msg.prerequisite.title.RequestQuota"),
                    txt("msg.prerequisite.text.AdviceTablespaceQuota", userName),
                    String.format("ALTER USER %s QUOTA 100M ON SYSTEM;", userName));
        };
    }

    @NonNls
    @Override
    public String toString() {
        return "System tablespace quota";
    }
}
