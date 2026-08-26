/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 */

package com.dbn.liquibase.operation;

import static com.dbn.nls.NlsResources.txt;

/** Groups Liquibase operations by their primary user intent. */
public enum LiquibaseOperationCategory {
    CHANGELOG,
    DEPLOY,
    INSPECT,
    MAINTENANCE,
    PREVIEW_SQL,
    MORE;

    public String getName() {
        return txt("app.liquibase.const.OperationCategory_" + name());
    }
}
