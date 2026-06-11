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
import com.dbn.connection.ConnectionId;
import com.dbn.credentials.DatabaseCredentialManager;
import com.dbn.credentials.Secret;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static com.dbn.connection.config.provider.CloudConfigProviderAuthentication.HCP_VAULT_TOKEN;
import static com.dbn.connection.config.provider.CloudConfigProviderType.HASHICORP_VAULT;
import static com.dbn.credentials.SecretType.CONNECTION_HASHICORP_APPROLE_SECRET_ID;
import static com.dbn.credentials.SecretType.CONNECTION_HASHICORP_VAULT_PASSWORD;
import static com.dbn.credentials.SecretType.CONNECTION_HASHICORP_VAULT_TOKEN;

@UtilityClass
public class ConfigProviderSecretStore {

    public static char[] loadHashicorpVaultToken(@NotNull ConnectionId connectionId) {
        Secret secret = DatabaseCredentialManager.getInstance().loadSecret(
                CONNECTION_HASHICORP_VAULT_TOKEN,
                connectionId,
                null);
        return secret.getToken();
    }

    public static void saveHashicorpVaultToken(@NotNull ConnectionId connectionId, char[] token) {
        Secret secret = new Secret(CONNECTION_HASHICORP_VAULT_TOKEN, null, token);
        DatabaseCredentialManager.getInstance().storeSecret(connectionId, secret);
    }

    public static void removeHashicorpVaultToken(@NotNull ConnectionId connectionId) {
        Secret secret = new Secret(CONNECTION_HASHICORP_VAULT_TOKEN, null, Secret.EMPTY);
        DatabaseCredentialManager.getInstance().removeSecret(connectionId, secret);
    }

    public static char[] loadHashicorpVaultPassword(@NotNull ConnectionId connectionId) {
        Secret secret = DatabaseCredentialManager.getInstance().loadSecret(
                CONNECTION_HASHICORP_VAULT_PASSWORD,
                connectionId,
                null);
        return secret.getToken();
    }

    public static void saveHashicorpVaultPassword(@NotNull ConnectionId connectionId, char[] password) {
        Secret secret = new Secret(CONNECTION_HASHICORP_VAULT_PASSWORD, null, password);
        DatabaseCredentialManager.getInstance().storeSecret(connectionId, secret);
    }

    public static void removeHashicorpVaultPassword(@NotNull ConnectionId connectionId) {
        Secret secret = new Secret(CONNECTION_HASHICORP_VAULT_PASSWORD, null, Secret.EMPTY);
        DatabaseCredentialManager.getInstance().removeSecret(connectionId, secret);
    }

    public static char[] loadHashicorpAppRoleSecretId(@NotNull ConnectionId connectionId) {
        Secret secret = DatabaseCredentialManager.getInstance().loadSecret(
                CONNECTION_HASHICORP_APPROLE_SECRET_ID,
                connectionId,
                null);
        return secret.getToken();
    }

    public static void saveHashicorpAppRoleSecretId(@NotNull ConnectionId connectionId, char[] secretId) {
        Secret secret = new Secret(CONNECTION_HASHICORP_APPROLE_SECRET_ID, null, secretId);
        DatabaseCredentialManager.getInstance().storeSecret(connectionId, secret);
    }

    public static void removeHashicorpAppRoleSecretId(@NotNull ConnectionId connectionId) {
        Secret secret = new Secret(CONNECTION_HASHICORP_APPROLE_SECRET_ID, null, Secret.EMPTY);
        DatabaseCredentialManager.getInstance().removeSecret(connectionId, secret);
    }

    public static void addRuntimeSecrets(
            @NotNull Map<String, String> parameters,
            @NotNull ConfigProviderInfo configProviderInfo,
            @NotNull ConnectionId connectionId) {
        if (configProviderInfo.getCloudProviderType() != HASHICORP_VAULT) return;

        CloudConfigProviderAuthentication authentication = configProviderInfo.getAuthentication();
        if (authentication == null) return;

        switch (authentication) {
            case HCP_VAULT_TOKEN ->
                    addRuntimeSecret(parameters, "VAULT_TOKEN", loadHashicorpVaultToken(connectionId));
            case HCP_USERPASS ->
                    addRuntimeSecret(parameters, "VAULT_PASSWORD", loadHashicorpVaultPassword(connectionId));
            case HCP_APPROLE ->
                    addRuntimeSecret(parameters, "SECRET_ID", loadHashicorpAppRoleSecretId(connectionId));
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
