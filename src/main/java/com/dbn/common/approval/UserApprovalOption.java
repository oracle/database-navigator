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

import com.intellij.openapi.util.NlsContexts.Button;

import java.time.Duration;

public final class UserApprovalOption {
    private final @Button String name;
    private final UserApprovalType type;
    private final Duration rejectionCooldown;

    private UserApprovalOption(@Button String name, UserApprovalType type, Duration rejectionCooldown) {
        this.name = name;
        this.type = type;
        this.rejectionCooldown = rejectionCooldown;
    }

    public static UserApprovalOption none(@Button String name) {
        return none(name, Duration.ofSeconds(10));
    }

    public static UserApprovalOption none(@Button String name, Duration rejectionCooldown) {
        return new UserApprovalOption(name, UserApprovalType.NONE, rejectionCooldown);
    }

    public static UserApprovalOption noneWithoutCooldown(@Button String name) {
        return none(name, null);
    }

    public static UserApprovalOption one(@Button String name) {
        return new UserApprovalOption(name, UserApprovalType.ONE, null);
    }

    public static UserApprovalOption all(@Button String name) {
        return new UserApprovalOption(name, UserApprovalType.ALL, null);
    }

    public @Button String name() {
        return name;
    }

    public UserApprovalType type() {
        return type;
    }

    public Duration rejectionCooldown() {
        return rejectionCooldown;
    }
}
