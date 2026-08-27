/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 */

package com.dbn.liquibase.workflow;

import static com.dbn.nls.NlsResources.txt;

/** Groups Liquibase workflows by their primary user intent. */
public enum LiquibaseWorkflowCategory {
    PREPARE,
    REVIEW,
    DEPLOY,
    RECOVER;

    public String getName() {
        return txt("app.liquibase.const.WorkflowCategory_" + name());
    }
}
