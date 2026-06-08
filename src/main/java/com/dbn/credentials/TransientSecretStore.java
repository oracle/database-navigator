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

package com.dbn.credentials;

import com.dbn.common.util.Chars;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.util.Base64.decode;
import static com.dbn.common.util.Base64.encode;
import static com.dbn.common.util.TimeUtil.Millis.THIRTY_SECONDS;
import static java.lang.System.currentTimeMillis;

/**
 * Short-lived in-memory storage for secrets that must be propagated during transient
 * configuration operations, such as settings cloning or applying form changes.
 * <p>
 * This store is intended to replace transient XML secret attributes. Callers provide a
 * stable key made from non-secret identifiers, for example connection id, secret type,
 * and user name. The XML/configuration object continues to carry only non-secret
 * configuration data while the secret value is temporarily available from this store.
 * <p>
 * Entries expire after thirty seconds and are single-use: {@link #consume(char[], Object...)}
 * removes the entry before returning it. Secrets are stored as Base64-encoded {@code char[]}
 * values only to avoid keeping the original clear text array shape in the map; Base64 is not
 * encryption and this class must not be used for persistent storage or cross-restart transfer.
 */
public final class TransientSecretStore {
    private static final long TIMEOUT = THIRTY_SECONDS;
    private static final Timer CLEANUP_TIMER = new Timer("DBN - Transient Secret Store", true);
    private static final Map<Key, Entry> DATA = new ConcurrentHashMap<>();

    private TransientSecretStore() {}

    /**
     * Stores a secret under the supplied key parts for a short transient window.
     *
     * @param secret the secret to store, copied into an encoded internal representation
     * @param keyParts stable non-secret values that uniquely identify the transient secret
     */
    public static void store(char[] secret, @NotNull Object ... keyParts) {
        cleanupExpired();

        Key key = new Key(keyParts);
        Entry entry = new Entry(encode(secret), currentTimeMillis() + TIMEOUT);
        Entry oldEntry = DATA.put(key, entry);
        if (oldEntry != null) oldEntry.clear();

        CLEANUP_TIMER.schedule(new CleanupTask(key, entry), TIMEOUT);
    }

    /**
     * Reads and removes the secret for the supplied key parts.
     *
     * @param defaultSecret fallback returned when the key is missing or expired
     * @param keyParts the same stable non-secret values used for {@link #store(char[], Object...)}
     * @return the stored secret, or {@code defaultSecret} when unavailable
     */
    public static char[] consume(@Nullable char[] defaultSecret, @NotNull Object ... keyParts) {
        Key key = new Key(keyParts);
        Entry entry = DATA.remove(key);
        if (entry == null) return defaultSecret;

        try {
            if (entry.isExpired()) return defaultSecret;
            return decode(entry.secret);
        } finally {
            entry.clear();
        }
    }

    private static void cleanupExpired() {
        long time = currentTimeMillis();
        DATA.entrySet().removeIf(e -> {
            Entry entry = e.getValue();
            boolean expired = entry.isExpired(time);
            if (expired) entry.clear();
            return expired;
        });
    }

    private static class CleanupTask extends TimerTask {
        private final Key key;
        private final Entry entry;

        private CleanupTask(Key key, Entry entry) {
            this.key = key;
            this.entry = entry;
        }

        @Override
        public void run() {
            if (DATA.remove(key, entry)) {
                entry.clear();
            }
        }
    }

    private static class Entry {
        private final char[] secret;
        private final long expiresAt;

        private Entry(char[] secret, long expiresAt) {
            this.secret = secret;
            this.expiresAt = expiresAt;
        }

        private boolean isExpired() {
            return isExpired(currentTimeMillis());
        }

        private boolean isExpired(long time) {
            return time >= expiresAt;
        }

        private void clear() {
            Chars.clear(secret);
        }
    }

    private static final class Key {
        private final Object[] parts;

        private Key(Object[] parts) {
            this.parts = Arrays.copyOf(parts, parts.length);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return Arrays.equals(parts, key.parts);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(parts);
        }
    }
}
