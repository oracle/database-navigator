/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.dbn.liquibase.execution;

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
