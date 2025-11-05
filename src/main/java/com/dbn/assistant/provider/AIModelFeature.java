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

package com.dbn.assistant.provider;

import com.dbn.common.constant.Constant;
import com.dbn.common.constant.Constants;
import com.dbn.common.property.Property;
import lombok.Getter;

@Getter
public enum AIModelFeature implements Property.ShortBase, Constant<AIModelFeature> {
    TOOLS,
    MEMORY,
    TEMPERATURE,
    INSTRUCTIONS;

    public static final AIModelFeature[] VALUES = values();

    private final ShortMasks masks = new ShortMasks(this);

    public static AIModelFeature get(String id) {
        return Constants.get(VALUES, id);
    }

    @Override
    public ShortMasks masks() {
        return masks;
    }
}
