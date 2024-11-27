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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DiagnosticEntryComposite<T extends Comparable<T>> extends DiagnosticEntry.Delegate<T> {
    private final T identifier;
    private final Map<String, DiagnosticEntry<T>> details = new ConcurrentHashMap<>();

    public DiagnosticEntryComposite(T identifier) {
        this.identifier = identifier;
    }

    public DiagnosticEntry<T> getDetail(String qualifier) {
        return details.computeIfAbsent(qualifier, q -> new DiagnosticEntryBase<>(identifier));
    }
}
