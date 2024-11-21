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

package com.dbn.common.dispose;

import com.dbn.common.property.Property;
import com.dbn.common.property.PropertyHolderBase;

public enum DisposeInstruction implements Property.IntBase {
    REGISTERED,
    BACKGROUND,
    CLEAR,
    NULLIFY;

    public static final DisposeInstruction[] VALUES = values();

    private final IntMasks masks = new IntMasks(this);

    @Override
    public IntMasks masks() {
        return masks;
    }

    public static class Bundle extends PropertyHolderBase.IntStore<DisposeInstruction> {
        public Bundle(DisposeInstruction... instructions) {
            for (DisposeInstruction instruction : instructions) {
                set(instruction, true);
            }
        }

        @Override
        protected DisposeInstruction[] properties() {
            return VALUES;
        }
    }

    public static Bundle from(DisposeInstruction ... instructions) {
        return new Bundle(instructions);
    }
}
