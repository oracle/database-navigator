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

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.util.Chars.isEmpty;
import static com.dbn.common.util.Commons.nvl;
import static java.util.Arrays.copyOf;

/**
 * A secret construct similar to {@link java.net.PasswordAuthentication}, holding additional
 * qualifying information like {@link SecretType}
 *
 * @author Dan Cioca (Oracle)
 */
@Data
public final class Secret {
    public static final char[] EMPTY = new char[0];

    private final SecretType type;
    private final String user;
    private final char[] token;

    public Secret(SecretType type, String user, String token) {
        this(type, user, toChars(token));
    }

    public Secret(SecretType type, String user, char[] token) {
        this.type = type;
        this.user = nvl(user, "");
        this.token = token == null ? EMPTY : copyOf(token, token.length);
    }

    @Nullable
    public String getStringToken() {
        return isEmpty(token) ? null : new String(token);
    }

    @NotNull
    private static char[] toChars(String token) {
        return token == null || token.isEmpty() ? EMPTY : token.toCharArray();
    }

    public String safePresentation() {
        // secret representation with length of token only
        return type + ":" + (isEmpty(token) ? "0" : token.length);
    }

    @Override
    public String toString() {
        // IMPORTANT: do not remove or alter this
        // (prevent sensitive data from ever being exposed in the logs)
        return safePresentation();
    }
}
