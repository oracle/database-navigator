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

package com.dbn.nls;

import com.dbn.common.util.Localization;
import com.dbn.common.util.Named;
import com.intellij.DynamicBundle;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

import java.io.File;
import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.util.Commons.nvl;

public class NlsResources extends DynamicBundle{
    public static final @NonNls String BUNDLE = "messages.DBNResources";
    private static final NlsResources INSTANCE = new NlsResources();
    private static final Object[] EMPTY_PARAMS = new Object[0];
    private static final Map<String, Boolean> KEY_VALIDITY_CACHE = new ConcurrentHashMap<>();

    static { Localization.initDefaultLocale(); }

    public NlsResources() {
        super(BUNDLE);
    }

    public static @Nls String txt(@PropertyKey(resourceBundle = BUNDLE) String key) {
        return txt(key, EMPTY_PARAMS);
    }

    public static @Nls String txt(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        adjustParams(params);
        if (isValidKey(key)) {
            key = key.intern();
            return INSTANCE.getMessage(key, params);
        } else if (params != null && params.length > 0) {
            return MessageFormat.format(key, params);
        }
        return key;
    }

    /**
     * NLS Key validator
     * Workaround for partial implementation of nls resources
     * The resource bundle is returning the key surrounded with exclamation marks if the key is not available.
     * This usually happens when the nls engine is invoked with the text itself (again because not all texts are captured yet in NLS).
     * <p>
     * @param key the key to be verified
     * @return true if the key is valid, false otherwise
     */
    private static boolean isValidKey(String key) {
        if (key == null) return false;

        key = key.intern();
        Boolean valid = KEY_VALIDITY_CACHE.computeIfAbsent(key, k -> checkKeyValidity(k));
        return valid == Boolean.TRUE;
    }

    private static Boolean checkKeyValidity(String k) {
        // avoid boxing and unboxing
        return k.matches("^[a-zA-Z0-9._-]+$") ? Boolean.TRUE : Boolean.FALSE;
    }

    private static void adjustParams(Object ... params) {
        if (params == null || params.length == 0) return;
        for (int i = 0; i < params.length; i++) {
            if (params[i] instanceof Exception) {
                Exception exception = (Exception) params[i];
                params[i] = nvl(
                        exception.getLocalizedMessage(),
                        exception.getClass().getSimpleName());
            } else if (params[i] instanceof Named) {
                Named named = (Named) params[i];
                params[i] = named.getName();
            } else if (params[i] instanceof File) {
                File file = (File) params[i];
                params[i] = file.getPath();
            } else if (params[i] instanceof VirtualFile) {
                VirtualFile file = (VirtualFile) params[i];
                params[i] = file.getPath();
            }
        }
    }
}
