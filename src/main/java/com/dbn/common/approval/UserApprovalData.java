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

import static com.dbn.common.dispose.Failsafe.nn;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;

final class UserApprovalData implements PersistentStateElement {
    private String key;
    private boolean approved;
    private boolean temporary;
    private boolean pending;
    private long rejectionExpirationTimestamp;
    private @Nullable String signature;

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
        return approved;
    }

    synchronized void setApproved(boolean approved) {
        this.approved = approved;
    }

    synchronized boolean isPending() {
        return pending;
    }

    synchronized void setPending(boolean pending) {
        this.pending = pending;
    }

    synchronized boolean consumeTemporary() {
        if (!temporary) return false;

        temporary = false;
        return true;
    }

    synchronized void setTemporary(boolean temporary) {
        this.temporary = temporary;
    }

    synchronized void reject(@Nullable Duration cooldown) {
        if (cooldown == null || cooldown.isZero() || cooldown.isNegative()) {
            rejectionExpirationTimestamp = 0;
            return;
        }

        rejectionExpirationTimestamp = System.currentTimeMillis() + cooldown.toMillis();
    }

    synchronized void clearRejection() {
        rejectionExpirationTimestamp = 0;
    }

    synchronized boolean isRejected() {
        return rejectionExpirationTimestamp > System.currentTimeMillis();
    }

    synchronized void setSignature(@Nullable String signature) {
        this.signature = signature;
    }

    synchronized boolean updateSignatureRequiresApprovalClear(@Nullable String signature) {
        if (signature == null) return false;

        String previousSignature = this.signature;
        if (Objects.equals(previousSignature, signature)) return false;

        this.signature = signature;
        return previousSignature != null || approved;
    }

    synchronized void clearApproval() {
        approved = false;
        temporary = false;
        pending = false;
        rejectionExpirationTimestamp = 0;
    }

    synchronized boolean isEmpty() {
        return !approved && !temporary && !pending && rejectionExpirationTimestamp == 0 && signature == null;
    }

    synchronized boolean isPersistent() {
        return approved;
    }

    @Override
    public synchronized void readState(Element element) {
        key = stringAttribute(element, "key");
        signature = stringAttribute(element, "signature");
        approved = true;
        temporary = false;
        pending = false;
        rejectionExpirationTimestamp = 0;
    }

    @Override
    public synchronized void writeState(Element element) {
        setStringAttribute(element, "key", key);
        if (signature != null) setStringAttribute(element, "signature", signature);
    }
}
