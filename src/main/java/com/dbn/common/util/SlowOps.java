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

package com.dbn.common.util;

import com.dbn.common.action.UserDataKeys;
import com.dbn.common.thread.ThreadMonitor;
import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.util.UserDataHolder;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static com.dbn.common.dispose.Failsafe.guarded;

@UtilityClass
public class SlowOps {

    public static <T extends UserDataHolder> boolean checkValid(@Nullable T entity, Predicate<T> verifier) {
        if (entity == null) return false;

        Boolean invalidEntity = entity.getUserData(UserDataKeys.INVALID_ENTITY);
        if (invalidEntity != null && invalidEntity) return false;

        if (ThreadMonitor.isDispatchThread()) return true;
        boolean valid = verifier.test(entity);

        if (!valid) entity.putUserData(UserDataKeys.INVALID_ENTITY, true);
        return valid;
    }

    public static boolean isValid(@Nullable DBObjectRef<?> ref) {
        if (ref == null) return false;

        DBObject object = ref.value();
        if (object != null && object.isValid()) return true;
        if (object == null && ThreadMonitor.isTimeSensitiveThread()) return true; // assume valid without loading

        object = guarded(null, ref, r -> r.get());
        return object != null && object.isValid();
    }
}
