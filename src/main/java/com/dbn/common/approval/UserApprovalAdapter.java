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

    /**
     * Returns the title shown in the approval dialog.
     */
    String getApprovalTitle(T approvable);

    /**
     * Returns the message shown in the approval dialog.
     */
    String getApprovalMessage(T approvable);

    /**
     * Returns the stable key used to persist approval for this approvable object.
     */
    String getApprovalKey(T approvable);
}
