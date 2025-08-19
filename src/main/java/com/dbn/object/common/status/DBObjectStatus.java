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
import lombok.Getter;

@Getter
public enum DBObjectStatus implements Property.ShortBase {
    PRESENT(Propagation.NONE, true),
    ENABLED(Propagation.NONE, true),
    EDITABLE(Propagation.NONE, false),
    VALID(Propagation.ALL, true),
    DEBUG(Propagation.ANY, false),
    COMPILING(Propagation.ANY, false),
    INITIALIZING(Propagation.ANY, false);

    public static final DBObjectStatus[] VALUES = values();

    private final ShortMasks masks = new ShortMasks(this);
    private final Propagation propagation;
    private final boolean defaultValue;

    DBObjectStatus(Propagation propagation, boolean defaultValue) {
        this.propagation = propagation;
        this.defaultValue = defaultValue;
    }

    @Override
    public ShortMasks masks() {
        return masks;
    }

    public boolean getDefaultValue() {
        return defaultValue;
    }

    /**
     * Status propagation policy from sub-content-type to main-content-type
     */
    public enum Propagation {
        NONE, // status does not propagate up
        ANY,  // status propagates up if any of the sub-contents matches the given status
        ALL   // status propagates up if all sub-contents match the given status
    }
}
