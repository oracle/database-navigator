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

package com.dbn.common.checksum.file;

import com.dbn.common.component.ApplicationComponentBase;
import com.dbn.common.event.ApplicationEvents;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Dispatch;
import com.dbn.diagnostics.Diagnostics;
import com.intellij.openapi.application.ModalityState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;

import static com.dbn.common.component.Components.applicationService;

/**
 * Application-scoped, transient cache of background file fingerprint captures.
 * Callers use the same stable file identity to share calculating, ready, and failed states.
 * Use {@link FileContentSignature#getIdentity(java.io.File)} for path-backed files.
 */
public class FileFingerprintCache extends ApplicationComponentBase {
    public static final String COMPONENT_NAME = "DBNavigator.Application.FileFingerprintCache";

    private static final long CACHE_TTL_MILLIS = 60_000;
    private static final int CACHE_SIZE_LIMIT = 256;

    private final Map<String, Entry> entries = new HashMap<>();

    private FileFingerprintCache() {
        super(COMPONENT_NAME);
    }

    public static FileFingerprintCache getInstance() {
        return applicationService(FileFingerprintCache.class);
    }

    @Nullable
    public Snapshot get(@NotNull String fileIdentity) {
        synchronized (entries) {
            Entry entry = entries.get(fileIdentity);
            return entry == null ? null : entry.snapshot();
        }
    }

    /**
     * Returns a recent fingerprint unless refreshing, otherwise starts or joins a background capture.
     */
    @NotNull
    public Snapshot capture(
            @NotNull String fileIdentity,
            @NotNull Supplier<@NotNull FileFingerprint> calculator,
            boolean refresh) {

        Entry entry;
        Snapshot snapshot;
        long now = System.currentTimeMillis();
        synchronized (entries) {
            prune(now);
            entry = entries.get(fileIdentity);
            if (entry != null && entry.status == FileFingerprintStatus.CALCULATING) {
                return entry.snapshot();
            }
            if (!refresh && entry != null &&
                    entry.status == FileFingerprintStatus.READY && now - entry.completedAt <= CACHE_TTL_MILLIS) {
                return entry.snapshot();
            }

            Entry previous = entry;
            if (previous != null) removeEntry(previous);
            entry = new Entry(fileIdentity);
            entries.put(fileIdentity, entry);
            trim();
            snapshot = entry.snapshot();
        }

        Entry capture = entry;
        Background.run(() -> calculate(fileIdentity, capture, calculator));
        return snapshot;
    }

    private void calculate(
            @NotNull String fileIdentity,
            @NotNull Entry capture,
            @NotNull Supplier<@NotNull FileFingerprint> calculator) {

        @Nullable FileFingerprint fingerprint = null;
        FileFingerprintStatus status = FileFingerprintStatus.FAILED;
        try {
            fingerprint = calculator.get();
            status = FileFingerprintStatus.READY;
        } catch (Exception e) {
            Diagnostics.conditionallyLog(e);
        }

        synchronized (entries) {
            if (entries.get(fileIdentity) != capture) return;

            capture.status = status;
            capture.fingerprint = fingerprint;
            capture.completedAt = System.currentTimeMillis();
        }

        if (isDisposed()) return;
        Dispatch.run(ModalityState.any(), () -> ApplicationEvents.notify(
                FileFingerprintListener.TOPIC,
                listener -> listener.fingerprintChanged(fileIdentity)));
    }

    private void prune(long now) {
        Iterator<Map.Entry<String, Entry>> iterator = entries.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next().getValue();
            if (entry.status != FileFingerprintStatus.CALCULATING && now - entry.completedAt > CACHE_TTL_MILLIS) {
                iterator.remove();
            }
        }
    }

    private void trim() {
        while (entries.size() > CACHE_SIZE_LIMIT) {
            Entry oldestEntry = null;
            long oldestCompletion = Long.MAX_VALUE;
            for (Entry entry : entries.values()) {
                if (entry.status != FileFingerprintStatus.CALCULATING && entry.completedAt < oldestCompletion) {
                    oldestEntry = entry;
                    oldestCompletion = entry.completedAt;
                }
            }
            if (oldestEntry == null) return;
            removeEntry(oldestEntry);
        }
    }

    private void removeEntry(Entry entry) {
        entries.remove(entry.identity, entry);
    }

    public record Snapshot(@NotNull FileFingerprintStatus status, @Nullable FileFingerprint fingerprint) {
    }

    private static class Entry {
        private final String identity;
        private FileFingerprintStatus status = FileFingerprintStatus.CALCULATING;
        private FileFingerprint fingerprint;
        private long completedAt;
        private Entry(String identity) {
            this.identity = identity;
        }

        private Snapshot snapshot() {
            return new Snapshot(status, fingerprint);
        }
    }
}
