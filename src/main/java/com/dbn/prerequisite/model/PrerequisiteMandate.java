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

package com.dbn.prerequisite.model;

import com.dbn.common.util.Lists;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.Nls;

import java.util.List;

/**
 * Represents a mandate defining a prerequisite requirement in a given context.
 * This class encapsulates the type of prerequisite and a descriptive reason for the mandate.
 */

@Getter
@EqualsAndHashCode
public class PrerequisiteMandate {
    private final PrerequisiteType type;
    @Nls
    private transient final String reason; // transient - do not include in equals and hash (prevent prerequisite duplicates with different reasons)

    public PrerequisiteMandate(PrerequisiteType type, @Nls String reason) {
        this.type = type;
        this.reason = reason;
    }

    public static List<PrerequisiteType> asPrerequisiteTypes(List<PrerequisiteMandate> mandates) {
        return Lists.convert(mandates, m -> m.getType());
    }
}
