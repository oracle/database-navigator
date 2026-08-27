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

package com.dbn.connection.config.provider;

import com.dbn.common.ui.Presentable;
import lombok.Getter;

import java.util.Arrays;

import static com.dbn.connection.config.provider.CloudConfigProviderType.AZURE_APP_CONFIG;
import static com.dbn.connection.config.provider.CloudConfigProviderType.AZURE_VAULT;
import static com.dbn.connection.config.provider.CloudConfigProviderType.HASHICORP_VAULT;
import static com.dbn.connection.config.provider.CloudConfigProviderType.OCI_DB_TOOLS;
import static com.dbn.connection.config.provider.CloudConfigProviderType.OCI_OBJECT;
import static com.dbn.connection.config.provider.CloudConfigProviderType.OCI_VAULT;

@Getter
public enum CloudConfigProviderAuthentication implements Presentable {
    OCI_DEFAULT("OCI Default", "OCI_DEFAULT", OCI_OBJECT, OCI_DB_TOOLS, OCI_VAULT),
    OCI_INTERACTIVE("Interactive", "OCI_INTERACTIVE", OCI_OBJECT, OCI_DB_TOOLS, OCI_VAULT),
    AZURE_DEFAULT("Azure Default", "AZURE_DEFAULT", AZURE_APP_CONFIG, AZURE_VAULT),
    AZURE_SERVICE_PRINCIPAL_SECRET("Service Principal Secret", "AZURE_SERVICE_PRINCIPAL", AZURE_APP_CONFIG, AZURE_VAULT),
    AZURE_SERVICE_PRINCIPAL_CERTIFICATE("Service Principal Certificate", "AZURE_SERVICE_PRINCIPAL", AZURE_APP_CONFIG, AZURE_VAULT),
    AZURE_INTERACTIVE("Interactive", "AZURE_INTERACTIVE", AZURE_APP_CONFIG, AZURE_VAULT),
    HCP_DEFAULT("HashiCorp Default", "auto_detect", HASHICORP_VAULT),
    HCP_VAULT_TOKEN("Vault Token", "vault_token", HASHICORP_VAULT),
    HCP_USERPASS("Userpass", "userpass", HASHICORP_VAULT),
    HCP_APPROLE("AppRole", "approle", HASHICORP_VAULT),
    HCP_GITHUB("GitHub", "github", HASHICORP_VAULT);

    private final String name;
    private final String parameterValue;
    private final CloudConfigProviderType[] providers;

    CloudConfigProviderAuthentication(String name, String parameterValue, CloudConfigProviderType... providers) {
        this.name = name;
        this.parameterValue = parameterValue;
        this.providers = providers;
    }

    public boolean supports(CloudConfigProviderType provider) {
        return Arrays.asList(providers).contains(provider);
    }

    public static CloudConfigProviderAuthentication[] values(CloudConfigProviderType provider) {
        return Arrays.stream(values())
                .filter(value -> value.supports(provider))
                .toArray(CloudConfigProviderAuthentication[]::new);
    }

    public static CloudConfigProviderAuthentication get(String name) {
        if (name == null) return null;

        for (CloudConfigProviderAuthentication value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
            if (value.parameterValue.equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }

    public static CloudConfigProviderAuthentication getAzure(String name, boolean certificateAuthentication) {
        if ("AZURE_SERVICE_PRINCIPAL".equalsIgnoreCase(name)) {
            return certificateAuthentication ? AZURE_SERVICE_PRINCIPAL_CERTIFICATE : AZURE_SERVICE_PRINCIPAL_SECRET;
        }
        return get(name);
    }

    public static CloudConfigProviderAuthentication getDefault(CloudConfigProviderType provider) {
        CloudConfigProviderAuthentication[] values = values(provider);
        return values.length == 0 ? null : values[0];
    }
}
