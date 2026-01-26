/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.execution.java.wrapper.support;

import com.dbn.object.DBJavaClass;
import com.dbn.object.lookup.DBJavaNameCache;
import com.dbn.object.lookup.DBObjectRef;
import lombok.Getter;

@Getter
public class WrapperSupportEntity {
    private final DBObjectRef<DBJavaClass> javaClass;
    private final short arrayDepth;
    private final boolean input;

    public WrapperSupportEntity(DBObjectRef<DBJavaClass> javaClass, short arrayDepth, boolean input) {
        this.javaClass = javaClass;
        this.arrayDepth = arrayDepth;
        this.input = input;
    }

    public String getJavaClassName() {
        return DBJavaNameCache.getCanonicalName(javaClass);
    }
}
