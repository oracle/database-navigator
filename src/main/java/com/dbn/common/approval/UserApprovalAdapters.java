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

package com.dbn.common.approval;

import com.dbn.common.extension.ExtensionPointCache;

import static com.dbn.common.util.Unsafe.cast;

/**
 * Cache for {@link UserApprovalAdapter} extensions keyed by the
 * {@link UserApprovable} implementation class.
 */
public class UserApprovalAdapters extends ExtensionPointCache<Class<? extends UserApprovable>, UserApprovalAdapter> {
    private static final UserApprovalAdapters INSTANCE = new UserApprovalAdapters();

    private UserApprovalAdapters() {
        super(UserApprovalAdapter.EP, a -> a.getApprovalClass());
    }

    public static <T extends UserApprovable> UserApprovalAdapter<T> get(T approval) {
        Class<? extends UserApprovable> approvalClass = cast(approval.getClass());
        return cast(INSTANCE.find(approvalClass));
    }

    /**
     * Resolves the approval key using the adapter registered for the given object.
     */
    public static String getApprovalKey(UserApprovable approval) {
        UserApprovalAdapter<UserApprovable> adapter = get(approval);
        return adapter.getApprovalKey(approval);
    }

    @Override
    protected Class<? extends UserApprovable> alternativeKey(Class<? extends UserApprovable> key) {
        Class<?> superclass = key.getSuperclass();
        if (superclass != null && UserApprovable.class.isAssignableFrom(superclass)) {
            return cast(superclass);
        }
        return null;
    }
}
