/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 */

package com.dbn.liquibase.execution;

import com.dbn.common.constant.Constant;
import com.dbn.common.ui.Presentable;

import static com.dbn.nls.NlsResources.txt;

/** Selects the criterion used to limit the changesets applied by an update. */
public enum LiquibaseUpdateType implements Constant<LiquibaseUpdateType>, Presentable {
    ALL,
    COUNT,
    TAG;

    public String getName() {
        return txt("app.liquibase.const.UpdateType_" + name());
    }
}
