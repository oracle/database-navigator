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

package com.dbn.diagnostics.data;

import com.dbn.common.latent.Latent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DiagnosticBundle<T extends Comparable<T>> {
    private final DiagnosticType type;
    private final boolean composite;
    private final Map<T, DiagnosticEntry<T>> entries = new ConcurrentHashMap<>();
    private final Latent<List<T>> keys = Latent.mutable(
            () -> getSignature(),
            () -> new ArrayList<>(entries.keySet()));

    private transient int signature;

    private DiagnosticBundle(DiagnosticType type, boolean composite) {
        this.type = type;
        this.composite = composite;
    }

    public static <T extends Comparable<T>> DiagnosticBundle<T> basic(DiagnosticType type) {
        return new DiagnosticBundle<>(type, false);
    }

    public static <T extends Comparable<T>> DiagnosticBundle<T> composite(DiagnosticType type) {
        return new DiagnosticBundle<>(type, true);
    }

    public int getSignature() {
        return signature;
    }

    public int size() {
        return getKeys().size();
    }

    public List<T> getKeys() {
        return keys.get();
    }

    public DiagnosticEntry<T> get(T identifier) {
        return this.entries.computeIfAbsent(identifier, i -> createEntry(i));
    }
    public DiagnosticEntry<T> get(T key, String qualifier) {
        return get(key).getDetail(qualifier);
    }

    public DiagnosticEntry<T> log(T identifier, boolean failure, boolean timeout, long value) {
        return log(identifier, DiagnosticEntry.DEFAULT_QUALIFIER, failure, timeout, value);
    }

    public DiagnosticEntry<T> log(T identifier, String qualifier, boolean failure, boolean timeout, long value) {
        DiagnosticEntry<T> entry = get(identifier, qualifier);
        entry.log(failure, timeout, value);
        signature++;
        return entry;
    }


    private DiagnosticEntry<T> createEntry(T identifier) {
        return composite ?
                new DiagnosticEntryComposite<>(identifier) :
                new DiagnosticEntryBase<>(identifier);
    }

    @Override
    public String toString() {
        return type + " (" + size() + " entries)";
    }
}
