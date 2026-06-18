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

import com.dbn.common.thread.Background;
import com.dbn.common.util.Chars;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

import static com.dbn.common.util.Commons.nvl;
import static java.util.Arrays.copyOf;

/**
 * A secret construct similar to {@link java.net.PasswordAuthentication}, holding additional
 * qualifying information like {@link SecretType}
 *
 * @author Dan Cioca (Oracle)
 */
public final class Secret {
    public static final char[] EMPTY = new char[0];

    private final @Getter SecretType type;
    private final Supplier<?> ownerId;
    private final Supplier<String> user;
    private char[] token;
    private volatile @Getter boolean loaded;
    private boolean loading;

    public Secret(SecretType type, String user, String token) {
        this(type, user, toChars(token));
    }

    public Secret(SecretType type, String user, char[] token) {
        this(type, null, () -> user, token, true);
    }

    public Secret(SecretType type, Supplier<?> ownerId, Supplier<String> user) {
        this(type, ownerId, user, EMPTY, false);
    }

    private Secret(SecretType type, Supplier<?> ownerId, Supplier<String> user, char[] token, boolean loaded) {
        this.type = type;
        this.ownerId = ownerId;
        this.user = user;
        this.token = token == null ? EMPTY : copyOf(token, token.length);
        this.loaded = loaded;
        Secrets.register(this);
    }

    @NotNull
    public String getUser() {
        return nvl(user == null ? null : user.get(), "");
    }

    @NotNull
    public char[] getToken() {
        loadIfAllowed();
        synchronized (this) {
            return copyOf(token, token.length);
        }
    }

    public synchronized void setToken(@Nullable char[] token) {
        this.token = token == null ? EMPTY : copyOf(token, token.length);
        loaded = true;
    }

    public void setToken(@Nullable Secret secret) {
        Secrets.transfer(this, secret);
    }

    @NotNull
    public synchronized Secret snapshot() {
        return new Secret(type, null, this::getUser, token, loaded);
    }

    public synchronized void ensureLoaded() {
        if (loaded) return;
        if (ownerId == null) {
            loaded = true;
            return;
        }

        Secret secret = DatabaseCredentialManager.getInstance().loadSecret(type, ownerId.get(), getUser());
        setToken(secret.getToken());
    }

    private void loadIfAllowed() {
        if (loaded) return;

        Application application = ApplicationManager.getApplication();
        if (application != null && application.isDispatchThread()) {
            queueLoad();
            return;
        }

        ensureLoaded();
    }

    private void queueLoad() {
        synchronized (this) {
            if (loaded || loading) return;
            if (ownerId == null) {
                loaded = true;
                return;
            }
            loading = true;
        }

        Background.run(() -> {
            try {
                ensureLoaded();
            } finally {
                synchronized (this) {
                    loading = false;
                }
            }
        });
    }

    synchronized boolean isPersistent() {
        return ownerId != null;
    }

    void copyState(@NotNull Secret secret) {
        char[] token;
        boolean loaded;
        synchronized (secret) {
            token = copyOf(secret.token, secret.token.length);
            loaded = secret.loaded;
        }

        synchronized (this) {
            this.token = token;
            this.loaded = loaded;
            this.loading = false;
        }
    }

    public boolean isProvided() {
        return Chars.isNotEmpty(getToken());
    }

    @NotNull
    private static char[] toChars(String token) {
        return token == null || token.isEmpty() ? EMPTY : token.toCharArray();
    }

    public String safePresentation() {
        // secret representation with length of token only
        return type + ":" + (loaded ? token.length : "unloaded");
    }

    @Override
    public String toString() {
        // IMPORTANT: do not remove or alter this
        // (prevent sensitive data from ever being exposed in the logs)
        return safePresentation();
    }
}
