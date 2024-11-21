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

package com.dbn.connection.jdbc;

import com.dbn.common.property.Property;
import lombok.Getter;

@Getter
public enum ResourceStatus implements Property.IntBase {
    ACTIVE,
    VALID,
    CLOSED,
    CANCELLED,
    RESERVED,
    READ_ONLY,
    AUTO_COMMIT,

    EVALUATING_VALID(true),
    EVALUATING_CLOSED(true),
    EVALUATING_CANCELLED(true),
    EVALUATING_READ_ONLY(true),
    EVALUATING_AUTO_COMMIT(true),

    CHANGING_VALID(true),
    CHANGING_CLOSED(true),
    CHANGING_CANCELLED(true),
    CHANGING_READ_ONLY(true),
    CHANGING_AUTO_COMMIT(true),

    // transient statuses
    CLOSING(true),
    CANCELLING(true),
    COMMITTING(true),
    ROLLING_BACK(true),

    CREATING_SAVEPOINT(true),
    COMMITTING_SAVEPOINT(true),
    ROLLING_BACK_SAVEPOINT(true),
    RELEASING_SAVEPOINT(true),

    RESOLVING_TRANSACTION(true);

    public static final ResourceStatus[] VALUES = values();

    private final boolean transitory;
    private final IntMasks masks = new IntMasks(this);

    ResourceStatus() {
        this(false);
    }

    ResourceStatus(boolean transitory) {
        this.transitory = transitory;
    }

    @Override
    public IntMasks masks() {
        return masks;
    }
}
