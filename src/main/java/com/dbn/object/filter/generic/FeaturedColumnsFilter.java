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

package com.dbn.object.filter.generic;

import com.dbn.common.filter.Filter;
import com.dbn.object.DBColumn;
import org.jetbrains.annotations.Nullable;


public class FeaturedColumnsFilter implements Filter<DBColumn> {
    public static final FeaturedColumnsFilter INSTANCE = new FeaturedColumnsFilter();

    private FeaturedColumnsFilter() {}

    @Nullable
    public static Filter<DBColumn> get(boolean pseudo, boolean audit) {
        if (pseudo && audit) return INSTANCE;
        if (pseudo) return NonPseudoColumnsFilter.INSTANCE;
        if (audit) return NonAuditColumnsFilter.INSTANCE;
        return null;
    }

    @Override
    public boolean accepts(DBColumn object) {
        return NonPseudoColumnsFilter.INSTANCE.accepts(object)
                && NonAuditColumnsFilter.INSTANCE.accepts(object);
    }
}
