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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static com.dbn.nls.NlsResources.txt;

/**
 * Enumeration for all possible token authentication types<br>
 * See: <a href="https://docs.oracle.com/en/database/oracle/oracle-database/23/odpnt/ConnectionTokenAuthentication.html">Oracle token auth types</a>
 *
 * @author Dan Cioca (Oracle)
 */
@Getter
@AllArgsConstructor
public enum AuthenticationTokenType implements Presentable, Constant<AuthenticationTokenType> {
    OCI_API_KEY(txt("cfg.connection.const.AuthenticationTokenType_OCI_API_KEY")),
    OCI_INSTANCE_PRINCIPAL(txt("cfg.connection.const.AuthenticationTokenType_OCI_INSTANCE_PRINCIPAL")),
    OCI_RESOURCE_PRINCIPAL(txt("cfg.connection.const.AuthenticationTokenType_OCI_RESOURCE_PRINCIPAL")),
    OCI_DELEGATION_TOKEN(txt("cfg.connection.const.AuthenticationTokenType_OCI_DELEGATION_TOKEN")),
    OCI_INTERACTIVE(txt("cfg.connection.const.AuthenticationTokenType_OCI_INTERACTIVE")),

    AZURE_SERVICE_PRINCIPAL_CERTIFICATE(txt("cfg.connection.const.AuthenticationTokenType_AZURE_SERVICE_PRINCIPAL_CERTIFICATE")),
    AZURE_SERVICE_PRINCIPAL_TOKEN(txt("cfg.connection.const.AuthenticationTokenType_AZURE_SERVICE_PRINCIPAL_TOKEN")),
    AZURE_INTERACTIVE(txt("cfg.connection.const.AuthenticationTokenType_AZURE_INTERACTIVE"));

    private final String name;

    public boolean isOci() {
        return isOneOf(
                OCI_API_KEY,
                OCI_INSTANCE_PRINCIPAL,
                OCI_RESOURCE_PRINCIPAL,
                OCI_DELEGATION_TOKEN,
                OCI_INTERACTIVE);
    }

    public boolean isAzure() {
        return isOneOf(
                AZURE_SERVICE_PRINCIPAL_CERTIFICATE,
                AZURE_SERVICE_PRINCIPAL_TOKEN,
                AZURE_INTERACTIVE);
    }

    public static final Set<AuthenticationTokenType> ALL_AZURE_TOKEN_TYPES =
        Collections.unmodifiableSet(
            EnumSet.of(AZURE_SERVICE_PRINCIPAL_CERTIFICATE, AZURE_SERVICE_PRINCIPAL_TOKEN, AZURE_INTERACTIVE));
}
