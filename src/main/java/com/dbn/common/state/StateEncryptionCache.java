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

package com.dbn.common.state;

import com.dbn.common.state.StateEncryption.StoredValue;
import com.dbn.common.util.Strings;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class StateEncryptionCache {
    private final ConcurrentMap<Key, Object> values = new ConcurrentHashMap<>();

    public StoredValue encrypt(@NonNls String encryptionScope, @Nullable String value) {
        if (Strings.isEmpty(value)) return new StoredValue(value, false);

        Key key = new Key(Operation.ENCRYPT, encryptionScope, value, StateEncryption.shouldEncrypt());
        return (StoredValue) values.computeIfAbsent(key, k -> StateEncryption.encrypt(encryptionScope, value));
    }

    @Nullable
    public String decrypt(@NonNls String encryptionScope, @Nullable String value) {
        if (Strings.isEmpty(value)) return value;

        Key key = new Key(Operation.DECRYPT, encryptionScope, value, true);
        return (String) values.computeIfAbsent(key, k -> StateEncryption.decrypt(encryptionScope, value));
    }

    public void clear() {
        values.clear();
    }

    private enum Operation {
        ENCRYPT,
        DECRYPT
    }

    private record Key(Operation operation, @NonNls String encryptionScope, @Nullable String value, boolean encrypted) {}
}
