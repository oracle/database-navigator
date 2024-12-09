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

package com.dbn.credentials;

import org.jetbrains.annotations.NotNull;

/**
 * Marker interface for configuration holding secret tokens like passwords, passphrases or authentication tokens
 * @author Dan Cioca (Oracle)
 */
public interface SecretsOwner {
    @NotNull
    Object getSecretOwnerId();

    @NotNull
    Secret[] getSecrets();

    /**
     * Utility for initializing the secrets from PasswordStore
     * (will be invoked once on settings-component initialization)
     */
    void initSecrets();

    /**
     * Utility for updating the secrets in PasswordStore via {@link DatabaseCredentialManager}
     * (will be invoked when settings are saved)
     * @param oldSecrets the old secrets to be discarded when new secrets are saved
     */
    default void updateSecrets(Secret[] oldSecrets) {
        DatabaseCredentialManager credentialManager = DatabaseCredentialManager.getInstance();
        credentialManager.queueSecretsUpdate(getSecretOwnerId(), oldSecrets, getSecrets());
    }

    default void removeSecrets() {
        DatabaseCredentialManager credentialManager = DatabaseCredentialManager.getInstance();
        credentialManager.queueSecretsRemove(getSecretOwnerId(), getSecrets());
    }

}
