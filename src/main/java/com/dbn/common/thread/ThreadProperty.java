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

package com.dbn.common.thread;

import com.dbn.common.property.Property;

public enum ThreadProperty implements Property.IntBase {
    CODE_COMPLETION (true),
    CODE_ANNOTATING (true),
    EDITOR_LOAD(true),
    COMPONENT_STATE(true),
    DATABASE_INTERFACE(true),
    WORKSPACE_RESTORE(true),
    DEBUGGER_NAVIGATION(true),

    TIMEOUT    (true),
    PROMPTED   (true),
    CANCELABLE (true),

    BACKGROUND (false),
    PROGRESS   (false),
    MODAL      (false),
    DISPOSER   (false)

;
    public static final ThreadProperty[] VALUES = values();

    private final IntMasks masks = new IntMasks(this);
    private final boolean propagatable;

    ThreadProperty(boolean propagatable) {
        this.propagatable = propagatable;
    }

    @Override
    public IntMasks masks() {
        return masks;
    }

    public boolean propagatable() {
        return propagatable;
    }
}
