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
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.util.Unsafe.cast;

/**
 * Cache for {@link UserApprovalAdapter} extensions keyed by the
 * {@link UserApprovalAction}.
 */
public class UserApprovalAdapters extends ExtensionPointCache<UserApprovalAction, UserApprovalAdapter> {
    private static final UserApprovalAdapters INSTANCE = new UserApprovalAdapters();

    private UserApprovalAdapters() {
        super(UserApprovalAdapter.EP, a -> a.getApprovalAction());
    }

    public static <T extends UserApprovable> UserApprovalAdapter<T> get(UserApprovalAction action, T approvable) {
        UserApprovalAdapter<T> adapter = cast(INSTANCE.find(action));
        if (adapter.getApprovalClass() != approvable.getClass()) {
            throw new IllegalArgumentException(
                    "Unexpected approvable class " + approvable.getClass().getName() +
                            " for approval action " + action +
                            ", expected " + adapter.getApprovalClass().getName());
        }
        if (!approvable.getApprovalActions().contains(action)) {
            throw new IllegalArgumentException(
                    "Approval action " + action + " is not supported by " + approvable.getClass().getName());
        }
        return adapter;
    }

    /**
     * Resolves the approval key using the adapter registered for the given object.
     */
    public static String getApprovalKey(UserApprovalAction action, UserApprovable approvable) {
        UserApprovalAdapter<UserApprovable> adapter = get(action, approvable);
        String approvalKey = adapter.getApprovalKey(approvable);
        String actionApprovalKey = "approval-action:" + action.name() + ":" + approvalKey;
        if (approvable instanceof ProjectUserApprovable projectApproval) {
            return actionApprovalKey + ":project:" + projectApproval.getProject().getLocationHash();
        }
        return actionApprovalKey;
    }

    public static @Nullable String getApprovalSignature(UserApprovalAction action, UserApprovable approvable) {
        UserApprovalAdapter<UserApprovable> adapter = get(action, approvable);
        return adapter.getApprovalSignature(approvable);
    }
}
