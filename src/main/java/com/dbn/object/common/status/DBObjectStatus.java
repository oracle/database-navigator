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

package com.dbn.object.common.status;

import com.dbn.common.property.Property;

public enum DBObjectStatus implements Property.IntBase {
    PRESENT(false, true),
    ENABLED(true, true),
    EDITABLE(false, false),
    VALID(true, true),
    DEBUG(true, true),
    COMPILING(false, false);

    public static final DBObjectStatus[] VALUES = values();

    private final IntMasks masks = new IntMasks(this);
    private final boolean propagable;
    private final boolean defaultValue;

    DBObjectStatus(boolean propagable, boolean defaultValue) {
        this.propagable = propagable;
        this.defaultValue = defaultValue;
    }

    @Override
    public IntMasks masks() {
        return masks;
    }

    public boolean isPropagable() {
        return propagable;
    }

    public boolean getDefaultValue() {
        return defaultValue;
    }
}
