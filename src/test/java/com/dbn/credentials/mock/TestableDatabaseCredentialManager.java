package com.dbn.credentials.mock;

import com.dbn.credentials.DatabaseCredentialManager;
import com.dbn.credentials.SecretType;
import com.intellij.credentialStore.CredentialAttributes;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class TestableDatabaseCredentialManager extends DatabaseCredentialManager {
    public static @NonNls @NotNull CredentialAttributes createAttributes(SecretType secretType, Object ownerId, String user) {
        return DatabaseCredentialManager.createAttributes(secretType, ownerId, user);
    }
}
