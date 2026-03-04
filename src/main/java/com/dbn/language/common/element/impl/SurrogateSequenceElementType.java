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
import com.dbn.language.common.element.parser.impl.SurrogateSequenceElementTypeParser;
import org.jetbrains.annotations.NotNull;

public final class SurrogateSequenceElementType extends SequenceElementType {
    public SurrogateSequenceElementType(ElementTypeBundle bundle, ElementTypeBase parent, String id) {
        super(bundle, parent, id);
    }

    public ElementTypeBase getLeadingElementType() {
        return children[0].elementType;
    }

    public ElementTypeBase getMainElementType() {
        return children[1].elementType;
    }

    @NotNull
    @Override
    public SurrogateSequenceElementTypeParser createParser() {
        return new SurrogateSequenceElementTypeParser(this);
    }

    @NotNull
    @Override
    public String getName() {
        return "one-of-sequence (" + getId() + ")";
    }
}
