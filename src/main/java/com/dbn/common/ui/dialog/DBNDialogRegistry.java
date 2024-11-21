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

package com.dbn.common.ui.dialog;

import com.dbn.common.dispose.Checks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class DBNDialogRegistry {
    private static final Map<Object, DBNDialog> cache = new ConcurrentHashMap<>();

    public static <T extends DBNDialog> T ensure(Object key, Supplier<T> provider) {
        DBNDialog dialog = cache.compute(key, (k, d) -> Checks.isValid(d) ? d : provider.get());
        dialog.addDialogListener(action -> {
            if (action == DBNDialogListener.Action.CLOSE) cache.remove(key);
        });
        return (T) dialog;
    }

}
