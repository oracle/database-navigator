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

package com.dbn.connection;

import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

@Getter
public final class ConnectionSecretOwnerId {
    // Stable identity component. This must not depend on mutable project XML fields
    // such as the connection display name.
    private final String key;

    // Human-readable PasswordSafe caption component. Keep this out of identity;
    // renaming a connection must not rebind or lose the underlying secret owner.
    private final transient String name;

    private ConnectionSecretOwnerId(@NotNull String key, @NotNull String name) {
        this.key = key;
        this.name = name;
    }

    public static @NotNull ConnectionSecretOwnerId create(
            @NotNull Project project,
            @NotNull ConnectionId connectionId,
            @NotNull String connectionName) {
        String projectHash = project.getLocationHash();
        String connectionIdValue = connectionId.id();
        String key = "project:" + projectHash + ":connection:" + connectionIdValue;
        String displayName = connectionName + " (" + project.getName() + ", scope " + fingerprint(key) + ")";
        return new ConnectionSecretOwnerId(key, displayName);
    }

    private static String fingerprint(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 8);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(value.hashCode());
        }
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ConnectionSecretOwnerId that)) return false;
        return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }
}
