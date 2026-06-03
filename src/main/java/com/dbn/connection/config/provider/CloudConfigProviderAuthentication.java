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

@Getter
public enum CloudConfigProviderAuthentication implements Presentable {
    OCI_DEFAULT("OCI Default", "OCI_DEFAULT", CloudConfigProviderType.OCI_OBJECT, CloudConfigProviderType.OCI_DB_TOOLS, CloudConfigProviderType.OCI_VAULT),
    OCI_INTERACTIVE("Interactive", "OCI_INTERACTIVE", CloudConfigProviderType.OCI_OBJECT, CloudConfigProviderType.OCI_DB_TOOLS, CloudConfigProviderType.OCI_VAULT),
    AZURE_DEFAULT("Azure Default", "AZURE_DEFAULT", CloudConfigProviderType.AZURE_APP_CONFIG, CloudConfigProviderType.AZURE_VAULT),
    AZURE_SERVICE_PRINCIPAL("Service Principal", "AZURE_SERVICE_PRINCIPAL", CloudConfigProviderType.AZURE_APP_CONFIG, CloudConfigProviderType.AZURE_VAULT),
    AZURE_INTERACTIVE("Interactive", "AZURE_INTERACTIVE", CloudConfigProviderType.AZURE_APP_CONFIG, CloudConfigProviderType.AZURE_VAULT),
    HCP_DEFAULT("HashiCorp Default", "auto_detect", CloudConfigProviderType.HASHICORP_VAULT),
    HCP_VAULT_TOKEN("Vault Token", "vault_token", CloudConfigProviderType.HASHICORP_VAULT),
    HCP_USERPASS("Userpass", "userpass", CloudConfigProviderType.HASHICORP_VAULT),
    HCP_APPROLE("AppRole", "approle", CloudConfigProviderType.HASHICORP_VAULT),
    HCP_GITHUB("GitHub", "github", CloudConfigProviderType.HASHICORP_VAULT);

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
            if (value.parameterValue.equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }

    public static CloudConfigProviderAuthentication getDefault(CloudConfigProviderType provider) {
        CloudConfigProviderAuthentication[] values = values(provider);
        return values.length == 0 ? null : values[0];
    }
}
