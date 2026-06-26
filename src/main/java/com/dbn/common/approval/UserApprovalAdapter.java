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

import com.dbn.common.extension.ExtensionPoint;
import com.intellij.openapi.extensions.ExtensionPointName;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

/**
 * Extension point for adapting a {@link UserApprovable} object into approval
 * metadata used by {@link UserApprovalManager}.
 *
 * @param <T> the approved object type handled by this adapter
 */
public interface UserApprovalAdapter<T extends UserApprovable> extends ExtensionPoint {
    ExtensionPointName<UserApprovalAdapter> EP = ExtensionPointName.create("com.dbn.userApprovalAdapter");

    /**
     * Returns the concrete {@link UserApprovable} class this adapter supports.
     */
    Class<T> getApprovalClass();

    UserApprovalAction getApprovalAction();

    /**
     * Returns the title shown in the approval dialog.
     */
    @Nls
    String getApprovalTitle(T approvable);

    /**
     * Returns the message shown in the approval dialog.
     */
    @Nls
    String getApprovalMessage(T approvable);

    /**
     * Returns the stable key used to persist approval for this approvable object.
     */
    @NonNls
    String getApprovalKey(T approvable);

    /**
     * Returns a signature for the external conditions that make this approval valid.
     * <p>
     * When the signature changes, any persisted approval for the same key is invalidated.
     */
    @Nullable
    @NonNls
    default String getApprovalSignature(T approvable) {
        return null;
    }

    /**
     * Returns how long a successful approval remains valid.
     */
    default UserApprovalLifetime getApprovalLifetime(T approvable) {
        return UserApprovalLifetime.PERSISTENT;
    }

    /**
     * Returns the dialog options. Option index {@code 0} is the approval action;
     * all other options are treated as cancellation/rejection after
     * {@link #processApprovalOption(UserApprovable, int)} is invoked.
     */
    @Nls
    String[] getApprovalOptions(T approvable);

    default void processApprovalOption(T approvable, int option) {
    }

    /**
     * Returns how long to suppress repeat approval prompts after
     * the user selects the given non-approval option.
     */
    @Nullable
    Duration getRejectionCooldown(T approvable, int option);
}
