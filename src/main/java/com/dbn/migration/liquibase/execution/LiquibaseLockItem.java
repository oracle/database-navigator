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

package com.dbn.migration.liquibase.execution;

import liquibase.lockservice.DatabaseChangeLogLock;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

/** Liquibase changelog lock reported by a lock-listing operation. */
@Getter
public class LiquibaseLockItem extends LiquibaseExecutionItem {
    private final int id;
    private final Date lockGranted;
    private final String lockedBy;

    public LiquibaseLockItem(@NotNull DatabaseChangeLogLock lock) {
        super(LiquibaseExecutionItemStatus.DISCOVERED, null);
        this.id = lock.getId();
        this.lockGranted = lock.getLockGranted();
        this.lockedBy = lock.getLockedBy();
    }

    @NotNull
    public String getKey() {
        return Integer.toString(id);
    }
}
