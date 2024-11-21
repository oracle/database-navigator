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

package com.dbn.connection;

import com.dbn.common.property.Property;
import com.dbn.common.property.PropertyHolderBase;

import java.util.Arrays;

public class ConnectionSelectorOptions extends PropertyHolderBase.IntStore<ConnectionSelectorOptions.Option> {

    @Override
    protected Option[] properties() {
        return Option.VALUES;
    }

    public enum Option implements Property.IntBase {
        SHOW_VIRTUAL_CONNECTIONS,
        SHOW_CREATE_CONNECTION,
        PROMPT_SCHEMA_SELECTION;

        public static final Option[] VALUES = values();

        private final IntMasks masks = new IntMasks(this);

        @Override
        public IntMasks masks() {
            return masks;
        }
    }

    public static ConnectionSelectorOptions options(Option ... options) {
        ConnectionSelectorOptions holder = new ConnectionSelectorOptions();
        Arrays.stream(options).forEach(option -> holder.set(option));
        return holder;
    }

}
