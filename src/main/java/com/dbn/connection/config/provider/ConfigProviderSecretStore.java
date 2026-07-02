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

import com.dbn.common.util.Chars;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static com.dbn.connection.config.provider.CloudConfigProviderAuthentication.AZURE_SERVICE_PRINCIPAL_CERTIFICATE;
import static com.dbn.connection.config.provider.CloudConfigProviderAuthentication.AZURE_SERVICE_PRINCIPAL_SECRET;
import static com.dbn.connection.config.provider.CloudConfigProviderAuthentication.HCP_VAULT_TOKEN;
import static com.dbn.connection.config.provider.CloudConfigProviderType.HASHICORP_VAULT;

@UtilityClass
public class ConfigProviderSecretStore {

    public static void addRuntimeSecrets(
            @NotNull Map<String, String> parameters,
            @NotNull ConfigProviderInfo configProviderInfo) {
        CloudConfigProviderAuthentication authentication = configProviderInfo.getAuthentication();
        if (authentication == null) return;

        if (configProviderInfo.getCloudProviderType() != HASHICORP_VAULT) {
            switch (authentication) {
                case AZURE_SERVICE_PRINCIPAL_SECRET ->
                        addRuntimeSecret(parameters, "AZURE_CLIENT_SECRET", configProviderInfo.getAzureClientSecret());
                case AZURE_SERVICE_PRINCIPAL_CERTIFICATE ->
                        addRuntimeSecret(parameters, "AZURE_CLIENT_CERTIFICATE_PASSWORD", configProviderInfo.getAzureClientCertificatePassword());
                default -> {
                }
            }
            return;
        }

        switch (authentication) {
            case HCP_VAULT_TOKEN ->
                    addRuntimeSecret(parameters, "VAULT_TOKEN", configProviderInfo.getHashicorpVaultToken());
            case HCP_USERPASS ->
                    addRuntimeSecret(parameters, "VAULT_PASSWORD", configProviderInfo.getHashicorpVaultPassword());
            case HCP_APPROLE ->
                    addRuntimeSecret(parameters, "SECRET_ID", configProviderInfo.getHashicorpAppRoleSecretId());
            case HCP_GITHUB ->
                    addRuntimeSecret(parameters, "GITHUB_TOKEN", configProviderInfo.getHashicorpGithubToken());
            default -> {
            }
        }
    }

    private static void addRuntimeSecret(
            @NotNull Map<String, String> parameters,
            @NotNull String parameterName,
            char[] value) {
        if (Chars.isNotEmpty(value)) {
            parameters.put(parameterName, Chars.toString(value));
        }
    }
}
