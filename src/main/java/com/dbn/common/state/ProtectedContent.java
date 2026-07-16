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

package com.dbn.common.state;

import com.dbn.common.util.Strings;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.component.PersistentStateContext.getEncryptionCache;
import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.readCdata;
import static com.dbn.common.options.setting.Settings.setBooleanAttribute;
import static com.dbn.common.options.setting.Settings.writeCdata;
import static com.dbn.common.util.Commons.nvl;

public final class ProtectedContent implements PersistentStateElement {
    private final @NonNls String encryptionScope;
    private String encryptedValue;
    private boolean encrypted;
    private String value;
    private boolean resolved;
    private final StateEncryptionCache encryptionCache;

    public ProtectedContent(@NonNls String encryptionScope) {
        this.encryptionScope = encryptionScope;
        this.encryptionCache = getEncryptionCache();
    }

    public ProtectedContent(@NonNls String encryptionScope, @Nullable String value) {
        this(encryptionScope);
        set(value);
    }

    @Override
    public void readState(Element element) {
        encryptedValue = readCdata(element);
        encrypted = booleanAttribute(element, "encrypted", false);
        resolved = !encrypted;
        value = encrypted ? null : encryptedValue;
    }

    @NotNull
    public String get() {
        if (!resolved) {
            value = nvl(encryptionCache == null ?
                    StateEncryption.decrypt(encryptionScope, encryptedValue) :
                    encryptionCache.decrypt(encryptionScope, encryptedValue), "");
            resolved = true;
        }
        return nvl(value, "");
    }

    public void set(@Nullable String value) {
        this.encryptedValue = null;
        this.encrypted = false;
        this.resolved = true;
        this.value = nvl(value, "");
    }

    @Override
    public void writeState(Element element) {
        if (!resolved && encrypted) {
            setBooleanAttribute(element, "encrypted", true);
            writeCdata(element, encryptedValue, true);
            return;
        }

        String value = get();
        if (Strings.isEmpty(value)) {
            setBooleanAttribute(element, "encrypted", false);
            writeCdata(element, value, true);
            return;
        }

        StateEncryption.StoredValue storedValue = encryptionCache == null ?
                StateEncryption.encrypt(encryptionScope, value) :
                encryptionCache.encrypt(encryptionScope, value);

        if (!storedValue.encrypted()) {
            StateEncryption.requestUnencryptedStateApproval();
        }

        setBooleanAttribute(element, "encrypted", storedValue.encrypted());
        writeCdata(element, storedValue.value(), true);
    }

    public boolean isEmpty() {
        return resolved ? Strings.isEmpty(value) : Strings.isEmpty(encryptedValue);
    }
}
