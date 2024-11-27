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

package com.dbn.common;

import java.util.HashSet;
import java.util.Set;

public class ThreadLocalRegister {
    private static final ThreadLocal<Set<Object>> threadLocal = new ThreadLocal<>();

    static {
        threadLocal.set(new HashSet<>());
    }

    private static Set<Object> getRegister() {
        Set<Object> register = threadLocal.get();
        if (register == null) {
            register = new HashSet<>();
            threadLocal.set(register);
        }
        return register;
    }

    public static void register(Object object) {
        getRegister().add(object);
    }

    public static void unregister(Object object) {
        getRegister().remove(object);
    }

    public static boolean isRegistered(Object object) {
        return getRegister().contains(object);
    }
}
