/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.execution.java.wrapper;

import com.dbn.common.ref.WeakRefCache;
import com.dbn.object.DBJavaClass;
import lombok.Data;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class WrapperSupport {
    private static final WeakRefCache<DBJavaClass, WrapperSupportData> data = WeakRefCache.weakKey();

    WrapperSupportData getSupport(DBJavaClass javaClass) {
        return data.computeIfAbsent(javaClass, k -> createSupportData());
    }

    private static @NotNull WrapperSupportData createSupportData() {
        WrapperSupportData wrapperSupportData = new WrapperSupportData();
        // TODO: implement
        return wrapperSupportData;
    }

    @Data
    public static class WrapperSupportData {
        private volatile boolean argumentSupportChecked;
        private boolean argumentSupported;
        private int argumentDisplayRowCount;
        private String argumentUnsupportedReason;

        private volatile boolean returnSupportChecked;
        private boolean returnSupported;
        private int returnDisplayRowCount;
        private String returnUnsupportedReason;
    }
}
