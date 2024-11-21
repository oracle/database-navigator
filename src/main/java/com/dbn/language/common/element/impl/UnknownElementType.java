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

package com.dbn.language.common.element.impl;

import com.dbn.language.common.element.ElementTypeBundle;
import com.dbn.language.common.element.cache.ElementTypeLookupCache;
import com.dbn.language.common.element.cache.VoidElementTypeLookupCache;

public final class UnknownElementType extends BasicElementType{
    public UnknownElementType(ElementTypeBundle bundle) {
        super(bundle, "UNKNOWN", "Unidentified element type.");
    }

    @Override
    public ElementTypeLookupCache createLookupCache() {
        return new VoidElementTypeLookupCache<>(this);
    }
}
