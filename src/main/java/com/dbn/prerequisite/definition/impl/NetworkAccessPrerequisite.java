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

import static com.dbn.nls.NlsResources.txt;

@Getter
public abstract class NetworkAccessPrerequisite extends PrerequisiteDefinitionProviderBase {
    private final String privilegeName;
    private final String hostName;

    protected NetworkAccessPrerequisite(
            PrerequisiteType prerequisiteType,
            @NonNls String privilegeName,
            @NonNls String hostName) {

        super(prerequisiteType);
        this.privilegeName = privilegeName;
        this.hostName = hostName;
    }

    @Override
    public PrerequisiteType getAlternativeType() {
        return null;
    }

    @NotNull
    @Override
    protected PrerequisiteEvaluator createEvaluator() {
        return context -> {
            String userName = context.getUserName();

            DatabaseMetadataInterface metadataInterface = context.getMetadataInterface();
            return DatabaseInterfaceInvoker.load(Priority.HIGH,
                    txt("prc.prerequisite.title.CheckingNetworkPrivilege"),
                    txt("prc.prerequisite.text.CheckingHostAcePrivilege", userName, hostName, privilegeName),
                    context.getProject(),
                    context.getConnectionId(),
                    c -> metadataInterface.hasNetworkPrivilege(
                            userName,
                            hostName,
                            privilegeName, c));
        };
    }

    @NotNull
    @Override
    protected PrerequisiteDefinition createDefinition(PrerequisiteEvaluator evaluator, PrerequisiteResolver resolver, PrerequisiteAdvisor advisor) {
        return new PrerequisiteDefinitionBase(
                txt("app.prerequisite.title.NetworkPrivilege", privilegeName, hostName),
                txt("app.prerequisite.text.NetworkPrivilege", privilegeName, hostName),
                getType(),
                getAlternativeType(),
                PrerequisiteCategory.GRANT,
                evaluator,
                resolver,
                advisor);
    }


    @NotNull
    @Override
    protected PrerequisiteResolver createResolver() {
        // users may under circumstances grant network ACL privileges to (for) themselves

        return context -> {
            String userName = context.getUserName();
            DatabaseMetadataInterface metadataInterface = context.getMetadataInterface();
            DatabaseInterfaceInvoker.execute(Priority.HIGH,
                    txt("prc.prerequisite.title.GrantingNetworkPrivilege"),
                    txt("prc.prerequisite.text.GrantingHostAcePrivilege", userName, hostName, privilegeName),
                    context.getProject(),
                    context.getConnectionId(),
                    c -> metadataInterface.grantNetworkPrivilege(
                            userName,
                            hostName,
                            privilegeName, c));
        };
    }

    @NotNull
    protected PrerequisiteAdvisor createAdvisor() {
        return context -> {
            String userName = context.getUserName();
            return new PrerequisiteAdvice(
                    txt("msg.prerequisite.title.RequestPrivilege"),
                    txt("msg.prerequisite.text.AdviceNetworkPrivilege", privilegeName, hostName),
                    String.format("""
                            BEGIN
                               DBMS_NETWORK_ACL_ADMIN.APPEND_HOST_ACE(
                                   host =>  '%s',
                                   ace  => xs$ace_type(
                                       privilege_list => xs$name_list('%s'),
                                       principal_name => '%s',
                                       principal_type => xs_acl.ptype_db
                                  )
                                );
                            END;""", hostName, privilegeName, userName));
        };
    }

    @Override
    public String toString() {
        return hostName + ":" + privilegeName;
    }
}
