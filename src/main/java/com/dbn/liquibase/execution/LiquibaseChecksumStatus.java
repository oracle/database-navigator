/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.dbn.liquibase.execution;

import static com.dbn.nls.NlsResources.txt;

/** Comparison outcome between a calculated and database-stored Liquibase checksum. */
public enum LiquibaseChecksumStatus {
    MATCHING,
    CHANGED,
    NOT_EXECUTED,
    NOT_RECORDED;

    public String getName() {
        return txt("cfg.liquibase.const.ChecksumStatus_" + name());
    }
}
