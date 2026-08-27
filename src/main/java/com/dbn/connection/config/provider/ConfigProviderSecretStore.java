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

@UtilityClass
public class ConfigProviderSecretStore {

    public static void addRuntimeSecrets(
            @NotNull Map<String, String> parameters,
            @NotNull ConfigProviderInfo configProvider) {
        CloudAuthenticationType authentication = configProvider.getCloudProviderAuthentication();
        if (authentication == null) return;

        if (configProvider.isAzureProvider()) {
            switch (authentication) {
                case AZURE_SERVICE_PRINCIPAL_SECRET -> addRuntimeSecret(parameters, "AZURE_CLIENT_SECRET", configProvider.getAzureClientSecret());
                case AZURE_SERVICE_PRINCIPAL_CERTIFICATE -> addRuntimeSecret(parameters, "AZURE_CLIENT_CERTIFICATE_PASSWORD", configProvider.getAzureClientCertificatePassword());
                default -> {}
            }
        } else if (configProvider.isHashicorpProvider()) {
            switch (authentication) {
                case HCP_VAULT_TOKEN -> addRuntimeSecret(parameters, "VAULT_TOKEN", configProvider.getHashicorpVaultToken());
                case HCP_USERPASS -> addRuntimeSecret(parameters, "VAULT_PASSWORD", configProvider.getHashicorpVaultPassword());
                case HCP_APPROLE -> addRuntimeSecret(parameters, "SECRET_ID", configProvider.getHashicorpAppRoleSecretId());
                case HCP_GITHUB -> addRuntimeSecret(parameters, "GITHUB_TOKEN", configProvider.getHashicorpGithubToken());
                default -> {}
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
