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

package com.dbn.common.state;

import com.dbn.common.approval.UserApprovable;
import com.dbn.common.approval.UserApprovalAction;

import java.util.Set;

import static com.dbn.common.approval.UserApprovalAction.STATE_ENCRYPTION_CHANGE;

public final class StateEncryptionApproval implements UserApprovable {
    static final StateEncryptionApproval INSTANCE = new StateEncryptionApproval();

    private StateEncryptionApproval() {}

    @Override
    public Set<UserApprovalAction> getApprovalActions() {
        return Set.of(STATE_ENCRYPTION_CHANGE);
    }
}
