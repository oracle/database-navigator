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

import com.dbn.common.thread.Background;
import com.intellij.util.containers.ContainerUtil;
import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.Set;

import static com.dbn.common.dispose.Failsafe.guarded;
import static com.dbn.common.options.ConfigActivity.APPLYING;
import static com.dbn.common.options.ConfigActivity.CLONING;
import static com.dbn.common.options.ConfigActivity.TRANSFERRING;
import static com.dbn.common.options.ConfigMonitor.is;
import static com.dbn.common.util.Commons.matchArrays;

@UtilityClass
public class Secrets {
    private static final Set<Secret> REGISTRY = ContainerUtil.createWeakSet();
    private static final Object REGISTRY_LOCK = new Object();

    static void register(Secret secret) {
        if (!secret.isPersistent()) return;
        if (is(CLONING)) return;
        if (is(APPLYING)) return;
        if (is(TRANSFERRING)) return;

        synchronized (REGISTRY_LOCK) {
            REGISTRY.add(secret);
        }
    }

    public static void initialize() {
        Background.run(() -> {
            Secret[] secrets;
            synchronized (REGISTRY_LOCK) {
                secrets = REGISTRY.toArray(new Secret[0]);
            }
            Arrays.stream(secrets).forEach(s -> guarded(() -> s.ensureLoaded()));
        });
    }

    public static boolean match(Secret secret1, Secret secret2) {
        if (secret1 == secret2) return true;
        if (secret1 == null || secret2 == null) return false;
        return matchArrays(secret1.getToken(), secret2.getToken());
    }

    public static int hash(Secret secret) {
        return secret == null ? 0 : Arrays.hashCode(secret.getToken());
    }

    public static void transfer(Secret target, Secret source) {
        if (source == null) {
            target.setToken(Secret.EMPTY);
            return;
        }

        target.copyState(source);
    }
}
