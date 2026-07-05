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

import com.dbn.common.state.PersistentStateElement;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;

import static com.dbn.common.approval.UserApprovalLifetime.NONE;
import static com.dbn.common.approval.UserApprovalLifetime.ONCE;
import static com.dbn.common.approval.UserApprovalLifetime.PERSISTENT;
import static com.dbn.common.approval.UserApprovalLifetime.SESSION;
import static com.dbn.common.dispose.Failsafe.nn;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;

final class UserApprovalData implements PersistentStateElement {
    private String key;
    private @Nullable String signature;
    private UserApprovalLifetime approvalLifetime = NONE;
    private boolean pending;
    private long rejectionExpiry;

    UserApprovalData(@NotNull String key) {
        this.key = key;
    }

    UserApprovalData(@NotNull Element element) {
        key = nn(stringAttribute(element, "key"));
        readState(element);
    }

    @NotNull
    synchronized String getKey() {
        return key;
    }

    synchronized boolean isApproved() {
        return approvalLifetime != NONE;
    }

    synchronized void setApprovalLifetime(UserApprovalLifetime approvalLifetime) {
        this.approvalLifetime = nn(approvalLifetime);
    }

    synchronized boolean isPending() {
        return pending;
    }

    synchronized void setPending(boolean pending) {
        this.pending = pending;
    }

    synchronized boolean consumeOnce() {
        if (approvalLifetime != ONCE) return false;

        approvalLifetime = NONE;
        return true;
    }

    synchronized void clearTransientApproval() {
        if (approvalLifetime == ONCE || approvalLifetime == SESSION) {
            approvalLifetime = NONE;
        }
    }

    synchronized void reject(@Nullable Duration cooldown) {
        if (cooldown == null || cooldown.isZero() || cooldown.isNegative()) {
            rejectionExpiry = 0;
            return;
        }

        rejectionExpiry = System.currentTimeMillis() + cooldown.toMillis();
    }

    synchronized void clearRejection() {
        rejectionExpiry = 0;
    }

    synchronized boolean isRejected() {
        return rejectionExpiry > System.currentTimeMillis();
    }

    synchronized void setSignature(@Nullable String signature) {
        this.signature = signature;
    }

    /**
     * Updates the approval signature and returns true when an existing approval
     * must be cleared because the signature changed.
     */
    synchronized boolean updateSignature(@Nullable String signature) {
        if (signature == null) return false;

        String previousSignature = this.signature;
        if (Objects.equals(previousSignature, signature)) return false;

        this.signature = signature;
        return previousSignature != null || isApproved();
    }

    synchronized void clearApproval() {
        approvalLifetime = NONE;
        rejectionExpiry = 0;
        pending = false;
    }

    synchronized boolean isEmpty() {
        return approvalLifetime == NONE && !pending && rejectionExpiry == 0 && signature == null;
    }

    synchronized boolean isPersistent() {
        return approvalLifetime == PERSISTENT;
    }

    @Override
    public synchronized void readState(Element element) {
        key = stringAttribute(element, "key");
        signature = stringAttribute(element, "signature");
        approvalLifetime = PERSISTENT;
        pending = false;
        rejectionExpiry = 0;
    }

    @Override
    public synchronized void writeState(Element element) {
        setStringAttribute(element, "key", key);
        if (signature != null) setStringAttribute(element, "signature", signature);
    }
}
