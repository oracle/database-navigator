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

import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.UnsafeWeakList;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A registry for managing and retrieving {@link SecretsOwner} instances. This class maintains
 * a weakly referenced list of secrets owners, allowing the retrieval of
 * the name associated with a secret owner via their identifier.
 */
@Slf4j
public class SecretsOwnerRegistry {
    private static final Collection<SecretsOwner> DATA = new UnsafeWeakList<>();
    private static final Map<Object, SecretsOwner> DATA_CACHE = ContainerUtil.createConcurrentWeakValueMap();
    private static final Lock lock = new ReentrantLock();

    /**
     * Registers the given {@link SecretsOwner} instance into the registry.
     *
     * @param secretsOwner the instance of {@link SecretsOwner} to be added to the registry
     */
    public static void register(SecretsOwner secretsOwner) {
        try {
            lock.lock();
            DATA.add(secretsOwner);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retrieves the name associated with a given secret owner identifier.
     * If the owner is not found, the method returns the string representation of the provided identifier.
     * The method also ensures that unresolved owners are removed from the cache.
     *
     * @param ownerId the identifier of the secret owner whose name is to be retrieved
     * @return the name of the secret owner if resolved, otherwise the string representation of the provided identifier
     */
    public static String getOwnerName(Object ownerId) {
        SecretsOwner secretsOwner = DATA_CACHE.computeIfAbsent(ownerId, id -> resolveOwner(id));
        if (secretsOwner == NULL) {
            DATA_CACHE.remove(ownerId);
            return Objects.toString(ownerId);
        }

        return secretsOwner.getSecretOwnerName();
    }

    private static SecretsOwner resolveOwner(Object ownerId) {
        try {
            lock.lock();
            return findOwner(ownerId);
        } finally {
            lock.unlock();
        }
    }

    private static SecretsOwner findOwner(Object ownerId) {
        for (SecretsOwner secretsOwner : DATA) {
            if (Objects.equals(secretsOwner.getSecretOwnerId(), ownerId)) {
                return secretsOwner;
            }
        }
        log.error("No secrets owner found for id: {}", ownerId);
        return NULL;
    }

    private static final SecretsOwner NULL = new SecretsOwner() {
        @NotNull
        @Override
        public Object getSecretOwnerId() {
            return "NULL";
        }

        @Override
        public String getSecretOwnerName() {
            return "NULL";
        }

        @Override
        public @NotNull Secret[] getSecrets() {
            return new Secret[0];
        }

        @Override
        public void initSecrets() {

        }
    };
}
